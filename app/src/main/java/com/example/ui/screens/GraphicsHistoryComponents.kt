package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.entity.FinancialCategory
import com.example.data.local.entity.FinancialRecord
import com.example.data.local.entity.Product
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
import com.example.ui.theme.isEnglishLanguageActive
import com.example.ui.theme.tr
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class AnalyticsPeriodMode(val labelId: String, val labelEn: String) {
    SPECIFIC_DATE("Pilih Tanggal", "By Date"),
    SPECIFIC_MONTH("Pilih Bulan", "By Month"),
    TODAY("Hari Ini", "Today"),
    DAYS_7("7 Hari", "7 Days"),
    DAYS_30("30 Hari", "30 Days"),
    ALL("Semua Waktu", "All Time")
}

data class DailyHistorySummary(
    val dayKey: String, // yyyy-MM-dd
    val startOfDayMillis: Long,
    val displayDate: String,
    val transactions: List<TransactionWithDetails>,
    val expenses: List<FinancialRecord>,
    val totalRevenue: Double,
    val totalPaid: Double,
    val totalHpp: Double,
    val totalOpEx: Double,
    val netProfit: Double,
    val totalSoldPieces: Int,
    val totalAddedPieces: Int
)

private val MONTH_NAMES_ID = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

private val MONTH_NAMES_EN = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private val MONTH_SHORT_ID = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
)

private val MONTH_SHORT_EN = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private fun monthName(idx: Int): String = if (isEnglishLanguageActive) MONTH_NAMES_EN[idx.coerceIn(0, 11)] else MONTH_NAMES_ID[idx.coerceIn(0, 11)]
private fun monthShort(idx: Int): String = if (isEnglishLanguageActive) MONTH_SHORT_EN[idx.coerceIn(0, 11)] else MONTH_SHORT_ID[idx.coerceIn(0, 11)]

fun computePeriodBounds(
    mode: AnalyticsPeriodMode,
    selectedYear: Int,
    selectedMonth: Int,
    selectedDayOfMonth: Int
): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    return when (mode) {
        AnalyticsPeriodMode.ALL -> 0L to Long.MAX_VALUE
        AnalyticsPeriodMode.TODAY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            start to cal.timeInMillis
        }
        AnalyticsPeriodMode.DAYS_7 -> {
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -6)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to end
        }
        AnalyticsPeriodMode.DAYS_30 -> {
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -29)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis to end
        }
        AnalyticsPeriodMode.SPECIFIC_MONTH -> {
            cal.set(selectedYear, selectedMonth, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(selectedYear, selectedMonth, maxDay, 23, 59, 59)
            cal.set(Calendar.MILLISECOND, 999)
            start to cal.timeInMillis
        }
        AnalyticsPeriodMode.SPECIFIC_DATE -> {
            cal.set(selectedYear, selectedMonth, 1, 0, 0, 0)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val safeDay = selectedDayOfMonth.coerceIn(1, maxDay)
            cal.set(selectedYear, selectedMonth, safeDay, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.set(selectedYear, selectedMonth, safeDay, 23, 59, 59)
            cal.set(Calendar.MILLISECOND, 999)
            start to cal.timeInMillis
        }
    }
}

