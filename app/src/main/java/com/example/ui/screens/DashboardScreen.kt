package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.Customer
import com.example.domain.model.CustomerWithStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.RoseError
import com.example.ui.theme.SupabaseGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.CustomerSortOption
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.RouteFilter
import com.example.util.LocationHelper
import com.example.util.PhotoChooserBottomSheet
import kotlinx.coroutines.launch
import java.util.Locale

val ROUTE_DAYS = listOf("Semua", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartVisit: (customerId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPerformance by viewModel.selectedPerformance.collectAsStateWithLifecycle()
    val selectedPerformanceCustomerName by viewModel.selectedPerformanceCustomerName.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var photoTargetCustomerId by remember { mutableStateOf<Long?>(null) }
    var editGpsTargetCustomer by remember { mutableStateOf<Customer?>(null) }
    val context = LocalContext.current

    // Request permission untuk GPS real-time & pengurutan terdekat
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            viewModel.startLocationTracking()
        }
    }

    // Pastikan otomatis memilih rute hari ini sesuai kalender di HP saat layar dibuka
    LaunchedEffect(Unit) {
        viewModel.selectToday()
        if (LocationHelper.hasLocationPermission(context)) {
            viewModel.startLocationTracking()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CharcoalBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SupabaseGreen,
                contentColor = Color(0xFF042114),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Toko")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar: Clean & Minimal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rute Distribusi",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Box(
                    modifier = Modifier
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${uiState.allCustomers.size} Warung",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SupabaseGreen
                    )
                }
            }

            // Real-Time GPS Tracking Status Bar: Clean & Compact
            if (LocationHelper.hasLocationPermission(context)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .background(SupabaseGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, SupabaseGreen.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (uiState.userLocation != null) SupabaseGreen else AmberWarning, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.userLocation != null) "GPS Aktif" else "Mencari GPS...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.userLocation != null) SupabaseGreen else AmberWarning
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { viewModel.refreshLocation() }
                    ) {
                        if (uiState.isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(11.dp), strokeWidth = 1.5.dp, color = SupabaseGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = "Update",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SupabaseGreen
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .clickable {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GPS Nonaktif",
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Text(
                        text = "Aktifkan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Top Metric Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Stok",
                    value = "${uiState.totalPiecesConsigned} pcs",
                    icon = Icons.Default.Inventory2,
                    accent = SupabaseGreen
                )
                MetricMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Omset Hari Ini",
                    value = "Rp %,.0f".format(uiState.todayTotalSoldAmount),
                    icon = Icons.Default.Store,
                    accent = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // FITUR RUTE HARIAN: Clean Single-Row Day Selector Chips
            val dayListState = rememberLazyListState()
            val dayIndex = remember(uiState.selectedDay) {
                ROUTE_DAYS.indexOfFirst { it.equals(uiState.selectedDay, ignoreCase = true) }.coerceAtLeast(0)
            }
            LaunchedEffect(dayIndex) {
                dayListState.animateScrollToItem(dayIndex)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (uiState.selectedDay != uiState.todayDayName) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rute: ${uiState.selectedDay}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = "Hari Ini",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen,
                            modifier = Modifier
                                .clickable { viewModel.selectToday() }
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                LazyRow(
                    state = dayListState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(ROUTE_DAYS) { day ->
                        val isSelected = uiState.selectedDay.equals(day, ignoreCase = true)
                        val isToday = day.equals(uiState.todayDayName, ignoreCase = true)
                        val count = if (day == "Semua") uiState.allCustomers.size else (uiState.dayCounts[day] ?: 0)

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedDay(day) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = day,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isToday) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) Color(0xFF042114) else SupabaseGreen,
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                        ) {
                                            Text(
                                                text = "KINI",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) SupabaseGreen else Color(0xFF042114)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "($count)",
                                        fontSize = 10.5.sp,
                                        color = if (isSelected) Color(0xFF042114) else TextMutedDark
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SupabaseGreen,
                                selectedLabelColor = Color(0xFF042114),
                                containerColor = CharcoalSurfaceElevated,
                                labelColor = TextSecondaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) SupabaseGreen else CharcoalBorder
                            )
                        )
                    }
                }
            }

            // Status Filter Tabs (Semua, Belum, Selesai)
            val tabs = listOf(
                "Semua (${uiState.filteredCustomers.size})",
                "Belum",
                "Selesai"
            )
            val selectedTabIndex = when (uiState.filter) {
                RouteFilter.ALL -> 0
                RouteFilter.UNVISITED -> 1
                RouteFilter.VISITED -> 2
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CharcoalBg,
                contentColor = SupabaseGreen,
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(CharcoalBorder)
                    )
                },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = SupabaseGreen,
                        height = 2.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            when (index) {
                                0 -> viewModel.setFilter(RouteFilter.ALL)
                                1 -> viewModel.setFilter(RouteFilter.UNVISITED)
                                2 -> viewModel.setFilter(RouteFilter.VISITED)
                            }
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) SupabaseGreen else TextSecondaryDark
                            )
                        }
                    )
                }
            }

            // FITUR SORTING OUTLET: Clean Compact Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 5.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Sort,
                    contentDescription = "Urutkan",
                    tint = TextSecondaryDark,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(CustomerSortOption.entries.toTypedArray()) { opt ->
                        val isSortSelected = uiState.sortOption == opt
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSortSelected) SupabaseGreen else CharcoalSurfaceElevated,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSortSelected) SupabaseGreen else CharcoalBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.setSortOption(opt) }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (opt == CustomerSortOption.NEAREST) {
                                    Icon(
                                        Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = if (isSortSelected) Color(0xFF042114) else SupabaseGreen,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = opt.shortLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSortSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSortSelected) Color(0xFF042114) else TextPrimaryDark
                                )
                            }
                        }
                    }
                }
            }

            // Customer Route List
            if (uiState.filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada warung di rute ini",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryDark
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredCustomers, key = { it.customer.id }) { item ->
                        CustomerRouteCard(
                            item = item,
                            onStartVisit = { onStartVisit(item.customer.id) },
                            onCall = {
                                if (item.customer.phone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.customer.phone}"))
                                    context.startActivity(intent)
                                }
                            },
                            onChangePhoto = {
                                photoTargetCustomerId = item.customer.id
                            },
                            onOpenMaps = {
                                val lat = item.customer.latitude
                                val lng = item.customer.longitude
                                if (lat != null && lng != null) {
                                    LocationHelper.openNavigationInGoogleMaps(context, lat, lng, item.customer.name)
                                }
                            },
                            onEditGps = {
                                editGpsTargetCustomer = item.customer
                            },
                            onViewPerformance = {
                                viewModel.openCustomerPerformance(item.customer)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Customer Dialog with GPS Auto-Fill & Manual Fill
    if (showAddDialog) {
        AddCustomerWithRouteDialog(
            defaultDay = if (uiState.selectedDay == "Semua") uiState.todayDayName else uiState.selectedDay,
            nextRouteOrder = (uiState.filteredCustomers.maxOfOrNull { it.customer.route_order } ?: 0) + 1,
            currentDeviceLocation = uiState.userLocation,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, addr, phone, day, order, photoUri, lat, lng ->
                viewModel.addCustomer(name, addr, phone, day, order, photoUri, lat, lng)
                showAddDialog = false
            }
        )
    }

    // Edit GPS Location Dialog
    if (editGpsTargetCustomer != null) {
        EditCustomerGpsDialog(
            customer = editGpsTargetCustomer!!,
            currentDeviceLocation = uiState.userLocation,
            onDismiss = { editGpsTargetCustomer = null },
            onSave = { lat, lng ->
                viewModel.updateCustomerLocation(editGpsTargetCustomer!!.id, lat, lng)
                editGpsTargetCustomer = null
            }
        )
    }

    // Photo Chooser Sheet for Store
    if (photoTargetCustomerId != null) {
        val targetCust = uiState.allCustomers.find { it.customer.id == photoTargetCustomerId }?.customer
        PhotoChooserBottomSheet(
            title = "Foto Toko: ${targetCust?.name ?: ""}",
            hasExistingPhoto = !targetCust?.photo_uri.isNullOrBlank(),
            onPhotoSelected = { uri ->
                viewModel.updateCustomerPhoto(photoTargetCustomerId!!, uri)
                photoTargetCustomerId = null
            },
            onDeletePhoto = {
                viewModel.updateCustomerPhoto(photoTargetCustomerId!!, null)
                photoTargetCustomerId = null
            },
            onDismiss = { photoTargetCustomerId = null }
        )
    }

    // Customer Performance & History Dialog
    if (selectedPerformanceCustomerName != null) {
        CustomerPerformanceDialog(
            performance = selectedPerformance,
            customerName = selectedPerformanceCustomerName ?: "Warung",
            onDismiss = { viewModel.closeCustomerPerformance() }
        )
    }
}

