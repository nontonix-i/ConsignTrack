package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.Product
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.SupabaseGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.tr
import com.example.ui.viewmodel.ProductViewModel
import com.example.ui.viewmodel.ProductWithStock

@Composable
fun ProductCatalogScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

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
                Icon(Icons.Default.Add, contentDescription = tr("Tambah Produk", "Add Product"))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tr("Katalog Produk", "Product Catalog"),
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
                        text = tr("${uiState.products.size} Produk", "${uiState.products.size} Products"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SupabaseGreen
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    KerupukRoutePresetBanner(
                        onLoadPreset = { viewModel.seedKerupukBlueprintProducts() }
                    )
                }

                items(uiState.products, key = { it.product.id }) { item ->
                    ProductCard(
                        item = item,
                        onEdit = { editingProduct = item.product },
                        onDelete = { viewModel.deleteProduct(item.product) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ProductMultiUnitDialog(
            product = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, unitSmall, unitBig, perPack, sellPack, costPack ->
                viewModel.saveProduct(0, name, unitSmall, unitBig, perPack, sellPack, costPack)
                showAddDialog = false
            }
        )
    }

    if (editingProduct != null) {
        ProductMultiUnitDialog(
            product = editingProduct,
            onDismiss = { editingProduct = null },
            onConfirm = { name, unitSmall, unitBig, perPack, sellPack, costPack ->
                viewModel.saveProduct(editingProduct!!.id, name, unitSmall, unitBig, perPack, sellPack, costPack)
                editingProduct = null
            }
        )
    }
}

private fun getRecommendedRouteQuota(productName: String): Int? {
    val lower = productName.lowercase()
    return when {
        lower.contains("sb pedas") || lower == "sb" -> 15
        lower.contains("kp original") || lower == "kp" -> 15
        lower.contains("st original") || lower == "st" -> 5
        lower.contains("dd rambak") || lower.contains("rambak") -> 5
        lower.startsWith("ao") -> 8
        lower.startsWith("jk") -> 6
        lower.startsWith("bo") -> 4
        lower.startsWith("mk") -> 2
        else -> null
    }
}

