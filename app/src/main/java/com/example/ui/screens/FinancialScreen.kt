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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.data.local.entity.FinancialCategory
import com.example.data.local.entity.FinancialRecord
import com.example.domain.model.BusinessFinancialSummary
import com.example.domain.model.PersonalFinancialSummary
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
import com.example.ui.viewmodel.FinanceTab
import com.example.ui.viewmodel.FinancialViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinancialScreen(
    viewModel: FinancialViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var dialogIsExpense by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CharcoalBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                    text = "Buku Kas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            // Segregated Tabs: Kas Bisnis vs Kas Pribadi
            val tabs = listOf("Kas Bisnis", "Kas Pribadi")
            val selectedIndex = if (uiState.selectedTab == FinanceTab.BUSINESS) 0 else 1

            TabRow(
                selectedTabIndex = selectedIndex,
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
                        Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                        color = SupabaseGreen,
                        height = 2.dp
                    )
                }
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedIndex == idx,
                        onClick = {
                            viewModel.selectTab(if (idx == 0) FinanceTab.BUSINESS else FinanceTab.PERSONAL)
                        },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.5.sp,
                                fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedIndex == idx) SupabaseGreen else TextSecondaryDark
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.selectedTab == FinanceTab.BUSINESS) {
                    // Kas Bisnis Section
                    item {
                        BusinessPnLCard(summary = uiState.businessSummary)
                    }

                    // Action Buttons for Business
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    dialogIsExpense = true
                                    showAddDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CharcoalSurfaceElevated,
                                    contentColor = RoseError
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pengeluaran", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    dialogIsExpense = false
                                    showAddDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CharcoalSurfaceElevated,
                                    contentColor = SupabaseGreen
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pemasukan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Riwayat Transaksi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (uiState.businessRecords.isEmpty()) {
                        item {
                            EmptyRecordsCard(text = "Belum ada transaksi.")
                        }
                    } else {
                        items(uiState.businessRecords, key = { it.id }) { record ->
                            RecordItemRow(
                                record = record,
                                onDelete = { viewModel.deleteRecord(record) }
                            )
                        }
                    }
                } else {
                    // Kas Pribadi Section (Completely Isolated from Business)
                    item {
                        PersonalBalanceCard(summary = uiState.personalSummary)
                    }

                    // Action Buttons for Personal
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    dialogIsExpense = true
                                    showAddDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CharcoalSurfaceElevated,
                                    contentColor = RoseError
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pengeluaran", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    dialogIsExpense = false
                                    showAddDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CharcoalSurfaceElevated,
                                    contentColor = SupabaseGreen
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pemasukan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Riwayat Transaksi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (uiState.personalRecords.isEmpty()) {
                        item {
                            EmptyRecordsCard(text = "Belum ada transaksi.")
                        }
                    } else {
                        items(uiState.personalRecords, key = { it.id }) { record ->
                            RecordItemRow(
                                record = record,
                                onDelete = { viewModel.deleteRecord(record) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val isBusiness = uiState.selectedTab == FinanceTab.BUSINESS
        AddFinancialRecordDialog(
            isBusiness = isBusiness,
            isExpense = dialogIsExpense,
            onDismiss = { showAddDialog = false },
            onConfirm = { amount, desc ->
                if (isBusiness) {
                    if (dialogIsExpense) viewModel.addBusinessExpense(amount, desc)
                    else viewModel.addBusinessIncome(amount, desc)
                } else {
                    if (dialogIsExpense) viewModel.addPersonalExpense(amount, desc)
                    else viewModel.addPersonalIncome(amount, desc)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun BusinessPnLCard(summary: BusinessFinancialSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Laba Rugi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = SupabaseGreen
                )
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = SupabaseGreen,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prominent Net Profit Hero Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (summary.netProfit >= 0) SupabaseGreen.copy(alpha = 0.12f) else RoseError.copy(alpha = 0.12f),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        1.dp,
                        if (summary.netProfit >= 0) SupabaseGreen.copy(alpha = 0.3f) else RoseError.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Laba Bersih",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rp %,.0f".format(summary.netProfit),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.netProfit >= 0) SupabaseGreen else RoseError
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Breakdown
            FinanceRow(label = "Penjualan", value = "Rp %,.0f".format(summary.totalSalesRevenue))
            FinanceRow(label = "HPP Modal", value = "- Rp %,.0f".format(summary.totalCostOfGoodsSold), valueColor = AmberWarning)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = CharcoalBorder)
            FinanceRow(label = "Laba Kotor", value = "Rp %,.0f".format(summary.grossProfit), isBold = true)
            FinanceRow(label = "Operasional", value = "- Rp %,.0f".format(summary.operationalExpenses), valueColor = RoseError)
        }
    }
}

@Composable
private fun PersonalBalanceCard(summary: PersonalFinancialSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Saldo Pribadi",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = BlueInfo
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurfaceElevated, RoundedCornerShape(10.dp))
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Saldo Kas",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Rp %,.0f".format(summary.balance),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.balance >= 0) SupabaseGreen else RoseError
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FinanceRow(label = "Pemasukan", value = "Rp %,.0f".format(summary.totalIncome), valueColor = SupabaseGreen)
            FinanceRow(label = "Pengeluaran", value = "- Rp %,.0f".format(summary.totalExpense), valueColor = RoseError)
        }
    }
}

@Composable
private fun FinanceRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimaryDark,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.5.sp, color = TextSecondaryDark)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun RecordItemRow(
    record: FinancialRecord,
    onDelete: () -> Unit
) {
    val isExpense = record.category.endsWith("EXPENSE")
    val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(record.transaction_date))

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
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (isExpense) RoseError.copy(alpha = 0.15f) else SupabaseGreen.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isExpense) RoseError else SupabaseGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = record.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "$dateStr • ${formatCategoryLabel(record.category)}",
                        fontSize = 11.sp,
                        color = TextMutedDark
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isExpense) "-" else "+"} Rp %,.0f".format(record.amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) RoseError else SupabaseGreen
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = TextMutedDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatCategoryLabel(cat: String): String = when (cat) {
    FinancialCategory.BUSINESS_INCOME -> "Setoran Usaha"
    FinancialCategory.BUSINESS_EXPENSE -> "Beban Usaha"
    FinancialCategory.PERSONAL_INCOME -> "Pemasukan Pribadi"
    FinancialCategory.PERSONAL_EXPENSE -> "Pengeluaran Pribadi"
    else -> cat
}

@Composable
private fun EmptyRecordsCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalSurfaceElevated, RoundedCornerShape(10.dp))
            .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 13.sp, color = TextMutedDark)
    }
}

@Composable
private fun AddFinancialRecordDialog(
    isBusiness: Boolean,
    isExpense: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, description: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val title = if (isExpense) "Catat Pengeluaran" else "Catat Pemasukan"
    val sampleDesc = if (isExpense) "Bensin, operasional, makan..." else "Pendapatan, komisi..."

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
                text = title,
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Nominal (Rp) *") },
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

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Keterangan *") },
                    placeholder = { Text(sampleDesc) },
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
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && description.isNotBlank()) {
                        onConfirm(amt, description.trim())
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
