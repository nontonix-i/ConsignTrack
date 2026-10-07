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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.example.ui.theme.isEnglishLanguageActive
import com.example.ui.theme.tr
import com.example.ui.theme.trDay
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
    var bulkProductTargetItem by remember { mutableStateOf<CustomerWithStatus?>(null) }
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
                Icon(Icons.Default.Add, contentDescription = tr("Tambah Toko", "Add Store"))
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
                    text = tr("Rute Distribusi", "Distribution Route"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1-Tap Quick Auto-Add Warung + GPS Button
                    Box(
                        modifier = Modifier
                            .background(SupabaseGreen, RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.instantAutoAddWarungWithGps { createdName, hasGps ->
                                    val gpsInfo = if (hasGps) tr("beserta koordinat GPS", "with GPS coordinates") else tr("(tanpa titik GPS)", "(without GPS)")
                                    Toast.makeText(
                                        context,
                                        tr("⚡ $createdName otomatis ditambahkan $gpsInfo!", "⚡ $createdName auto-added $gpsInfo!"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = tr("Auto Add Cepat Warung + GPS", "Quick Auto-Add Store + GPS"),
                                tint = Color(0xFF042114),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("+ Auto Warung GPS", "+ Auto Store GPS"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042114)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, SupabaseGreen.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable { onOpenBackupRestore() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                                text = ".ZIP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SupabaseGreen
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tr("${uiState.allCustomers.size} Toko", "${uiState.allCustomers.size} Stores"),
                            fontSize = 11.5.sp,
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
                                tr(
                                    "Mencari titik GPS (${if (uiState.isOnline) "Online Fused" else "Satelit Offline"})...",
                                    "Acquiring GPS (${if (uiState.isOnline) "Online Fused" else "Offline Satellite"})..."
                                )
                            },
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (loc != null) SupabaseGreen else AmberWarning,
                            maxLines = 1
                        )
                        if (uiState.pendingOfflineAddressCount > 0 && !uiState.isOnline) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr("• ${uiState.pendingOfflineAddressCount} antre alamat", "• ${uiState.pendingOfflineAddressCount} pending addresses"),
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
                            text = if (uiState.isOnline) tr("Akurat", "Accurate") else "Offline",
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
                            text = tr("GPS Nonaktif", "GPS Disabled"),
                            fontSize = 11.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Text(
                        text = tr("Aktifkan", "Enable"),
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
                    label = tr("Total Stok", "Total Stock"),
                    value = "${uiState.totalPiecesConsigned} pcs",
                    icon = Icons.Default.Inventory2,
                    accent = SupabaseGreen
                )
                MetricMiniCard(
                    modifier = Modifier.weight(1f),
                    label = tr("Omset Hari Ini", "Today's Sales"),
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
                            text = "${tr("Rute", "Route")}: ${trDay(uiState.selectedDay)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryDark
                        )
                        Text(
                            text = tr("Hari Ini", "Today"),
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
                                        text = trDay(day),
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
                                                text = tr("KINI", "TODAY"),
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
                tr("Semua (${uiState.filteredCustomers.size})", "All (${uiState.filteredCustomers.size})"),
                tr("Belum", "Pending"),
                tr("Selesai", "Visited")
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
                    contentDescription = tr("Urutkan", "Sort"),
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
                                tr("Data Warung Masih Kosong (Pre-Production)", "No Stores Yet (Pre-Production)")
                            } else {
                                tr("Tidak ada warung di rute ${uiState.selectedDay}", "No stores on ${trDay(uiState.selectedDay)} route")
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (uiState.allCustomers.isEmpty()) {
                                tr("Tambahkan warung baru atau pulihkan data & foto dari file Backup .ZIP", "Add a new store or restore data & photos from a Backup .ZIP file")
                            } else {
                                tr("Pilih hari rute lain atau tambah warung baru", "Select another route day or add a new store")
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
                                Text(tr("Tambah Warung", "Add Store"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.instantAutoAddWarungWithGps { createdName, hasGps ->
                                        val gpsInfo = if (hasGps) tr("+ Koordinat GPS", "+ GPS Coords") else tr("(Tanpa GPS)", "(No GPS)")
                                        Toast.makeText(
                                            context,
                                            tr("⚡ $createdName otomatis ditambahkan $gpsInfo!", "⚡ $createdName auto-added $gpsInfo!"),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SupabaseGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Auto-Add GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            onBulkAddProducts = {
                                bulkProductTargetItem = item
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

    // Add Customer Dialog with Smart Auto-Naming (3-Char Unique Code + Lat/Lng), GPS Auto-Fill & Bulk Product Selection
    if (showAddDialog) {
        AddCustomerWithRouteDialog(
            defaultDay = if (uiState.selectedDay == "Semua") uiState.todayDayName else uiState.selectedDay,
            nextRouteOrder = (uiState.filteredCustomers.maxOfOrNull { it.customer.route_order } ?: 0) + 1,
            existingCustomerNames = uiState.allCustomers.map { it.customer.name },
            allProducts = uiState.allProducts,
            currentDeviceLocation = uiState.userLocation,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, addr, phone, day, order, photoUri, lat, lng, initialProducts ->
                viewModel.addCustomer(name, addr, phone, day, order, photoUri, lat, lng, initialProducts)
                showAddDialog = false
            }
        )
    }

    // Bulk Add Products Dialog directly from Warung Card on Dashboard
    if (bulkProductTargetItem != null) {
        val targetItem = uiState.allCustomers.find { it.customer.id == bulkProductTargetItem!!.customer.id }
            ?: bulkProductTargetItem!!
        AddProductToOutletDialog(
            catalogProducts = uiState.allProducts,
            existingItemIds = targetItem.stockItems.filter { it.quantityPieces > 0 }.map { it.productId }.toSet(),
            outletName = targetItem.customer.name,
            allowExistingProducts = true,
            onAddProductsBulk = { bulkList ->
                val additions = bulkList.map { sel ->
                    Triple(sel.product.id, sel.totalPieces, sel.customPricePack)
                }
                viewModel.bulkAddProductsToCustomer(targetItem.customer.id, additions)
                Toast.makeText(
                    context,
                    tr(
                        "✅ ${bulkList.size} produk berhasil ditambahkan ke ${targetItem.customer.name}!",
                        "✅ ${bulkList.size} products added to ${targetItem.customer.name}!"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                bulkProductTargetItem = null
            },
            onDismiss = { bulkProductTargetItem = null }
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
            title = tr("Foto Toko: ${targetCust?.name ?: ""}", "Store Photo: ${targetCust?.name ?: ""}"),
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
            customerName = selectedPerformanceCustomerName ?: tr("Warung", "Store"),
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
    onBulkAddProducts: () -> Unit,
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
                                contentDescription = tr("Foto ${item.customer.name}", "Photo ${item.customer.name}"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = tr("Foto Warung", "Store Photo"),
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
                                    contentDescription = tr("Selesai", "Visited"),
                                    tint = SupabaseGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Sub-baris minimalis: #Urut • Hari • Alamat / Jarak GPS
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${item.customer.route_order} ${trDay(item.customer.route_day)}",
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
                                    text = tr(" • Auto-Alamat (GPS)", " • Auto-Address (GPS)"),
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
                                contentDescription = tr("Telepon", "Call"),
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
                            contentDescription = tr("Riwayat", "History"),
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
                            contentDescription = tr("Edit Warung", "Edit Store"),
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
                            tr("Titip Lalu: ${item.totalActiveStock} pcs", "Consigned: ${item.totalActiveStock} pcs")
                        } else {
                            tr("Titip Lalu: Kosong", "Consigned: Empty")
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.totalActiveStock > 0) TextPrimaryDark else TextMutedDark
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.customPricesByProduct.isNotEmpty()) {
                            Text(
                                text = tr("${item.customPricesByProduct.size} Harga Khusus", "${item.customPricesByProduct.size} Custom Prices"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AmberWarning
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(SupabaseGreen.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                                .border(1.dp, SupabaseGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .clickable { onBulkAddProducts() }
                                .padding(horizontal = 7.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = tr("+ Bulk Produk", "+ Bulk Products"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SupabaseGreen
                            )
                        }
                        Text(
                            text = tr("Atur Stok", "Edit Stock"),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark
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
                val lastVisitLabel = remember(item.lastVisitDate, item.lastTransactionAmount, isEnglishLanguageActive) {
                    if (item.lastVisitDate != null) {
                        val loc = if (isEnglishLanguageActive) Locale.ENGLISH else Locale.forLanguageTag("id-ID")
                        val d = java.text.SimpleDateFormat("dd MMM", loc).format(java.util.Date(item.lastVisitDate))
                        val amt = item.lastTransactionAmount?.let { " • Rp %,.0f".format(it) } ?: ""
                        if (isEnglishLanguageActive) "Last: $d$amt" else "Lalu: $d$amt"
                    } else {
                        if (isEnglishLanguageActive) "Never visited" else "Belum pernah dikunjungi"
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
                        text = if (item.hasVisitedToday) tr("Buka Lagi", "Reopen") else tr("Kunjungan", "Visit"),
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
    existingCustomerNames: List<String>,
    allProducts: List<Product>,
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
        longitude: Double?,
        initialProducts: List<Triple<Long, Int, Double?>>
    ) -> Unit
) {
    var uniqueCode by remember {
        mutableStateOf(LocationHelper.generateUniqueWarungCode(existingCustomerNames))
    }
    var useLongitudeForAutoName by remember { mutableStateOf(false) }
    var isAutoCoordNameActive by remember { mutableStateOf(true) }

    var latitudeStr by remember {
        mutableStateOf(currentDeviceLocation?.latitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }
    var longitudeStr by remember {
        mutableStateOf(currentDeviceLocation?.longitude?.let { "%.6f".format(Locale.US, it) } ?: "")
    }

    val defaultAutoName = remember(uniqueCode, latitudeStr, longitudeStr, useLongitudeForAutoName) {
        LocationHelper.formatWarungAutoCoordNameFromStr(
            uniqueCode = uniqueCode,
            latitudeStr = latitudeStr,
            longitudeStr = longitudeStr,
            useLongitude = useLongitudeForAutoName
        )
    }

    var name by remember { mutableStateOf(defaultAutoName) }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf(defaultDay) }
    var routeOrder by remember { mutableStateOf(nextRouteOrder.toString()) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var showPhotoChooser by remember { mutableStateOf(false) }
    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var lastAutoAddress by remember { mutableStateOf("") }

    // Keep auto-generated name synced whenever GPS coordinates lock or uniqueCode/lat/lng preference changes
    LaunchedEffect(defaultAutoName, isAutoCoordNameActive) {
        if (isAutoCoordNameActive) {
            name = defaultAutoName
        }
    }

    // Bulk Initial Product Checkboxes & Pack Quantities inside Add Warung Dialog
    val checkedProducts = remember(allProducts) {
        androidx.compose.runtime.mutableStateMapOf<Long, Boolean>()
    }
    val productPacks = remember(allProducts) {
        androidx.compose.runtime.mutableStateMapOf<Long, Int>().apply {
            allProducts.forEach { put(it.id, 1) }
        }
    }

    // Dynamic naming suggestions: [CODE - Latitude], [CODE - Longitude], and optional street preset
    val latAutoPreset = remember(uniqueCode, latitudeStr, longitudeStr) {
        LocationHelper.formatWarungAutoCoordNameFromStr(
            uniqueCode = uniqueCode,
            latitudeStr = latitudeStr,
            longitudeStr = longitudeStr,
            useLongitude = false
        )
    }
    val lngAutoPreset = remember(uniqueCode, latitudeStr, longitudeStr) {
        LocationHelper.formatWarungAutoCoordNameFromStr(
            uniqueCode = uniqueCode,
            latitudeStr = latitudeStr,
            longitudeStr = longitudeStr,
            useLongitude = true
        )
    }
    val namingPresets = remember(latAutoPreset, lngAutoPreset, uniqueCode, address) {
        val streetShort = address.split(",").firstOrNull()?.trim()?.take(18)?.takeIf {
            it.isNotBlank() && !it.startsWith("Koordinat", ignoreCase = true) && !it.startsWith("Coord", ignoreCase = true) && !it.startsWith("-")
        }
        buildList {
            add(latAutoPreset)
            if (lngAutoPreset != latAutoPreset) {
                add(lngAutoPreset)
            }
            if (streetShort != null) {
                add("$uniqueCode - $streetShort")
            }
        }.distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .heightIn(max = 700.dp)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = CharcoalSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Auto-Add Warung & Bulk Produk", "Auto-Add Store & Bulk Products"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = tr("Nama otomatis + titik GPS real-time + centang produk awal", "Auto name + real-time GPS + bulk select initial products"),
                        fontSize = 11.sp,
                        color = SupabaseGreen
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Thumbnail Foto + Nama Warung (Pre-filled otomatis!)
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
                            Icon(Icons.Default.AddAPhoto, contentDescription = tr("Foto", "Photo"), tint = SupabaseGreen, modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            isAutoCoordNameActive = false
                        },
                        label = { Text(tr("Nama Otomatis (3 Kode Unik - Lat/Lng)", "Auto Name (3-Char Code - Lat/Lng)")) },
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

                // Quick Auto-Naming Chips (3-Char Unique Code + Latitude / Longitude + Regenerate Code)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(namingPresets) { preset ->
                        val isSelectedName = name == preset
                        val chipLabel = when (preset) {
                            latAutoPreset -> "$preset (Lat)"
                            lngAutoPreset -> "$preset (Lng)"
                            else -> preset
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelectedName) SupabaseGreen else CharcoalSurfaceElevated)
                                .border(
                                    1.dp,
                                    if (isSelectedName) SupabaseGreen else CharcoalBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    when (preset) {
                                        latAutoPreset -> {
                                            useLongitudeForAutoName = false
                                            isAutoCoordNameActive = true
                                            name = latAutoPreset
                                        }
                                        lngAutoPreset -> {
                                            useLongitudeForAutoName = true
                                            isAutoCoordNameActive = true
                                            name = lngAutoPreset
                                        }
                                        else -> {
                                            isAutoCoordNameActive = false
                                            name = preset
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = chipLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelectedName) Color(0xFF042114) else TextSecondaryDark
                            )
                        }
                    }

                    // Chip to generate a fresh 3-character alphanumeric unique code
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CharcoalSurfaceElevated)
                                .border(1.dp, SupabaseGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .clickable {
                                    val newCode = LocationHelper.generateUniqueWarungCode(existingCustomerNames + uniqueCode)
                                    uniqueCode = newCode
                                    isAutoCoordNameActive = true
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tr("🔄 Kode Baru ($uniqueCode)", "🔄 New Code ($uniqueCode)"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SupabaseGreen
                            )
                        }
                    }
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
                            value = trDay(selectedDay),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(tr("Hari Rute", "Route Day")) },
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
                                    text = { Text(trDay(day), color = TextPrimaryDark) },
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
                        label = { Text(tr("Urutan", "Order #")) },
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

                // Row 3: Smart Multi-Source GPS + Real-Time + Auto Address Section
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

                // Row 4: Alamat & No. WA
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(tr("Alamat (Otomatis dari GPS / Ketik Manual)", "Address (Auto from GPS / Manual)")) },
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
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(tr("No. WhatsApp (Opsional)", "WhatsApp / Phone (Optional)")) },
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

                // Row 5: Bulk Pilih Produk Titipan Awal (Centang Produk & Jumlah Pack)
                if (allProducts.isNotEmpty()) {
                    val allChecked = allProducts.all { checkedProducts[it.id] == true }
                    val selectedCount = allProducts.count { checkedProducts[it.id] == true }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(10.dp))
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr("Bulk Pilih Produk Titipan Awal", "Bulk Select Initial Consigned Products"),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (selectedCount > 0) tr("$selectedCount produk dipilih", "$selectedCount products selected") else tr("Centang produk yang langsung dititipkan", "Check products to consign immediately"),
                                    fontSize = 10.5.sp,
                                    color = if (selectedCount > 0) SupabaseGreen else TextSecondaryDark
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (allChecked) SupabaseGreen else CharcoalSurface)
                                        .border(1.dp, SupabaseGreen, RoundedCornerShape(6.dp))
                                        .clickable {
                                            val target = !allChecked
                                            allProducts.forEach { p ->
                                                checkedProducts[p.id] = target
                                                if (target && (productPacks[p.id] ?: 0) <= 0) {
                                                    productPacks[p.id] = 1
                                                }
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (allChecked) tr("Batal Semua", "Unselect All") else tr("Centang Semua", "Select All"),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (allChecked) Color(0xFF042114) else SupabaseGreen
                                    )
                                }
                            }
                        }

                        // Quick pack presets for checked products
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("Set Jumlah:", "Set Qty:"), fontSize = 10.sp, color = TextMutedDark)
                            listOf(1, 2, 3).forEach { packPreset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(CharcoalSurface)
                                        .border(1.dp, CharcoalBorder, RoundedCornerShape(5.dp))
                                        .clickable {
                                            val targets = allProducts.filter { checkedProducts[it.id] == true }
                                                .ifEmpty {
                                                    allProducts.forEach { checkedProducts[it.id] = true }
                                                    allProducts
                                                }
                                            targets.forEach { productPacks[it.id] = packPreset }
                                        }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "$packPreset Pack",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SupabaseGreen
                                    )
                                }
                            }
                        }

                        allProducts.forEach { prod ->
                            val isChecked = checkedProducts[prod.id] == true
                            val packs = productPacks[prod.id] ?: 1
                            val totalPcs = packs * maxOf(1, prod.pieces_per_pack)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isChecked) SupabaseGreen.copy(alpha = 0.10f) else CharcoalSurface)
                                    .border(
                                        1.dp,
                                        if (isChecked) SupabaseGreen else CharcoalBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        val next = !isChecked
                                        checkedProducts[prod.id] = next
                                        if (next && packs <= 0) productPacks[prod.id] = 1
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            val next = !isChecked
                                            checkedProducts[prod.id] = next
                                            if (next && packs <= 0) productPacks[prod.id] = 1
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = SupabaseGreen,
                                            uncheckedColor = TextMutedDark,
                                            checkmarkColor = Color(0xFF042114)
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = prod.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Text(
                                            text = "Rp %,.0f/%s (%d %s)".format(
                                                prod.selling_price_pack,
                                                prod.unit_big,
                                                prod.pieces_per_pack,
                                                prod.unit_small
                                            ),
                                            fontSize = 10.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(CharcoalSurfaceElevated, RoundedCornerShape(5.dp))
                                            .border(1.dp, CharcoalBorder, RoundedCornerShape(5.dp))
                                            .clickable {
                                                if (packs > 1) {
                                                    productPacks[prod.id] = packs - 1
                                                } else {
                                                    checkedProducts[prod.id] = false
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = TextPrimaryDark, modifier = Modifier.size(12.dp))
                                    }

                                    Text(
                                        text = "$packs ${prod.unit_big} ($totalPcs)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChecked) SupabaseGreen else TextSecondaryDark
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(SupabaseGreen.copy(alpha = 0.2f), RoundedCornerShape(5.dp))
                                            .clickable {
                                                if (!isChecked) {
                                                    checkedProducts[prod.id] = true
                                                    productPacks[prod.id] = maxOf(1, packs)
                                                } else {
                                                    productPacks[prod.id] = packs + 1
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val selectedInitialProducts = allProducts.mapNotNull { prod ->
                if (checkedProducts[prod.id] == true) {
                    val pks = (productPacks[prod.id] ?: 1).coerceAtLeast(1)
                    val totalPcs = pks * maxOf(1, prod.pieces_per_pack)
                    Triple(prod.id, totalPcs, null as Double?)
                } else null
            }
            Button(
                onClick = {
                    val finalName = name.trim().ifBlank { defaultAutoName }
                    val lat = latitudeStr.toDoubleOrNull()
                    val lng = longitudeStr.toDoubleOrNull()
                    val order = routeOrder.toIntOrNull() ?: 1
                    onConfirm(
                        finalName,
                        address.trim(),
                        phone.trim(),
                        selectedDay,
                        order,
                        photoUri,
                        lat,
                        lng,
                        selectedInitialProducts
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114))
            ) {
                Text(
                    text = if (selectedInitialProducts.isNotEmpty()) {
                        tr("Simpan Warung + ${selectedInitialProducts.size} Produk", "Save Store + ${selectedInitialProducts.size} Products")
                    } else {
                        tr("Simpan Warung", "Save Store")
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Batal", "Cancel"), color = TextSecondaryDark)
            }
        }
    )

    if (showPhotoChooser) {
        PhotoChooserBottomSheet(
            title = tr("Foto Warung", "Store Photo"),
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
                    tr("Lokasi GPS • ${customer.name}", "GPS Location • ${customer.name}"),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    tr("Multi-GPS (Fused GMaps + Satelit Offline) & Auto Alamat", "Multi-GPS (Fused GMaps + Offline Satellite) & Auto Address"),
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
                    label = { Text(tr("Alamat Warung (Otomatis dari Koordinat)", "Store Address (Auto from Coordinates)")) },
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
                Text(tr("Simpan", "Save"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Batal", "Cancel"), color = TextSecondaryDark)
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
                            contentDescription = tr("Hapus Warung", "Delete Store"),
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
                    listOf(tr("Info Warung", "Store Info"), tr("Harga & Titip Lalu", "Prices & Stock")).forEachIndexed { idx, tabTitle ->
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
                            label = { Text(tr("Nama Warung *", "Store Name *")) },
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

                    // Quick Auto-Naming Chips for existing store (3-Char Unique Code - Lat/Lng)
                    var editUniqueCode by remember(cust.id) {
                        val existingPrefix = cust.name.substringBefore(" - ").trim().uppercase(Locale.ROOT)
                        val initialCode = if (existingPrefix.length == 3 && existingPrefix.all { it.isLetterOrDigit() }) {
                            existingPrefix
                        } else {
                            LocationHelper.generateUniqueWarungCode(listOf(cust.name))
                        }
                        mutableStateOf(initialCode)
                    }
                    val editLatPreset = remember(editUniqueCode, latitudeStr, longitudeStr) {
                        LocationHelper.formatWarungAutoCoordNameFromStr(editUniqueCode, latitudeStr, longitudeStr, useLongitude = false)
                    }
                    val editLngPreset = remember(editUniqueCode, latitudeStr, longitudeStr) {
                        LocationHelper.formatWarungAutoCoordNameFromStr(editUniqueCode, latitudeStr, longitudeStr, useLongitude = true)
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isLatSel = name == editLatPreset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isLatSel) SupabaseGreen else CharcoalSurfaceElevated)
                                    .border(1.dp, if (isLatSel) SupabaseGreen else CharcoalBorder, RoundedCornerShape(6.dp))
                                    .clickable { name = editLatPreset }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$editLatPreset (Lat)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLatSel) Color(0xFF042114) else TextSecondaryDark
                                )
                            }
                        }
                        if (editLngPreset != editLatPreset) {
                            item {
                                val isLngSel = name == editLngPreset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isLngSel) SupabaseGreen else CharcoalSurfaceElevated)
                                        .border(1.dp, if (isLngSel) SupabaseGreen else CharcoalBorder, RoundedCornerShape(6.dp))
                                        .clickable { name = editLngPreset }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$editLngPreset (Lng)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLngSel) Color(0xFF042114) else TextSecondaryDark
                                    )
                                }
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CharcoalSurfaceElevated)
                                    .border(1.dp, SupabaseGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        val nextCode = LocationHelper.generateUniqueWarungCode(listOf(cust.name, editUniqueCode))
                                        editUniqueCode = nextCode
                                        name = LocationHelper.formatWarungAutoCoordNameFromStr(nextCode, latitudeStr, longitudeStr, useLongitude = false)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tr("🔄 Kode Baru ($editUniqueCode)", "🔄 New Code ($editUniqueCode)"),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SupabaseGreen
                                )
                            }
                        }
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
                                value = trDay(selectedDay),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(tr("Hari Rute", "Route Day")) },
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
                                        text = { Text(trDay(day), color = TextPrimaryDark) },
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
                            label = { Text(tr("Urutan", "Order #")) },
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
                        label = { Text(tr("No. WhatsApp", "WhatsApp / Phone")) },
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
                        label = { Text(tr("Alamat (Otomatis dari GPS / Ketik Manual)", "Address (Auto from GPS / Manual)")) },
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
                // Tab 1: Minimalist Harga Khusus & Titip Lalu per Produk (Bulk Checkbox Enabled)
                if (allProducts.isEmpty()) {
                    Text(
                        text = tr("Belum ada produk di katalog.", "No products in catalog yet."),
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                } else {
                    val allActive = allProducts.all { (stockQuantityMap[it.id] ?: 0) > 0 }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bulk Action Bar for Tab 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (allActive) SupabaseGreen else CharcoalSurface)
                                    .border(1.dp, SupabaseGreen, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (allActive) {
                                            allProducts.forEach { p -> stockQuantityMap[p.id] = 0 }
                                        } else {
                                            allProducts.forEach { p ->
                                                if ((stockQuantityMap[p.id] ?: 0) <= 0) {
                                                    stockQuantityMap[p.id] = maxOf(1, p.pieces_per_pack)
                                                }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (allActive) tr("Kosongkan Semua", "Clear All") else tr("Centang Semua", "Select All"),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allActive) Color(0xFF042114) else SupabaseGreen
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf(1, 2, 3).forEach { pCount ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(CharcoalSurface)
                                            .border(1.dp, CharcoalBorder, RoundedCornerShape(5.dp))
                                            .clickable {
                                                val checkedList = allProducts.filter { (stockQuantityMap[it.id] ?: 0) > 0 }
                                                    .ifEmpty { allProducts }
                                                checkedList.forEach { p ->
                                                    stockQuantityMap[p.id] = pCount * maxOf(1, p.pieces_per_pack)
                                                }
                                            }
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Set $pCount Pack",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SupabaseGreen
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allProducts, key = { it.id }) { prod ->
                                val pcsPerPack = maxOf(1, prod.pieces_per_pack)
                                val currentQtyPcs = stockQuantityMap[prod.id] ?: 0
                                val isProdChecked = currentQtyPcs > 0
                                val pricePackStr = customPricePackMap[prod.id] ?: "%.0f".format(prod.selling_price_pack)
                                val parsedPackPrice = pricePackStr.toDoubleOrNull() ?: prod.selling_price_pack
                                val parsedUnitPrice = parsedPackPrice / pcsPerPack
                                val isCustomPrice = kotlin.math.abs(parsedPackPrice - prod.selling_price_pack) >= 0.5

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isProdChecked) SupabaseGreen.copy(alpha = 0.08f) else CharcoalSurfaceElevated,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            when {
                                                isProdChecked -> SupabaseGreen.copy(alpha = 0.7f)
                                                isCustomPrice -> AmberWarning.copy(alpha = 0.45f)
                                                else -> CharcoalBorder
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    // Top line: Checkbox + Nama Produk & Harga Standar / Reset
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    if (isProdChecked) {
                                                        stockQuantityMap[prod.id] = 0
                                                    } else {
                                                        stockQuantityMap[prod.id] = pcsPerPack
                                                    }
                                                }
                                        ) {
                                            Checkbox(
                                                checked = isProdChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        if (currentQtyPcs <= 0) stockQuantityMap[prod.id] = pcsPerPack
                                                    } else {
                                                        stockQuantityMap[prod.id] = 0
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = SupabaseGreen,
                                                    uncheckedColor = TextMutedDark,
                                                    checkmarkColor = Color(0xFF042114)
                                                ),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = prod.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                        }

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
                                            text = tr("Titip Lalu: ${prod.formatPackAndPieces(currentQtyPcs)}", "Consigned: ${prod.formatPackAndPieces(currentQtyPcs)}"),
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
                Text(tr("Simpan", "Save"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Batal", "Cancel"), color = TextSecondaryDark)
            }
        }
    )

    if (showPhotoChooser) {
        PhotoChooserBottomSheet(
            title = tr("Foto Warung: $name", "Store Photo: $name"),
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
                Text(tr("Hapus Warung?", "Delete Store?"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            },
            text = {
                Text(
                    tr("Hapus '${cust.name}' beserta data stok titipannya?", "Delete '${cust.name}' and all its consigned stock records?"),
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
                    Text(tr("Hapus", "Delete"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(tr("Batal", "Cancel"), color = TextSecondaryDark)
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
                            tr("Titik Koordinat GPS", "GPS Coordinates")
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasValidCoords) SupabaseGreen else TextSecondaryDark,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val accBadge = liveAccuracyMeters?.let { " • ±${it.toInt().coerceAtLeast(1)}m" } ?: ""
                val modeBadge = if (isOnline) "Online ($liveProviderLabel$accBadge)" else tr("Offline (Satelit GPS$accBadge)", "Offline (Satellite GPS$accBadge)")
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
                                Toast.makeText(context, tr("Salin koordinat atau link Google Maps lalu tempel", "Copy coordinates or a Google Maps link then paste"), Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(context, tr("Koordinat GMaps berhasil ditempel!", "GMaps coordinates pasted!"), Toast.LENGTH_SHORT).show()
                                    } else {
                                        showManualInputs = true
                                        Toast.makeText(context, tr("Format clipboard bukan koordinat/link GMaps", "Clipboard does not contain valid coordinates or GMaps link"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = tr("Tempel GMaps", "Paste GMaps"),
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
                                contentDescription = tr("Ambil GPS Akurat", "Get Accurate GPS"),
                                tint = SupabaseGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (hasValidCoords) tr("Akurat", "Locked") else tr("Ambil GPS", "Get GPS"),
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
                        text = tr("Mengonversi koordinat ke alamat jalan...", "Converting coordinates to street address..."),
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
                        text = tr("Alamat GPS: $addr", "GPS Address: $addr"),
                        fontSize = 10.5.sp,
                        color = if (isDifferentFromField) TextSecondaryDark else SupabaseGreen,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    if (isDifferentFromField) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("Pakai", "Apply"),
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
                    text = tr("Offline Satelit • Alamat otomatis dikonversi saat koneksi internet aktif", "Offline Satellite • Address will auto-convert when online"),
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

