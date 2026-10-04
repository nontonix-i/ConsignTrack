package com.example.ui.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Customer
import com.example.data.repository.ConsignmentRepository
import com.example.domain.model.CustomerPerformance
import com.example.domain.model.CustomerWithStatus
import com.example.util.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class RouteFilter {
    ALL,
    UNVISITED,
    VISITED
}

enum class CustomerSortOption(val label: String, val shortLabel: String) {
    NEAREST("Lokasi Terdekat (GPS)", "Terdekat"),
    ROUTE_ORDER("Urutan Rute (#1, #2...)", "Rute"),
    NAME("Nama Toko (A - Z)", "Nama A-Z"),
    STOCK_HIGHEST("Stok Terbanyak", "Stok"),
    UNVISITED_FIRST("Belum Dikunjungi Dulu", "Belum Visit")
}

data class DashboardUiState(
    val allCustomers: List<CustomerWithStatus> = emptyList(),
    val filteredCustomers: List<CustomerWithStatus> = emptyList(),
    val todayDayName: String = DashboardViewModel.getTodayDayName(),
    val selectedDay: String = DashboardViewModel.getTodayDayName(),
    val filter: RouteFilter = RouteFilter.ALL,
    val sortOption: CustomerSortOption = CustomerSortOption.NEAREST,
    val userLocation: Location? = null,
    val isLocating: Boolean = false,
    val dayCounts: Map<String, Int> = emptyMap(),
    val totalPiecesConsigned: Int = 0,
    val todayTotalSoldAmount: Double = 0.0,
    val isLoading: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = ConsignmentRepository(db)

    private val _selectedDay = MutableStateFlow(getTodayDayName())
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private val _filter = MutableStateFlow(RouteFilter.ALL)
    val filter: StateFlow<RouteFilter> = _filter.asStateFlow()

    private val _sortOption = MutableStateFlow(CustomerSortOption.NEAREST)
    val sortOption: StateFlow<CustomerSortOption> = _sortOption.asStateFlow()

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    init {
        // Otomatis mulai pelacakan lokasi real-time jika izin tersedia
        startLocationTracking()
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.customersWithStatus,
        _selectedDay,
        _filter,
        _sortOption,
        _userLocation,
        _isLocating,
        repository.totalPiecesConsigned,
        repository.todayTotalSoldAmount
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val rawCustomers = args[0] as List<CustomerWithStatus>
        val day = args[1] as String
        val filter = args[2] as RouteFilter
        val sort = args[3] as CustomerSortOption
        val userLoc = args[4] as? Location
        val locating = args[5] as Boolean
        val totalPieces = args[6] as Int
        val todayAmount = args[7] as Double

        val today = getTodayDayName()
        val counts = rawCustomers.groupBy { it.customer.route_day }.mapValues { it.value.size }

        // Hitung jarak real-time dari posisi pengguna saat ini jika ada koordinat
        val customersWithDistance = rawCustomers.map { item ->
            val cust = item.customer
            val dist = if (userLoc != null && cust.latitude != null && cust.longitude != null) {
                LocationHelper.calculateDistanceMeters(
                    startLat = userLoc.latitude,
                    startLng = userLoc.longitude,
                    endLat = cust.latitude,
                    endLng = cust.longitude
                )
            } else {
                null
            }
            item.copy(distanceMeters = dist)
        }

        // Filter Hari
        val dayFiltered = if (day == "Semua") {
            customersWithDistance
        } else {
            customersWithDistance.filter { it.customer.route_day.equals(day, ignoreCase = true) }
        }

        // Filter Status Kunjungan
        val statusFiltered = when (filter) {
            RouteFilter.ALL -> dayFiltered
            RouteFilter.UNVISITED -> dayFiltered.filter { !it.hasVisitedToday }
            RouteFilter.VISITED -> dayFiltered.filter { it.hasVisitedToday }
        }

        // Sorting Fleksibel (Terdekat, Rute #, Nama A-Z, Stok Terbanyak, Belum Dikunjungi)
        val sortedList = when (sort) {
            CustomerSortOption.NEAREST -> statusFiltered.sortedWith { a, b ->
                val distA = a.distanceMeters ?: Float.MAX_VALUE
                val distB = b.distanceMeters ?: Float.MAX_VALUE
                if (distA != distB) {
                    distA.compareTo(distB)
                } else {
                    a.customer.route_order.compareTo(b.customer.route_order)
                }
            }
            CustomerSortOption.ROUTE_ORDER -> statusFiltered.sortedBy { it.customer.route_order }
            CustomerSortOption.NAME -> statusFiltered.sortedBy { it.customer.name.lowercase(Locale.ROOT) }
            CustomerSortOption.STOCK_HIGHEST -> statusFiltered.sortedByDescending { it.totalActiveStock }
            CustomerSortOption.UNVISITED_FIRST -> statusFiltered.sortedWith(
                compareBy({ it.hasVisitedToday }, { it.customer.route_order })
            )
        }

        DashboardUiState(
            allCustomers = customersWithDistance,
            filteredCustomers = sortedList,
            todayDayName = today,
            selectedDay = day,
            filter = filter,
            sortOption = sort,
            userLocation = userLoc,
            isLocating = locating,
            dayCounts = counts,
            totalPiecesConsigned = totalPieces,
            todayTotalSoldAmount = todayAmount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(
            todayDayName = getTodayDayName(),
            selectedDay = getTodayDayName(),
            isLoading = true
        )
    )

    fun startLocationTracking() {
        val app = getApplication<Application>()
        if (!LocationHelper.hasLocationPermission(app)) return

        viewModelScope.launch {
            _isLocating.value = true
            // Ambil fast location dulu
            val initial = LocationHelper.getFreshLocation(app, timeoutMs = 4000L)
            if (initial != null) {
                _userLocation.value = initial
            }
            _isLocating.value = false

            // Dapatkan continuous flow pembaruan real-time
            LocationHelper.getLocationFlow(app).collect { loc ->
                if (loc != null) {
                    _userLocation.value = loc
                }
            }
        }
    }

    fun refreshLocation() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _isLocating.value = true
            val loc = LocationHelper.getFreshLocation(app, timeoutMs = 6000L)
            if (loc != null) {
                _userLocation.value = loc
            }
            _isLocating.value = false
        }
    }

    fun setSortOption(option: CustomerSortOption) {
        _sortOption.value = option
    }

    fun setSelectedDay(day: String) {
        _selectedDay.value = day
    }

    fun selectToday() {
        _selectedDay.value = getTodayDayName()
    }

    fun setFilter(filter: RouteFilter) {
        _filter.value = filter
    }

    fun addCustomer(
        name: String,
        address: String,
        phone: String,
        routeDay: String,
        routeOrder: Int,
        photoUri: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            repository.saveCustomer(
                Customer(
                    name = name,
                    address = address,
                    phone = phone,
                    route_day = routeDay,
                    route_order = routeOrder,
                    photo_uri = photoUri,
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
        }
    }

    fun updateCustomerPhoto(customerId: Long, photoUri: String?) {
        viewModelScope.launch {
            repository.updateCustomerPhoto(customerId, photoUri)
        }
    }

    fun updateCustomerLocation(customerId: Long, latitude: Double?, longitude: Double?) {
        viewModelScope.launch {
            repository.updateCustomerLocation(customerId, latitude, longitude)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    private val _selectedPerformance = MutableStateFlow<CustomerPerformance?>(null)
    val selectedPerformance: StateFlow<CustomerPerformance?> = _selectedPerformance.asStateFlow()

    private val _selectedPerformanceCustomerName = MutableStateFlow<String?>(null)
    val selectedPerformanceCustomerName: StateFlow<String?> = _selectedPerformanceCustomerName.asStateFlow()

    private var perfJob: Job? = null

    fun openCustomerPerformance(customer: Customer) {
        _selectedPerformanceCustomerName.value = customer.name
        perfJob?.cancel()
        perfJob = viewModelScope.launch {
            repository.getCustomerPerformance(customer.id).collect { perf ->
                _selectedPerformance.value = perf
            }
        }
    }

    fun closeCustomerPerformance() {
        perfJob?.cancel()
        _selectedPerformanceCustomerName.value = null
        _selectedPerformance.value = null
    }

    companion object {
        fun getTodayDayName(): String {
            val cal = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
            return when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Senin"
                Calendar.TUESDAY -> "Selasa"
                Calendar.WEDNESDAY -> "Rabu"
                Calendar.THURSDAY -> "Kamis"
                Calendar.FRIDAY -> "Jumat"
                Calendar.SATURDAY -> "Sabtu"
                Calendar.SUNDAY -> "Minggu"
                else -> "Senin"
            }
        }
    }
}
