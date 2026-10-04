package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Customer
import com.example.data.local.entity.Product
import com.example.data.repository.ConsignmentRepository
import com.example.domain.model.CustomerPerformance
import com.example.domain.model.ReceiptData
import com.example.domain.model.ReconciliationItem
import com.example.util.thermal.BluetoothPrinterManager
import com.example.util.thermal.PairedPrinter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReconciliationUiState(
    val customer: Customer? = null,
    val items: List<ReconciliationItem> = emptyList(),
    val catalogProducts: List<Product> = emptyList(),
    val performance: CustomerPerformance? = null,
    val showAddProductDialog: Boolean = false,
    val showPerformanceDialog: Boolean = false,
    val amountPaid: Double = 0.0,
    val notes: String = "",
    val visitPhotoUri: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val generatedReceipt: ReceiptData? = null,
    val showReceiptModal: Boolean = false,
    val pairedPrinters: List<PairedPrinter> = emptyList(),
    val selectedPrinterAddress: String? = null,
    val isPrinting: Boolean = false,
    val printMessage: String? = null
) {
    val totalSoldQuantity: Int
        get() = items.sumOf { it.soldQuantity }

    val totalSoldAmount: Double
        get() = items.sumOf { it.subtotal }

    val totalReturnedQuantity: Int
        get() = items.sumOf { it.returnedQuantity }

    val totalAddedQuantity: Int
        get() = items.sumOf { it.addedQuantity }

    val totalAddedPacks: Int
        get() = items.sumOf { it.addedPacks }

    val changeOrDebt: Double
        get() = amountPaid - totalSoldAmount
}

class ReconciliationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ConsignmentRepository(db)
    private val printerManager = BluetoothPrinterManager(application)

    private val _uiState = MutableStateFlow(ReconciliationUiState())
    val uiState: StateFlow<ReconciliationUiState> = _uiState.asStateFlow()

    private var customerPerformanceJob: Job? = null
    private var catalogJob: Job? = null

    init {
        catalogJob = viewModelScope.launch {
            repository.allProducts.collect { prods ->
                _uiState.update { it.copy(catalogProducts = prods) }
            }
        }
    }

    fun loadCustomer(customerId: Long) {
        customerPerformanceJob?.cancel()
        customerPerformanceJob = viewModelScope.launch {
            repository.getCustomerPerformance(customerId).collect { perf ->
                _uiState.update { it.copy(performance = perf) }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, saveSuccess = false, generatedReceipt = null) }
            val customer = repository.getCustomerById(customerId)
            val items = repository.prepareReconciliationItems(customerId)
            val printers = printerManager.getPairedPrinters()
            val defaultPrinter = printers.firstOrNull()?.address

            _uiState.update {
                it.copy(
                    customer = customer,
                    items = items,
                    amountPaid = 0.0,
                    notes = "",
                    visitPhotoUri = null,
                    isLoading = false,
                    pairedPrinters = printers,
                    selectedPrinterAddress = defaultPrinter
                )
            }
        }
    }

    fun addProductToOutlet(product: Product, initialPacks: Int, customPricePack: Double? = null) {
        val custId = _uiState.value.customer?.id
        _uiState.update { state ->
            if (state.items.any { it.product.id == product.id }) {
                state.copy(showAddProductDialog = false)
            } else {
                val newItem = ReconciliationItem(
                    product = product,
                    previousStock = 0,
                    remainingStock = 0,
                    isAutoSwapReturned = false,
                    manualReturnedQuantity = 0,
                    addedPacks = maxOf(1, initialPacks),
                    addedPiecesExtra = 0,
                    customPricePack = customPricePack?.takeIf { it > 0.0 }
                )
                state.copy(items = state.items + newItem, showAddProductDialog = false)
            }
        }
        if (custId != null && customPricePack != null && customPricePack > 0.0) {
            viewModelScope.launch {
                repository.updateProductCustomPriceForCustomer(custId, product.id, customPricePack)
            }
        }
    }

    fun updateCustomPricePack(productId: Long, customPricePack: Double?) {
        val custId = _uiState.value.customer?.id
        val normalizedPrice = customPricePack?.takeIf { it > 0.0 }
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.product.id == productId) {
                    item.copy(customPricePack = normalizedPrice)
                } else item
            }
            state.copy(items = updated)
        }
        if (custId != null) {
            viewModelScope.launch {
                repository.updateProductCustomPriceForCustomer(custId, productId, normalizedPrice)
            }
        }
    }

    fun updatePreviousStock(productId: Long, previousStockPcs: Int) {
        val safePrev = maxOf(0, previousStockPcs)
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.product.id == productId) {
                    val adjustedRemaining = minOf(item.remainingStock, safePrev)
                    item.copy(previousStock = safePrev, remainingStock = adjustedRemaining)
                } else item
            }
            state.copy(items = updated)
        }
    }

    fun updateCustomerInfo(updatedCustomer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(updatedCustomer)
            _uiState.update { it.copy(customer = updatedCustomer) }
        }
    }

    fun removeProductFromOutlet(productId: Long) {
        val custId = _uiState.value.customer?.id
        _uiState.update { state ->
            state.copy(items = state.items.filterNot { it.product.id == productId })
        }
        if (custId != null) {
            viewModelScope.launch {
                repository.deleteStockForCustomerAndProduct(custId, productId)
            }
        }
    }

    fun setShowAddProductDialog(show: Boolean) {
        _uiState.update { it.copy(showAddProductDialog = show) }
    }

    fun setShowPerformanceDialog(show: Boolean) {
        _uiState.update { it.copy(showPerformanceDialog = show) }
    }

    fun updateVisitPhoto(uri: String?) {
        _uiState.update { it.copy(visitPhotoUri = uri) }
    }

    fun updateRemainingStock(productId: Long, remainingPcs: Int) {
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.product.id == productId) {
                    val safeRemaining = maxOf(0, remainingPcs)
                    item.copy(remainingStock = safeRemaining)
                } else item
            }
            state.copy(items = updated)
        }
    }

    fun updateAutoSwapReturned(productId: Long, isAutoSwap: Boolean) {
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.product.id == productId) {
                    item.copy(isAutoSwapReturned = isAutoSwap)
                } else item
            }
            state.copy(items = updated)
        }
    }

    fun updateAddedPacks(productId: Long, packs: Int) {
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.product.id == productId) {
                    item.copy(addedPacks = maxOf(0, packs))
                } else item
            }
            state.copy(items = updated)
        }
    }

    fun updateAddedPiecesExtra(productId: Long, extraPcs: Int) {
        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.product.id == productId) {
                    item.copy(addedPiecesExtra = maxOf(0, extraPcs))
                } else item
            }
            state.copy(items = updated)
        }
    }

    fun updateAmountPaid(amount: Double) {
        _uiState.update { it.copy(amountPaid = maxOf(0.0, amount)) }
    }

    fun setAmountPaidExact() {
        val total = _uiState.value.totalSoldAmount
        _uiState.update { it.copy(amountPaid = total) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun selectPrinter(address: String) {
        _uiState.update { it.copy(selectedPrinterAddress = address) }
    }

    fun refreshPairedPrinters() {
        val printers = printerManager.getPairedPrinters()
        _uiState.update {
            it.copy(
                pairedPrinters = printers,
                selectedPrinterAddress = it.selectedPrinterAddress ?: printers.firstOrNull()?.address
            )
        }
    }

    fun saveTransaction(openReceiptAfterSave: Boolean = true, onComplete: () -> Unit = {}) {
        val state = _uiState.value
        val custId = state.customer?.id ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val prefs = getApplication<Application>().getSharedPreferences("consigntrack_settings", android.content.Context.MODE_PRIVATE)
                val bizName = prefs.getString("business_name", "CONSIGNTRACK DISTRIBUSI") ?: "CONSIGNTRACK DISTRIBUSI"
                val bizAddr = prefs.getString("business_address", "Sentra Makanan Ringan") ?: "Sentra Makanan Ringan"
                val bizPhone = prefs.getString("business_phone", "0812-9988-7766") ?: "0812-9988-7766"

                val receipt = repository.saveReconciliation(
                    customerId = custId,
                    items = state.items,
                    amountPaid = state.amountPaid,
                    notes = state.notes.ifBlank { null },
                    photoUri = state.visitPhotoUri,
                    businessName = bizName,
                    businessAddress = bizAddr,
                    businessPhone = bizPhone
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        generatedReceipt = receipt,
                        showReceiptModal = openReceiptAfterSave
                    )
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        printMessage = "Gagal menyimpan: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun showReceiptModal() {
        _uiState.update { it.copy(showReceiptModal = true) }
    }

    fun dismissReceiptModal() {
        _uiState.update { it.copy(showReceiptModal = false, printMessage = null) }
    }

    fun printCurrentReceipt(is80mm: Boolean) {
        val receipt = _uiState.value.generatedReceipt ?: return
        val address = _uiState.value.selectedPrinterAddress

        if (address.isNullOrBlank()) {
            _uiState.update { it.copy(printMessage = "Pilih printer Bluetooth terlebih dahulu") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true, printMessage = "Menghubungkan ke printer...") }
            val result = printerManager.printReceipt(address, receipt, is80mm)
            _uiState.update {
                it.copy(
                    isPrinting = false,
                    printMessage = if (result.isSuccess) "Struk berhasil dicetak!" else "Gagal cetak: ${result.exceptionOrNull()?.localizedMessage}"
                )
            }
        }
    }
}
