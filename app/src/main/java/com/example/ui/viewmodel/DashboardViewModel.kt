package com.example.ui.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.Customer
import com.example.data.local.entity.Product
import com.example.data.repository.ConsignmentRepository
import com.example.domain.model.CustomerPerformance
import com.example.domain.model.CustomerWithStatus
import com.example.util.LocationHelper
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val allProducts: List<Product> = emptyList(),
    val todayDayName: String = DashboardViewModel.getTodayDayName(),
    val selectedDay: String = DashboardViewModel.getTodayDayName(),
    val filter: RouteFilter = RouteFilter.ALL,
    val sortOption: CustomerSortOption = CustomerSortOption.NEAREST,
    val userLocation: Location? = null,
    val isLocating: Boolean = false,
    val isOnline: Boolean = true,
    val gpsProviderLabel: String = "Multi-GPS",
    val gpsAccuracyMeters: Float? = null,
    val pendingOfflineAddressCount: Int = 0,
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

    private val _isOnline = MutableStateFlow(LocationHelper.isOnline(application))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var locationTrackingJob: Job? = null
    private var addressSyncJob: Job? = null

    init {
        startLocationTracking()
        observeNetworkAndAutoSyncAddresses()
    }

    private fun observeNetworkAndAutoSyncAddresses() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            LocationHelper.observeOnlineStatus(app).collect { online ->
                _isOnline.value = online
                if (online) {
                    triggerAutoReverseGeocodeSync()
                }
            }
        }
    }

    /**
     * Automatically converts any saved warung coordinates (latitude, longitude) that don't have
     * a street address yet into a full address when online or from local cache.
     */
    fun triggerAutoReverseGeocodeSync() {
        val app = getApplication<Application>()
        if (addressSyncJob?.isActive == true) return
        addressSyncJob = viewModelScope.launch {
            runCatching {
                repository.syncMissingAddressesFromCoordinates(app)
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.customersWithStatus,
        _selectedDay,
        _filter,
        _sortOption,
        _userLocation,
        _isLocating,
        _isOnline,
        repository.totalPiecesConsigned,
        repository.todayTotalSoldAmount,
        repository.allProducts
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val rawCustomers = args[0] as List<CustomerWithStatus>
        val day = args[1] as String
        val filter = args[2] as RouteFilter
        val sort = args[3] as CustomerSortOption
        val userLoc = args[4] as? Location
        val locating = args[5] as Boolean
        val online = args[6] as Boolean
        val totalPieces = args[7] as Int
        val todayAmount = args[8] as Double
        @Suppress("UNCHECKED_CAST")
        val products = args[9] as List<Product>

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

        val pendingAddressCount = customersWithDistance.count { item ->
            LocationHelper.isValidCoordinate(item.customer.latitude, item.customer.longitude) &&
                LocationHelper.isAddressNeedingAutoConversion(item.customer.address)
        }

        if (online && pendingAddressCount > 0) {
            triggerAutoReverseGeocodeSync()
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
            allProducts = products,
            todayDayName = today,
            selectedDay = day,
            filter = filter,
            sortOption = sort,
            userLocation = userLoc,
            isLocating = locating,
            isOnline = online,
            gpsProviderLabel = LocationHelper.getProviderDisplayLabel(userLoc, online),
            gpsAccuracyMeters = if (userLoc?.hasAccuracy() == true) userLoc.accuracy else null,
            pendingOfflineAddressCount = pendingAddressCount,
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
        if (locationTrackingJob?.isActive == true) return

        locationTrackingJob = viewModelScope.launch {
            _isLocating.value = true
            // Ambil multi-source high-accuracy location dengan progressive callback
            val initial = LocationHelper.getFreshLocation(
                context = app,
                timeoutMs = 5500L,
                targetAccuracyMeters = 10f,
                onIntermediateFix = { fix ->
                    if (LocationHelper.isBetterLocation(fix, _userLocation.value)) {
                        _userLocation.value = fix
                    }
                }
            )
            if (initial != null && LocationHelper.isBetterLocation(initial, _userLocation.value)) {
                _userLocation.value = initial
            }
            _isLocating.value = false

            // Dapatkan continuous flow pembaruan real-time (Fused GMaps + Satelit GPS Offline)
            LocationHelper.getLocationFlow(app, intervalMs = 2500L).collect { loc ->
                if (loc != null && LocationHelper.isBetterLocation(loc, _userLocation.value)) {
                    _userLocation.value = loc
                }
            }
        }
    }

    fun refreshLocation() {
        val app = getApplication<Application>()
        if (!LocationHelper.hasLocationPermission(app)) return
        viewModelScope.launch {
            _isLocating.value = true
            val loc = LocationHelper.getFreshLocation(
                context = app,
                timeoutMs = 6500L,
                targetAccuracyMeters = 8f,
                onIntermediateFix = { fix ->
                    _userLocation.value = fix
                }
            )
            if (loc != null) {
                _userLocation.value = loc
            }
            _isLocating.value = false
            if (_isOnline.value) {
                triggerAutoReverseGeocodeSync()
            }
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
        val app = getApplication<Application>()
        viewModelScope.launch {
            var finalAddress = address.trim()
            if (LocationHelper.isValidCoordinate(latitude, longitude) &&
                LocationHelper.isAddressNeedingAutoConversion(finalAddress)
            ) {
                // Coba ambil dari cache offline dulu atau konversi langsung jika online
                val resolved = LocationHelper.reverseGeocodeAddress(app, latitude!!, longitude!!)
                if (!resolved.isNullOrBlank()) {
                    finalAddress = resolved
                }
            }

            repository.saveCustomer(
                Customer(
                    name = name,
                    address = finalAddress,
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
        val app = getApplication<Application>()
        viewModelScope.launch {
            var finalCust = customer
            if (LocationHelper.isValidCoordinate(customer.latitude, customer.longitude) &&
                LocationHelper.isAddressNeedingAutoConversion(customer.address)
            ) {
                val resolved = LocationHelper.reverseGeocodeAddress(
                    app,
                    customer.latitude!!,
                    customer.longitude!!
                )
                if (!resolved.isNullOrBlank()) {
                    finalCust = customer.copy(address = resolved)
                }
            }
            repository.saveCustomer(finalCust)
        }
    }

    fun saveCustomerWithPricesAndStocks(
        customer: Customer,
        productStocksAndPrices: List<Triple<Long, Int, Double?>>
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            var finalCust = customer
            if (LocationHelper.isValidCoordinate(customer.latitude, customer.longitude) &&
                LocationHelper.isAddressNeedingAutoConversion(customer.address)
            ) {
                val resolved = LocationHelper.reverseGeocodeAddress(
                    app,
                    customer.latitude!!,
                    customer.longitude!!
                )
                if (!resolved.isNullOrBlank()) {
                    finalCust = customer.copy(address = resolved)
                }
            }
            repository.saveCustomerWithCustomPricesAndStocks(
                customer = finalCust,
                productConfigs = productStocksAndPrices
            )
        }
    }

    fun updateCustomerPhoto(customerId: Long, photoUri: String?) {
        viewModelScope.launch {
            repository.updateCustomerPhoto(customerId, photoUri)
        }
    }

    fun updateCustomerLocation(
        customerId: Long,
        latitude: Double?,
        longitude: Double?,
        resolvedAddress: String? = null
    ) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            var addressToSave = resolvedAddress?.trim()
            if (addressToSave.isNullOrBlank() && LocationHelper.isValidCoordinate(latitude, longitude)) {
                val existing = repository.getCustomerById(customerId)
                if (existing != null && LocationHelper.isAddressNeedingAutoConversion(existing.address)) {
                    addressToSave = LocationHelper.reverseGeocodeAddress(app, latitude!!, longitude!!)
                }
            }
            repository.updateCustomerLocation(
                customerId = customerId,
                lat = latitude,
                lng = longitude,
                resolvedAddress = addressToSave
            )
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