@Composable
private fun KerupukRoutePresetBanner(
    onLoadPreset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SupabaseGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("Paket 8 Varian Kerupuk (60 Bks/Rute)", "8 Kerupuk Variants Preset (60 Packs/Route)"),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SupabaseGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tr(
                            "SB(15) • KP(15) • AO(8) • JK(6) • ST(5) • DD(5) • BO(4) • MK(2) | Modal Rp 11rb → Jual Rp 16rb (Laba Rp 5rb)",
                            "SB(15) • KP(15) • AO(8) • JK(6) • ST(5) • DD(5) • BO(4) • MK(2) | Cost Rp 11k → Sell Rp 16k (Profit Rp 5k)"
                        ),
                        fontSize = 10.5.sp,
                        color = TextSecondaryDark
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onLoadPreset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SupabaseGreen,
                        contentColor = Color(0xFF042114)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tr("⚡ Muat 8 Varian", "⚡ Load 8 Variants"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCard(
    item: ProductWithStock,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val p = item.product
    val routeQuota = getRecommendedRouteQuota(p.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Name & Unit Info + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = p.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        if (routeQuota != null) {
                            Box(
                                modifier = Modifier
                                    .background(SupabaseGreen.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                                    .border(1.dp, SupabaseGreen.copy(alpha = 0.4f), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = tr("Jatah Rute: $routeQuota bks", "Route Quota: $routeQuota pks"),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SupabaseGreen
                                )
                            }
                        }
                    }
                    Text(
                        text = tr(
                            "1 ${p.unit_big} = ${p.pieces_per_pack} ${p.unit_small} • Stok Aktif: ${p.formatPackAndPieces(item.totalConsignedStock)}",
                            "1 ${p.unit_big} = ${p.pieces_per_pack} ${p.unit_small} • Active Stock: ${p.formatPackAndPieces(item.totalConsignedStock)}"
                        ),
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = tr("Edit", "Edit"), tint = SupabaseGreen, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = tr("Hapus", "Delete"), tint = TextMutedDark, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Minimalist Pricing Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(tr("Modal / ${p.unit_big}", "Cost / ${p.unit_big}"), fontSize = 10.sp, color = TextMutedDark)
                    Text("Rp %,.0f".format(p.cost_price_pack), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryDark)
                }
                Column {
                    Text(tr("Jual / ${p.unit_big}", "Sell / ${p.unit_big}"), fontSize = 10.sp, color = TextMutedDark)
                    Text("Rp %,.0f".format(p.selling_price_pack), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(tr("Laba / ${p.unit_big}", "Profit / ${p.unit_big}"), fontSize = 10.sp, color = TextMutedDark)
                    Text(
                        "+Rp %,.0f (%.0f%%)".format(item.profitMarginPack, item.profitMarginPercentage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SupabaseGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductMultiUnitDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, unitSmall: String, unitBig: String, piecesPerPack: Int, sellingPricePack: Double, costPricePack: Double) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var unitBig by remember { mutableStateOf(product?.unit_big ?: "Bks") }
    var unitSmall by remember { mutableStateOf(product?.unit_small ?: "Bks") }
    var piecesPerPackStr by remember { mutableStateOf(product?.pieces_per_pack?.toString() ?: "1") }
    var sellingPricePackStr by remember { mutableStateOf(product?.selling_price_pack?.let { "%.0f".format(it) } ?: "16000") }
    var costPricePackStr by remember { mutableStateOf(product?.cost_price_pack?.let { "%.0f".format(it) } ?: "11000") }

    val pieces = piecesPerPackStr.toIntOrNull() ?: 1
    val sellPack = sellingPricePackStr.toDoubleOrNull() ?: 16000.0
    val costPack = costPricePackStr.toDoubleOrNull() ?: 11000.0
    val sellPcs = if (pieces > 0) sellPack / pieces else 0.0
    val costPcs = if (pieces > 0) costPack / pieces else 0.0
    val profitPack = sellPack - costPack

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
                text = if (product == null) tr("Tambah Produk", "Add Product") else tr("Edit Produk", "Edit Product"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Nama Produk *", "Product Name *")) },
                    placeholder = { Text(tr("Kerupuk Kaleng", "Cassava Chips")) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SupabaseGreen,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Row: Satuan Besar & Satuan Kecil & Isi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unitBig,
                        onValueChange = { unitBig = it },
                        label = { Text(tr("Satuan Besar", "Pack Unit")) },
                        placeholder = { Text("Pack") },
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
                        value = unitSmall,
                        onValueChange = { unitSmall = it },
                        label = { Text(tr("Satuan Kecil", "Small Unit")) },
                        placeholder = { Text("Pcs") },
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
                        value = piecesPerPackStr,
                        onValueChange = { piecesPerPackStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(tr("Isi/Pack", "Pcs/Pack")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        ),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                // Row: Harga Jual & Harga Modal per PACK
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sellingPricePackStr,
                        onValueChange = { sellingPricePackStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(tr("Harga Jual / $unitBig", "Sell Price / $unitBig")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        value = costPricePackStr,
                        onValueChange = { costPricePackStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text(tr("Harga Modal / $unitBig", "Cost Price / $unitBig")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                // Sleek Calculation Summary Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Eceran: Rp %,.0f / %s", "Unit: Rp %,.0f / %s").format(sellPcs, unitSmall),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = tr("Laba: Rp %,.0f / %s", "Profit: Rp %,.0f / %s").format(profitPack, unitBig),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SupabaseGreen
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && sellPack > 0 && pieces > 0) {
                        onConfirm(name.trim(), unitSmall.trim().ifBlank { "Pcs" }, unitBig.trim().ifBlank { "Pack" }, pieces, sellPack, costPack)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SupabaseGreen,
                    contentColor = Color(0xFF042114)
                ),
                shape = RoundedCornerShape(8.dp)
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
