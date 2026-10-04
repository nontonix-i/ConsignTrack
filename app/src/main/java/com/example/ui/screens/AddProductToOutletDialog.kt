package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@Composable
fun AddProductToOutletDialog(
    catalogProducts: List<Product>,
    existingItemIds: Set<Long>,
    onAddProduct: (Product, Int, Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val availableProducts = remember(catalogProducts, existingItemIds, searchQuery) {
        catalogProducts.filter { product ->
            !existingItemIds.contains(product.id) &&
                    (searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .systemBarsPadding()
                .imePadding()
                .heightIn(max = 620.dp)
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
                    Text(
                        text = "Tambah Produk Titipan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari produk...", color = TextMutedDark, fontSize = 12.5.sp) },
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

                Spacer(modifier = Modifier.height(10.dp))

                if (availableProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (existingItemIds.size >= catalogProducts.size && catalogProducts.isNotEmpty())
                                    "Semua produk sudah dititipkan"
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableProducts, key = { it.id }) { product ->
                            ProductToConsignItem(
                                product = product,
                                onAdd = { packs, customPricePack ->
                                    onAddProduct(product, packs, customPricePack)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CharcoalSurfaceElevated,
                        contentColor = TextPrimaryDark
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                ) {
                    Text("Tutup", fontSize = 12.5.sp)
                }
            }
        }
    }
}

@Composable
private fun ProductToConsignItem(
    product: Product,
    onAdd: (Int, Double?) -> Unit
) {
    var initialPacks by remember { mutableIntStateOf(1) }
    var showCustomPriceField by remember { mutableStateOf(false) }
    var customPricePackStr by remember { mutableStateOf("%.0f".format(product.selling_price_pack)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalBg),
        shape = RoundedCornerShape(8.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val parsedCustomPack = customPricePackStr.toDoubleOrNull()
                    val effectivePackPrice = if (showCustomPriceField && parsedCustomPack != null && parsedCustomPack > 0) {
                        parsedCustomPack
                    } else {
                        product.selling_price_pack
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showCustomPriceField = !showCustomPriceField }
                    ) {
                        Text(
                            text = "Rp %,.0f/%s".format(effectivePackPrice, product.unit_big),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showCustomPriceField) AmberWarning else TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Ubah Harga",
                            tint = if (showCustomPriceField) AmberWarning else SupabaseGreen,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Stepper Packs + Add Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(6.dp))
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(CharcoalSurface, RoundedCornerShape(4.dp))
                                .clickable { if (initialPacks > 1) initialPacks-- },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = TextPrimaryDark, modifier = Modifier.size(12.dp))
                        }

                        Text(
                            text = "$initialPacks ${product.unit_big}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SupabaseGreen,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(CharcoalSurface, RoundedCornerShape(4.dp))
                                .clickable { initialPacks++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah", tint = TextPrimaryDark, modifier = Modifier.size(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            val customPack = if (showCustomPriceField) {
                                customPricePackStr.toDoubleOrNull()?.takeIf { it > 0.0 && kotlin.math.abs(it - product.selling_price_pack) >= 0.5 }
                            } else null
                            onAdd(initialPacks, customPack)
                        },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SupabaseGreen),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "+ Titip",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF042114)
                        )
                    }
                }
            }

            if (showCustomPriceField) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = customPricePackStr,
                    onValueChange = { customPricePackStr = it.filter { ch -> ch.isDigit() } },
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
