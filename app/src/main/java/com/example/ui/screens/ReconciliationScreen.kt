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
                // Customer Header Info Card
                item {
                    CustomerHeaderCard(
                        customerName = uiState.customer?.name ?: "",
                        routeDay = uiState.customer?.route_day ?: "Senin",
                        routeOrder = uiState.customer?.route_order ?: 1,
                        address = uiState.customer?.address ?: "",
                        phone = uiState.customer?.phone ?: ""
                    )
                }

                // Visit & Shelf Photo Card
                item {
                    VisitPhotoCard(
                        photoUri = uiState.visitPhotoUri,
                        onTakePhoto = { showPhotoChooser = true },
                        onDeletePhoto = { viewModel.updateVisitPhoto(null) }
                    )
                }

                // Section Title: Produk Dititipkan
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Daftar Produk",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${uiState.items.size})",
                                fontSize = 12.sp,
                                color = SupabaseGreen
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Button Performa Toko
                            Surface(
                                modifier = Modifier
                                    .clickable { viewModel.setShowPerformanceDialog(true) }
                                    .border(1.dp, AmberWarning.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                                shape = RoundedCornerShape(6.dp),
                                color = AmberWarning.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Insights,
                                        contentDescription = null,
                                        tint = AmberWarning,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Riwayat",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AmberWarning
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Button + Tambah Produk
                            Surface(
                                modifier = Modifier
                                    .clickable { viewModel.setShowAddProductDialog(true) }
                                    .border(1.dp, SupabaseGreen.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                                shape = RoundedCornerShape(6.dp),
                                color = SupabaseGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = SupabaseGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "+ Produk",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SupabaseGreen
                                    )
                                }
                            }
                        }
                    }
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
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Belum Ada Produk Dititipkan",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Warung ini belum memiliki produk konsinyasi. Silakan tambahkan produk yang ingin dititip ke warung ini.",
                                    fontSize = 11.5.sp,
                                    color = TextSecondaryDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.setShowAddProductDialog(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF042114))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+ Tambah Produk Pertama",
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
            onAddProduct = { product, packs ->
                viewModel.addProductToOutlet(product, packs)
            },
            onDismiss = { viewModel.setShowAddProductDialog(false) }
        )
    }
}