@Composable
private fun MetricMiniCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = label, fontSize = 11.sp, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
        }
    }
}

/**
 * Customer Route Card dengan Hero Banner Foto Warung jika ada, badge jarak realtime,
 * dan tombol navigasi Maps / setting koordinat GPS.
 */
@Composable
private fun CustomerRouteCard(
    item: CustomerWithStatus,
    onStartVisit: () -> Unit,
    onCall: () -> Unit,
    onChangePhoto: () -> Unit,
    onOpenMaps: () -> Unit,
    onEditGps: () -> Unit,
    onViewPerformance: () -> Unit
) {
    val hasPhoto = !item.customer.photo_uri.isNullOrBlank()
    val hasGps = item.customer.latitude != null && item.customer.longitude != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            // HERO BANNER FOTO WARUNG (Jika ada foto, tampilkan banner elegan di atas card)
            if (hasPhoto) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .clickable { onChangePhoto() }
                ) {
                    AsyncImage(
                        model = item.customer.photo_uri,
                        contentDescription = "Foto ${item.customer.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient overlay agar teks badge di atas foto selalu terbaca tajam
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.70f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Top Bar di atas foto: Route order & Jarak realtime
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                    .border(1.dp, SupabaseGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "#${item.customer.route_order} • ${item.customer.route_day}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SupabaseGreen
                                )
                            }
                        }

                        // Distance Badge di atas banner
                        if (item.distanceMeters != null) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (item.distanceMeters < 1000f) SupabaseGreen else Color(0xFF0284C7),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = if (item.distanceMeters < 1000f) Color(0xFF042114) else Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.formattedDistance,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.distanceMeters < 1000f) Color(0xFF042114) else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Bar di atas foto: Status kunjungan & tombol ubah foto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.hasVisitedToday) {
                            Row(
                                modifier = Modifier
                                    .background(SupabaseGreen.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF042114), modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Selesai", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF042114))
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Belum", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                            }
                        }

                        // Icon ganti foto
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .size(26.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Ubah Foto", tint = Color.White, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }

            // CARD BODY: Info toko & Action buttons
            Column(modifier = Modifier.padding(14.dp)) {
                // Jika TIDAK ada foto banner, tampilkan Header Bar ringkas
                if (!hasPhoto) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(SupabaseGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, SupabaseGreen.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
                            ) {
                                Text(
                                    text = "#${item.customer.route_order} • ${item.customer.route_day}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SupabaseGreen
                                )
                            }

                            // Distance Badge
                            if (item.distanceMeters != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (item.distanceMeters < 1000f) SupabaseGreen.copy(alpha = 0.18f) else Color(0xFF38BDF8).copy(alpha = 0.18f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (item.distanceMeters < 1000f) SupabaseGreen.copy(alpha = 0.4f) else Color(0xFF38BDF8).copy(alpha = 0.4f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = if (item.distanceMeters < 1000f) SupabaseGreen else Color(0xFF38BDF8),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = item.formattedDistance,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.distanceMeters < 1000f) SupabaseGreen else Color(0xFF38BDF8)
                                        )
                                    }
                                }
                            }
                        }

                        // Visit Status Badge
                        if (item.hasVisitedToday) {
                            Row(
                                modifier = Modifier
                                    .background(SupabaseGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, SupabaseGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Selesai", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SupabaseGreen)
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .background(AmberWarning.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .border(1.dp, AmberWarning.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Belum", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AmberWarning)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Nama Toko & Tombol Telepon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.customer.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )

                        if (item.customer.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.customer.address,
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!hasPhoto) {
                            IconButton(
                                onClick = onChangePhoto,
                                modifier = Modifier
                                    .background(CharcoalSurfaceElevated, CircleShape)
                                    .size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = "Tambah Foto",
                                    tint = SupabaseGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        if (item.customer.phone.isNotBlank()) {
                            IconButton(
                                onClick = onCall,
                                modifier = Modifier
                                    .background(CharcoalSurfaceElevated, CircleShape)
                                    .size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Telepon",
                                    tint = SupabaseGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // GPS & Jarak Info Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (hasGps) Icons.Default.LocationOn else Icons.Default.EditLocation,
                            contentDescription = null,
                            tint = if (hasGps) SupabaseGreen else AmberWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        if (hasGps) {
                            Text(
                                text = if (item.distanceMeters != null) {
                                    item.formattedDistance
                                } else {
                                    "GPS: %.4f, %.4f".format(item.customer.latitude, item.customer.longitude)
                                },
                                fontSize = 11.5.sp,
                                color = if (item.distanceMeters != null) SupabaseGreen else TextSecondaryDark
                            )
                        } else {
                            Text(
                                text = "Belum set GPS",
                                fontSize = 11.5.sp,
                                color = AmberWarning
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasGps) {
                            Text(
                                text = "Maps",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier
                                    .clickable { onOpenMaps() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (hasGps) "Ubah GPS" else "+ GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen,
                            modifier = Modifier
                                .clickable { onEditGps() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stock Info & Action: Mulai Kunjungan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Stok:", fontSize = 10.5.sp, color = TextMutedDark)
                        Text(
                            text = "${item.totalActiveStock} pcs",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = onViewPerformance,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Insights,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = AmberWarning
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Riwayat",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberWarning
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onStartVisit,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.hasVisitedToday) CharcoalSurfaceElevated else SupabaseGreen,
                                contentColor = if (item.hasVisitedToday) TextPrimaryDark else Color(0xFF042114)
                            ),
                            border = if (item.hasVisitedToday) androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder) else null
                        ) {
                            Text(
                                text = if (item.hasVisitedToday) "Ulangi" else "Kunjungan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog Tambah Outlet Warung dengan Dukungan Auto-Fill GPS & Manual Fill Koordinat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomerWithRouteDialog(
    defaultDay: String,
    nextRouteOrder: Int,
    currentDeviceLocation: android.location.Location?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        address: String,
        phone: String,
        routeDay: String,
        routeOrder: Int,
        photoUri: String?,
        latitude: Double?,
        longitude: Double?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf(defaultDay) }
    var routeOrder by remember { mutableStateOf(nextRouteOrder.toString()) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var showPhotoChooser by remember { mutableStateOf(false) }
    var dayDropdownExpanded by remember { mutableStateOf(false) }

    // GPS State
    var latitudeStr by remember {
        mutableStateOf(currentDeviceLocation?.latitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }
    var longitudeStr by remember {
        mutableStateOf(currentDeviceLocation?.longitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }
    var isDetectingGps by remember { mutableStateOf(false) }
    var gpsStatusMessage by remember {
        mutableStateOf(
            if (currentDeviceLocation != null) "✅ GPS Terhubung" else null
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isDetectingGps = true
            scope.launch {
                val loc = LocationHelper.getFreshLocation(context, timeoutMs = 6000L)
                if (loc != null) {
                    latitudeStr = "%.6f".format(Locale.US, loc.latitude)
                    longitudeStr = "%.6f".format(Locale.US, loc.longitude)
                    gpsStatusMessage = "✅ GPS Terdeteksi"
                } else {
                    gpsStatusMessage = "⚠️ GPS Tidak Ditemukan"
                }
                isDetectingGps = false
            }
        } else {
            gpsStatusMessage = "Izin lokasi ditolak"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        title = {
            Text(
                text = "Tambah Warung",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    // Photo Picker Header in Dialog
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPhotoChooser = true }
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CharcoalSurface)
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (photoUri != null) "Foto Warung" else "+ Foto Warung",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (photoUri != null) SupabaseGreen else TextPrimaryDark
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Warung *") },
                        placeholder = { Text("Warung Bu Aminah") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // Route Day Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = dayDropdownExpanded,
                        onExpandedChange = { dayDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "Hari: $selectedDay",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SupabaseGreen,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = dayDropdownExpanded,
                            onDismissRequest = { dayDropdownExpanded = false },
                            modifier = Modifier.background(CharcoalSurfaceElevated)
                        ) {
                            listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu").forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(day, color = TextPrimaryDark) },
                                    onClick = {
                                        selectedDay = day
                                        dayDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = routeOrder,
                            onValueChange = { routeOrder = it.filter { ch -> ch.isDigit() } },
                            label = { Text("No. Urut") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SupabaseGreen,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(0.45f)
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("No. WhatsApp") },
                            placeholder = { Text("0812-xxxx") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SupabaseGreen,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(0.55f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat") },
                        placeholder = { Text("Jl. Merdeka No. 10") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // FITUR AUTO FILL & MANUAL FILL KOORDINAT GPS
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(10.dp))
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Koordinat GPS",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SupabaseGreen
                                )
                            }

                            // Tombol Auto-Detect GPS
                            Button(
                                onClick = {
                                    if (LocationHelper.hasLocationPermission(context)) {
                                        isDetectingGps = true
                                        scope.launch {
                                            val loc = LocationHelper.getFreshLocation(context, timeoutMs = 6000L)
                                            if (loc != null) {
                                                latitudeStr = "%.6f".format(Locale.US, loc.latitude)
                                                longitudeStr = "%.6f".format(Locale.US, loc.longitude)
                                                gpsStatusMessage = "✅ GPS Terdeteksi"
                                            } else {
                                                gpsStatusMessage = "⚠️ GPS Tidak Ditemukan"
                                            }
                                            isDetectingGps = false
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SupabaseGreen,
                                    contentColor = Color(0xFF042114)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                if (isDetectingGps) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color(0xFF042114))
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("Auto GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!gpsStatusMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = gpsStatusMessage!!,
                                fontSize = 10.5.sp,
                                color = if (gpsStatusMessage!!.startsWith("✅")) SupabaseGreen else AmberWarning
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Manual Fill Input Fields: Latitude & Longitude
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = latitudeStr,
                                onValueChange = { latitudeStr = it },
                                label = { Text("Latitude") },
                                placeholder = { Text("-6.214500") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SupabaseGreen,
                                    unfocusedBorderColor = CharcoalBorder,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = longitudeStr,
                                onValueChange = { longitudeStr = it },
                                label = { Text("Longitude") },
                                placeholder = { Text("106.845100") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SupabaseGreen,
                                    unfocusedBorderColor = CharcoalBorder,
                                    focusedTextColor = TextPrimaryDark,
                                    unfocusedTextColor = TextPrimaryDark
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val lat = latitudeStr.toDoubleOrNull()
                        val lng = longitudeStr.toDoubleOrNull()
                        val order = routeOrder.toIntOrNull() ?: 1
                        onConfirm(name, address, phone, selectedDay, order, photoUri, lat, lng)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114))
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondaryDark)
            }
        }
    )

    if (showPhotoChooser) {
        PhotoChooserBottomSheet(
            title = "Pilih Foto Warung",
            hasExistingPhoto = photoUri != null,
            onPhotoSelected = { uri ->
                photoUri = uri
                showPhotoChooser = false
            },
            onDeletePhoto = {
                photoUri = null
                showPhotoChooser = false
            },
            onDismiss = { showPhotoChooser = false }
        )
    }
}

/**
 * Dialog Cepat Update / Tag Koordinat GPS Warung langsung dari card
 */
@Composable
private fun EditCustomerGpsDialog(
    customer: Customer,
    currentDeviceLocation: android.location.Location?,
    onDismiss: () -> Unit,
    onSave: (lat: Double?, lng: Double?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var latitudeStr by remember {
        mutableStateOf(customer.latitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }
    var longitudeStr by remember {
        mutableStateOf(customer.longitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }
    var isDetectingGps by remember { mutableStateOf(false) }
    var gpsStatusMessage by remember { mutableStateOf<String?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isDetectingGps = true
            scope.launch {
                val loc = LocationHelper.getFreshLocation(context, timeoutMs = 6000L)
                if (loc != null) {
                    latitudeStr = "%.6f".format(Locale.US, loc.latitude)
                    longitudeStr = "%.6f".format(Locale.US, loc.longitude)
                    gpsStatusMessage = "✅ Akurat ±${loc.accuracy.toInt()}m"
                }
                isDetectingGps = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        title = {
            Text("Lokasi GPS: ${customer.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            isDetectingGps = true
                            scope.launch {
                                val loc = LocationHelper.getFreshLocation(context, timeoutMs = 6000L)
                                if (loc != null) {
                                    latitudeStr = "%.6f".format(Locale.US, loc.latitude)
                                    longitudeStr = "%.6f".format(Locale.US, loc.longitude)
                                    gpsStatusMessage = "✅ GPS Terdeteksi"
                                } else {
                                    gpsStatusMessage = "⚠️ GPS Tidak Ditemukan"
                                }
                                isDetectingGps = false
                            }
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isDetectingGps) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF042114))
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Ambil Posisi GPS", fontWeight = FontWeight.Bold)
                }

                if (!gpsStatusMessage.isNullOrBlank()) {
                    Text(
                        text = gpsStatusMessage!!,
                        fontSize = 11.sp,
                        color = if (gpsStatusMessage!!.startsWith("✅")) SupabaseGreen else AmberWarning
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latitudeStr,
                        onValueChange = { latitudeStr = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = longitudeStr,
                        onValueChange = { longitudeStr = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latitudeStr.toDoubleOrNull()
                    val lng = longitudeStr.toDoubleOrNull()
                    onSave(lat, lng)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114))
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondaryDark)
            }
        }
    )
}
