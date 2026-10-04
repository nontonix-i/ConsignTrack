package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.ReceiptData
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.SupabaseGreen
import com.example.ui.theme.SupabaseGreenDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.util.thermal.EscPosHelper
import com.example.util.thermal.PairedPrinter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewDialog(
    receipt: ReceiptData,
    pairedPrinters: List<PairedPrinter>,
    selectedPrinterAddress: String?,
    isPrinting: Boolean,
    printMessage: String?,
    onSelectPrinter: (String) -> Unit,
    onRefreshPrinters: () -> Unit,
    onPrint: (is80mm: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var is80mm by remember { mutableStateOf(false) }
    var printerDropdownExpanded by remember { mutableStateOf(false) }

    val formattedReceiptText = remember(receipt, is80mm) {
        EscPosHelper.generateMonospaceReceipt(receipt, is80mm)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .heightIn(max = 720.dp)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = CharcoalSurface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bukti Transaksi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Nota #TRX-${receipt.headerId} • ${receipt.customerName}",
                            fontSize = 12.sp,
                            color = TextSecondaryDark
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Paper Width Mode Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Format:",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                    FilterChip(
                        selected = !is80mm,
                        onClick = { is80mm = false },
                        label = { Text("58mm") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SupabaseGreen.copy(alpha = 0.2f),
                            selectedLabelColor = SupabaseGreen,
                            containerColor = CharcoalSurfaceElevated,
                            labelColor = TextSecondaryDark
                        )
                    )
                    FilterChip(
                        selected = is80mm,
                        onClick = { is80mm = true },
                        label = { Text("80mm") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SupabaseGreen.copy(alpha = 0.2f),
                            selectedLabelColor = SupabaseGreen,
                            containerColor = CharcoalSurfaceElevated,
                            labelColor = TextSecondaryDark
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Receipt Monospace Preview Box (Styled like actual thermal paper)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1115), RoundedCornerShape(8.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = formattedReceiptText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bluetooth Printer Selection
                Text(
                    text = "Printer:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = printerDropdownExpanded,
                        onExpandedChange = { printerDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        val selectedName = pairedPrinters.find { it.address == selectedPrinterAddress }?.name
                            ?: if (pairedPrinters.isEmpty()) "Printer belum terhubung" else "Pilih Printer..."

                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = printerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SupabaseGreen,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark,
                                focusedContainerColor = CharcoalSurfaceElevated,
                                unfocusedContainerColor = CharcoalSurfaceElevated
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = printerDropdownExpanded,
                            onDismissRequest = { printerDropdownExpanded = false },
                            modifier = Modifier.background(CharcoalSurfaceElevated)
                        ) {
                            if (pairedPrinters.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Printer belum terhubung", color = TextMutedDark) },
                                    onClick = { printerDropdownExpanded = false }
                                )
                            } else {
                                pairedPrinters.forEach { printer ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(printer.name, color = TextPrimaryDark, fontWeight = FontWeight.Medium)
                                                Text(printer.address, color = TextSecondaryDark, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            onSelectPrinter(printer.address)
                                            printerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onRefreshPrinters,
                        modifier = Modifier
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SupabaseGreen)
                    }
                }

                if (!printMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = printMessage,
                        fontSize = 12.sp,
                        color = if (printMessage.contains("Berhasil")) SupabaseGreen else Color(0xFFF87171)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Print & Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Struk Konsinyasi", formattedReceiptText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Teks struk berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimaryDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Nota", fontSize = 13.sp)
                    }

                    Button(
                        onClick = { onPrint(is80mm) },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SupabaseGreen,
                            contentColor = Color(0xFF042114)
                        ),
                        enabled = !isPrinting
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF042114),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mencetak...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak Struk", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
