package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.AppLanguage
import com.example.util.BackupRestoreManager

/**
 * Minimalist & Useful Backup/Restore Modal Dialog (Export/Import .ZIP + All Photos)
 */
@Composable
fun BackupRestoreDialog(
    viewModel: AnalyticsViewModel,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.refreshBackupStats()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = CharcoalSurface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = SupabaseGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Backup & Restore (.ZIP)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Database Lengkap + Semua Foto Warung & Kunjungan",
                                fontSize = 10.5.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                BackupRestoreSectionContent(
                    viewModel = viewModel,
                    showHeader = false
                )
            }
        }
    }
}

/**
 * Minimalist Card for the Menu Drawer that embeds all ZIP Export, Share, Import & Reset controls.
 */
@Composable
fun BackupRestoreDrawerCard(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        viewModel.refreshBackupStats()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            BackupRestoreSectionContent(
                viewModel = viewModel,
                showHeader = true
            )
        }
    }
}

@Composable
fun BackupRestoreSectionContent(
    viewModel: AnalyticsViewModel,
    showHeader: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEn = uiState.language == AppLanguage.EN
    val stats = uiState.currentBackupStats
    val context = LocalContext.current
    var showConfirmClearAll by remember { mutableStateOf(false) }

    // SAF Launcher: Save .zip file to device storage / Downloads / Drive
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { targetUri ->
        if (targetUri != null) {
            viewModel.exportBackupToUri(targetUri) { res ->
                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF Launcher: Open .zip file from device storage / Downloads / WhatsApp
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { sourceUri ->
        if (sourceUri != null) {
            viewModel.importBackupFromUri(sourceUri) { res ->
                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = SupabaseGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "BACKUP & RESTORE (.ZIP + PHOTOS)" else "BACKUP & RESTORE (.ZIP + FOTO)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                        color = SupabaseGreen
                    )
                }
            }
        }

        // Minimalist Live Database + Photo Summary Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CharcoalBg, RoundedCornerShape(8.dp))
                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniBackupMetric(label = if (isEn) "Stores" else "Warung", count = stats.customerCount)
            MiniBackupMetric(label = if (isEn) "Products" else "Produk", count = stats.productCount)
            MiniBackupMetric(label = if (isEn) "Visits" else "Transaksi", count = stats.transactionCount)
            MiniBackupMetric(label = if (isEn) "Cash" else "Kas", count = stats.financialCount)
            MiniBackupMetric(label = if (isEn) "Photos" else "Foto", count = stats.photoCount, highlight = true)
        }

        // 1. Export / Backup Section (Save ZIP + Share ZIP)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isEn) "Export Backup (Database + Photos)" else "Export Backup (Data + Semua Foto)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        exportZipLauncher.launch(BackupRestoreManager.generateDefaultBackupFileName())
                    },
                    enabled = !uiState.isBackupInProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_export_backup_zip"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SupabaseGreen,
                        contentColor = Color(0xFF042114)
                    )
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Save .ZIP" else "Simpan .ZIP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.shareBackupZip() },
                    enabled = !uiState.isBackupInProgress,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_share_backup_zip"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    border = BorderStroke(1.dp, SupabaseGreen.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SupabaseGreen)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "Share .ZIP" else "Bagikan .ZIP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(color = CharcoalBorder, thickness = 0.5.dp)

        // 2. Import / Restore Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEn) "Import / Restore (.ZIP)" else "Import / Restore dari .ZIP",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )

                // Minimalist Mode Selector: Timpa vs Gabung
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        true to (if (isEn) "Replace" else "Timpa"),
                        false to (if (isEn) "Merge" else "Gabung")
                    ).forEach { (isReplace, label) ->
                        val selected = uiState.replaceOnRestore == isReplace
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selected) BlueInfo.copy(alpha = 0.18f) else CharcoalSurfaceElevated,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (selected) BlueInfo else CharcoalBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.setReplaceOnRestore(isReplace) }
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) BlueInfo else TextSecondaryDark
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    importZipLauncher.launch(
                        arrayOf(
                            "application/zip",
                            "application/x-zip-compressed",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },
                enabled = !uiState.isBackupInProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("btn_import_backup_zip"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CharcoalSurfaceElevated,
                    contentColor = TextPrimaryDark
                ),
                border = BorderStroke(1.dp, BlueInfo.copy(alpha = 0.65f))
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = BlueInfo, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEn) "Select Backup .ZIP File to Restore" else "Pilih File Backup .ZIP untuk Dipulihkan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Progress or Result Status Banner
        if (uiState.isBackupInProgress) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = SupabaseGreen,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.backupStatusMessage ?: "Memproses...",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            }
        } else if (!uiState.backupStatusMessage.isNullOrBlank()) {
            val statusColor = if (uiState.backupStatusSuccess) SupabaseGreen else RoseError
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                    .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.backupStatusMessage ?: "",
                        fontSize = 11.sp,
                        color = statusColor
                    )
                }
                IconButton(
                    onClick = { viewModel.clearBackupStatus() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                }
            }
        }

        HorizontalDivider(color = CharcoalBorder, thickness = 0.5.dp)

        // 3. Minimalist Reset / Clear All Data button (Pre-Production Clean Slate)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEn) "Clean Pre-Production Slate" else "Reset Bersih Semua Data",
                fontSize = 11.sp,
                color = TextMutedDark
            )
            TextButton(
                onClick = { showConfirmClearAll = true },
                enabled = !uiState.isBackupInProgress,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RoseError, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEn) "Clear All" else "Kosongkan",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RoseError
                )
            }
        }
    }

    if (showConfirmClearAll) {
        AlertDialog(
            onDismissRequest = { showConfirmClearAll = false },
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            containerColor = CharcoalSurface,
            title = {
                Text(
                    text = if (isEn) "Clear All Data & Photos?" else "Kosongkan Semua Data & Foto?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Text(
                    text = if (isEn) {
                        "All stores, products, stocks, transactions, cashbook records, and saved photos will be permanently deleted. Make sure you have exported a .ZIP backup if needed."
                    } else {
                        "Seluruh data warung, produk, stok titipan, transaksi, buku kas, dan foto akan dihapus bersih. Pastikan Anda sudah menyimpan Backup .ZIP jika diperlukan."
                    },
                    fontSize = 12.5.sp,
                    color = TextSecondaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearAll = false
                        viewModel.clearAllPreproductionData { res ->
                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError, contentColor = Color.White)
                ) {
                    Text(if (isEn) "Delete All" else "Ya, Kosongkan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearAll = false }) {
                    Text(if (isEn) "Cancel" else "Batal", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
private fun MiniBackupMetric(
    label: String,
    count: Int,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) SupabaseGreen else TextPrimaryDark
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = TextMutedDark
        )
    }
}