@Composable
private fun VisitPhotoCard(
    photoUri: String?,
    onTakePhoto: () -> Unit,
    onDeletePhoto: () -> Unit
) {
    if (photoUri == null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTakePhoto() }
                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = CharcoalSurfaceElevated
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = SupabaseGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Foto Rak / Toko",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
                Text(
                    text = "+ Ambil Foto",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SupabaseGreen
                )
            }
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            color = CharcoalSurfaceElevated
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onTakePhoto() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                    ) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Foto Rak Toko",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Foto Rak Terlampir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SupabaseGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ganti",
                        fontSize = 11.5.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier
                            .clickable { onTakePhoto() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "Hapus",
                        fontSize = 11.5.sp,
                        color = RoseError,
                        modifier = Modifier
                            .clickable { onDeletePhoto() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerHeaderCard(
    customerName: String,
    routeDay: String,
    routeOrder: Int,
    address: String,
    phone: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customerName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = address,
                        fontSize = 11.5.sp,
                        color = TextSecondaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(SupabaseGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, SupabaseGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = routeDay.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SupabaseGreen
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(6.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "#$routeOrder",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
            }
        }
    }
}

@Composable
private fun ReconciliationItemCard(
    item: ReconciliationItem,
    onRemainingChanged: (Int) -> Unit,
    onAutoSwapChanged: (Boolean) -> Unit,
    onAddedPacksChanged: (Int) -> Unit,
    onAddedExtraPcsChanged: (Int) -> Unit,
    onRemoveProduct: () -> Unit
) {
    val p = item.product
    var showDirectInputFor by remember { mutableStateOf<String?>(null) }
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
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            // Row 1: Nama Produk, Stok Lalu, dan Ringkasan Terjual/Tagihan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = p.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lalu: ${p.formatPackAndPieces(item.previousStock)}",
                        fontSize = 10.5.sp,
                        color = TextSecondaryDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Terjual: ${item.soldQuantity}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.soldQuantity > 0) SupabaseGreen else TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "•",
                        fontSize = 9.sp,
                        color = TextMutedDark
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Rp %,.0f".format(item.subtotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.subtotal > 0) SupabaseGreen else TextMutedDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hapus Produk dari Warung",
                            tint = TextMutedDark,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Minimalist Inline Controls
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
                        modifier = Modifier.padding(start = 2.dp, end = 3.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(CharcoalSurface, RoundedCornerShape(4.dp))
                            .border(0.5.dp, CharcoalBorder, RoundedCornerShape(4.dp))
                            .clickable { if (item.remainingStock > 0) onRemainingChanged(item.remainingStock - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = TextPrimaryDark, modifier = Modifier.size(13.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clickable { showDirectInputFor = "sisa" }
                            .padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.remainingStock} ${p.unit_small}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(CharcoalSurface, RoundedCornerShape(4.dp))
                            .border(0.5.dp, CharcoalBorder, RoundedCornerShape(4.dp))
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
                        modifier = Modifier.padding(start = 2.dp, end = 3.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(CharcoalSurface, RoundedCornerShape(4.dp))
                            .border(0.5.dp, CharcoalBorder, RoundedCornerShape(4.dp))
                            .clickable { if (item.addedPacks > 0) onAddedPacksChanged(item.addedPacks - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = TextPrimaryDark, modifier = Modifier.size(13.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clickable { showDirectInputFor = "titip" }
                            .padding(horizontal = 5.dp),
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
                            .border(0.5.dp, CharcoalBorder, RoundedCornerShape(4.dp))
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
        val isSisa = showDirectInputFor == "sisa"
        val currVal = if (isSisa) item.remainingStock else item.addedPacks
        val unit = if (isSisa) p.unit_small else p.unit_big
        var inputStr by remember { mutableStateOf(currVal.toString()) }

        AlertDialog(
            onDismissRequest = { showDirectInputFor = null },
            containerColor = CharcoalSurface,
            title = {
                Text(
                    text = if (isSisa) "Input Sisa (${p.name})" else "Input Titip Baru (${p.name})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        val quickList = if (isSisa) listOf(0, 1, 2, 5, item.previousStock) else listOf(0, 1, 2, 3, 5)
                        quickList.distinct().forEach { qVal ->
                            Box(
                                modifier = Modifier
                                    .background(CharcoalSurfaceElevated, RoundedCornerShape(4.dp))
                                    .border(1.dp, CharcoalBorder, RoundedCornerShape(4.dp))
                                    .clickable { inputStr = qVal.toString() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("$qVal", fontSize = 11.5.sp, color = SupabaseGreen)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = inputStr.toIntOrNull() ?: 0
                        if (isSisa) onRemainingChanged(maxOf(0, num)) else onAddedPacksChanged(maxOf(0, num))
                        showDirectInputFor = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen, contentColor = Color(0xFF042114)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectInputFor = null }) {
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
                Text("Hapus dari Outlet?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
            },
            text = {
                Text(
                    "Hapus '${p.name}' dari daftar titipan warung ini? Stok titipan produk ini di warung ini akan direset.",
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
                    Text("Hapus", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
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
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pembayaran",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = SupabaseGreen
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Breakdown Rows
            SummaryRow(label = "Barang Terjual", value = "$totalSoldQty Pcs", valueColor = SupabaseGreen)
            if (totalReturned > 0) {
                SummaryRow(label = "Sisa Ditarik", value = "$totalReturned Pcs", valueColor = RoseError)
            }
            if (totalAddedPacks > 0 || totalAddedQty > 0) {
                SummaryRow(label = "Titipan Baru", value = "$totalAddedPacks Pack ($totalAddedQty Pcs)", valueColor = Color(0xFF38BDF8))
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = CharcoalBorder
            )

            // Total Tagihan Besar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Tagihan",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Rp %,.0f".format(totalSoldAmount),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SupabaseGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Uang Diterima
            Text(
                text = "Uang Diterima",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = if (amountPaid == 0.0) "" else "%.0f".format(amountPaid),
                onValueChange = { str ->
                    val clean = str.filter { it.isDigit() }
                    val dbl = clean.toDoubleOrNull() ?: 0.0
                    onAmountPaidChanged(dbl)
                },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Amount Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSetExactAmount,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SupabaseGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("Uang Pas", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }

                QuickChipButton(
                    text = "+50 rb",
                    onClick = { onAmountPaidChanged(amountPaid + 50000.0) },
                    modifier = Modifier.weight(1f)
                )
                QuickChipButton(
                    text = "+100 rb",
                    onClick = { onAmountPaidChanged(amountPaid + 100000.0) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Change or Debt Status
            if (amountPaid > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                changeOrDebt == 0.0 -> SupabaseGreen.copy(alpha = 0.12f)
                                changeOrDebt > 0 -> BlueInfo.copy(alpha = 0.12f)
                                else -> AmberWarning.copy(alpha = 0.12f)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            changeOrDebt == 0.0 -> "Status:"
                            changeOrDebt > 0 -> "Kembalian:"
                            else -> "Kurang:"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            changeOrDebt == 0.0 -> SupabaseGreen
                            changeOrDebt > 0 -> BlueInfo
                            else -> AmberWarning
                        }
                    )
                    Text(
                        text = when {
                            changeOrDebt == 0.0 -> "LUNAS"
                            changeOrDebt > 0 -> "Rp %,.0f".format(changeOrDebt)
                            else -> "Rp %,.0f".format(-changeOrDebt)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            changeOrDebt == 0.0 -> SupabaseGreen
                            changeOrDebt > 0 -> BlueInfo
                            else -> AmberWarning
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                label = { Text("Catatan") },
                maxLines = 2,
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 11.5.sp)
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimaryDark
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.5.sp, color = TextSecondaryDark)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
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
