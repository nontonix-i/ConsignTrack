package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ConsignmentStock
import com.example.data.local.entity.Customer
import com.example.data.local.entity.FinancialCategory
import com.example.data.local.entity.Product
import com.example.domain.model.BusinessFinancialSummary
import com.example.domain.model.PersonalFinancialSummary
import com.example.domain.model.TransactionWithDetails
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
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.AppLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private data class ChartPoint(
    val label: String,
    val primaryValue: Double,
    val secondaryValue: Double,
    val subtitle: String
)

private data class DonutSliceData(
    val name: String,
    val value: Double,
    val piecesCount: Int,
    val marginPct: Double,
    val color: Color
)

private data class StoreRankingBar(
    val customer: Customer,
    val salesRevenue: Double,
    val activeStockPieces: Int,
    val stockRetailValue: Double
)

private enum class StoreBarMetric(val labelId: String, val labelEn: String) {
    SALES("Omset Transaksi", "Sales Revenue"),
    STOCK_VALUE("Nilai Stok (Rp)", "Stock Value"),
    STOCK_PCS("Stok Aktif (Pcs)", "Stock Units")
}

@Composable
fun GraphicsAnalysisScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenAiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEn = uiState.language == AppLanguage.EN

    val customers by viewModel.repository.allCustomers.collectAsStateWithLifecycle(initialValue = emptyList())
    val products by viewModel.repository.allProducts.collectAsStateWithLifecycle(initialValue = emptyList())
    val stocks by viewModel.repository.allStocks.collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by viewModel.repository.allTransactionsWithDetails.collectAsStateWithLifecycle(initialValue = emptyList())
    val businessSummary by viewModel.repository.businessSummary.collectAsStateWithLifecycle(
        initialValue = BusinessFinancialSummary(0.0, 0.0, 0.0, 0.0, 0.0)
    )
    val personalSummary by viewModel.repository.personalSummary.collectAsStateWithLifecycle(
        initialValue = PersonalFinancialSummary(0.0, 0.0, 0.0)
    )
    val bizRecords by viewModel.repository.businessRecords.collectAsStateWithLifecycle(initialValue = emptyList())

    val initialCal = remember { Calendar.getInstance() }
    var selectedPeriodMode by remember { mutableStateOf(AnalyticsPeriodMode.ALL) }
    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var hasSyncedInitialDateWithHistory by remember { mutableStateOf(false) }
    var showCalendarModal by remember { mutableStateOf(false) }

    // Sync initial calendar month/day to the most recent transaction date so user immediately sees history dots
    LaunchedEffect(transactions.size) {
        if (!hasSyncedInitialDateWithHistory && transactions.isNotEmpty()) {
            val latestTxMillis = transactions.maxOf { it.header.transaction_date }
            val c = Calendar.getInstance().apply { timeInMillis = latestTxMillis }
            selectedYear = c.get(Calendar.YEAR)
            selectedMonth = c.get(Calendar.MONTH)
            selectedDay = c.get(Calendar.DAY_OF_MONTH)
            hasSyncedInitialDateWithHistory = true
        }
    }

    var selectedStoreMetric by remember { mutableStateOf(StoreBarMetric.STOCK_VALUE) }

    var animTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animTrigger = true }
    val animProgress by animateFloatAsState(
        targetValue = if (animTrigger) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "ChartAnim"
    )

    val periodBounds = remember(selectedPeriodMode, selectedYear, selectedMonth, selectedDay) {
        computePeriodBounds(
            mode = selectedPeriodMode,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            selectedDayOfMonth = selectedDay
        )
    }

    val filteredTransactions = remember(transactions, periodBounds) {
        val (startMs, endMs) = periodBounds
        transactions.filter { tx ->
            tx.header.transaction_date in startMs..endMs
        }
    }

    val filteredBizRecords = remember(bizRecords, periodBounds) {
        val (startMs, endMs) = periodBounds
        bizRecords.filter { rec ->
            rec.transaction_date in startMs..endMs
        }
    }

    val productMap = remember(products) { products.associateBy { it.id } }

    val dailyHistorySummaries = remember(filteredTransactions, filteredBizRecords, productMap) {
        buildDailyHistoryList(
            transactions = filteredTransactions,
            bizRecords = filteredBizRecords,
            productMap = productMap
        )
    }

    val periodDescriptionLabel = remember(selectedPeriodMode, selectedYear, selectedMonth, selectedDay) {
        formatPeriodLabel(selectedPeriodMode, selectedYear, selectedMonth, selectedDay)
    }

    // Calculate filtered metrics
    val filteredSalesRevenue = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.header.total_sold_amount }
    }
    val filteredPaidAmount = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.header.amount_paid }
    }
    val filteredHpp = remember(filteredTransactions, productMap) {
        filteredTransactions.sumOf { tx ->
            tx.details.sumOf { (d, p) ->
                val prod = p ?: productMap[d.product_id]
                d.sold_quantity * (prod?.cost_price ?: 0.0)
            }
        }
    }
    val filteredOpEx = remember(filteredBizRecords) {
        filteredBizRecords
            .filter { it.category == FinancialCategory.BUSINESS_EXPENSE }
            .sumOf { it.amount }
    }
    val filteredNetProfit = (filteredSalesRevenue - filteredHpp) - filteredOpEx

    val filteredBusinessSummary = remember(
        selectedPeriodMode,
        businessSummary,
        filteredSalesRevenue,
        filteredHpp,
        filteredOpEx,
        filteredNetProfit
    ) {
        if (selectedPeriodMode == AnalyticsPeriodMode.ALL) {
            businessSummary
        } else {
            BusinessFinancialSummary(
                filteredSalesRevenue,
                filteredHpp,
                filteredSalesRevenue - filteredHpp,
                filteredOpEx,
                filteredNetProfit
            )
        }
    }

    val totalConsignedPieces = remember(stocks) { stocks.sumOf { it.current_quantity } }
    val totalConsignedRetailValue = remember(stocks, productMap) {
        stocks.sumOf { s ->
            val p = productMap[s.product_id]
            s.current_quantity * (p?.let { s.effectivePriceSmall(it) } ?: 0.0)
        }
    }
    val totalConsignedCostValue = remember(stocks, productMap) {
        stocks.sumOf { s ->
            val p = productMap[s.product_id]
            s.current_quantity * (p?.cost_price ?: 0.0)
        }
    }
    val potentialGrossProfit = totalConsignedRetailValue - totalConsignedCostValue

    val chartPalette = remember {
        listOf(
            Color(0xFF3ECF8E), // Emerald
            Color(0xFF38BDF8), // Sky Blue
            Color(0xFFF59E0B), // Amber
            Color(0xFFA855F7), // Purple
            Color(0xFFF43F5E), // Rose
            Color(0xFF14B8A6)  // Teal
        )
    }

    // Build Trend Line / Area Chart Points
    // Uses real transaction history if available; otherwise plots real Consigned Value & Potential Profit across Stores
    val hasTransactionHistory = filteredTransactions.isNotEmpty()
    val isSpecificDateOrMonth = selectedPeriodMode == AnalyticsPeriodMode.SPECIFIC_DATE ||
        selectedPeriodMode == AnalyticsPeriodMode.SPECIFIC_MONTH ||
        selectedPeriodMode == AnalyticsPeriodMode.TODAY

    val trendPoints = remember(filteredTransactions, customers, stocks, productMap, isSpecificDateOrMonth) {
        if (filteredTransactions.isNotEmpty()) {
            val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val shortFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
            val sortedAsc = filteredTransactions.sortedBy { it.header.transaction_date }
            sortedAsc.takeLast(12).map { tx ->
                val rev = tx.header.total_sold_amount
                val cost = tx.details.sumOf { (d, p) ->
                    d.sold_quantity * ((p ?: productMap[d.product_id])?.cost_price ?: 0.0)
                }
                val profit = rev - cost
                ChartPoint(
                    label = tx.customer?.name?.replace("Warung ", "")?.take(7)
                        ?: shortFmt.format(Date(tx.header.transaction_date)),
                    primaryValue = rev,
                    secondaryValue = profit.coerceAtLeast(0.0),
                    subtitle = "${tx.customer?.name ?: "Transaksi"} (${fmt.format(Date(tx.header.transaction_date))})"
                )
            }
        } else if (!isSpecificDateOrMonth) {
            val stocksByCust = stocks.groupBy { it.customer_id }
            customers.map { c ->
                val cStocks = stocksByCust[c.id].orEmpty()
                val retailVal = cStocks.sumOf { s ->
                    val prod = productMap[s.product_id]
                    s.current_quantity * (prod?.let { s.effectivePriceSmall(it) } ?: 0.0)
                }
                val costVal = cStocks.sumOf { s ->
                    s.current_quantity * (productMap[s.product_id]?.cost_price ?: 0.0)
                }
                val potProfit = (retailVal - costVal).coerceAtLeast(0.0)
                ChartPoint(
                    label = c.name.replace("Warung ", "").replace("Toko ", "").take(8),
                    primaryValue = retailVal,
                    secondaryValue = potProfit,
                    subtitle = "${c.name} • Rute ${c.route_day}"
                )
            }
        } else {
            emptyList()
        }
    }

    // Build Donut Chart Data (Product Distribution by Sold Value in Period or Consigned Value)
    val donutSlices = remember(products, stocks, filteredTransactions, isSpecificDateOrMonth) {
        val stockByProd = stocks.groupBy { it.product_id }
            .mapValues { entry -> entry.value.sumOf { it.current_quantity } }
        val soldByProd = filteredTransactions.flatMap { it.details }
            .groupBy { it.first.product_id }
            .mapValues { entry -> entry.value.sumOf { it.first.sold_quantity } }

        products.mapIndexed { idx, p ->
            val activePcs = stockByProd[p.id] ?: 0
            val soldPcs = soldByProd[p.id] ?: 0
            val relevantPcs = if (filteredTransactions.isNotEmpty()) soldPcs else if (isSpecificDateOrMonth) 0 else activePcs
            val valueRp = (relevantPcs.coerceAtLeast(if (isSpecificDateOrMonth && filteredTransactions.isEmpty()) 0 else 1)) * p.selling_price
            val marginPct = if (p.selling_price_pack > 0) {
                ((p.selling_price_pack - p.cost_price_pack) / p.selling_price_pack) * 100.0
            } else 0.0
            DonutSliceData(
                name = p.name,
                value = valueRp,
                piecesCount = if (filteredTransactions.isNotEmpty()) soldPcs else activePcs,
                marginPct = marginPct,
                color = chartPalette[idx % chartPalette.size]
            )
        }
    }

    // Build Store Ranking Data
    val storeRankings = remember(customers, stocks, filteredTransactions, productMap, selectedStoreMetric) {
        val stocksByCust = stocks.groupBy { it.customer_id }
        val txByCust = filteredTransactions.groupBy { it.header.customer_id }
        val list = customers.map { c ->
            val cStocks = stocksByCust[c.id].orEmpty()
            val pcs = cStocks.sumOf { it.current_quantity }
            val retailVal = cStocks.sumOf { s ->
                val prod = productMap[s.product_id]
                s.current_quantity * (prod?.let { s.effectivePriceSmall(it) } ?: 0.0)
            }
            val salesRev = txByCust[c.id].orEmpty().sumOf { it.header.total_sold_amount }
            StoreRankingBar(
                customer = c,
                salesRevenue = salesRev,
                activeStockPieces = pcs,
                stockRetailValue = retailVal
            )
        }
        when (selectedStoreMetric) {
            StoreBarMetric.SALES -> list.sortedByDescending { it.salesRevenue }
            StoreBarMetric.STOCK_PCS -> list.sortedByDescending { it.activeStockPieces }
            StoreBarMetric.STOCK_VALUE -> list.sortedByDescending { it.stockRetailValue }
        }
    }

    if (showCalendarModal) {
        CalendarDateMonthPickerModal(
            initialYear = selectedYear,
            initialMonth = selectedMonth,
            initialDay = selectedDay,
            allTransactions = transactions,
            onSelectFullMonth = { y, m ->
                selectedYear = y
                selectedMonth = m
                selectedPeriodMode = AnalyticsPeriodMode.SPECIFIC_MONTH
                if (transactions.any {
                        val c = Calendar.getInstance().apply { timeInMillis = it.header.transaction_date }
                        c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m
                    }
                ) {
                    selectedStoreMetric = StoreBarMetric.SALES
                }
                showCalendarModal = false
            },
            onSelectSpecificDate = { y, m, d ->
                selectedYear = y
                selectedMonth = m
                selectedDay = d
                selectedPeriodMode = AnalyticsPeriodMode.SPECIFIC_DATE
                selectedStoreMetric = StoreBarMetric.SALES
                showCalendarModal = false
            },
            onDismiss = { showCalendarModal = false }
        )
    }

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
            // Top App Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("graphics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEn) "Back" else "Kembali",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isEn) "Graphics & Chart Studio" else "Analisis Grafik & Riwayat",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(SupabaseGreen.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
                                    .border(0.8.dp, SupabaseGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HISTORY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SupabaseGreen
                                )
                            }
                        }
                        Text(
                            text = periodDescriptionLabel,
                            fontSize = 11.sp,
                            color = SupabaseGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier
                            .clickable { onOpenAiChat() }
                            .border(1.dp, SupabaseGreen.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = SupabaseGreen.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = SupabaseGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SupabaseGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("graphics_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Drawer",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = CharcoalBorder, thickness = 1.dp)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Interactive Date, Month & Period Filter Control Card
                item {
                    DateAndMonthFilterControlCard(
                        isEn = isEn,
                        selectedMode = selectedPeriodMode,
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        selectedDay = selectedDay,
                        allTransactions = transactions,
                        onSelectMode = { mode ->
                            selectedPeriodMode = mode
                            if (mode == AnalyticsPeriodMode.SPECIFIC_DATE || mode == AnalyticsPeriodMode.SPECIFIC_MONTH) {
                                selectedStoreMetric = StoreBarMetric.SALES
                            }
                        },
                        onUpdateDate = { y, m, d ->
                            selectedYear = y
                            selectedMonth = m
                            selectedDay = d
                        },
                        onOpenCalendarModal = { showCalendarModal = true }
                    )
                }

                // 2. Executive KPI Summary Grid (2x2)
                item {
                    val useTxMetrics = isSpecificDateOrMonth || filteredSalesRevenue > 0
                    val primaryDisplayVal = if (useTxMetrics) filteredSalesRevenue else totalConsignedRetailValue
                    val primaryLabel = if (useTxMetrics) {
                        if (isEn) "Sales Revenue" else "Omset Penjualan"
                    } else {
                        if (isEn) "Active Stock Value" else "Nilai Stok Aktif"
                    }

                    val profitDisplayVal = if (useTxMetrics) filteredNetProfit else potentialGrossProfit
                    val profitLabel = if (useTxMetrics) {
                        if (isEn) "Net Profit" else "Laba Bersih"
                    } else {
                        if (isEn) "Potential Profit" else "Potensi Laba Stok"
                    }

                    val marginPct = if (primaryDisplayVal > 0) (profitDisplayVal / primaryDisplayVal) * 100.0 else 0.0
                    val collectionRate = if (filteredSalesRevenue > 0) {
                        ((filteredPaidAmount / filteredSalesRevenue) * 100.0).coerceIn(0.0, 100.0)
                    } else if (isSpecificDateOrMonth) 0.0 else 100.0

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = primaryLabel,
                                value = formatRupiahCompact(primaryDisplayVal),
                                subValue = if (useTxMetrics) "${filteredTransactions.size} Nota Transaksi" else "${customers.size} Warung Mitra",
                                accentColor = SupabaseGreen,
                                progress = 0.82f * animProgress,
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = profitLabel,
                                value = formatRupiahCompact(profitDisplayVal),
                                subValue = "Margin %.1f%%".format(marginPct),
                                accentColor = BlueInfo,
                                progress = (marginPct.toFloat() / 50f).coerceIn(0.15f, 1f) * animProgress,
                                icon = Icons.Default.ShowChart,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = if (isEn) "Consigned Stock" else "Stok Tersebar",
                                value = "$totalConsignedPieces Pcs",
                                subValue = formatRupiahCompact(totalConsignedRetailValue),
                                accentColor = AmberWarning,
                                progress = (totalConsignedPieces / 150f).coerceIn(0.2f, 1f) * animProgress,
                                icon = Icons.Default.Inventory2,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = if (isEn) "Collection Rate" else "Setoran Kas Masuk",
                                value = if (useTxMetrics) formatRupiahCompact(filteredPaidAmount) else "%.0f%%".format(collectionRate),
                                subValue = if (useTxMetrics) "Rasio Bayar %.0f%%".format(collectionRate) else "${customers.size} Warung • ${products.size} Produk",
                                accentColor = Color(0xFFA855F7),
                                progress = (collectionRate.toFloat() / 100f).coerceIn(0.15f, 1f) * animProgress,
                                icon = Icons.Default.AccountBalanceWallet,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Interactive Daily Performance Bar Chart (by Date in Selected Month)
                item {
                    DailyPerformanceBarChartCard(
                        isEn = isEn,
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        selectedDay = selectedDay,
                        selectedMode = selectedPeriodMode,
                        allTransactions = transactions,
                        allBizRecords = bizRecords,
                        productMap = productMap,
                        animProgress = animProgress,
                        onSelectSpecificDate = { y, m, d ->
                            selectedYear = y
                            selectedMonth = m
                            selectedDay = d
                            selectedPeriodMode = AnalyticsPeriodMode.SPECIFIC_DATE
                            selectedStoreMetric = StoreBarMetric.SALES
                        }
                    )
                }

                // 4. Interactive Cubic Bezier Area & Dual-Line Chart
                item {
                    BezierTrendChartCard(
                        title = if (hasTransactionHistory || isSpecificDateOrMonth) {
                            if (isEn) "Revenue vs Profit Trend" else "Grafik Tren Omset vs Laba (${selectedPeriodMode.labelId})"
                        } else {
                            if (isEn) "Consigned Value & Profit by Store" else "Grafik Nilai Titipan & Potensi Laba Warung"
                        },
                        subtitle = if (hasTransactionHistory) {
                            if (isEn) "Tap any node on the curve to inspect transaction details" else "Sentuh titik pada kurva untuk melihat detail transaksi pada periode terpilih"
                        } else if (isSpecificDateOrMonth) {
                            "Belum ada nota kunjungan pada tanggal/bulan ini. Pilih tanggal bertitik hijau di atas."
                        } else {
                            if (isEn) "Tap any store node to inspect consigned value & profit" else "Sentuh titik warung pada grafik untuk inspeksi nilai & margin"
                        },
                        primaryLegend = if (hasTransactionHistory || isSpecificDateOrMonth) "Omset" else "Nilai Jual",
                        secondaryLegend = if (hasTransactionHistory || isSpecificDateOrMonth) "Laba" else "Potensi Laba",
                        points = trendPoints,
                        animProgress = animProgress
                    )
                }

                // 5. Interactive Product Distribution Donut Chart
                item {
                    ProductDonutChartCard(
                        title = if (isEn) "Product Share & Margin Distribution" else "Distribusi Produk & Margin Keuntungan",
                        subtitle = if (isEn) "Tap a product below to highlight its ring segment" else "Ketuk nama produk untuk menyorot porsi penjualan/stok & margin",
                        slices = donutSlices,
                        animProgress = animProgress
                    )
                }

                // 6. Horizontal Gradient Bar Chart — Store Performance Ranking
                item {
                    StoreRankingBarChartCard(
                        isEn = isEn,
                        rankings = storeRankings,
                        selectedMetric = selectedStoreMetric,
                        onSelectMetric = { selectedStoreMetric = it },
                        animProgress = animProgress
                    )
                }

                // 7. Weekly Route Load & Consignment Column Chart (Senin - Minggu)
                item {
                    WeeklyRouteColumnChartCard(
                        isEn = isEn,
                        customers = customers,
                        stocks = stocks,
                        animProgress = animProgress
                    )
                }

                // 8. Financial Cashflow Structure & Profit Waterfall
                item {
                    CashflowComparisonChartCard(
                        isEn = isEn,
                        businessSummary = filteredBusinessSummary,
                        personalSummary = personalSummary,
                        potentialRetailValue = totalConsignedRetailValue,
                        potentialCostValue = totalConsignedCostValue,
                        animProgress = animProgress
                    )
                }

                // 9. Historical Performance & Daily Visit Log by Date
                item {
                    HistoricalPerformanceLogCard(
                        isEn = isEn,
                        periodLabel = periodDescriptionLabel,
                        dailySummaries = dailyHistorySummaries,
                        productMap = productMap,
                        onFocusSpecificDate = { y, m, d ->
                            selectedYear = y
                            selectedMonth = m
                            selectedDay = d
                            selectedPeriodMode = AnalyticsPeriodMode.SPECIFIC_DATE
                            selectedStoreMetric = StoreBarMetric.SALES
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subValue: String,
    accentColor: Color,
    progress: Float,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subValue,
                fontSize = 10.5.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animated mini progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CharcoalSurfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0.08f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(accentColor.copy(alpha = 0.7f), accentColor)
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun BezierTrendChartCard(
    title: String,
    subtitle: String,
    primaryLegend: String,
    secondaryLegend: String,
    points: List<ChartPoint>,
    animProgress: Float
) {
    var selectedIndex by remember(points.size) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = SupabaseGreen,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }

                // Legend Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = SupabaseGreen, label = primaryLegend)
                    LegendDot(color = BlueInfo, label = secondaryLegend)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada data untuk ditampilkan", color = TextMutedDark, fontSize = 12.sp)
                }
            } else {
                val safeIdx = selectedIndex.coerceIn(0, points.lastIndex)
                val activePoint = points[safeIdx]

                // Selected Point Inspector Banner
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    color = CharcoalSurfaceElevated
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
                                text = activePoint.subtitle,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Titik #${safeIdx + 1} dari ${points.size}",
                                fontSize = 10.sp,
                                color = TextMutedDark
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(primaryLegend, fontSize = 9.5.sp, color = TextSecondaryDark)
                                Text(
                                    text = formatRupiahCompact(activePoint.primaryValue),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SupabaseGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(secondaryLegend, fontSize = 9.5.sp, color = TextSecondaryDark)
                                Text(
                                    text = formatRupiahCompact(activePoint.secondaryValue),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueInfo
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val maxVal = remember(points) {
                    max(points.maxOfOrNull { max(it.primaryValue, it.secondaryValue) } ?: 1.0, 1.0)
                }
                val greenColor = SupabaseGreen
                val blueColor = BlueInfo
                val gridColor = CharcoalBorder

                // Custom Canvas Cubic Bezier Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(points.size) {
                                detectTapGestures { tapOffset ->
                                    if (points.size == 1) {
                                        selectedIndex = 0
                                    } else {
                                        val stepX = size.width.toFloat() / (points.size - 1).coerceAtLeast(1)
                                        val idx = ((tapOffset.x / stepX).roundToInt()).coerceIn(0, points.lastIndex)
                                        selectedIndex = idx
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val topPad = 14.dp.toPx()
                        val bottomPad = 24.dp.toPx()
                        val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)

                        // Draw 4 horizontal dashed grid lines
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        for (i in 0..3) {
                            val y = topPad + (chartH * i / 3f)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (i < 3) dashEffect else null
                            )
                        }

                        val n = points.size
                        val coordsPrimary = points.mapIndexed { idx, pt ->
                            val x = if (n == 1) w / 2f else (w * idx / (n - 1).toFloat())
                            val norm = ((pt.primaryValue / maxVal).toFloat() * animProgress).coerceIn(0f, 1f)
                            val y = topPad + chartH * (1f - norm)
                            Offset(x, y)
                        }
                        val coordsSecondary = points.mapIndexed { idx, pt ->
                            val x = if (n == 1) w / 2f else (w * idx / (n - 1).toFloat())
                            val norm = ((pt.secondaryValue / maxVal).toFloat() * animProgress).coerceIn(0f, 1f)
                            val y = topPad + chartH * (1f - norm)
                            Offset(x, y)
                        }

                        if (coordsPrimary.size >= 2) {
                            val linePath = Path().apply {
                                moveTo(coordsPrimary.first().x, coordsPrimary.first().y)
                                for (i in 0 until coordsPrimary.size - 1) {
                                    val p0 = coordsPrimary[i]
                                    val p1 = coordsPrimary[i + 1]
                                    val ctrlX = (p0.x + p1.x) / 2f
                                    cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                }
                            }

                            val fillPath = Path().apply {
                                addPath(linePath)
                                lineTo(coordsPrimary.last().x, topPad + chartH)
                                lineTo(coordsPrimary.first().x, topPad + chartH)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        greenColor.copy(alpha = 0.35f),
                                        greenColor.copy(alpha = 0.02f)
                                    ),
                                    startY = topPad,
                                    endY = topPad + chartH
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = greenColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Secondary line (Profit)
                            val secPath = Path().apply {
                                moveTo(coordsSecondary.first().x, coordsSecondary.first().y)
                                for (i in 0 until coordsSecondary.size - 1) {
                                    val p0 = coordsSecondary[i]
                                    val p1 = coordsSecondary[i + 1]
                                    val ctrlX = (p0.x + p1.x) / 2f
                                    cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                }
                            }
                            drawPath(
                                path = secPath,
                                color = blueColor,
                                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Vertical highlight line on selected index
                        val selOffset = coordsPrimary[safeIdx]
                        drawLine(
                            color = greenColor.copy(alpha = 0.5f),
                            start = Offset(selOffset.x, topPad),
                            end = Offset(selOffset.x, topPad + chartH),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        // Draw nodes
                        coordsSecondary.forEachIndexed { idx, offset ->
                            drawCircle(
                                color = blueColor,
                                radius = if (idx == safeIdx) 5.dp.toPx() else 3.dp.toPx(),
                                center = offset
                            )
                        }
                        coordsPrimary.forEachIndexed { idx, offset ->
                            if (idx == safeIdx) {
                                drawCircle(
                                    color = greenColor.copy(alpha = 0.28f),
                                    radius = 10.dp.toPx(),
                                    center = offset
                                )
                            }
                            drawCircle(
                                color = CharcoalSurface,
                                radius = if (idx == safeIdx) 6.dp.toPx() else 4.dp.toPx(),
                                center = offset
                            )
                            drawCircle(
                                color = greenColor,
                                radius = if (idx == safeIdx) 4.5.dp.toPx() else 3.dp.toPx(),
                                center = offset
                            )
                        }
                    }

                    // X-Axis Labels Row at Bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        points.forEachIndexed { idx, pt ->
                            val isSelected = idx == safeIdx
                            Text(
                                text = pt.label,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SupabaseGreen else TextMutedDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clickable { selectedIndex = idx }
                                    .padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDonutChartCard(
    title: String,
    subtitle: String,
    slices: List<DonutSliceData>,
    animProgress: Float
) {
    var selectedSliceIdx by remember(slices.size) { mutableIntStateOf(0) }
    val totalValue = remember(slices) { slices.sumOf { it.value }.coerceAtLeast(1.0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DonutLarge,
                    contentDescription = null,
                    tint = BlueInfo,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (slices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada katalog produk", color = TextMutedDark, fontSize = 12.sp)
                }
            } else {
                val safeIdx = selectedSliceIdx.coerceIn(0, slices.lastIndex)
                val activeSlice = slices[safeIdx]
                val activeSharePct = (activeSlice.value / totalValue) * 100.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier.size(152.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(138.dp)) {
                            val strokeW = 18.dp.toPx()
                            val selectedStrokeW = 23.dp.toPx()
                            var startAngle = -90f
                            val gapAngle = if (slices.size > 1) 4f else 0f

                            slices.forEachIndexed { idx, slice ->
                                val rawSweep = ((slice.value / totalValue).toFloat() * 360f) * animProgress
                                val sweep = (rawSweep - gapAngle).coerceAtLeast(2f)
                                val isSel = idx == safeIdx
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle + gapAngle / 2f,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(
                                        width = if (isSel) selectedStrokeW else strokeW,
                                        cap = StrokeCap.Butt
                                    )
                                )
                                startAngle += rawSweep
                            }
                        }

                        // Center Label inside Donut
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "%.0f%%".format(activeSharePct),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeSlice.color
                            )
                            Text(
                                text = activeSlice.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${activeSlice.piecesCount} Pcs",
                                fontSize = 9.5.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    // Interactive Product Legend List
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slices.forEachIndexed { idx, slice ->
                            val isSelected = idx == safeIdx
                            val share = (slice.value / totalValue) * 100.0
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSliceIdx = idx }
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) slice.color else CharcoalBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) slice.color.copy(alpha = 0.12f) else CharcoalSurfaceElevated
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(slice.color, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = slice.name,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${slice.piecesCount} Pcs • Margin %.0f%%".format(slice.marginPct),
                                                fontSize = 9.5.sp,
                                                color = TextSecondaryDark
                                            )
                                        }
                                    }
                                    Text(
                                        text = "%.0f%%".format(share),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = slice.color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreRankingBarChartCard(
    isEn: Boolean,
    rankings: List<StoreRankingBar>,
    selectedMetric: StoreBarMetric,
    onSelectMetric: (StoreBarMetric) -> Unit,
    animProgress: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEn) "Store Performance & Stock Ranking" else "Grafik Ranking Warung & Stok Titipan",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric Switcher Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StoreBarMetric.entries.forEach { metric ->
                    val selected = selectedMetric == metric
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectMetric(metric) }
                            .border(
                                1.dp,
                                if (selected) SupabaseGreen else CharcoalBorder,
                                RoundedCornerShape(8.dp)
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) SupabaseGreen.copy(alpha = 0.15f) else CharcoalSurfaceElevated
                    ) {
                        Text(
                            text = if (isEn) metric.labelEn else metric.labelId,
                            fontSize = 10.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) SupabaseGreen else TextSecondaryDark,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (rankings.isEmpty()) {
                Text("Belum ada data warung", color = TextMutedDark, fontSize = 12.sp)
            } else {
                val maxMetricVal = remember(rankings, selectedMetric) {
                    val m = rankings.maxOfOrNull {
                        when (selectedMetric) {
                            StoreBarMetric.SALES -> it.salesRevenue
                            StoreBarMetric.STOCK_PCS -> it.activeStockPieces.toDouble()
                            StoreBarMetric.STOCK_VALUE -> it.stockRetailValue
                        }
                    } ?: 1.0
                    max(m, 1.0)
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rankings.take(8).forEachIndexed { idx, item ->
                        val rawVal = when (selectedMetric) {
                            StoreBarMetric.SALES -> item.salesRevenue
                            StoreBarMetric.STOCK_PCS -> item.activeStockPieces.toDouble()
                            StoreBarMetric.STOCK_VALUE -> item.stockRetailValue
                        }
                        val formattedVal = when (selectedMetric) {
                            StoreBarMetric.STOCK_PCS -> "${item.activeStockPieces} Pcs"
                            else -> formatRupiahCompact(rawVal)
                        }
                        val fraction = ((rawVal / maxMetricVal).toFloat() * animProgress).coerceIn(0.06f, 1f)
                        val barColor = when (idx) {
                            0 -> SupabaseGreen
                            1 -> BlueInfo
                            2 -> AmberWarning
                            else -> Color(0xFFA855F7)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(barColor.copy(alpha = 0.18f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = barColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.customer.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${item.customer.route_day}",
                                        fontSize = 10.sp,
                                        color = TextMutedDark
                                    )
                                }
                                Text(
                                    text = formattedVal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(CharcoalSurfaceElevated)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(barColor.copy(alpha = 0.65f), barColor)
                                            ),
                                            RoundedCornerShape(5.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyRouteColumnChartCard(
    isEn: Boolean,
    customers: List<Customer>,
    stocks: List<ConsignmentStock>,
    animProgress: Float
) {
    val days = remember {
        listOf(
            "Senin" to "Sen",
            "Selasa" to "Sel",
            "Rabu" to "Rab",
            "Kamis" to "Kam",
            "Jumat" to "Jum",
            "Sabtu" to "Sab",
            "Minggu" to "Min"
        )
    }

    val dayStats = remember(customers, stocks) {
        val stockByCust = stocks.groupBy { it.customer_id }
            .mapValues { entry -> entry.value.sumOf { it.current_quantity } }

        days.map { (fullDay, shortDay) ->
            val dayCusts = customers.filter { it.route_day.equals(fullDay, ignoreCase = true) }
            val totalPcs = dayCusts.sumOf { stockByCust[it.id] ?: 0 }
            Triple(shortDay, dayCusts.size, totalPcs)
        }
    }

    val maxPcs = remember(dayStats) {
        max(dayStats.maxOfOrNull { it.third } ?: 1, 1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = SupabaseGreen,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEn) "Weekly Route & Stock Distribution" else "Distribusi Beban Rute & Stok Harian (Senin–Minggu)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dayStats.forEach { (shortDay, storeCount, pcsCount) ->
                    val ratio = ((pcsCount.toFloat() / maxPcs.toFloat()) * animProgress).coerceIn(0.08f, 1f)
                    val isActive = storeCount > 0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = if (pcsCount > 0) "$pcsCount" else "0",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) SupabaseGreen else TextMutedDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barH = size.height * ratio
                                drawRoundRect(
                                    color = CharcoalSurfaceElevated,
                                    topLeft = Offset(0f, 0f),
                                    size = size,
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = if (isActive) {
                                            listOf(SupabaseGreen, SupabaseGreen.copy(alpha = 0.5f))
                                        } else {
                                            listOf(CharcoalBorder, CharcoalBorder)
                                        }
                                    ),
                                    topLeft = Offset(0f, size.height - barH),
                                    size = Size(size.width, barH),
                                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = shortDay,
                            fontSize = 10.5.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) TextPrimaryDark else TextMutedDark
                        )
                        Text(
                            text = "$storeCount toko",
                            fontSize = 8.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashflowComparisonChartCard(
    isEn: Boolean,
    businessSummary: BusinessFinancialSummary,
    personalSummary: PersonalFinancialSummary,
    potentialRetailValue: Double,
    potentialCostValue: Double,
    animProgress: Float
) {
    val effectiveRevenue = if (businessSummary.totalSalesRevenue > 0) businessSummary.totalSalesRevenue else potentialRetailValue
    val effectiveHpp = if (businessSummary.totalCostOfGoodsSold > 0) businessSummary.totalCostOfGoodsSold else potentialCostValue
    val effectiveNet = (effectiveRevenue - effectiveHpp) - businessSummary.operationalExpenses

    val maxRef = max(
        max(effectiveRevenue, effectiveHpp),
        max(abs(effectiveNet), max(personalSummary.totalIncome, personalSummary.totalExpense))
    ).coerceAtLeast(1.0)

    val bars = listOf(
        Triple(if (isEn) "Gross Revenue / Stock Value" else "Omset / Nilai Jual", effectiveRevenue, SupabaseGreen),
        Triple(if (isEn) "COGS (Modal HPP)" else "Modal HPP Produk", effectiveHpp, AmberWarning),
        Triple(if (isEn) "Operational Expenses" else "Biaya Operasional Usaha", businessSummary.operationalExpenses, RoseError),
        Triple(if (isEn) "Net Business Profit" else "Laba Bersih Usaha", effectiveNet, BlueInfo),
        Triple(if (isEn) "Personal Cash Balance" else "Saldo Buku Kas Pribadi", personalSummary.balance, Color(0xFFA855F7))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color(0xFFA855F7),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEn) "Financial Structure & Profit Waterfall" else "Struktur Arus Kas, HPP & Profitabilitas",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                bars.forEach { (label, amount, color) ->
                    val ratio = ((abs(amount) / maxRef).toFloat() * animProgress).coerceIn(0.05f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = formatRupiahCompact(amount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(CharcoalSurfaceElevated)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .background(color, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = TextSecondaryDark
        )
    }
}

private fun formatRupiahCompact(amount: Double): String {
    val sign = if (amount < 0) "-" else ""
    val absVal = abs(amount)
    return when {
        absVal >= 1_000_000_000 -> "${sign}Rp %.1f M".format(absVal / 1_000_000_000.0)
        absVal >= 1_000_000 -> "${sign}Rp %.1f Jt".format(absVal / 1_000_000.0)
        absVal >= 1_000 -> "${sign}Rp %,.0f".format(absVal)
        else -> "${sign}Rp %.0f".format(absVal)
    }
}
