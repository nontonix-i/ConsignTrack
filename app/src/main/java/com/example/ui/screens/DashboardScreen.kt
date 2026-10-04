package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sell
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
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.Customer
import com.example.data.local.entity.Product
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

val ROUTE_DAYS = listOf("Semua", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onStartVisit: (customerId: Long) -> Unit,
    onOpenBackupRestore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPerformance by viewModel.selectedPerformance.collectAsStateWithLifecycle()
    val selectedPerformanceCustomerName by viewModel.selectedPerformanceCustomerName.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var photoTargetCustomerId by remember { mutableStateOf<Long?>(null) }
    var editGpsTargetCustomer by remember { mutableStateOf<Customer?>(null) }
    var editWarungTargetItem by remember { mutableStateOf<CustomerWithStatus?>(null) }
    var editWarungInitialTab by remember { mutableStateOf(0) }
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, SupabaseGreen.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable { onOpenBackupRestore() }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FolderZip,
                                contentDescription = "Backup & Restore ZIP",
                                tint = SupabaseGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Backup .ZIP",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SupabaseGreen
                            )
                        }
                    }

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
                            color = TextPrimaryDark
                        )
                    }
                }
            }

            // Real-Time GPS Tracking Status Bar: Clean, Multi-Source & Online/Offline Aware
            if (LocationHelper.hasLocationPermission(context)) {
                val loc = uiState.userLocation
                val accText = uiState.gpsAccuracyMeters?.let { "±${it.toInt().coerceAtLeast(1)}m" } ?: ""
                val coordsText = loc?.let { "%.5f, %.5f".format(Locale.US, it.latitude, it.longitude) } ?: ""
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .background(SupabaseGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, SupabaseGreen.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (loc != null) SupabaseGreen else AmberWarning, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (loc != null) {
                                "${uiState.gpsProviderLabel} $accText • $coordsText"
                            } else {
                                "Mencari titik GPS (${if (uiState.isOnline) "Online Fused" else "Satelit Offline"})..."
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (loc != null) SupabaseGreen else AmberWarning,
                            maxLines = 1
                        )
                        if (uiState.pendingOfflineAddressCount > 0 && !uiState.isOnline) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${uiState.pendingOfflineAddressCount} antre alamat",
                                fontSize = 10.sp,
                                color = AmberWarning,
                                maxLines = 1
                            )
                        }
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
                            text = if (uiState.isOnline) "Akurat" else "Offline",
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
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = if (uiState.allCustomers.isEmpty()) {
                                "Data Warung Masih Kosong (Pre-Production)"
                            } else {
                                "Tidak ada warung di rute ${uiState.selectedDay}"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (uiState.allCustomers.isEmpty()) {
                                "Tambahkan warung baru atau pulihkan data & foto dari file Backup .ZIP"
                            } else {
                                "Pilih hari rute lain atau tambah warung baru"
                            },
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SupabaseGreen,
                                    contentColor = Color(0xFF042114)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah Warung", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onOpenBackupRestore,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SupabaseGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Backup / Restore .ZIP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
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
                            onEditWarungInfo = { initialTab ->
                                editWarungInitialTab = initialTab
                                editWarungTargetItem = item
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
            onSave = { lat, lng, resolvedAddress ->
                viewModel.updateCustomerLocation(
                    customerId = editGpsTargetCustomer!!.id,
                    latitude = lat,
                    longitude = lng,
                    resolvedAddress = resolvedAddress
                )
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
            existingPhotoUri = targetCust?.photo_uri,
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

    // Edit Warung Info, Custom Prices & Previous Week Stock Dialog
    if (editWarungTargetItem != null) {
        val latestTarget = uiState.allCustomers.find { it.customer.id == editWarungTargetItem!!.customer.id } ?: editWarungTargetItem!!
        EditCustomerAndPricesDialog(
            item = latestTarget,
            allProducts = uiState.allProducts,
            initialTab = editWarungInitialTab,
            currentDeviceLocation = uiState.userLocation,
            onDismiss = { editWarungTargetItem = null },
            onSave = { updatedCust, stocksAndPrices ->
                viewModel.saveCustomerWithPricesAndStocks(updatedCust, stocksAndPrices)
                editWarungTargetItem = null
            },
            onDelete = { custToDelete ->
                viewModel.deleteCustomer(custToDelete)
                editWarungTargetItem = null
            }
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
 * Customer Route Card — Minimalist, Clean & Useful
 */
@Composable
private fun CustomerRouteCard(
    item: CustomerWithStatus,
    onStartVisit: () -> Unit,
    onCall: () -> Unit,
    onChangePhoto: () -> Unit,
    onOpenMaps: () -> Unit,
    onEditGps: () -> Unit,
    onEditWarungInfo: (initialTab: Int) -> Unit,
    onViewPerformance: () -> Unit
) {
    val hasPhoto = !item.customer.photo_uri.isNullOrBlank()
    val hasGps = item.customer.latitude != null && item.customer.longitude != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ROW 1: Thumbnail/Avatar (opsional/compact) + Nama Warung + Badge Rute + Quick Icon Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Compact Store Photo / Camera Avatar (42.dp)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CharcoalSurfaceElevated)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                            .clickable { onChangePhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasPhoto) {
                            AsyncImage(
                                model = item.customer.photo_uri,
                                contentDescription = "Foto ${item.customer.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = "Foto Warung",
                                tint = TextMutedDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.customer.name,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (item.hasVisitedToday) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selesai",
                                    tint = SupabaseGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Sub-baris minimalis: #Urut • Hari • Alamat / Jarak GPS
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${item.customer.route_order} ${item.customer.route_day}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SupabaseGreen
                            )
                            if (item.distanceMeters != null) {
                                Text(
                                    text = " • ${item.formattedDistance}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.clickable { if (hasGps) onOpenMaps() else onEditGps() }
                                )
                            } else if (!hasGps) {
                                Text(
                                    text = " • +GPS",
                                    fontSize = 11.sp,
                                    color = AmberWarning,
                                    modifier = Modifier.clickable { onEditGps() }
                                )
                            }
                            if (item.customer.address.isNotBlank()) {
                                Text(
                                    text = " • ${item.customer.address}",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 1
                                )
                            } else if (hasGps) {
                                Text(
                                    text = " • Auto-Alamat (GPS)",
                                    fontSize = 10.5.sp,
                                    color = TextMutedDark,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Minimalist Action Icons on top-right
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasGps) {
                        IconButton(
                            onClick = onOpenMaps,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.NearMe,
                                contentDescription = "Maps",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    if (item.customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = onCall,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Telepon",
                                tint = SupabaseGreen,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onViewPerformance,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = "Riwayat",
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { onEditWarungInfo(0) },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Warung",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ROW 2: Minimalist Titip Lalu & Harga Warung Strip (1-tap untuk edit harga & titip lalu)
            val activeOrCustomItems = remember(item.stockItems) {
                item.stockItems.filter { it.quantityPieces > 0 || it.hasCustomPrice }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurfaceElevated.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .border(0.5.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                    .clickable { onEditWarungInfo(1) }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.totalActiveStock > 0) {
                            "Titip Lalu: ${item.totalActiveStock} pcs"
                        } else {
                            "Titip Lalu: Kosong"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.totalActiveStock > 0) TextPrimaryDark else TextMutedDark
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.customPricesByProduct.isNotEmpty()) {
                            Text(
                                text = "${item.customPricesByProduct.size} Harga Khusus • ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AmberWarning
                            )
                        }
                        Text(
                            text = "Atur Harga/Stok",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen
                        )
                    }
                }

                if (activeOrCustomItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    activeOrCustomItems.forEach { stockItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${stockItem.productName}",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Rp %,.0f/%s".format(stockItem.effectivePricePack, stockItem.unitBig),
                                    fontSize = 10.sp,
                                    fontWeight = if (stockItem.hasCustomPrice) FontWeight.Bold else FontWeight.Normal,
                                    color = if (stockItem.hasCustomPrice) AmberWarning else TextMutedDark
                                )
                            }
                            Text(
                                text = if (stockItem.quantityPieces > 0) stockItem.formattedStock else "0 ${stockItem.unitSmall}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (stockItem.quantityPieces > 0) SupabaseGreen else TextMutedDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ROW 3: Clean Bottom Footer (Last visit info on left, Primary CTA on right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val lastVisitLabel = remember(item.lastVisitDate, item.lastTransactionAmount) {
                    if (item.lastVisitDate != null) {
                        val d = java.text.SimpleDateFormat("dd MMM", Locale.forLanguageTag("id-ID")).format(java.util.Date(item.lastVisitDate))
                        val amt = item.lastTransactionAmount?.let { " • Rp %,.0f".format(it) } ?: ""
                        "Lalu: $d$amt"
                    } else {
                        "Belum pernah dikunjungi"
                    }
                }

                Text(
                    text = lastVisitLabel,
                    fontSize = 11.sp,
                    color = TextMutedDark
                )

                Button(
                    onClick = onStartVisit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.hasVisitedToday) CharcoalSurfaceElevated else SupabaseGreen,
                        contentColor = if (item.hasVisitedToday) TextPrimaryDark else Color(0xFF042114)
                    ),
                    border = if (item.hasVisitedToday) androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder) else null,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (item.hasVisitedToday) "Buka Lagi" else "Kunjungan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
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

/**
 * Dialog Tambah Warung — Minimalist, Clean & Ringkas
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
    var showManualGps by remember { mutableStateOf(false) }

    var latitudeStr by remember {
        mutableStateOf(currentDeviceLocation?.latitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }
    var longitudeStr by remember {
        mutableStateOf(currentDeviceLocation?.longitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }
    var lastAutoAddress by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = CharcoalSurface,
        title = {
            Text(
                text = "Tambah Warung",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Thumbnail Foto + Nama Warung
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CharcoalSurfaceElevated)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                            .clickable { showPhotoChooser = true },
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
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Foto", tint = SupabaseGreen, modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Warung *") },
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

                // Row 2: Hari Rute & No. Urut
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = dayDropdownExpanded,
                        onExpandedChange = { dayDropdownExpanded = it },
                        modifier = Modifier.weight(0.6f)
                    ) {
                        OutlinedTextField(
                            value = selectedDay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Hari Rute") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                            singleLine = true,
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

                    OutlinedTextField(
                        value = routeOrder,
                        onValueChange = { routeOrder = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Urutan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.weight(0.4f)
                    )
                }

                // Row 3: No. WA & Alamat (Auto-filled from GPS when online/cached)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. WhatsApp (Opsional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SupabaseGreen,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat (Otomatis dari GPS / Ketik Manual)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SupabaseGreen,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Row 4: Smart Multi-Source GPS + Real-Time + Auto Address Section
                GpsSmartCoordinateSection(
                    latitudeStr = latitudeStr,
                    longitudeStr = longitudeStr,
                    currentAddress = address,
                    autoLockInitialIfEmpty = true,
                    onCoordinatesChanged = { newLat, newLng ->
                        latitudeStr = newLat
                        longitudeStr = newLng
                    },
                    onAutoAddressResolved = { resolvedAddr, forceApply ->
                        if (forceApply || address.isBlank() || address == lastAutoAddress) {
                            address = resolvedAddr
                            lastAutoAddress = resolvedAddr
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val lat = latitudeStr.toDoubleOrNull()
                        val lng = longitudeStr.toDoubleOrNull()
                        val order = routeOrder.toIntOrNull() ?: 1
                        onConfirm(name.trim(), address.trim(), phone.trim(), selectedDay, order, photoUri, lat, lng)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114))
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
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
            title = "Foto Warung",
            hasExistingPhoto = photoUri != null,
            existingPhotoUri = photoUri,
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
 * Dialog Cepat Update Koordinat GPS Warung & Konversi Alamat Otomatis — Minimalist
 */
@Composable
private fun EditCustomerGpsDialog(
    customer: Customer,
    currentDeviceLocation: android.location.Location?,
    onDismiss: () -> Unit,
    onSave: (lat: Double?, lng: Double?, resolvedAddress: String?) -> Unit
) {
    var latitudeStr by remember {
        mutableStateOf(
            customer.latitude?.let { "%.6f".format(Locale.US, it) }
                ?: currentDeviceLocation?.latitude?.let { "%.6f".format(Locale.US, it) }
                ?: ""
        )
    }
    var longitudeStr by remember {
        mutableStateOf(
            customer.longitude?.let { "%.6f".format(Locale.US, it) }
                ?: currentDeviceLocation?.longitude?.let { "%.6f".format(Locale.US, it) }
                ?: ""
        )
    }
    var addressPreview by remember { mutableStateOf(customer.address) }
    var lastAutoAddress by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = CharcoalSurface,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Lokasi GPS • ${customer.name}",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    "Multi-GPS (Fused GMaps + Satelit Offline) & Auto Alamat",
                    fontSize = 11.sp,
                    color = SupabaseGreen
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GpsSmartCoordinateSection(
                    latitudeStr = latitudeStr,
                    longitudeStr = longitudeStr,
                    currentAddress = addressPreview,
                    autoLockInitialIfEmpty = customer.latitude == null || customer.longitude == null,
                    alwaysExpandInputs = true,
                    onCoordinatesChanged = { newLat, newLng ->
                        latitudeStr = newLat
                        longitudeStr = newLng
                    },
                    onAutoAddressResolved = { resolvedAddr, forceApply ->
                        if (forceApply || addressPreview.isBlank() || addressPreview == lastAutoAddress) {
                            addressPreview = resolvedAddr
                            lastAutoAddress = resolvedAddr
                        }
                    }
                )

                OutlinedTextField(
                    value = addressPreview,
                    onValueChange = { addressPreview = it },
                    label = { Text("Alamat Warung (Otomatis dari Koordinat)") },
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        latitudeStr.toDoubleOrNull(),
                        longitudeStr.toDoubleOrNull(),
                        addressPreview.trim().ifBlank { null }
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114))
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextSecondaryDark)
            }
        }
    )
}

/**
 * Modal Edit Warung, Harga Khusus & Titip Lalu — Minimalist, Simple & Useful
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCustomerAndPricesDialog(
    item: CustomerWithStatus,
    allProducts: List<Product>,
    initialTab: Int = 0,
    currentDeviceLocation: android.location.Location?,
    onDismiss: () -> Unit,
    onSave: (
        updatedCustomer: Customer,
        stocksAndPrices: List<Triple<Long, Int, Double?>>
    ) -> Unit,
    onDelete: (Customer) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cust = item.customer

    var activeTab by remember { mutableStateOf(initialTab) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showManualGps by remember { mutableStateOf(false) }

    // Tab 0: Info Warung State
    var name by remember(cust.id) { mutableStateOf(cust.name) }
    var address by remember(cust.id) { mutableStateOf(cust.address) }
    var phone by remember(cust.id) { mutableStateOf(cust.phone) }
    var selectedDay by remember(cust.id) { mutableStateOf(cust.route_day) }
    var routeOrder by remember(cust.id) { mutableStateOf(cust.route_order.toString()) }
    var photoUri by remember(cust.id) { mutableStateOf(cust.photo_uri) }
    var showPhotoChooser by remember { mutableStateOf(false) }
    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var latitudeStr by remember(cust.id) {
        mutableStateOf(cust.latitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }
    var longitudeStr by remember(cust.id) {
        mutableStateOf(cust.longitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }
    var lastAutoAddress by remember(cust.id) { mutableStateOf("") }

    // Tab 1: Harga Khusus & Titip Lalu State
    val stockQuantityMap = remember(cust.id, item.stockItems, allProducts) {
        androidx.compose.runtime.mutableStateMapOf<Long, Int>().apply {
            allProducts.forEach { prod ->
                val existingQty = item.stockItems.find { it.productId == prod.id }?.quantityPieces ?: 0
                put(prod.id, existingQty)
            }
        }
    }

    val customPricePackMap = remember(cust.id, item.stockItems, allProducts) {
        androidx.compose.runtime.mutableStateMapOf<Long, String>().apply {
            allProducts.forEach { prod ->
                val existingCustom = item.stockItems.find { it.productId == prod.id }?.customPricePack
                    ?: item.customPricesByProduct[prod.id]
                put(prod.id, existingCustom?.let { "%.0f".format(it) } ?: "%.0f".format(prod.selling_price_pack))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .heightIn(max = 680.dp)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = CharcoalSurface,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cust.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Warung",
                            tint = RoseError,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Minimalist Segmented Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Info Warung", "Harga & Titip Lalu").forEachIndexed { idx, tabTitle ->
                        val isSelected = activeTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) SupabaseGreen else Color.Transparent)
                                .clickable { activeTab = idx }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF042114) else TextSecondaryDark
                            )
                        }
                    }
                }
            }
        },
        text = {
            if (activeTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Foto Thumbnail + Nama Warung
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CharcoalSurfaceElevated)
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                                .clickable { showPhotoChooser = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(18.dp))
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Warung *") },
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

                    // Row 2: Hari Rute & No. Urut
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = dayDropdownExpanded,
                            onExpandedChange = { dayDropdownExpanded = it },
                            modifier = Modifier.weight(0.6f)
                        ) {
                            OutlinedTextField(
                                value = selectedDay,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hari Rute") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                                singleLine = true,
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

                        OutlinedTextField(
                            value = routeOrder,
                            onValueChange = { routeOrder = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Urutan") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SupabaseGreen,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(0.4f)
                        )
                    }

                    // Row 3: No. WA & Alamat
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("No. WhatsApp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat (Otomatis dari GPS / Ketik Manual)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Row 4: Smart Multi-Source GPS + Real-Time + Auto Address Section
                    GpsSmartCoordinateSection(
                        latitudeStr = latitudeStr,
                        longitudeStr = longitudeStr,
                        currentAddress = address,
                        autoLockInitialIfEmpty = false,
                        onCoordinatesChanged = { newLat, newLng ->
                            latitudeStr = newLat
                            longitudeStr = newLng
                        },
                        onAutoAddressResolved = { resolvedAddr, forceApply ->
                            if (forceApply || address.isBlank() || address == lastAutoAddress) {
                                address = resolvedAddr
                                lastAutoAddress = resolvedAddr
                            }
                        }
                    )
                }
            } else {
                // Tab 1: Minimalist Harga Khusus & Titip Lalu per Produk
                if (allProducts.isEmpty()) {
                    Text(
                        text = "Belum ada produk di katalog.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allProducts, key = { it.id }) { prod ->
                            val pcsPerPack = maxOf(1, prod.pieces_per_pack)
                            val currentQtyPcs = stockQuantityMap[prod.id] ?: 0
                            val pricePackStr = customPricePackMap[prod.id] ?: "%.0f".format(prod.selling_price_pack)
                            val parsedPackPrice = pricePackStr.toDoubleOrNull() ?: prod.selling_price_pack
                            val parsedUnitPrice = parsedPackPrice / pcsPerPack
                            val isCustomPrice = kotlin.math.abs(parsedPackPrice - prod.selling_price_pack) >= 0.5

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        if (isCustomPrice) AmberWarning.copy(alpha = 0.45f) else CharcoalBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                // Top line: Nama Produk & Harga Standar / Reset
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prod.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )

                                    if (isCustomPrice) {
                                        Text(
                                            text = "Reset (Rp %,.0f)".format(prod.selling_price_pack),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberWarning,
                                            modifier = Modifier.clickable {
                                                customPricePackMap[prod.id] = "%.0f".format(prod.selling_price_pack)
                                            }
                                        )
                                    } else {
                                        Text(
                                            text = "@Rp %,.0f/%s".format(parsedUnitPrice, prod.unit_small),
                                            fontSize = 10.sp,
                                            color = TextMutedDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Bottom line: Harga / Pack Input (Left) + Titip Lalu Stepper (Right)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = pricePackStr,
                                        onValueChange = { str ->
                                            customPricePackMap[prod.id] = str.filter { ch -> ch.isDigit() }
                                        },
                                        label = { Text("Rp / ${prod.unit_big}", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AmberWarning,
                                            unfocusedBorderColor = if (isCustomPrice) AmberWarning.copy(alpha = 0.5f) else CharcoalBorder,
                                            focusedTextColor = TextPrimaryDark,
                                            unfocusedTextColor = TextPrimaryDark
                                        ),
                                        modifier = Modifier.weight(0.48f)
                                    )

                                    // Compact Stepper Titip Lalu
                                    Column(
                                        modifier = Modifier
                                            .weight(0.52f)
                                            .background(CharcoalSurface, RoundedCornerShape(8.dp))
                                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Titip Lalu: ${prod.formatPackAndPieces(currentQtyPcs)}",
                                            fontSize = 9.5.sp,
                                            color = if (currentQtyPcs > 0) SupabaseGreen else TextMutedDark,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // -1 Pack
                                            Box(
                                                modifier = Modifier
                                                    .background(CharcoalSurfaceElevated, RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        stockQuantityMap[prod.id] = maxOf(0, currentQtyPcs - pcsPerPack)
                                                    }
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text("-1${prod.unit_big.take(1)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondaryDark)
                                            }
                                            // -1 Pcs
                                            Box(
                                                modifier = Modifier
                                                    .background(CharcoalSurfaceElevated, RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        if (currentQtyPcs > 0) stockQuantityMap[prod.id] = currentQtyPcs - 1
                                                    }
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text("-1", fontSize = 10.sp, color = TextSecondaryDark)
                                            }
                                            // +1 Pcs
                                            Box(
                                                modifier = Modifier
                                                    .background(CharcoalSurfaceElevated, RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        stockQuantityMap[prod.id] = currentQtyPcs + 1
                                                    }
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text("+1", fontSize = 10.sp, color = TextPrimaryDark)
                                            }
                                            // +1 Pack
                                            Box(
                                                modifier = Modifier
                                                    .background(SupabaseGreen.copy(alpha = 0.18f), RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        stockQuantityMap[prod.id] = currentQtyPcs + pcsPerPack
                                                    }
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text("+1${prod.unit_big.take(1)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SupabaseGreen)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val updatedCust = cust.copy(
                            name = name.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            route_day = selectedDay,
                            route_order = routeOrder.toIntOrNull() ?: cust.route_order,
                            photo_uri = photoUri,
                            latitude = latitudeStr.toDoubleOrNull(),
                            longitude = longitudeStr.toDoubleOrNull()
                        )
                        val stocksAndPrices = allProducts.map { prod ->
                            val qtyPcs = stockQuantityMap[prod.id] ?: 0
                            val priceStr = customPricePackMap[prod.id]
                            val parsedPrice = priceStr?.toDoubleOrNull()
                            val customPriceOrNull = if (parsedPrice != null && parsedPrice > 0.0 && kotlin.math.abs(parsedPrice - prod.selling_price_pack) >= 0.5) {
                                parsedPrice
                            } else {
                                null
                            }
                            Triple(prod.id, qtyPcs, customPriceOrNull)
                        }
                        onSave(updatedCust, stocksAndPrices)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114))
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
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
            title = "Foto Warung: $name",
            hasExistingPhoto = !photoUri.isNullOrBlank(),
            existingPhotoUri = photoUri,
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            containerColor = CharcoalSurface,
            title = {
                Text("Hapus Warung?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            },
            text = {
                Text(
                    "Hapus '${cust.name}' beserta data stok titipannya?",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(cust)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            }
        )
    }
}

/**
 * Reusable Multi-Source Real-Time GPS & Auto Reverse-Geocoding Coordinate Component.
 * - Combines Google Play Fused Location (GMaps engine), Hardware Satellite GPS (100% Offline), and Network providers.
 * - Continuously refines coordinates when live tracking is active.
 * - Automatically converts (latitude, longitude) to a street address when online or from offline local cache.
 * - Supports 1-tap paste from Google Maps URLs or coordinate strings.
 */
@Composable
fun GpsSmartCoordinateSection(
    latitudeStr: String,
    longitudeStr: String,
    currentAddress: String,
    autoLockInitialIfEmpty: Boolean = false,
    alwaysExpandInputs: Boolean = false,
    onCoordinatesChanged: (latStr: String, lngStr: String) -> Unit,
    onAutoAddressResolved: (resolvedAddress: String, forceApply: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOnline by LocationHelper.observeOnlineStatus(context)
        .collectAsStateWithLifecycle(initialValue = LocationHelper.isOnline(context))

    var isDetectingGps by remember { mutableStateOf(false) }
    var isGeocoding by remember { mutableStateOf(false) }
    var showManualInputs by remember { mutableStateOf(alwaysExpandInputs) }
    var liveProviderLabel by remember { mutableStateOf(LocationHelper.getProviderDisplayLabel(null, isOnline)) }
    var liveAccuracyMeters by remember { mutableStateOf<Float?>(null) }
    var resolvedGpsAddress by remember { mutableStateOf<String?>(null) }
    var bestSessionLocation by remember { mutableStateOf<android.location.Location?>(null) }

    fun applyLocationFix(loc: android.location.Location) {
        bestSessionLocation = loc
        liveProviderLabel = LocationHelper.getProviderDisplayLabel(loc, isOnline)
        if (loc.hasAccuracy()) {
            liveAccuracyMeters = loc.accuracy
        }
        onCoordinatesChanged(
            "%.6f".format(Locale.US, loc.latitude),
            "%.6f".format(Locale.US, loc.longitude)
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
                val loc = LocationHelper.getFreshLocation(
                    context = context,
                    timeoutMs = 6000L,
                    targetAccuracyMeters = 8f,
                    onIntermediateFix = { fix -> applyLocationFix(fix) }
                )
                if (loc != null) {
                    applyLocationFix(loc)
                }
                isDetectingGps = false
            }
        }
    }

    // Automatically acquire initial high-accuracy fix and refine in real-time when empty/requested
    LaunchedEffect(autoLockInitialIfEmpty) {
        if (autoLockInitialIfEmpty && LocationHelper.hasLocationPermission(context)) {
            if (latitudeStr.isBlank() || longitudeStr.isBlank()) {
                isDetectingGps = true
            }
            val initial = LocationHelper.getFreshLocation(
                context = context,
                timeoutMs = 5500L,
                targetAccuracyMeters = 8f,
                onIntermediateFix = { fix ->
                    if (latitudeStr.isBlank() || longitudeStr.isBlank() ||
                        LocationHelper.isBetterLocation(fix, bestSessionLocation)
                    ) {
                        applyLocationFix(fix)
                    }
                }
            )
            if (initial != null && (latitudeStr.isBlank() || longitudeStr.isBlank() ||
                    LocationHelper.isBetterLocation(initial, bestSessionLocation))
            ) {
                applyLocationFix(initial)
            }
            isDetectingGps = false
        }
    }

    // Automatic Coordinate -> Street Address conversion whenever lat/lng or online status changes!
    LaunchedEffect(latitudeStr, longitudeStr, isOnline) {
        val lat = latitudeStr.toDoubleOrNull()
        val lng = longitudeStr.toDoubleOrNull()
        if (!LocationHelper.isValidCoordinate(lat, lng)) {
            resolvedGpsAddress = null
            return@LaunchedEffect
        }

        // Check instant offline cache first
        val cached = LocationHelper.getCachedAddress(context, lat!!, lng!!)
        if (!cached.isNullOrBlank()) {
            resolvedGpsAddress = cached
            onAutoAddressResolved(cached, false)
            return@LaunchedEffect
        }

        if (!isOnline) {
            resolvedGpsAddress = null
            return@LaunchedEffect
        }

        delay(300) // Debounce manual coordinate typing
        isGeocoding = true
        val converted = LocationHelper.reverseGeocodeAddress(context, lat, lng)
        isGeocoding = false
        if (!converted.isNullOrBlank()) {
            resolvedGpsAddress = converted
            onAutoAddressResolved(converted, false)
        }
    }

    val latVal = latitudeStr.toDoubleOrNull()
    val lngVal = longitudeStr.toDoubleOrNull()
    val hasValidCoords = LocationHelper.isValidCoordinate(latVal, lngVal)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (hasValidCoords) SupabaseGreen.copy(alpha = 0.4f) else CharcoalBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top Line: Coordinate Status + Provider / Accuracy + Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { if (!alwaysExpandInputs) showManualInputs = !showManualInputs }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (hasValidCoords) SupabaseGreen else TextMutedDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasValidCoords) {
                            "$latitudeStr, $longitudeStr"
                        } else {
                            "Titik Koordinat GPS"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasValidCoords) SupabaseGreen else TextSecondaryDark,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val accBadge = liveAccuracyMeters?.let { " • ±${it.toInt().coerceAtLeast(1)}m" } ?: ""
                val modeBadge = if (isOnline) "Online ($liveProviderLabel$accBadge)" else "Offline (Satelit GPS$accBadge)"
                Text(
                    text = modeBadge,
                    fontSize = 10.sp,
                    color = if (isOnline) Color(0xFF38BDF8) else AmberWarning,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1-Tap Paste from Google Maps / Clipboard
                Box(
                    modifier = Modifier
                        .background(CharcoalSurface, RoundedCornerShape(6.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                        .clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (clipText.isBlank()) {
                                showManualInputs = true
                                Toast.makeText(context, "Salin koordinat atau link Google Maps lalu tempel", Toast.LENGTH_SHORT).show()
                            } else {
                                scope.launch {
                                    isDetectingGps = true
                                    val parsed = LocationHelper.parseCoordinatesOrMapsUrl(clipText)
                                    isDetectingGps = false
                                    if (parsed != null) {
                                        onCoordinatesChanged(
                                            "%.6f".format(Locale.US, parsed.first),
                                            "%.6f".format(Locale.US, parsed.second)
                                        )
                                        Toast.makeText(context, "Koordinat GMaps berhasil ditempel!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showManualInputs = true
                                        Toast.makeText(context, "Format clipboard bukan koordinat/link GMaps", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "Tempel GMaps",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "GMaps",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                // 1-Tap High-Accuracy Multi-Source GPS Lock
                Box(
                    modifier = Modifier
                        .background(SupabaseGreen.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                        .border(1.dp, SupabaseGreen.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .clickable {
                            if (LocationHelper.hasLocationPermission(context)) {
                                isDetectingGps = true
                                scope.launch {
                                    val loc = LocationHelper.getFreshLocation(
                                        context = context,
                                        timeoutMs = 6000L,
                                        targetAccuracyMeters = 8f,
                                        onIntermediateFix = { fix -> applyLocationFix(fix) }
                                    )
                                    if (loc != null) {
                                        applyLocationFix(loc)
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
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isDetectingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(11.dp),
                                strokeWidth = 1.5.dp,
                                color = SupabaseGreen
                            )
                        } else {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = "Ambil GPS Akurat",
                                tint = SupabaseGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasValidCoords) "Akurat" else "Ambil GPS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen
                        )
                    }
                }
            }
        }

        // Auto-Converted Address Strip (Seamless Online / Offline Feedback)
        if (hasValidCoords) {
            if (isGeocoding) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = SupabaseGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mengonversi koordinat ke alamat jalan...",
                        fontSize = 10.sp,
                        color = TextSecondaryDark
                    )
                }
            } else if (!resolvedGpsAddress.isNullOrBlank()) {
                val addr = resolvedGpsAddress!!
                val isDifferentFromField = currentAddress.isNotBlank() && !currentAddress.equals(addr, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurface, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alamat GPS: $addr",
                        fontSize = 10.5.sp,
                        color = if (isDifferentFromField) TextSecondaryDark else SupabaseGreen,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    if (isDifferentFromField) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pakai",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen,
                            modifier = Modifier.clickable {
                                onAutoAddressResolved(addr, true)
                            }
                        )
                    }
                }
            } else if (!isOnline) {
                Text(
                    text = "Offline Satelit • Alamat otomatis dikonversi saat koneksi internet aktif",
                    fontSize = 10.sp,
                    color = AmberWarning
                )
            }
        }

        // Expandable / Manual Coordinate Inputs (also accepts pasted "lat, lng" or GMaps link)
        if (showManualInputs || alwaysExpandInputs) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitudeStr,
                    onValueChange = { input ->
                        val parsedPair = LocationHelper.extractLatLngFromString(input)
                        if (parsedPair != null) {
                            onCoordinatesChanged(
                                "%.6f".format(Locale.US, parsedPair.first),
                                "%.6f".format(Locale.US, parsedPair.second)
                            )
                        } else {
                            onCoordinatesChanged(input, longitudeStr)
                        }
                    },
                    label = { Text("Latitude", fontSize = 10.5.sp) },
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
                    onValueChange = { input ->
                        val parsedPair = LocationHelper.extractLatLngFromString(input)
                        if (parsedPair != null) {
                            onCoordinatesChanged(
                                "%.6f".format(Locale.US, parsedPair.first),
                                "%.6f".format(Locale.US, parsedPair.second)
                            )
                        } else {
                            onCoordinatesChanged(latitudeStr, input)
                        }
                    },
                    label = { Text("Longitude", fontSize = 10.5.sp) },
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

