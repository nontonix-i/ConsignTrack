package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.ConsignmentRepository
import com.example.domain.model.ReceiptData
import com.example.domain.model.ReceiptItemData
import com.example.util.thermal.BluetoothPrinterManager
import com.example.util.thermal.EscPosHelper
import com.example.util.thermal.PairedPrinter
import com.example.util.thermal.PrintResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "USER" or "AGENT"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AppThemeMode(val labelId: String, val labelEn: String) {
    DARK("Gelap", "Dark"),
    LIGHT("Terang", "Light"),
    SYSTEM("Sistem", "System")
}

enum class AppLanguage(val code: String, val label: String) {
    ID("ID", "Bahasa Indonesia"),
    EN("EN", "English")
}

data class AnalyticsUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = "AGENT",
            text = "Halo! Saya **Asisten AI ConsignTrack**. Tanyakan performa penjualan, stok warung, atau analisis laba Anda kapan saja."
        )
    ),
    val isAnalyzing: Boolean = false,
    val currentInput: String = "",
    val pairedPrinters: List<PairedPrinter> = emptyList(),
    val selectedPrinter: String? = null,
    val isPrintingTest: Boolean = false,
    val testPrintStatus: String? = null,
    // App Settings
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ID,
    val showFloatingAiChat: Boolean = true,
    val defaultPaper80mm: Boolean = false,
    val autoSortNearestGps: Boolean = true,
    val businessName: String = "CONSIGNTRACK DISTRIBUSI",
    val businessAddress: String = "Sentra Makanan Ringan",
    val businessPhone: String = "0812-9988-7766"
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ConsignmentRepository(db)
    private val printerManager = BluetoothPrinterManager(application)
    private val prefs = application.getSharedPreferences("consigntrack_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadPrinters()
    }

    private fun loadInitialState(): AnalyticsUiState {
        val themeStr = prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val langStr = prefs.getString("language", AppLanguage.ID.name) ?: AppLanguage.ID.name
        val showFloating = prefs.getBoolean("show_floating_ai", true)
        val paper80mm = prefs.getBoolean("default_paper_80mm", false)
        val autoGps = prefs.getBoolean("auto_sort_gps", true)
        val bizName = prefs.getString("business_name", "CONSIGNTRACK DISTRIBUSI") ?: "CONSIGNTRACK DISTRIBUSI"
        val bizAddr = prefs.getString("business_address", "Sentra Makanan Ringan") ?: "Sentra Makanan Ringan"
        val bizPhone = prefs.getString("business_phone", "0812-9988-7766") ?: "0812-9988-7766"

        val themeMode = runCatching { AppThemeMode.valueOf(themeStr) }.getOrDefault(AppThemeMode.DARK)
        val language = runCatching { AppLanguage.valueOf(langStr) }.getOrDefault(AppLanguage.ID)

        val initialGreeting = if (language == AppLanguage.EN) {
            "Hello! I am your **ConsignTrack AI Assistant**. Ask me about store sales, consigned stock, or profit analysis anytime."
        } else {
            "Halo! Saya **Asisten AI ConsignTrack**. Tanyakan performa penjualan, stok warung, atau analisis laba Anda kapan saja."
        }

        return AnalyticsUiState(
            messages = listOf(ChatMessage(sender = "AGENT", text = initialGreeting)),
            themeMode = themeMode,
            language = language,
            showFloatingAiChat = showFloating,
            defaultPaper80mm = paper80mm,
            autoSortNearestGps = autoGps,
            businessName = bizName,
            businessAddress = bizAddr,
            businessPhone = bizPhone
        )
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("language", lang.name).apply()
        _uiState.update { it.copy(language = lang) }
    }

    fun setShowFloatingAiChat(show: Boolean) {
        prefs.edit().putBoolean("show_floating_ai", show).apply()
        _uiState.update { it.copy(showFloatingAiChat = show) }
    }

    fun setDefaultPaper80mm(is80mm: Boolean) {
        prefs.edit().putBoolean("default_paper_80mm", is80mm).apply()
        _uiState.update { it.copy(defaultPaper80mm = is80mm) }
    }

    fun setAutoSortNearestGps(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sort_gps", enabled).apply()
        _uiState.update { it.copy(autoSortNearestGps = enabled) }
    }

    fun updateBusinessProfile(name: String, address: String, phone: String) {
        val cleanName = name.trim().ifBlank { "CONSIGNTRACK DISTRIBUSI" }
        val cleanAddr = address.trim().ifBlank { "Sentra Makanan Ringan" }
        val cleanPhone = phone.trim().ifBlank { "0812-9988-7766" }
        prefs.edit()
            .putString("business_name", cleanName)
            .putString("business_address", cleanAddr)
            .putString("business_phone", cleanPhone)
            .apply()
        _uiState.update {
            it.copy(
                businessName = cleanName,
                businessAddress = cleanAddr,
                businessPhone = cleanPhone
            )
        }
    }

    fun clearChatHistory() {
        val greeting = if (_uiState.value.language == AppLanguage.EN) {
            "Chat history cleared. How can I help analyze your business today?"
        } else {
            "Riwayat obrolan dibersihkan. Ada data bisnis yang ingin dianalisis?"
        }
        _uiState.update {
            it.copy(
                messages = listOf(ChatMessage(sender = "AGENT", text = greeting)),
                isAnalyzing = false
            )
        }
    }

    fun loadPrinters() {
        val printers = printerManager.getPairedPrinters()
        _uiState.update {
            it.copy(
                pairedPrinters = printers,
                selectedPrinter = it.selectedPrinter ?: printers.firstOrNull()?.address
            )
        }
    }

    fun setSelectedPrinter(address: String) {
        _uiState.update { it.copy(selectedPrinter = address) }
    }

    fun updateInput(input: String) {
        _uiState.update { it.copy(currentInput = input) }
    }

    fun askQuestion(query: String) {
        if (query.isBlank()) return
        val userMsg = ChatMessage(sender = "USER", text = query)
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                isAnalyzing = true,
                currentInput = ""
            )
        }

        viewModelScope.launch {
            val answer = repository.askLocalAgent(query)
            val agentMsg = ChatMessage(sender = "AGENT", text = answer)
            _uiState.update {
                it.copy(
                    messages = it.messages + agentMsg,
                    isAnalyzing = false
                )
            }
        }
    }

    fun printTestReceipt(is80mm: Boolean = _uiState.value.defaultPaper80mm) {
        val address = _uiState.value.selectedPrinter
        if (address.isNullOrBlank()) {
            _uiState.update { it.copy(testPrintStatus = "Pilih printer Bluetooth terlebih dahulu.") }
            return
        }

        val state = _uiState.value
        val testReceipt = ReceiptData(
            headerId = 9999,
            businessName = state.businessName,
            businessSub = state.businessAddress,
            businessPhone = state.businessPhone,
            transactionDate = System.currentTimeMillis(),
            customerName = "Toko Uji Coba",
            customerAddress = "Jl. Demo Sistem No. 1",
            items = listOf(
                ReceiptItemData(
                    productName = "Kerupuk Uji Coba",
                    unit = "Bungkus",
                    prevStock = 20,
                    remStock = 5,
                    returStock = 0,
                    soldQty = 15,
                    unitPrice = 2000.0,
                    subtotal = 30000.0,
                    addedQty = 10,
                    newTotalStock = 15
                )
            ),
            totalSoldQuantity = 15,
            totalAmount = 30000.0,
            amountPaid = 30000.0,
            changeOrDebt = 0.0,
            notes = "Uji cetak berhasil. Printer siap digunakan di lapangan."
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isPrintingTest = true, testPrintStatus = "Mengirim data ke printer...") }
            val bytes = EscPosHelper.buildEscPosBytes(testReceipt, is80mm)
            val result = printerManager.printData(address, bytes)
            when (result) {
                is PrintResult.Success -> {
                    _uiState.update { it.copy(isPrintingTest = false, testPrintStatus = "Berhasil mencetak nota uji!") }
                }
                is PrintResult.Error -> {
                    _uiState.update { it.copy(isPrintingTest = false, testPrintStatus = result.message) }
                }
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(testPrintStatus = null) }
    }
}
