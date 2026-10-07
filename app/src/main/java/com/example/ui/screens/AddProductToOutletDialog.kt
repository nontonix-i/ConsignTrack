package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlin.math.abs

data class BulkProductSelection(
    val product: Product,
    val packs: Int,
    val extraPieces: Int = 0,
    val customPricePack: Double? = null
) {
    val totalPieces: Int
        get() = (packs * maxOf(1, product.pieces_per_pack)) + extraPieces
}

@Composable
fun AddProductToOutletDialog(
    catalogProducts: List<Product>,
    existingItemIds: Set<Long>,
    outletName: String? = null,
    allowExistingProducts: Boolean = false,
    onAddProduct: (Product, Int, Double?) -> Unit = { _, _, _ -> },
    onAddProductsBulk: ((List<BulkProductSelection>) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val availableProducts = remember(catalogProducts, existingItemIds, searchQuery, allowExistingProducts) {
        catalogProducts.filter { product ->
            (allowExistingProducts || !existingItemIds.contains(product.id)) &&
                (searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true))
        }
    }

    // Track checked state, packs, extra pieces, and custom price per product ID
    val checkedMap = remember(catalogProducts) { mutableStateMapOf<Long, Boolean>() }
    val packsMap = remember(catalogProducts) {
        mutableStateMapOf<Long, Int>().apply {
            catalogProducts.forEach { put(it.id, 1) }
        }
    }
    val extraPcsMap = remember(catalogProducts) {
        mutableStateMapOf<Long, Int>().apply {
            catalogProducts.forEach { put(it.id, 0) }
        }
    }
    val customPriceStrMap = remember(catalogProducts) {
        mutableStateMapOf<Long, String>().apply {
            catalogProducts.forEach { put(it.id, "%.0f".format(it.selling_price_pack)) }
        }
    }
    val showCustomPriceMap = remember(catalogProducts) { mutableStateMapOf<Long, Boolean>() }

    val selectedProducts = remember(
        availableProducts,
        checkedMap.toMap(),
        packsMap.toMap(),
        extraPcsMap.toMap(),
        customPriceStrMap.toMap()
    ) {
        catalogProducts.filter { checkedMap[it.id] == true }.mapNotNull { prod ->
            val pks = packsMap[prod.id] ?: 1
            val ext = extraPcsMap[prod.id] ?: 0
            val totalPcs = (pks * maxOf(1, prod.pieces_per_pack)) + ext
            if (totalPcs <= 0) return@mapNotNull null
            val rawPrice = customPriceStrMap[prod.id]?.toDoubleOrNull()
            val customPrice = if (rawPrice != null && rawPrice > 0.0 && abs(rawPrice - prod.selling_price_pack) >= 0.5) {
                rawPrice
            } else null
            BulkProductSelection(
                product = prod,
                packs = pks,
                extraPieces = ext,
                customPricePack = customPrice
            )
        }
    }

    val allVisibleSelected = availableProducts.isNotEmpty() && availableProducts.all { checkedMap[it.id] == true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .systemBarsPadding()
                .imePadding()
                .heightIn(max = 710.dp)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = CharcoalSurface
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!outletName.isNullOrBlank()) {
                                "Bulk Pilih Produk • $outletName"
                            } else {
                                "Bulk Tambah Produk Titipan"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Centang produk yang ingin dititipkan sekaligus & atur jumlahnya",
                            fontSize = 11.sp,
                            color = SupabaseGreen
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar + Select All Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Cari produk...", color = TextMutedDark, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(16.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CharcoalSurfaceElevated,
                            unfocusedContainerColor = CharcoalSurfaceElevated,
                            focusedBorderColor = SupabaseGreen,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    if (availableProducts.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    val target = !allVisibleSelected
                                    availableProducts.forEach { p ->
                                        checkedMap[p.id] = target
                                        if (target && (packsMap[p.id] ?: 0) == 0 && (extraPcsMap[p.id] ?: 0) == 0) {
                                            packsMap[p.id] = 1
                                        }
                                    }
                                }
                                .border(
                                    1.dp,
                                    if (allVisibleSelected) SupabaseGreen else CharcoalBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("bulk_select_all_button"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (allVisibleSelected) SupabaseGreen.copy(alpha = 0.18f) else CharcoalSurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (allVisibleSelected) Icons.Default.CheckBox else Icons.Default.DoneAll,
                                    contentDescription = "Pilih Semua",
                                    tint = if (allVisibleSelected) SupabaseGreen else TextSecondaryDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (allVisibleSelected) "Batal Semua" else "Pilih Semua",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allVisibleSelected) SupabaseGreen else TextPrimaryDark
                                )
                            }
                        }
                    }
                }

                // Quick Bulk Quantity Presets for Checked Products (or checks all if none checked yet)
                if (availableProducts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Text(
                                text = "Isi Cepat:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryDark,
                                modifier = Modifier.padding(top = 5.dp, end = 2.dp)
                            )
                        }
                        items(listOf(1, 2, 3, 5)) { presetPack ->
                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        val targets = availableProducts.filter { checkedMap[it.id] == true }
                                            .ifEmpty {
                                                availableProducts.forEach { checkedMap[it.id] = true }
                                                availableProducts
                                            }
                                        targets.forEach { p ->
                                            packsMap[p.id] = presetPack
                                            extraPcsMap[p.id] = 0
                                        }
                                    }
                                    .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp)),
                                shape = RoundedCornerShape(6.dp),
                                color = CharcoalSurfaceElevated
                            ) {
                                Text(
                                    text = "Semua $presetPack Pack",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SupabaseGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (availableProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (existingItemIds.size >= catalogProducts.size && catalogProducts.isNotEmpty())
                                    "Semua produk katalog sudah ada di daftar warung ini"
                                else
                                    "Produk tidak ditemukan",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        items(availableProducts, key = { it.id }) { product ->
                            val isChecked = checkedMap[product.id] == true
                            val packs = packsMap[product.id] ?: 1
                            val extraPcs = extraPcsMap[product.id] ?: 0
                            val customPriceStr = customPriceStrMap[product.id] ?: "%.0f".format(product.selling_price_pack)
                            val showCustomPrice = showCustomPriceMap[product.id] == true
                            val alreadyInOutlet = existingItemIds.contains(product.id)

                            BulkProductToConsignItem(
                                product = product,
                                isChecked = isChecked,
                                alreadyInOutlet = alreadyInOutlet,
                                packs = packs,
                                extraPcs = extraPcs,
                                customPricePackStr = customPriceStr,
                                showCustomPriceField = showCustomPrice,
                                onToggleChecked = {
                                    val newChecked = !isChecked
                                    checkedMap[product.id] = newChecked
                                    if (newChecked && packs == 0 && extraPcs == 0) {
                                        packsMap[product.id] = 1
                                    }
                                },
                                onPacksChange = { newPacks ->
                                    val safe = newPacks.coerceAtLeast(0)
                                    packsMap[product.id] = safe
                                    if (safe > 0 || (extraPcsMap[product.id] ?: 0) > 0) {
                                        checkedMap[product.id] = true
                                    } else {
                                        checkedMap[product.id] = false
                                    }
                                },
                                onExtraPcsChange = { newExtra ->
                                    val safe = newExtra.coerceAtLeast(0)
                                    extraPcsMap[product.id] = safe
                                    if (safe > 0 || (packsMap[product.id] ?: 0) > 0) {
                                        checkedMap[product.id] = true
                                    } else {
                                        checkedMap[product.id] = false
                                    }
                                },
                                onToggleCustomPrice = {
                                    showCustomPriceMap[product.id] = !showCustomPrice
                                },
                                onCustomPriceChange = { newStr ->
                                    customPriceStrMap[product.id] = newStr.filter { ch -> ch.isDigit() }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = CharcoalBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Summary & Bulk Action Footer
                val totalSelectedProducts = selectedProducts.size
                val totalSelectedPcs = selectedProducts.sumOf { it.totalPieces }
                val totalEstimatedValue = selectedProducts.sumOf { sel ->
                    val packPrice = sel.customPricePack ?: sel.product.selling_price_pack
                    val unitPrice = packPrice / maxOf(1, sel.product.pieces_per_pack)
                    sel.totalPieces * unitPrice
                }

                if (totalSelectedProducts > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SupabaseGreen.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(1.dp, SupabaseGreen.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$totalSelectedProducts Produk Dicentang ($totalSelectedPcs Pcs)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen
                        )
                        Text(
                            text = "Nilai: Rp %,.0f".format(totalEstimatedValue),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.35f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                        border = BorderStroke(1.dp, CharcoalBorder)
                    ) {
                        Text("Batal", fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = {
                            if (selectedProducts.isNotEmpty()) {
                                if (onAddProductsBulk != null) {
                                    onAddProductsBulk(selectedProducts)
                                } else {
                                    selectedProducts.forEach { sel ->
                                        onAddProduct(sel.product, sel.packs.coerceAtLeast(1), sel.customPricePack)
                                    }
                                }
                                onDismiss()
                            }
                        },
                        enabled = selectedProducts.isNotEmpty(),
                        modifier = Modifier
                            .weight(0.65f)
                            .testTag("confirm_bulk_add_products_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SupabaseGreen,
                            contentColor = Color(0xFF042114),
                            disabledContainerColor = CharcoalSurfaceElevated,
                            disabledContentColor = TextMutedDark
                        )
                    ) {
                        Text(
                            text = if (totalSelectedProducts > 0) {
                                "+ Simpan $totalSelectedProducts Produk ($totalSelectedPcs Pcs)"
                            } else {
                                "Centang Produk Dulu"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkProductToConsignItem(
    product: Product,
    isChecked: Boolean,
    alreadyInOutlet: Boolean,
    packs: Int,
    extraPcs: Int,
    customPricePackStr: String,
    showCustomPriceField: Boolean,
    onToggleChecked: () -> Unit,
    onPacksChange: (Int) -> Unit,
    onExtraPcsChange: (Int) -> Unit,
    onToggleCustomPrice: () -> Unit,
    onCustomPriceChange: (String) -> Unit
) {
    val parsedCustomPack = customPricePackStr.toDoubleOrNull()
    val effectivePackPrice = if (showCustomPriceField && parsedCustomPack != null && parsedCustomPack > 0) {
        parsedCustomPack
    } else {
        product.selling_price_pack
    }
    val totalPcs = (packs * maxOf(1, product.pieces_per_pack)) + extraPcs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isChecked) 1.5.dp else 1.dp,
                color = if (isChecked) SupabaseGreen else CharcoalBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onToggleChecked() },
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) SupabaseGreen.copy(alpha = 0.08f) else CharcoalBg
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleChecked() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SupabaseGreen,
                            uncheckedColor = TextMutedDark,
                            checkmarkColor = Color(0xFF042114)
                        ),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = product.name,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (alreadyInOutlet) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(Sudah Ada)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmberWarning
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onToggleCustomPrice() }
                        ) {
                            Text(
                                text = "Rp %,.0f/%s (%d %s)".format(
                                    effectivePackPrice,
                                    product.unit_big,
                                    product.pieces_per_pack,
                                    product.unit_small
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (showCustomPriceField) AmberWarning else TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Ubah Harga",
                                tint = if (showCustomPriceField) AmberWarning else SupabaseGreen,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Pack Stepper on the right (always visible for fast 1-tap adjustment)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            if (isChecked) SupabaseGreen.copy(alpha = 0.6f) else CharcoalBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(CharcoalSurface, RoundedCornerShape(6.dp))
                            .clickable {
                                if (packs > 0) onPacksChange(packs - 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Kurang Pack",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "$packs ${product.unit_big}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isChecked) SupabaseGreen else TextPrimaryDark
                        )
                        Text(
                            text = "$totalPcs ${product.unit_small}",
                            fontSize = 9.5.sp,
                            color = TextMutedDark
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(SupabaseGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .clickable {
                                if (!isChecked) {
                                    onPacksChange(maxOf(1, packs))
                                } else {
                                    onPacksChange(packs + 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Tambah Pack",
                            tint = SupabaseGreen,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Expanded Quantity Presets + Extra Pcs when Checked
            if (isChecked) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Pack Chips (1, 2, 3, 5 Pack)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(1, 2, 3, 5).forEach { pVal ->
                            val isActivePreset = packs == pVal && extraPcs == 0
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (isActivePreset) SupabaseGreen else CharcoalSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isActivePreset) SupabaseGreen else CharcoalBorder,
                                        RoundedCornerShape(5.dp)
                                    )
                                    .clickable {
                                        onPacksChange(pVal)
                                        onExtraPcsChange(0)
                                    }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$pVal ${product.unit_big}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActivePreset) Color(0xFF042114) else TextSecondaryDark
                                )
                            }
                        }
                    }

                    // Extra Eceran Pcs Stepper (+Pcs)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Ecer:",
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(CharcoalSurfaceElevated, RoundedCornerShape(4.dp))
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(4.dp))
                                .clickable { if (extraPcs > 0) onExtraPcsChange(extraPcs - 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                        }
                        Text(
                            text = "+$extraPcs ${product.unit_small}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (extraPcs > 0) AmberWarning else TextMutedDark
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(CharcoalSurfaceElevated, RoundedCornerShape(4.dp))
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(4.dp))
                                .clickable { onExtraPcsChange(extraPcs + 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SupabaseGreen)
                        }
                    }
                }
            }

            if (showCustomPriceField) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customPricePackStr,
                    onValueChange = onCustomPriceChange,
                    label = { Text("Harga Khusus / ${product.unit_big} (Rp)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberWarning,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
