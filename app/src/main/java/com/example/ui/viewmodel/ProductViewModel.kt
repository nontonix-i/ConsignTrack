package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Product
import com.example.data.repository.ConsignmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductWithStock(
    val product: Product,
    val totalConsignedStock: Int = 0,
    val profitMarginPack: Double = 0.0,
    val profitMarginPercentage: Double = 0.0
)

data class ProductUiState(
    val products: List<ProductWithStock> = emptyList(),
    val isLoading: Boolean = false
)

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ConsignmentRepository(db)

    val uiState: StateFlow<ProductUiState> = combine(
        repository.allProducts,
        db.consignmentStockDao().getAllStocks()
    ) { products, stocks ->
        val stockByProd = stocks.groupBy { it.product_id }
        val list = products.map { p ->
            val total = stockByProd[p.id]?.sumOf { it.current_quantity } ?: 0
            val profitPack = p.selling_price_pack - p.cost_price_pack
            val pct = if (p.selling_price_pack > 0) (profitPack / p.selling_price_pack) * 100.0 else 0.0
            ProductWithStock(
                product = p,
                totalConsignedStock = total,
                profitMarginPack = profitPack,
                profitMarginPercentage = pct
            )
        }
        ProductUiState(products = list, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProductUiState(isLoading = true)
    )

    fun saveProduct(
        id: Long,
        name: String,
        unitSmall: String = "Bks",
        unitBig: String = "Bks",
        piecesPerPack: Int = 1,
        sellingPricePack: Double = 16000.0,
        costPricePack: Double = 11000.0
    ) {
        val safePieces = if (piecesPerPack <= 0) 1 else piecesPerPack
        val pricePerPcs = sellingPricePack / safePieces
        val costPerPcs = costPricePack / safePieces

        viewModelScope.launch {
            repository.saveProduct(
                Product(
                    id = id,
                    name = name,
                    unit = unitSmall,
                    unit_small = unitSmall,
                    unit_big = unitBig,
                    pieces_per_pack = safePieces,
                    selling_price_pack = sellingPricePack,
                    cost_price_pack = costPricePack,
                    selling_price = pricePerPcs,
                    cost_price = costPerPcs
                )
            )
        }
    }

    /**
     * Memuat 8 varian kerupuk sesuai komposisi 60 bungkus per rute (Modal Rp 11.000, Jual Rp 16.000, Laba Rp 5.000/bks):
     * SB Pedas (15), KP Original (15), ST Original (5), DD Rambak Tidak Pedas (5),
     * AO Combine (8), JK Combine (6), BO Combine (4), MK Combine (2).
     */
    fun seedKerupukBlueprintProducts() {
        val blueprintVariants = listOf(
            "SB Pedas",
            "KP Original",
            "ST Original",
            "DD Rambak Tidak Pedas",
            "AO (Combine)",
            "JK (Combine)",
            "BO (Combine)",
            "MK (Combine)"
        )
        viewModelScope.launch {
            val existingByName = uiState.value.products.associateBy { it.product.name.lowercase() }
            blueprintVariants.forEach { variantName ->
                val existing = existingByName[variantName.lowercase()]?.product
                repository.saveProduct(
                    Product(
                        id = existing?.id ?: 0L,
                        name = variantName,
                        unit = "Bks",
                        unit_small = "Bks",
                        unit_big = "Bks",
                        pieces_per_pack = 1,
                        selling_price_pack = 16000.0,
                        cost_price_pack = 11000.0,
                        selling_price = 16000.0,
                        cost_price = 11000.0
                    )
                )
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }
}
