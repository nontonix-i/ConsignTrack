package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FinancialCategory
import com.example.data.local.entity.FinancialRecord
import com.example.data.repository.ConsignmentRepository
import com.example.domain.model.BusinessFinancialSummary
import com.example.domain.model.PersonalFinancialSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FinanceTab {
    BUSINESS,
    PERSONAL
}

data class FinancialUiState(
    val selectedTab: FinanceTab = FinanceTab.BUSINESS,
    val businessRecords: List<FinancialRecord> = emptyList(),
    val personalRecords: List<FinancialRecord> = emptyList(),
    val businessSummary: BusinessFinancialSummary = BusinessFinancialSummary(0.0, 0.0, 0.0, 0.0, 0.0),
    val personalSummary: PersonalFinancialSummary = PersonalFinancialSummary(0.0, 0.0, 0.0),
    val isLoading: Boolean = false
)

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ConsignmentRepository(db)

    private val _selectedTab = MutableStateFlow(FinanceTab.BUSINESS)
    val selectedTab: StateFlow<FinanceTab> = _selectedTab.asStateFlow()

    val uiState: StateFlow<FinancialUiState> = combine(
        _selectedTab,
        repository.businessRecords,
        repository.personalRecords,
        repository.businessSummary,
        repository.personalSummary
    ) { tab, bizRecords, persRecords, bizSummary, persSummary ->
        FinancialUiState(
            selectedTab = tab,
            businessRecords = bizRecords,
            personalRecords = persRecords,
            businessSummary = bizSummary,
            personalSummary = persSummary,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialUiState(isLoading = true)
    )

    fun selectTab(tab: FinanceTab) {
        _selectedTab.value = tab
    }

    fun addBusinessExpense(amount: Double, description: String) {
        viewModelScope.launch {
            repository.addFinancialRecord(
                FinancialRecord(
                    category = FinancialCategory.BUSINESS_EXPENSE,
                    amount = amount,
                    description = description
                )
            )
        }
    }

    fun addBusinessIncome(amount: Double, description: String) {
        viewModelScope.launch {
            repository.addFinancialRecord(
                FinancialRecord(
                    category = FinancialCategory.BUSINESS_INCOME,
                    amount = amount,
                    description = description
                )
            )
        }
    }

    fun addPersonalExpense(amount: Double, description: String) {
        viewModelScope.launch {
            repository.addFinancialRecord(
                FinancialRecord(
                    category = FinancialCategory.PERSONAL_EXPENSE,
                    amount = amount,
                    description = description
                )
            )
        }
    }

    fun addPersonalIncome(amount: Double, description: String) {
        viewModelScope.launch {
            repository.addFinancialRecord(
                FinancialRecord(
                    category = FinancialCategory.PERSONAL_INCOME,
                    amount = amount,
                    description = description
                )
            )
        }
    }

    fun deleteRecord(record: FinancialRecord) {
        viewModelScope.launch {
            repository.deleteFinancialRecord(record)
        }
    }
}