fun buildDailyHistoryList(
    transactions: List<TransactionWithDetails>,
    bizRecords: List<FinancialRecord>,
    productMap: Map<Long, Product>
): List<DailyHistorySummary> {
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayFmt = SimpleDateFormat("EEEE, dd MMM yyyy", if (isEnglishLanguageActive) Locale.ENGLISH else Locale("id", "ID"))

    val txByDay = transactions.groupBy { keyFmt.format(Date(it.header.transaction_date)) }
    val expByDay = bizRecords
        .filter { it.category == FinancialCategory.BUSINESS_EXPENSE }
        .groupBy { keyFmt.format(Date(it.transaction_date)) }

    val allKeys = (txByDay.keys + expByDay.keys).distinct().sortedDescending()

    return allKeys.map { key ->
        val dayTx = txByDay[key].orEmpty().sortedByDescending { it.header.transaction_date }
        val dayExp = expByDay[key].orEmpty().sortedByDescending { it.transaction_date }

        val sampleMillis = dayTx.firstOrNull()?.header?.transaction_date
            ?: dayExp.firstOrNull()?.transaction_date
            ?: System.currentTimeMillis()

        val cal = Calendar.getInstance().apply {
            timeInMillis = sampleMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val rev = dayTx.sumOf { it.header.total_sold_amount }
        val paid = dayTx.sumOf { it.header.amount_paid }
        var hpp = 0.0
        var soldPcs = 0
        var addedPcs = 0

        dayTx.forEach { tx ->
            tx.details.forEach { (d, p) ->
                val prod = p ?: productMap[d.product_id]
                hpp += d.sold_quantity * (prod?.cost_price ?: 0.0)
                soldPcs += d.sold_quantity
                addedPcs += d.added_quantity
            }
        }

        val opEx = dayExp.sumOf { it.amount }
        val net = (rev - hpp) - opEx

        DailyHistorySummary(
            dayKey = key,
            startOfDayMillis = cal.timeInMillis,
            displayDate = displayFmt.format(Date(cal.timeInMillis)),
            transactions = dayTx,
            expenses = dayExp,
            totalRevenue = rev,
            totalPaid = paid,
            totalHpp = hpp,
            totalOpEx = opEx,
            netProfit = net,
            totalSoldPieces = soldPcs,
            totalAddedPieces = addedPcs
        )
    }
}

/**
 * Interactive Filter & Historical Date/Month Navigator Bar
 */
@Composable
fun DateAndMonthFilterControlCard(
    isEn: Boolean,
    selectedMode: AnalyticsPeriodMode,
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    allTransactions: List<TransactionWithDetails>,
    onSelectMode: (AnalyticsPeriodMode) -> Unit,
    onUpdateDate: (year: Int, month: Int, day: Int) -> Unit,
    onOpenCalendarModal: () -> Unit
) {
    // Active transaction count per day in the currently selected month
    val monthDayTxMap = remember(allTransactions, selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        val map = mutableMapOf<Int, Pair<Int, Double>>() // dayOfMonth -> (count, totalRevenue)
        allTransactions.forEach { tx ->
            cal.timeInMillis = tx.header.transaction_date
            if (cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth) {
                val d = cal.get(Calendar.DAY_OF_MONTH)
                val prev = map[d] ?: (0 to 0.0)
                map[d] = (prev.first + 1) to (prev.second + tx.header.total_sold_amount)
            }
        }
        map
    }

    val maxDaysInSelectedMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val formattedSelectedDate = remember(selectedYear, selectedMonth, selectedDay, isEn) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth, selectedDay.coerceIn(1, maxDaysInSelectedMonth))
        SimpleDateFormat("EEEE, dd MMM yyyy", if (isEn) Locale.ENGLISH else Locale("id", "ID")).format(cal.time)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Mode Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AnalyticsPeriodMode.entries) { mode ->
                val selected = selectedMode == mode
                Surface(
                    modifier = Modifier
                        .clickable { onSelectMode(mode) }
                        .border(
                            width = 1.dp,
                            color = if (selected) SupabaseGreen else CharcoalBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .testTag("filter_mode_${mode.name}"),
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) SupabaseGreen.copy(alpha = 0.16f) else CharcoalSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mode == AnalyticsPeriodMode.SPECIFIC_DATE || mode == AnalyticsPeriodMode.SPECIFIC_MONTH) {
                            Icon(
                                imageVector = if (mode == AnalyticsPeriodMode.SPECIFIC_DATE) Icons.Default.CalendarToday else Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (selected) SupabaseGreen else TextSecondaryDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = if (isEn) mode.labelEn else mode.labelId,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) SupabaseGreen else TextSecondaryDark
                        )
                    }
                }
            }
        }

        // 2. Interactive Historical Date / Month Selector Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SupabaseGreen.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: Month/Year Navigator + Button "Kalender History"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev / Next Month Controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, 1)
                                    add(Calendar.MONTH, -1)
                                }
                                val newY = cal.get(Calendar.YEAR)
                                val newM = cal.get(Calendar.MONTH)
                                val maxD = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                onUpdateDate(newY, newM, selectedDay.coerceIn(1, maxD))
                                if (selectedMode != AnalyticsPeriodMode.SPECIFIC_DATE && selectedMode != AnalyticsPeriodMode.SPECIFIC_MONTH) {
                                    onSelectMode(AnalyticsPeriodMode.SPECIFIC_MONTH)
                                }
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Bulan Lalu",
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onOpenCalendarModal() }
                                .padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "${monthName(selectedMonth)} $selectedYear",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = when (selectedMode) {
                                    AnalyticsPeriodMode.SPECIFIC_DATE -> formattedSelectedDate
                                    AnalyticsPeriodMode.SPECIFIC_MONTH -> tr(
                                        "Performa 1 Bulan Penuh (${monthDayTxMap.values.sumOf { it.first }} Nota)",
                                        "Full Month Performance (${monthDayTxMap.values.sumOf { it.first }} Receipts)"
                                    )
                                    AnalyticsPeriodMode.TODAY -> tr("Filter: Hari Ini", "Filter: Today")
                                    AnalyticsPeriodMode.DAYS_7 -> tr("Filter: 7 Hari Terakhir", "Filter: Last 7 Days")
                                    AnalyticsPeriodMode.DAYS_30 -> tr("Filter: 30 Hari Terakhir", "Filter: Last 30 Days")
                                    AnalyticsPeriodMode.ALL -> tr("Filter: Semua Waktu (Ketuk tgl untuk detail)", "Filter: All Time (Tap date for details)")
                                },
                                fontSize = 10.5.sp,
                                color = SupabaseGreen
                            )
                        }

                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, 1)
                                    add(Calendar.MONTH, 1)
                                }
                                val newY = cal.get(Calendar.YEAR)
                                val newM = cal.get(Calendar.MONTH)
                                val maxD = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                onUpdateDate(newY, newM, selectedDay.coerceIn(1, maxD))
                                if (selectedMode != AnalyticsPeriodMode.SPECIFIC_DATE && selectedMode != AnalyticsPeriodMode.SPECIFIC_MONTH) {
                                    onSelectMode(AnalyticsPeriodMode.SPECIFIC_MONTH)
                                }
                            },
                            modifier = Modifier
                                .size(30.dp)
                                .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Bulan Depan",
                                tint = TextPrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Open Full Calendar Picker Modal Button
                    Surface(
                        modifier = Modifier
                            .clickable { onOpenCalendarModal() }
                            .border(1.dp, SupabaseGreen, RoundedCornerShape(8.dp))
                            .testTag("open_calendar_history_modal_button"),
                        shape = RoundedCornerShape(8.dp),
                        color = SupabaseGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Pilih Tanggal/Bulan",
                                tint = SupabaseGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isEn) "Pick Date" else "Kalender",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SupabaseGreen
                            )
                        }
                    }
                }

                // Horizontal Scrollable Date Strip for Selected Month (1..maxDays)
                // Allows 1-tap switching to any specific date in that month, with green dots for days that have history!
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    // "1 Bulan Penuh" chip at start
                    item {
                        val isMonthFullSelected = selectedMode == AnalyticsPeriodMode.SPECIFIC_MONTH
                        Surface(
                            modifier = Modifier
                                .clickable { onSelectMode(AnalyticsPeriodMode.SPECIFIC_MONTH) }
                                .border(
                                    1.dp,
                                    if (isMonthFullSelected) SupabaseGreen else CharcoalBorder,
                                    RoundedCornerShape(10.dp)
                                ),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isMonthFullSelected) SupabaseGreen else CharcoalSurfaceElevated
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tr("1 BULAN", "1 MONTH"),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMonthFullSelected) Color(0xFF042114) else TextSecondaryDark
                                )
                                Text(
                                    text = monthShort(selectedMonth),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isMonthFullSelected) Color(0xFF042114) else TextPrimaryDark
                                )
                            }
                        }
                    }

                    items((1..maxDaysInSelectedMonth).toList()) { dayNum ->
                        val isDateSelected = selectedMode == AnalyticsPeriodMode.SPECIFIC_DATE && selectedDay == dayNum
                        val dayStat = monthDayTxMap[dayNum]
                        val hasTx = dayStat != null && dayStat.first > 0

                        Surface(
                            modifier = Modifier
                                .clickable {
                                    onUpdateDate(selectedYear, selectedMonth, dayNum)
                                    onSelectMode(AnalyticsPeriodMode.SPECIFIC_DATE)
                                }
                                .border(
                                    width = 1.dp,
                                    color = when {
                                        isDateSelected -> SupabaseGreen
                                        hasTx -> SupabaseGreen.copy(alpha = 0.55f)
                                        else -> CharcoalBorder
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isDateSelected -> SupabaseGreen
                                hasTx -> SupabaseGreen.copy(alpha = 0.12f)
                                else -> CharcoalSurfaceElevated
                            }
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(42.dp)
                                    .padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "%02d".format(dayNum),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDateSelected) Color(0xFF042114) else TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (hasTx) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(
                                                if (isDateSelected) Color(0xFF042114) else SupabaseGreen,
                                                CircleShape
                                            )
                                    )
                                } else {
                                    Text(
                                        text = monthShort(selectedMonth),
                                        fontSize = 8.5.sp,
                                        color = if (isDateSelected) Color(0xFF042114) else TextMutedDark
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Prev/Next Day bar when SPECIFIC_DATE is active
                if (selectedMode == AnalyticsPeriodMode.SPECIFIC_DATE) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tr("◀ Hari Sebelumnya", "◀ Prev Day"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SupabaseGreen,
                            modifier = Modifier
                                .clickable {
                                    val cal = Calendar.getInstance().apply {
                                        set(selectedYear, selectedMonth, selectedDay)
                                        add(Calendar.DAY_OF_MONTH, -1)
                                    }
                                    onUpdateDate(
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    )
                                }
                                .padding(4.dp)
                        )

                        Text(
                            text = formattedSelectedDate,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )

                        Text(
                            text = tr("Hari Berikutnya ▶", "Next Day ▶"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SupabaseGreen,
                            modifier = Modifier
                                .clickable {
                                    val cal = Calendar.getInstance().apply {
                                        set(selectedYear, selectedMonth, selectedDay)
                                        add(Calendar.DAY_OF_MONTH, 1)
                                    }
                                    onUpdateDate(
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    )
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Wide (96%) Interactive Calendar Date & Month Picker Modal
 * Shows full month grid + highlights dates with historical transactions + quick active dates list.
 */
@Composable
fun CalendarDateMonthPickerModal(
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    allTransactions: List<TransactionWithDetails>,
    onSelectFullMonth: (year: Int, month: Int) -> Unit,
    onSelectSpecificDate: (year: Int, month: Int, day: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pickerYear by remember { mutableIntStateOf(initialYear) }
    var pickerMonth by remember { mutableIntStateOf(initialMonth) }
    var pickerDay by remember { mutableIntStateOf(initialDay) }

    val dayActivityMap = remember(allTransactions, pickerYear, pickerMonth) {
        val cal = Calendar.getInstance()
        val map = mutableMapOf<Int, Pair<Int, Double>>()
        allTransactions.forEach { tx ->
            cal.timeInMillis = tx.header.transaction_date
            if (cal.get(Calendar.YEAR) == pickerYear && cal.get(Calendar.MONTH) == pickerMonth) {
                val d = cal.get(Calendar.DAY_OF_MONTH)
                val curr = map[d] ?: (0 to 0.0)
                map[d] = (curr.first + 1) to (curr.second + tx.header.total_sold_amount)
            }
        }
        map
    }

    // Recent active dates across all history so user can jump directly to any past date with data
    val recentActiveDates = remember(allTransactions, isEnglishLanguageActive) {
        val fmt = SimpleDateFormat("dd MMM yyyy", if (isEnglishLanguageActive) Locale.ENGLISH else Locale("id", "ID"))
        val cal = Calendar.getInstance()
        allTransactions
            .groupBy {
                cal.timeInMillis = it.header.transaction_date
                Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            }
            .entries
            .sortedByDescending { entry -> entry.value.maxOf { it.header.transaction_date } }
            .take(10)
            .map { (ymd, list) ->
                cal.set(ymd.first, ymd.second, ymd.third)
                val label = fmt.format(cal.time)
                val rev = list.sumOf { it.header.total_sold_amount }
                Triple(ymd, tr("$label (${list.size} nota)", "$label (${list.size} tx)"), rev)
            }
    }

    val calendarCells = remember(pickerYear, pickerMonth) {
        val cal = Calendar.getInstance()
        cal.set(pickerYear, pickerMonth, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun .. 7 = Sat
        val offset = (firstDayOfWeek + 5) % 7 // Monday-based (0 = Mon .. 6 = Sun)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cells = mutableListOf<Int?>()
        repeat(offset) { cells.add(null) }
        for (d in 1..maxDay) {
            cells.add(d)
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        cells
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 700.dp)
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = CharcoalSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tr("Pilih Tanggal & Bulan Analisis", "Select Analysis Date & Month"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = tr("Titik hijau menandakan tanggal dengan riwayat transaksi", "Green dots indicate dates with transaction history"),
                            fontSize = 11.sp,
                            color = SupabaseGreen
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = tr("Tutup", "Close"), tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Year & Month Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(10.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (pickerMonth == 0) {
                                pickerMonth = 11
                                pickerYear -= 1
                            } else {
                                pickerMonth -= 1
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = TextPrimaryDark)
                    }

                    Text(
                        text = "${monthName(pickerMonth)} $pickerYear",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    IconButton(
                        onClick = {
                            if (pickerMonth == 11) {
                                pickerMonth = 0
                                pickerYear += 1
                            } else {
                                pickerMonth += 1
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next", tint = TextPrimaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Month Grid (12 Months)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items((0..11).toList()) { mIdx ->
                        val selected = pickerMonth == mIdx
                        Surface(
                            modifier = Modifier
                                .clickable { pickerMonth = mIdx }
                                .border(
                                    1.dp,
                                    if (selected) SupabaseGreen else CharcoalBorder,
                                    RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) SupabaseGreen.copy(alpha = 0.16f) else CharcoalSurfaceElevated
                        ) {
                            Text(
                                text = monthShort(mIdx),
                                fontSize = 11.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) SupabaseGreen else TextSecondaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Day of week header (Sen..Min)
                Row(modifier = Modifier.fillMaxWidth()) {
                    val dayHeaders = if (isEnglishLanguageActive) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    } else {
                        listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                    }
                    dayHeaders.forEach { dName ->
                        Text(
                            text = dName,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Calendar Day Grid
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    calendarCells.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { dayVal ->
                                if (dayVal == null) {
                                    Spacer(modifier = Modifier.weight(1f).height(40.dp))
                                } else {
                                    val isSel = dayVal == pickerDay
                                    val act = dayActivityMap[dayVal]
                                    val hasTx = act != null && act.first > 0
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .clickable {
                                                pickerDay = dayVal
                                                onSelectSpecificDate(pickerYear, pickerMonth, dayVal)
                                            }
                                            .border(
                                                width = 1.dp,
                                                color = when {
                                                    isSel -> SupabaseGreen
                                                    hasTx -> SupabaseGreen.copy(alpha = 0.6f)
                                                    else -> CharcoalBorder
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isSel -> SupabaseGreen
                                            hasTx -> SupabaseGreen.copy(alpha = 0.14f)
                                            else -> CharcoalSurfaceElevated
                                        }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayVal",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color(0xFF042114) else TextPrimaryDark
                                            )
                                            if (hasTx) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(
                                                            if (isSel) Color(0xFF042114) else SupabaseGreen,
                                                            CircleShape
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

                // Quick Jump to Active History Dates
                if (recentActiveDates.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = tr("Pintasan Tanggal dengan Riwayat Transaksi:", "Quick Jump to Active History Dates:"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(recentActiveDates) { (ymd, label, _) ->
                            Surface(
                                modifier = Modifier
                                    .clickable {
                                        onSelectSpecificDate(ymd.first, ymd.second, ymd.third)
                                    }
                                    .border(1.dp, SupabaseGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                shape = RoundedCornerShape(8.dp),
                                color = CharcoalSurfaceElevated
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = SupabaseGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: "Lihat 1 Bulan Penuh" vs "Lihat Tanggal Ini"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSelectFullMonth(pickerYear, pickerMonth) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CharcoalSurfaceElevated,
                            contentColor = SupabaseGreen
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SupabaseGreen)
                    ) {
                        Text(
                            text = tr("Analisis Bulan ${monthShort(pickerMonth)}", "Analyze ${monthShort(pickerMonth)} Month"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { onSelectSpecificDate(pickerYear, pickerMonth, pickerDay) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SupabaseGreen,
                            contentColor = Color(0xFF042114)
                        )
                    ) {
                        Text(
                            text = tr("Lihat Tgl $pickerDay ${monthShort(pickerMonth)}", "View $pickerDay ${monthShort(pickerMonth)}"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Historical Performance & Daily Visit Log Card at the bottom of GraphicsAnalysisScreen.
 * Lets the user inspect exact performance & transaction details for any selected date or month.
 */
@Composable
fun HistoricalPerformanceLogCard(
    isEn: Boolean,
    periodLabel: String,
    dailySummaries: List<DailyHistorySummary>,
    productMap: Map<Long, Product>,
    onFocusSpecificDate: (year: Int, month: Int, day: Int) -> Unit
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = SupabaseGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (isEn) "Historical Performance by Date" else "Riwayat Performa & Detail Transaksi per Tanggal",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = periodLabel,
                            fontSize = 11.sp,
                            color = SupabaseGreen
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tr("${dailySummaries.size} Hari Aktif", "${dailySummaries.size} Active Days"),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SupabaseGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (dailySummaries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurfaceElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tr("Belum Ada Riwayat Transaksi pada Periode/Tanggal Ini", "No Transaction History on This Date/Period"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tr("Gunakan tombol Kalender di atas atau geser tanggal/bulan untuk melihat riwayat performa di tanggal lainnya.", "Use the Calendar button above or swipe dates/months to inspect historical performance."),
                            fontSize = 11.sp,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    dailySummaries.forEachIndexed { index, daySummary ->
                        DailyPerformanceAccordionItem(
                            daySummary = daySummary,
                            initiallyExpanded = index == 0 || dailySummaries.size == 1,
                            productMap = productMap,
                            onFocusDate = {
                                val cal = Calendar.getInstance().apply { timeInMillis = daySummary.startOfDayMillis }
                                onFocusSpecificDate(
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyPerformanceAccordionItem(
    daySummary: DailyHistorySummary,
    initiallyExpanded: Boolean,
    productMap: Map<Long, Product>,
    onFocusDate: () -> Unit
) {
    var expanded by remember(daySummary.dayKey) { mutableStateOf(initiallyExpanded) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (expanded) SupabaseGreen.copy(alpha = 0.5f) else CharcoalBorder,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = CharcoalSurfaceElevated
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Header Row of the Day
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = daySummary.displayDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = tr(
                            "${daySummary.transactions.size} Kunjungan • Laku ${daySummary.totalSoldPieces} Pcs • Titip +${daySummary.totalAddedPieces} Pcs",
                            "${daySummary.transactions.size} Visits • Sold ${daySummary.totalSoldPieces} Pcs • Added +${daySummary.totalAddedPieces} Pcs"
                        ),
                        fontSize = 10.5.sp,
                        color = TextSecondaryDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Rp %,.0f".format(daySummary.totalRevenue),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SupabaseGreen
                        )
                        Text(
                            text = "${tr("Laba", "Profit")}: Rp %,.0f".format(daySummary.netProfit),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (daySummary.netProfit >= 0) BlueInfo else RoseError
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = CharcoalBorder, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Mini KPI Breakdown for that specific day
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(tr("Omset", "Revenue"), fontSize = 9.5.sp, color = TextMutedDark)
                        Text("Rp %,.0f".format(daySummary.totalRevenue), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SupabaseGreen)
                    }
                    Column {
                        Text(tr("Modal HPP", "COGS"), fontSize = 9.5.sp, color = TextMutedDark)
                        Text("Rp %,.0f".format(daySummary.totalHpp), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = AmberWarning)
                    }
                    Column {
                        Text(tr("Operasional", "Expenses"), fontSize = 9.5.sp, color = TextMutedDark)
                        Text("Rp %,.0f".format(daySummary.totalOpEx), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = RoseError)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(tr("Setoran Kas", "Cash Paid"), fontSize = 9.5.sp, color = TextMutedDark)
                        Text("Rp %,.0f".format(daySummary.totalPaid), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button to focus all charts on this exact date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        modifier = Modifier
                            .clickable { onFocusDate() }
                            .border(1.dp, SupabaseGreen, RoundedCornerShape(6.dp)),
                        shape = RoundedCornerShape(6.dp),
                        color = SupabaseGreen.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = SupabaseGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tr("Tampilkan Grafik Khusus Tanggal Ini", "Show Charts for This Date"),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SupabaseGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of individual store visits / transactions on this date
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    daySummary.transactions.forEach { tx ->
                        val timeStr = timeFmt.format(Date(tx.header.transaction_date))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            color = CharcoalBg
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            Icons.Default.Store,
                                            contentDescription = null,
                                            tint = SupabaseGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = tx.customer?.name ?: "Warung #${tx.header.customer_id}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• $timeStr (#TRX-${tx.header.id})",
                                            fontSize = 10.sp,
                                            color = TextMutedDark
                                        )
                                    }
                                    Text(
                                        text = "Rp %,.0f".format(tx.header.total_sold_amount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SupabaseGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                tx.details.forEach { (d, p) ->
                                    val prodName = (p ?: productMap[d.product_id])?.name ?: "Produk #${d.product_id}"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• $prodName",
                                            fontSize = 10.5.sp,
                                            color = TextSecondaryDark,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = tr(
                                                "Laku ${d.sold_quantity} | Sisa ${d.remaining_stock} | +Titip ${d.added_quantity}",
                                                "Sold ${d.sold_quantity} | Rem ${d.remaining_stock} | +Add ${d.added_quantity}"
                                            ),
                                            fontSize = 10.sp,
                                            color = TextPrimaryDark
                                        )
                                    }
                                }

                                if (!tx.header.photo_uri.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    AsyncImage(
                                        model = tx.header.photo_uri,
                                        contentDescription = "Foto Kunjungan",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(95.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                        }
                    }

                    // Show Operational Expenses on that date if any
                    daySummary.expenses.forEach { exp ->
                        val timeStr = timeFmt.format(Date(exp.transaction_date))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RoseError.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, RoseError.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${tr("Pengeluaran", "Expense")} ($timeStr): ${exp.description}",
                                fontSize = 11.sp,
                                color = TextPrimaryDark,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "- Rp %,.0f".format(exp.amount),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoseError
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatPeriodLabel(
    mode: AnalyticsPeriodMode,
    year: Int,
    month: Int,
    day: Int
): String {
    return when (mode) {
        AnalyticsPeriodMode.SPECIFIC_DATE -> {
            val cal = Calendar.getInstance()
            cal.set(year, month, 1)
            val maxD = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(year, month, day.coerceIn(1, maxD))
            val fmt = SimpleDateFormat("EEEE, dd MMMM yyyy", if (isEnglishLanguageActive) Locale.ENGLISH else Locale("id", "ID"))
            "${tr("Tanggal Spesifik", "Specific Date")}: ${fmt.format(cal.time)}"
        }
        AnalyticsPeriodMode.SPECIFIC_MONTH -> "${tr("Bulan Spesifik", "Specific Month")}: ${monthName(month)} $year"
        AnalyticsPeriodMode.TODAY -> tr("Periode: Hari Ini", "Period: Today")
        AnalyticsPeriodMode.DAYS_7 -> tr("Periode: 7 Hari Terakhir", "Period: Last 7 Days")
        AnalyticsPeriodMode.DAYS_30 -> tr("Periode: 30 Hari Terakhir", "Period: Last 30 Days")
        AnalyticsPeriodMode.ALL -> tr("Periode: Semua Waktu (Riwayat Penuh)", "Period: All Time (Full History)")
    }
}

/**
 * Interactive Daily Performance Bar Chart by Date/Month.
 * Lets the user visually compare daily Omset & Net Profit across every day of the selected month
 * (or active historical dates), and tap any bar to inspect or lock onto that specific date.
 */
@Composable
fun DailyPerformanceBarChartCard(
    isEn: Boolean,
    selectedYear: Int,
    selectedMonth: Int,
    selectedDay: Int,
    selectedMode: AnalyticsPeriodMode,
    allTransactions: List<TransactionWithDetails>,
    allBizRecords: List<FinancialRecord>,
    productMap: Map<Long, Product>,
    animProgress: Float,
    onSelectSpecificDate: (year: Int, month: Int, day: Int) -> Unit
) {
    var showOnlyActiveDays by remember { mutableStateOf(false) }

    val maxDays = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Compute per-day stats for the selected month (1..maxDays)
    data class DayBarEntry(
        val dayOfMonth: Int,
        val revenue: Double,
        val netProfit: Double,
        val paid: Double,
        val soldPcs: Int,
        val visitCount: Int
    )

    val monthDayEntries = remember(allTransactions, allBizRecords, productMap, selectedYear, selectedMonth, maxDays) {
        val cal = Calendar.getInstance()
        val txMap = mutableMapOf<Int, MutableList<TransactionWithDetails>>()
        allTransactions.forEach { tx ->
            cal.timeInMillis = tx.header.transaction_date
            if (cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth) {
                val d = cal.get(Calendar.DAY_OF_MONTH)
                txMap.getOrPut(d) { mutableListOf() }.add(tx)
            }
        }
        val expMap = mutableMapOf<Int, Double>()
        allBizRecords.filter { it.category == FinancialCategory.BUSINESS_EXPENSE }.forEach { r ->
            cal.timeInMillis = r.transaction_date
            if (cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth) {
                val d = cal.get(Calendar.DAY_OF_MONTH)
                expMap[d] = (expMap[d] ?: 0.0) + r.amount
            }
        }

        (1..maxDays).map { d ->
            val list = txMap[d].orEmpty()
            val rev = list.sumOf { it.header.total_sold_amount }
            val paid = list.sumOf { it.header.amount_paid }
            var hpp = 0.0
            var pcs = 0
            list.forEach { tx ->
                tx.details.forEach { (det, p) ->
                    hpp += det.sold_quantity * ((p ?: productMap[det.product_id])?.cost_price ?: 0.0)
                    pcs += det.sold_quantity
                }
            }
            val opEx = expMap[d] ?: 0.0
            val profit = (rev - hpp) - opEx
            DayBarEntry(
                dayOfMonth = d,
                revenue = rev,
                netProfit = profit,
                paid = paid,
                soldPcs = pcs,
                visitCount = list.size
            )
        }
    }

    val displayedEntries = remember(monthDayEntries, showOnlyActiveDays) {
        if (showOnlyActiveDays) {
            val active = monthDayEntries.filter { it.visitCount > 0 || it.revenue > 0 }
            if (active.isNotEmpty()) active else monthDayEntries
        } else {
            monthDayEntries
        }
    }

    var inspectedDay by remember(selectedYear, selectedMonth, selectedDay) {
        mutableIntStateOf(selectedDay.coerceIn(1, maxDays))
    }

    val inspectedEntry = remember(monthDayEntries, inspectedDay) {
        monthDayEntries.find { it.dayOfMonth == inspectedDay }
            ?: monthDayEntries.firstOrNull()
    }

    val maxRev = remember(displayedEntries) {
        max(displayedEntries.maxOfOrNull { max(it.revenue, it.netProfit) } ?: 1.0, 1.0)
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
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = SupabaseGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) {
                                "Daily Performance Chart (${monthName(selectedMonth)} $selectedYear)"
                            } else {
                                "Grafik Performa Harian (${monthName(selectedMonth)} $selectedYear)"
                            },
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEn) {
                            "Tap any date bar to inspect daily sales & profit history"
                        } else {
                            "Ketuk batang tanggal untuk cek histori omset & laba di tanggal tersebut"
                        },
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }

                // Toggle All Days vs Active Days
                Surface(
                    modifier = Modifier
                        .clickable { showOnlyActiveDays = !showOnlyActiveDays }
                        .border(1.dp, SupabaseGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp),
                    color = if (showOnlyActiveDays) SupabaseGreen.copy(alpha = 0.16f) else CharcoalSurfaceElevated
                ) {
                    Text(
                        text = if (showOnlyActiveDays) tr("Tgl Aktif", "Active Days") else "1–$maxDays ${monthShort(selectedMonth)}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SupabaseGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inspected Day Summary Banner
            if (inspectedEntry != null) {
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
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "%s %02d %s %d".format(
                                    tr("Tgl", "Date"),
                                    inspectedEntry.dayOfMonth,
                                    monthName(selectedMonth),
                                    selectedYear
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = tr(
                                    "${inspectedEntry.visitCount} Nota • Laku ${inspectedEntry.soldPcs} Pcs • Kas Rp %,.0f".format(inspectedEntry.paid),
                                    "${inspectedEntry.visitCount} Receipts • Sold ${inspectedEntry.soldPcs} Pcs • Cash Rp %,.0f".format(inspectedEntry.paid)
                                ),
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(tr("Omset", "Revenue"), fontSize = 9.5.sp, color = TextMutedDark)
                                Text(
                                    text = "Rp %,.0f".format(inspectedEntry.revenue),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SupabaseGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(tr("Laba", "Profit"), fontSize = 9.5.sp, color = TextMutedDark)
                                Text(
                                    text = "Rp %,.0f".format(inspectedEntry.netProfit),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (inspectedEntry.netProfit >= 0) BlueInfo else RoseError
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Daily Bars (1..maxDays)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(displayedEntries) { entry ->
                    val isSelected = (selectedMode == AnalyticsPeriodMode.SPECIFIC_DATE && selectedDay == entry.dayOfMonth) ||
                        inspectedDay == entry.dayOfMonth
                    val revFraction = if (entry.revenue > 0) {
                        ((entry.revenue / maxRev).toFloat() * animProgress).coerceIn(0.10f, 1f)
                    } else 0.04f
                    val profitFraction = if (entry.netProfit > 0) {
                        ((entry.netProfit / maxRev).toFloat() * animProgress).coerceIn(0.08f, 1f)
                    } else 0.03f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) SupabaseGreen.copy(alpha = 0.12f) else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) SupabaseGreen else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                inspectedDay = entry.dayOfMonth
                                onSelectSpecificDate(selectedYear, selectedMonth, entry.dayOfMonth)
                            }
                            .padding(vertical = 6.dp, horizontal = 3.dp)
                    ) {
                        // Visit count badge on top if > 0
                        Text(
                            text = if (entry.visitCount > 0) "${entry.visitCount}x" else "-",
                            fontSize = 9.sp,
                            fontWeight = if (entry.visitCount > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (entry.visitCount > 0) SupabaseGreen else TextMutedDark
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Dual Vertical Bar Container
                        Row(
                            modifier = Modifier
                                .height(105.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Revenue Bar
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight(revFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (entry.revenue > 0) {
                                            Brush.verticalGradient(listOf(SupabaseGreen, SupabaseGreen.copy(alpha = 0.6f)))
                                        } else {
                                            Brush.verticalGradient(listOf(CharcoalBorder, CharcoalBorder))
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            // Profit Bar
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight(profitFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (entry.netProfit > 0) {
                                            Brush.verticalGradient(listOf(BlueInfo, BlueInfo.copy(alpha = 0.6f)))
                                        } else {
                                            Brush.verticalGradient(listOf(CharcoalBorder, CharcoalBorder))
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = "%02d".format(entry.dayOfMonth),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected || entry.visitCount > 0) FontWeight.ExtraBold else FontWeight.Medium,
                            color = when {
                                isSelected -> SupabaseGreen
                                entry.visitCount > 0 -> TextPrimaryDark
                                else -> TextMutedDark
                            }
                        )
                        Text(
                            text = monthShort(selectedMonth),
                            fontSize = 8.5.sp,
                            color = TextMutedDark
                        )
                    }
                }
            }
        }
    }
}

private data class KerupukVariantQuota(
    val code: String,
    val name: String,
    val packsPerRoute: Int,
    val color: Color
)

/**
 * Grafik Rekapitulasi Strategi Usaha Kerupuk (60 Bks/Rute • 360 Bks/Mgg • 24 Rute/Bln):
 * 1. Komposisi 8 Varian per Rute (Total 60 Bks untuk 20-27 Warung, 3 Varian/Warung)
 * 2. Target & Struktur Keuangan Bulanan (Omzet Rp 23.040.000, Modal Rp 15.840.000, Laba Dagang Rp 7.200.000)
 * 3. Alokasi Dana Bersih Bulanan (Tabungan Bersih Rp 5.170.000 [71.8%], Makan Maks Rp 30rb/hr, Rokok Maks Rp 20rb/hr, Bensin & Servis Motor Rute 25 km)
 */
@Composable
fun KerupukBusinessStrategyRecapCard(
    actualRevenue: Double,
    actualGrossProfit: Double,
    actualOpEx: Double,
    animProgress: Float
) {
    val variants = remember {
        listOf(
            KerupukVariantQuota("SB", "SB Pedas", 15, Color(0xFF3ECF8E)),
            KerupukVariantQuota("KP", "KP Original", 15, Color(0xFF38BDF8)),
            KerupukVariantQuota("AO", "AO (Combine)", 8, Color(0xFFF59E0B)),
            KerupukVariantQuota("JK", "JK (Combine)", 6, Color(0xFFA855F7)),
            KerupukVariantQuota("ST", "ST Original", 5, Color(0xFF14B8A6)),
            KerupukVariantQuota("DD", "DD Rambak Tdk Pedas", 5, Color(0xFFEC4899)),
            KerupukVariantQuota("BO", "BO (Combine)", 4, Color(0xFF6366F1)),
            KerupukVariantQuota("MK", "MK (Combine)", 2, Color(0xFFF43F5E))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SupabaseGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(14.dp)
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
                        text = tr(
                            "Rekapitulasi Strategi Usaha Kerupuk",
                            "Kerupuk Business Strategy Recap"
                        ),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = tr(
                            "60 Bks/Rute (20–27 Warung) • 6 Rute/Mgg (360 Bks) • 24 Rute/Bln (1.440 Bks)",
                            "60 Pks/Route (20–27 Stores) • 6 Routes/Wk (360 Pks) • 24 Routes/Mo (1,440 Pks)"
                        ),
                        fontSize = 10.5.sp,
                        color = SupabaseGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SECTION 1: Komposisi 60 Bungkus per Rute (Stacked Bar + Breakdown)
            Text(
                text = tr(
                    "1. Komposisi Varian per Rute (Total 60 Bks • 3 Varian/Warung)",
                    "1. Variant Composition per Route (Total 60 Pks • 3 Variants/Store)"
                ),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Multi-segment horizontal stacked bar for 60 packs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CharcoalSurfaceElevated)
            ) {
                variants.forEach { v ->
                    val weightFrac = (v.packsPerRoute / 60f) * animProgress.coerceAtLeast(0.05f)
                    Box(
                        modifier = Modifier
                            .weight(weightFrac.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(v.color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2-column grid of the 8 variants
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                variants.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { v ->
                            val pct = (v.packsPerRoute / 60.0) * 100.0
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(CharcoalSurfaceElevated, RoundedCornerShape(7.dp))
                                    .border(1.dp, CharcoalBorder, RoundedCornerShape(7.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(v.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = v.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${v.packsPerRoute} (${"%.0f".format(pct)}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = v.color
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CharcoalBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // SECTION 2: Grafik Keuangan Bulanan (Modal Rp 11.000 -> Jual Rp 16.000 -> Laba Rp 5.000/bks)
            Text(
                text = tr(
                    "2. Perhitungan Keuangan Bulanan (Modal Rp 11rb • Jual Rp 16rb • Untung Rp 5rb/Bks)",
                    "2. Monthly Financials (Cost Rp 11k • Sell Rp 16k • Profit Rp 5k/Pk)"
                ),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(8.dp))

            val monthlyTargetOmzet = 23_040_000.0
            val monthlyTargetCost = 15_840_000.0
            val monthlyTargetGrossProfit = 7_200_000.0

            StrategyBarRow(
                label = tr("Omzet Kotor Bulanan (1.440 bks × Rp 16rb)", "Monthly Gross Revenue (1,440 × Rp 16k)"),
                valueText = "Rp 23.040.000",
                subText = if (actualRevenue > 0) tr("Aktual tercatat: Rp %,.0f", "Recorded actual: Rp %,.0f").format(actualRevenue) else tr("Rp 960.000 / rute × 24 rute", "Rp 960,000 / route × 24 routes"),
                fraction = 1f * animProgress,
                color = SupabaseGreen
            )
            Spacer(modifier = Modifier.height(6.dp))
            StrategyBarRow(
                label = tr("Modal Produksi / Kulakan (1.440 bks × Rp 11rb)", "Monthly Production / COGS (1,440 × Rp 11k)"),
                valueText = "Rp 15.840.000",
                subText = tr("68,75% dari Omzet (Rp 660.000 / rute)", "68.75% of Revenue (Rp 660,000 / route)"),
                fraction = (monthlyTargetCost / monthlyTargetOmzet).toFloat() * animProgress,
                color = AmberWarning
            )
            Spacer(modifier = Modifier.height(6.dp))
            StrategyBarRow(
                label = tr("Keuntungan Bersih Dagang (1.440 bks × Rp 5rb)", "Monthly Trading Profit (1,440 × Rp 5k)"),
                valueText = "Rp 7.200.000",
                subText = if (actualGrossProfit > 0) tr("Laba kotor tercatat: Rp %,.0f", "Recorded gross profit: Rp %,.0f").format(actualGrossProfit) else tr("31,25% Margin (Rp 300.000 / rute)", "31.25% Margin (Rp 300,000 / route)"),
                fraction = (monthlyTargetGrossProfit / monthlyTargetOmzet).toFloat() * animProgress,
                color = BlueInfo
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CharcoalBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // SECTION 3: Persentase Alokasi Dana Bersih (Rp 7.200.000 -> Sisa Bersih Ditabung Rp 5.170.000)
            Text(
                text = tr(
                    "3. Alokasi Laba Dagang (Rp 7,2 Jt) & Sisa Bersih Ditabung",
                    "3. Profit Allocation (Rp 7.2M) & Net Savings"
                ),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Stacked Bar of Profit Allocation (7.2M = 5.17M Savings + 900k Meals + 600k Cigarettes + 530k Motor/Fuel 25km)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(CharcoalSurfaceElevated)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.718f)
                        .fillMaxHeight()
                        .background(SupabaseGreen)
                )
                Box(
                    modifier = Modifier
                        .weight(0.125f)
                        .fillMaxHeight()
                        .background(AmberWarning)
                )
                Box(
                    modifier = Modifier
                        .weight(0.083f)
                        .fillMaxHeight()
                        .background(RoseError)
                )
                Box(
                    modifier = Modifier
                        .weight(0.074f)
                        .fillMaxHeight()
                        .background(BlueInfo)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AllocationRowItem(
                dotColor = SupabaseGreen,
                title = tr("Sisa Bersih Murni (Aman Ditabung)", "Pure Net Savings (Safe to Save)"),
                subtitle = tr("Setelah potong operasional motor & biaya hidup", "After motor ops & daily living expenses"),
                amount = "Rp 5.170.000",
                pct = "71,8%",
                highlight = true
            )
            Spacer(modifier = Modifier.height(4.dp))
            AllocationRowItem(
                dotColor = AmberWarning,
                title = tr("Batas Makan Harian (Maks Rp 30.000/hari)", "Daily Meal Cap (Max Rp 30,000/day)"),
                subtitle = tr("Estimasi 30 hari × Rp 30.000", "Est. 30 days × Rp 30,000"),
                amount = "Rp 900.000",
                pct = "12,5%",
                highlight = false
            )
            Spacer(modifier = Modifier.height(4.dp))
            AllocationRowItem(
                dotColor = RoseError,
                title = tr("Batas Rokok Harian (Maks Rp 20.000/hari)", "Daily Cigarette Cap (Max Rp 20,000/day)"),
                subtitle = tr("Estimasi 30 hari × Rp 20.000", "Est. 30 days × Rp 20,000"),
                amount = "Rp 600.000",
                pct = "8,3%",
                highlight = false
            )
            Spacer(modifier = Modifier.height(4.dp))
            AllocationRowItem(
                dotColor = BlueInfo,
                title = tr("Operasional Motor Rute ~25 km (Bensin & Servis)", "Motor Ops ~25 km Route (Fuel & Service)"),
                subtitle = if (actualOpEx > 0) tr("Operasional tercatat: Rp %,.0f", "Recorded ops: Rp %,.0f").format(actualOpEx) else tr("Bensin 24 rute + cadangan servis/oli bulanan", "24 routes fuel + monthly service reserve"),
                amount = "Rp 530.000",
                pct = "7,4%",
                highlight = false
            )
        }
    }
}

@Composable
private fun StrategyBarRow(
    label: String,
    valueText: String,
    subText: String,
    fraction: Float,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalSurfaceElevated, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(CharcoalBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subText,
            fontSize = 9.5.sp,
            color = TextMutedDark
        )
    }
}

@Composable
private fun AllocationRowItem(
    dotColor: Color,
    title: String,
    subtitle: String,
    amount: String,
    pct: String,
    highlight: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (highlight) SupabaseGreen.copy(alpha = 0.12f) else CharcoalSurfaceElevated,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (highlight) SupabaseGreen.copy(alpha = 0.4f) else CharcoalBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) SupabaseGreen else TextPrimaryDark
                )
                Text(
                    text = subtitle,
                    fontSize = 9.5.sp,
                    color = TextSecondaryDark
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = amount,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlight) SupabaseGreen else TextPrimaryDark
            )
            Text(
                text = pct,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = dotColor
            )
        }
    }
}


