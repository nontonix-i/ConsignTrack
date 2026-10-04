package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.ReconciliationItem
import com.example.util.PhotoChooserBottomSheet
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueInfo
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.RoseError
import com.example.ui.theme.SupabaseGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.ReconciliationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    customerId: Long,
    viewModel: ReconciliationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPhotoChooser by remember { mutableStateOf(false) }
    var showEditWarungInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customerId) {
        viewModel.loadCustomer(customerId)
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CharcoalBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kunjungan Warung",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        if (!uiState.customer?.name.isNullOrBlank()) {
                            Text(
                                text = uiState.customer!!.name,
                                fontSize = 12.sp,
                                color = SupabaseGreen
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setShowPerformanceDialog(true) }) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = "Statistik & Riwayat Warung",
                            tint = AmberWarning
                        )
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalBg
                )
            )
        },
        bottomBar = {
            // Persistent Bottom Bar for Action
            SurfaceBottomBar(
                isSaving = uiState.isSaving,
                saveSuccess = uiState.saveSuccess,
                totalSoldAmount = uiState.totalSoldAmount,
                onSaveAndPrint = {
                    viewModel.saveTransaction(openReceiptAfterSave = true)
                },
                onSaveOnly = {
                    viewModel.saveTransaction(openReceiptAfterSave = false) {
                        onNavigateBack()
                    }
                },
                onOpenReceipt = {
                    viewModel.showReceiptModal()
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SupabaseGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Combined Minimalist Header Bar: Warung Info + Foto Rak + Tambah Produk
                item {
                    MinimalistVisitHeaderBar(
                        routeDay = uiState.customer?.route_day ?: "Senin",
                        routeOrder = uiState.customer?.route_order ?: 1,
                        address = uiState.customer?.address ?: "",
                        photoUri = uiState.visitPhotoUri,
                        itemCount = uiState.items.size,
                        onEditCustomer = { showEditWarungInfoDialog = true },
                        onTakePhoto = { showPhotoChooser = true },
                        onAddProduct = { viewModel.setShowAddProductDialog(true) }
                    )
                }

                // Empty state if outlet has no consignment products yet
                if (uiState.items.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp)),
                            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Belum Ada Produk Dititipkan",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.setShowAddProductDialog(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF042114))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tambah Produk",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF042114)
                                    )
                                }
                            }
                        }
                    }
                }

                // List of items
                items(uiState.items, key = { it.product.id }) { item ->
                    ReconciliationItemCard(
                        item = item,
                        onPreviousStockChanged = { prevQty -> viewModel.updatePreviousStock(item.product.id, prevQty) },
                        onCustomPricePackChanged = { customPack -> viewModel.updateCustomPricePack(item.product.id, customPack) },
                        onRemainingChanged = { qty -> viewModel.updateRemainingStock(item.product.id, qty) },
                        onAutoSwapChanged = { isAutoSwap -> viewModel.updateAutoSwapReturned(item.product.id, isAutoSwap) },
                        onAddedPacksChanged = { packs -> viewModel.updateAddedPacks(item.product.id, packs) },
                        onAddedExtraPcsChanged = { extra -> viewModel.updateAddedPiecesExtra(item.product.id, extra) },
                        onRemoveProduct = { viewModel.removeProductFromOutlet(item.product.id) }
                    )
                }

                // Settlement & Payment Card
                item {
                    SettlementCard(
                        totalSoldQty = uiState.totalSoldQuantity,
                        totalSoldAmount = uiState.totalSoldAmount,
                        totalReturned = uiState.totalReturnedQuantity,
                        totalAddedPacks = uiState.totalAddedPacks,
                        totalAddedQty = uiState.totalAddedQuantity,
                        amountPaid = uiState.amountPaid,
                        changeOrDebt = uiState.changeOrDebt,
                        notes = uiState.notes,
                        onAmountPaidChanged = { viewModel.updateAmountPaid(it) },
                        onSetExactAmount = { viewModel.setAmountPaidExact() },
                        onNotesChanged = { viewModel.updateNotes(it) }
                    )
                }
            }
        }
    }

    // Receipt Thermal Dialog
    if (uiState.showReceiptModal && uiState.generatedReceipt != null) {
        ReceiptPreviewDialog(
            receipt = uiState.generatedReceipt!!,
            pairedPrinters = uiState.pairedPrinters,
            selectedPrinterAddress = uiState.selectedPrinterAddress,
            isPrinting = uiState.isPrinting,
            printMessage = uiState.printMessage,
            onSelectPrinter = { viewModel.selectPrinter(it) },
            onRefreshPrinters = { viewModel.refreshPairedPrinters() },
            onPrint = { is80mm -> viewModel.printCurrentReceipt(is80mm) },
            onDismiss = { viewModel.dismissReceiptModal() }
        )
    }

    if (showPhotoChooser) {
        PhotoChooserBottomSheet(
            title = "Foto Kunjungan / Rak Warung",
            hasExistingPhoto = uiState.visitPhotoUri != null,
            onPhotoSelected = { uri ->
                viewModel.updateVisitPhoto(uri)
                showPhotoChooser = false
            },
            onDeletePhoto = {
                viewModel.updateVisitPhoto(null)
                showPhotoChooser = false
            },
            onDismiss = { showPhotoChooser = false }
        )
    }

    // Performance & History Dialog
    if (uiState.showPerformanceDialog) {
        CustomerPerformanceDialog(
            performance = uiState.performance,
            customerName = uiState.customer?.name ?: "Warung",
            onDismiss = { viewModel.setShowPerformanceDialog(false) }
        )
    }

    // Add Product To Outlet Dialog
    if (uiState.showAddProductDialog) {
        AddProductToOutletDialog(
            catalogProducts = uiState.catalogProducts,
            existingItemIds = uiState.items.map { it.product.id }.toSet(),
            onAddProduct = { product, packs, customPricePack ->
                viewModel.addProductToOutlet(product, packs, customPricePack)
            },
            onDismiss = { viewModel.setShowAddProductDialog(false) }
        )
    }

    // Quick Edit Warung Info Dialog from Visit Screen
    if (showEditWarungInfoDialog && uiState.customer != null) {
        val currentCust = uiState.customer!!
        var editName by remember(currentCust) { mutableStateOf(currentCust.name) }
        var editAddress by remember(currentCust) { mutableStateOf(currentCust.address) }
        var editPhone by remember(currentCust) { mutableStateOf(currentCust.phone) }
        var editRouteDay by remember(currentCust) { mutableStateOf(currentCust.route_day) }
        var editRouteOrder by remember(currentCust) { mutableStateOf(currentCust.route_order.toString()) }

        AlertDialog(
            onDismissRequest = { showEditWarungInfoDialog = false },
            containerColor = CharcoalSurface,
            title = {
                Text(
                    text = "Edit Info Warung",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama Warung") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editRouteDay,
                            onValueChange = { editRouteDay = it },
                            label = { Text("Hari Rute") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SupabaseGreen,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(0.6f)
                        )
                        OutlinedTextField(
                            value = editRouteOrder,
                            onValueChange = { editRouteOrder = it.filter { ch -> ch.isDigit() } },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = { Text("No. WA") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SupabaseGreen,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(0.48f)
                        )
                        OutlinedTextField(
                            value = editAddress,
                            onValueChange = { editAddress = it },
                            label = { Text("Alamat") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SupabaseGreen,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            modifier = Modifier.weight(0.52f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateCustomerInfo(
                                currentCust.copy(
                                    name = editName.trim(),
                                    address = editAddress.trim(),
                                    phone = editPhone.trim(),
                                    route_day = editRouteDay.trim().ifBlank { currentCust.route_day },
                                    route_order = editRouteOrder.toIntOrNull() ?: currentCust.route_order
                                )
                            )
                            showEditWarungInfoDialog = false
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114))
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWarungInfoDialog = false }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
private fun MinimalistVisitHeaderBar(
    routeDay: String,
    routeOrder: Int,
    address: String,
    photoUri: String?,
    itemCount: Int,
    onEditCustomer: () -> Unit,
    onTakePhoto: () -> Unit,
    onAddProduct: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalSurface, RoundedCornerShape(10.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Route Badge + Address + Edit Tap
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onEditCustomer() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(SupabaseGreen.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "#$routeOrder • $routeDay",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SupabaseGreen
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = address.ifBlank { "Info Warung" },
                fontSize = 11.5.sp,
                color = TextSecondaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit Info",
                tint = TextMutedDark,
                modifier = Modifier.size(12.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Compact Action Pills (Foto Rak & + Produk)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Foto Rak Chip
            Box(
                modifier = Modifier
                    .background(
                        if (photoUri != null) SupabaseGreen.copy(alpha = 0.15f) else CharcoalSurfaceElevated,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        if (photoUri != null) SupabaseGreen.copy(alpha = 0.4f) else CharcoalBorder,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onTakePhoto() }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Foto Rak",
                        tint = if (photoUri != null) SupabaseGreen else TextSecondaryDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (photoUri != null) "Foto ✓" else "Foto",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (photoUri != null) SupabaseGreen else TextSecondaryDark
                    )
                }
            }

            // + Produk Chip
            Box(
                modifier = Modifier
                    .background(SupabaseGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .border(1.dp, SupabaseGreen.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .clickable { onAddProduct() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = SupabaseGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Produk ($itemCount)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SupabaseGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ReconciliationItemCard(
    item: ReconciliationItem,
    onPreviousStockChanged: (Int) -> Unit,
    onCustomPricePackChanged: (Double?) -> Unit,
    onRemainingChanged: (Int) -> Unit,
    onAutoSwapChanged: (Boolean) -> Unit,
    onAddedPacksChanged: (Int) -> Unit,
    onAddedExtraPcsChanged: (Int) -> Unit,
    onRemoveProduct: () -> Unit
) {
    val p = item.product
    var showDirectInputFor by remember { mutableStateOf<String?>(null) }
    var showPriceEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // ROW 1: Nama Produk + Chip Harga Warung + Chip Titip Lalu (Left) | Subtotal & Laku (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = p.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Clickable Price Pill (Warung Custom or Standard)
                        Row(
                            modifier = Modifier
                                .background(
                                    if (item.hasCustomPrice) AmberWarning.copy(alpha = 0.14f) else CharcoalSurfaceElevated,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { showPriceEditDialog = true }
                                .padding(horizontal = 5.dp, vertical = 1.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rp %,.0f/%s".format(item.effectivePricePack, p.unit_big),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.hasCustomPrice) AmberWarning else TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Ubah Harga",
                                tint = if (item.hasCustomPrice) AmberWarning else TextMutedDark,
                                modifier = Modifier.size(9.dp)
                            )
                        }

                        // Clickable Titip Lalu Pill
                        Row(
                            modifier = Modifier
                                .background(CharcoalSurfaceElevated, RoundedCornerShape(4.dp))
                                .clickable { showDirectInputFor = "lalu" }
                                .padding(horizontal = 5.dp, vertical = 1.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lalu: ${p.formatPackAndPieces(item.previousStock)}",
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Lalu",
                                tint = SupabaseGreen,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }

                // Right: Laku & Subtotal + Remove
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Rp %,.0f".format(item.subtotal),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.subtotal > 0) SupabaseGreen else TextMutedDark
                        )
                        Text(
                            text = "Laku ${item.soldQuantity} ${p.unit_small}",
                            fontSize = 10.sp,
                            color = if (item.soldQuantity > 0) SupabaseGreen else TextMutedDark
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hapus",
                            tint = TextMutedDark,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // ROW 2: Minimalist Inline Controls (Sisa, Titip Baru, Tukar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stepper Sisa
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Sisa:",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(start = 3.dp, end = 3.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(CharcoalSurface, RoundedCornerShape(4.dp))
                            .clickable { if (item.remainingStock > 0) onRemainingChanged(item.remainingStock - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = TextPrimaryDark, modifier = Modifier.size(13.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clickable { showDirectInputFor = "sisa" }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.remainingStock}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(CharcoalSurface, RoundedCornerShape(4.dp))
                            .clickable { onRemainingChanged(item.remainingStock + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = TextPrimaryDark, modifier = Modifier.size(13.dp))
                    }
                }

                // Stepper Titip Baru
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Titip:",
                        fontSize = 11.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(start = 3.dp, end = 3.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(CharcoalSurface, RoundedCornerShape(4.dp))
                            .clickable { if (item.addedPacks > 0) onAddedPacksChanged(item.addedPacks - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = TextPrimaryDark, modifier = Modifier.size(13.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clickable { showDirectInputFor = "titip" }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.addedPacks} ${p.unit_big}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(CharcoalSurface, RoundedCornerShape(4.dp))
                            .clickable { onAddedPacksChanged(item.addedPacks + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = TextPrimaryDark, modifier = Modifier.size(13.dp))
                    }
                }

                // Tukar Sisa Chip Toggle
                Box(
                    modifier = Modifier
                        .background(
                            if (item.isAutoSwapReturned) SupabaseGreen.copy(alpha = 0.15f) else CharcoalSurfaceElevated,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (item.isAutoSwapReturned) SupabaseGreen else CharcoalBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onAutoSwapChanged(!item.isAutoSwapReturned) }
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = if (item.isAutoSwapReturned) SupabaseGreen else TextMutedDark,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Tukar",
                            fontSize = 10.5.sp,
                            fontWeight = if (item.isAutoSwapReturned) FontWeight.Bold else FontWeight.Normal,
                            color = if (item.isAutoSwapReturned) SupabaseGreen else TextSecondaryDark
                        )
                    }
                }
            }
        }
    }

    if (showDirectInputFor != null) {
        val mode = showDirectInputFor
        val isSisa = mode == "sisa"
        val isLalu = mode == "lalu"
        val currVal = when {
            isSisa -> item.remainingStock
            isLalu -> item.previousStock
            else -> item.addedPacks
        }
        val unit = if (isSisa || isLalu) p.unit_small else p.unit_big
        var inputStr by remember(mode) { mutableStateOf(currVal.toString()) }

        AlertDialog(
            onDismissRequest = { showDirectInputFor = null },
            containerColor = CharcoalSurface,
            title = {
                Text(
                    text = when {
                        isSisa -> "Sisa • ${p.name}"
                        isLalu -> "Titip Lalu • ${p.name}"
                        else -> "Titip Baru • ${p.name}"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputStr,
                        onValueChange = { inputStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Jumlah ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickList = when {
                            isSisa -> listOf(0, 1, 2, 5, item.previousStock)
                            isLalu -> listOf(
                                0,
                                p.pieces_per_pack,
                                p.pieces_per_pack * 2,
                                p.pieces_per_pack * 3
                            )
                            else -> listOf(0, 1, 2, 3, 5)
                        }
                        quickList.distinct().forEach { qVal ->
                            Box(
                                modifier = Modifier
                                    .background(CharcoalSurfaceElevated, RoundedCornerShape(6.dp))
                                    .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                                    .clickable { inputStr = qVal.toString() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isLalu && qVal > 0) "${qVal / maxOf(1, p.pieces_per_pack)}${p.unit_big.take(1)} ($qVal)" else "$qVal",
                                    fontSize = 11.sp,
                                    color = SupabaseGreen
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = inputStr.toIntOrNull() ?: 0
                        when {
                            isSisa -> onRemainingChanged(maxOf(0, num))
                            isLalu -> onPreviousStockChanged(maxOf(0, num))
                            else -> onAddedPacksChanged(maxOf(0, num))
                        }
                        showDirectInputFor = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectInputFor = null }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            }
        )
    }

    if (showPriceEditDialog) {
        var packPriceStr by remember { mutableStateOf("%.0f".format(item.effectivePricePack)) }
        var unitPriceStr by remember { mutableStateOf("%.0f".format(item.effectivePriceUnit)) }
        val pcsPerPack = maxOf(1, p.pieces_per_pack)

        AlertDialog(
            onDismissRequest = { showPriceEditDialog = false },
            containerColor = CharcoalSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Harga Warung • ${p.name}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1
                        )
                        Text(
                            text = "Standar: Rp %,.0f/%s".format(p.selling_price_pack, p.unit_big),
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                    Text(
                        text = "Reset",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SupabaseGreen,
                        modifier = Modifier
                            .clickable {
                                packPriceStr = "%.0f".format(p.selling_price_pack)
                                unitPriceStr = "%.0f".format(p.selling_price)
                            }
                            .padding(4.dp)
                    )
                }
            },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = packPriceStr,
                        onValueChange = { str ->
                            val clean = str.filter { it.isDigit() }
                            packPriceStr = clean
                            val packVal = clean.toDoubleOrNull() ?: 0.0
                            unitPriceStr = "%.0f".format(packVal / pcsPerPack)
                        },
                        label = { Text("Per ${p.unit_big} (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberWarning,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unitPriceStr,
                        onValueChange = { str ->
                            val clean = str.filter { it.isDigit() }
                            unitPriceStr = clean
                            val unitVal = clean.toDoubleOrNull() ?: 0.0
                            packPriceStr = "%.0f".format(unitVal * pcsPerPack)
                        },
                        label = { Text("Per ${p.unit_small} (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberWarning,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPackPrice = packPriceStr.toDoubleOrNull()
                        if (newPackPrice == null || kotlin.math.abs(newPackPrice - p.selling_price_pack) < 0.5) {
                            onCustomPricePackChanged(null)
                        } else {
                            onCustomPricePackChanged(newPackPrice)
                        }
                        showPriceEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPriceEditDialog = false }) {
                    Text("Batal", color = TextSecondaryDark)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = CharcoalSurface,
            title = {
                Text("Hapus Produk?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            },
            text = {
                Text(
                    "Hapus '${p.name}' dari daftar titipan warung ini?",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onRemoveProduct()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Hapus", fontSize = 12.sp, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", fontSize = 12.sp, color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
private fun SettlementCard(
    totalSoldQty: Int,
    totalSoldAmount: Double,
    totalReturned: Int,
    totalAddedPacks: Int,
    totalAddedQty: Int,
    amountPaid: Double,
    changeOrDebt: Double,
    notes: String,
    onAmountPaidChanged: (Double) -> Unit,
    onSetExactAmount: () -> Unit,
    onNotesChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Compact Top Summary Row: Terjual, Titip Baru, Retur, & Total Tagihan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Tagihan",
                        fontSize = 11.5.sp,
                        color = TextSecondaryDark
                    )
                    Text(
                        text = "Laku $totalSoldQty pcs • Titip +$totalAddedPacks pack" +
                                (if (totalReturned > 0) " • Retur $totalReturned" else ""),
                        fontSize = 10.5.sp,
                        color = TextMutedDark
                    )
                }
                Text(
                    text = "Rp %,.0f".format(totalSoldAmount),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = SupabaseGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Uang Diterima + Quick Buttons
            OutlinedTextField(
                value = if (amountPaid == 0.0) "" else "%.0f".format(amountPaid),
                onValueChange = { str ->
                    val clean = str.filter { it.isDigit() }
                    onAmountPaidChanged(clean.toDoubleOrNull() ?: 0.0)
                },
                label = { Text("Uang Diterima (Rp)") },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                trailingIcon = {
                    TextButton(
                        onClick = onSetExactAmount,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Uang Pas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SupabaseGreen)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SupabaseGreen,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = CharcoalSurfaceElevated,
                    unfocusedContainerColor = CharcoalSurfaceElevated
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Amount Pills + Status Kembalian/Kurang in 1 row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickChipButton(
                        text = "+20 rb",
                        onClick = { onAmountPaidChanged(amountPaid + 20000.0) }
                    )
                    QuickChipButton(
                        text = "+50 rb",
                        onClick = { onAmountPaidChanged(amountPaid + 50000.0) }
                    )
                    QuickChipButton(
                        text = "+100 rb",
                        onClick = { onAmountPaidChanged(amountPaid + 100000.0) }
                    )
                }

                if (amountPaid > 0) {
                    val statusColor = when {
                        changeOrDebt == 0.0 -> SupabaseGreen
                        changeOrDebt > 0 -> BlueInfo
                        else -> AmberWarning
                    }
                    val statusText = when {
                        changeOrDebt == 0.0 -> "LUNAS"
                        changeOrDebt > 0 -> "Kembali Rp %,.0f".format(changeOrDebt)
                        else -> "Kurang Rp %,.0f".format(-changeOrDebt)
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Notes (Minimalist Single-line)
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                label = { Text("Catatan (Opsional)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SupabaseGreen,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = CharcoalSurfaceElevated,
                    unfocusedContainerColor = CharcoalSurfaceElevated
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuickChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CharcoalSurfaceElevated, RoundedCornerShape(6.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, color = TextPrimaryDark)
    }
}

@Composable
private fun SurfaceBottomBar(
    isSaving: Boolean,
    saveSuccess: Boolean,
    totalSoldAmount: Double,
    onSaveAndPrint: () -> Unit,
    onSaveOnly: () -> Unit,
    onOpenReceipt: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder)
            .navigationBarsPadding()
            .imePadding(),
        color = CharcoalSurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (saveSuccess) {
                Button(
                    onClick = onOpenReceipt,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SupabaseGreen,
                        contentColor = Color(0xFF042114)
                    )
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak Struk", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onSaveOnly,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
                    enabled = !isSaving
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan", fontSize = 12.sp)
                }

                Button(
                    onClick = onSaveAndPrint,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SupabaseGreen,
                        contentColor = Color(0xFF042114)
                    ),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF042114),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan & Cetak", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
