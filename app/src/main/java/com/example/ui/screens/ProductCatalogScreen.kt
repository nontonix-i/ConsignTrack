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
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
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
                    text = "Katalog Produk",
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
                        text = "${uiState.products.size} Produk",
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

@Composable
private fun ProductCard(
    item: ProductWithStock,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val p = item.product

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Name & Multi-Unit Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = p.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(SupabaseGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, SupabaseGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "1 ${p.unit_big} = ${p.pieces_per_pack} ${p.unit_small}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SupabaseGreen
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondaryDark, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = TextMutedDark, modifier = Modifier.size(17.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Margin Grid: Per Pack and Per Pcs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurfaceElevated, RoundedCornerShape(10.dp))
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Modal", fontSize = 10.5.sp, color = TextMutedDark)
                    Text("Rp %,.0f / %s".format(p.cost_price_pack, p.unit_big), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                    Text("(@ Rp %,.0f / %s)".format(p.cost_price, p.unit_small), fontSize = 10.sp, color = TextMutedDark)
                }
                Column {
                    Text("Harga Jual", fontSize = 10.5.sp, color = TextMutedDark)
                    Text("Rp %,.0f / %s".format(p.selling_price_pack, p.unit_big), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Text("(@ Rp %,.0f / %s)".format(p.selling_price, p.unit_small), fontSize = 10.sp, color = TextMutedDark)
                }
                Column {
                    Text("Laba", fontSize = 10.5.sp, color = TextMutedDark)
                    Text(
                        "+Rp %,.0f".format(item.profitMarginPack),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SupabaseGreen
                    )
                    Text("%.0f%%".format(item.profitMarginPercentage), fontSize = 10.sp, color = SupabaseGreen)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Active Stock Across All Stores in Pack + Pcs
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Stok: ",
                    fontSize = 11.5.sp,
                    color = TextSecondaryDark
                )
                Text(
                    text = p.formatPackAndPieces(item.totalConsignedStock),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
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
    var unitBig by remember { mutableStateOf(product?.unit_big ?: "Pack") }
    var unitSmall by remember { mutableStateOf(product?.unit_small ?: "Pcs") }
    var piecesPerPackStr by remember { mutableStateOf(product?.pieces_per_pack?.toString() ?: "10") }
    var sellingPricePackStr by remember { mutableStateOf(product?.selling_price_pack?.let { "%.0f".format(it) } ?: "16000") }
    var costPricePackStr by remember { mutableStateOf(product?.cost_price_pack?.let { "%.0f".format(it) } ?: "11500") }

    val pieces = piecesPerPackStr.toIntOrNull() ?: 10
    val sellPack = sellingPricePackStr.toDoubleOrNull() ?: 16000.0
    val costPack = costPricePackStr.toDoubleOrNull() ?: 11500.0
    val sellPcs = if (pieces > 0) sellPack / pieces else 0.0
    val costPcs = if (pieces > 0) costPack / pieces else 0.0
    val profitPack = sellPack - costPack

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CharcoalSurface,
        title = {
            Text(
                text = if (product == null) "Tambah Produk" else "Edit Produk",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Produk *") },
                    placeholder = { Text("Kerupuk Kaleng") },
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
                        label = { Text("Satuan Besar") },
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
                        label = { Text("Satuan Kecil") },
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
                        label = { Text("Isi/Pack") },
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
                        label = { Text("Harga Jual / $unitBig") },
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
                        label = { Text("Harga Modal / $unitBig") },
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
                        text = "Eceran: Rp %,.0f / %s".format(sellPcs, unitSmall),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "Laba: Rp %,.0f / %s".format(profitPack, unitBig),
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
