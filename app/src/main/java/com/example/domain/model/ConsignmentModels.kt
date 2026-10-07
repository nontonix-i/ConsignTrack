package com.example.domain.model

import com.example.data.local.entity.Customer
import com.example.data.local.entity.Product
import com.example.data.local.entity.TransactionDetail
import com.example.data.local.entity.TransactionHeader

data class CustomerStockItemSummary(
    val productId: Long,
    val productName: String,
    val unitSmall: String,
    val unitBig: String,
    val piecesPerPack: Int,
    val quantityPieces: Int,
    val formattedStock: String,
    val catalogPricePack: Double,
    val customPricePack: Double?,
    val effectivePricePack: Double,
    val effectivePriceUnit: Double
) {
    val isCustomPrice: Boolean
        get() = customPricePack != null && customPricePack > 0.0 && customPricePack != catalogPricePack

    val hasCustomPrice: Boolean
        get() = isCustomPrice

    val effectivePriceSmall: Double
        get() = effectivePriceUnit
}

data class CustomerWithStatus(
    val customer: Customer,
    val totalActiveStock: Int = 0,
    val stockItems: List<CustomerStockItemSummary> = emptyList(),
    val hasVisitedToday: Boolean = false,
    val lastVisitDate: Long? = null,
    val lastTransactionAmount: Double? = null,
    val distanceMeters: Float? = null
) {
    val customPricesByProduct: Map<Long, Double>
        get() = stockItems.mapNotNull { item ->
            val cp = item.customPricePack
            if (cp != null && cp > 0.0 && item.isCustomPrice) item.productId to cp else null
        }.toMap()

    val formattedDistance: String
        get() = when {
            distanceMeters == null -> ""
            distanceMeters < 1000f -> "${distanceMeters.toInt()} m"
            else -> "%.1f km".format(distanceMeters / 1000f)
        }
}

data class ReconciliationItem(
    val product: Product,
    val previousStock: Int,                 // in pieces (pcs) - Titip minggu/kunjungan lalu
    val remainingStock: Int = 0,            // in pieces (pcs) - dihitung saat cek toples/rak warung
    val isAutoSwapReturned: Boolean = true, // Default: sisa ditarik & diganti baru
    val manualReturnedQuantity: Int = 0,    // in pieces jika sisa tidak ditarik semua
    val addedPacks: Int = if (product.pieces_per_pack > 0) previousStock / product.pieces_per_pack else 0, // Ganti baru dalam satuan PACK
    val addedPiecesExtra: Int = 0,          // Tambahan eceran pcs jika ada
    val customPricePack: Double? = null     // Harga khusus warung per Pack (null = harga standar katalog)
) {
    // Harga efektif per Pack untuk warung ini
    val effectivePricePack: Double
        get() = if (customPricePack != null && customPricePack > 0.0) customPricePack else product.selling_price_pack

    // Harga efektif per Pcs (satuan kecil) untuk warung ini
    val effectivePriceUnit: Double
        get() = if (customPricePack != null && customPricePack > 0.0) {
            if (product.pieces_per_pack > 0) customPricePack / product.pieces_per_pack else customPricePack
        } else {
            product.selling_price
        }

    val isCustomPrice: Boolean
        get() = customPricePack != null && customPricePack > 0.0 && customPricePack != product.selling_price_pack

    val hasCustomPrice: Boolean
        get() = isCustomPrice

    // Sisa yang ditarik dari warung
    val returnedQuantity: Int
        get() = if (isAutoSwapReturned) remainingStock else manualReturnedQuantity

    // Sisa stok lama yang dibiarkan di warung (0 jika ditarik semua)
    val remainingLeftAtStore: Int
        get() = if (isAutoSwapReturned) 0 else maxOf(0, remainingStock - manualReturnedQuantity)

    // Yang terjual / laku: Stok Lalu - Sisa yang ada
    // Contoh: Stok lalu 30 pcs, sisa 7 pcs -> laku = 23 pcs!
    val soldQuantity: Int
        get() = maxOf(0, previousStock - remainingStock)

    // Subtotal yang WAJIB DIBAYAR warung HANYA yang laku (menggunakan harga khusus warung jika ada)!
    val subtotal: Double
        get() = soldQuantity * effectivePriceUnit

    // HPP / Biaya pokok barang yang laku
    val costTotal: Double
        get() = soldQuantity * product.cost_price

    // Total pcs titipan baru yang ditambah (Pack x Isi + Eceran)
    val addedQuantity: Int
        get() = (addedPacks * product.pieces_per_pack) + addedPiecesExtra

    // Stok akhir yang ada di warung setelah kunjungan
    // Jika ditarik: 0 + addedQuantity
    // Jika ditinggal: sisa + addedQuantity
    val finalStock: Int
        get() = remainingLeftAtStore + addedQuantity
}

data class TransactionWithDetails(
    val header: TransactionHeader,
    val customer: Customer?,
    val details: List<Pair<TransactionDetail, Product?>>
)

data class BusinessFinancialSummary(
    val totalSalesRevenue: Double,
    val totalCostOfGoodsSold: Double,
    val grossProfit: Double,
    val operationalExpenses: Double,
    val netProfit: Double
)

data class PersonalFinancialSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double
)

data class ReceiptData(
    val headerId: Long,
    val businessName: String = "CONSIGNTRACK DISTRIBUSI",
    val businessSub: String = com.example.ui.theme.tr("Distribusi & Titip Jual Makanan Ringan", "Snack Distribution & Consignment"),
    val businessPhone: String = "0812-9988-7766",
    val transactionDate: Long,
    val customerName: String,
    val customerAddress: String,
    val items: List<ReceiptItemData>,
    val totalSoldQuantity: Int,
    val totalAmount: Double,
    val amountPaid: Double,
    val changeOrDebt: Double,
    val notes: String? = null,
    val photoUri: String? = null
)

data class ReceiptItemData(
    val productName: String,
    val unit: String,
    val unitBig: String = "Pack",
    val piecesPerPack: Int = 10,
    val prevStock: Int,
    val remStock: Int,
    val returStock: Int,
    val soldQty: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val addedPacks: Int = 0,
    val addedQty: Int,
    val newTotalStock: Int
)

data class CustomerPerformance(
    val customer: Customer,
    val totalVisits: Int,
    val totalRevenue: Double,
    val totalPaid: Double,
    val currentDebtOrOverpaid: Double,
    val totalSoldPieces: Int,
    val averageRevenuePerVisit: Double,
    val topSellingProduct: String?,
    val topSellingQuantity: Int,
    val currentConsignedPieces: Int,
    val history: List<CustomerVisitRecord>
)

data class CustomerVisitRecord(
    val headerId: Long,
    val transactionDate: Long,
    val totalSoldAmount: Double,
    val amountPaid: Double,
    val notes: String? = null,
    val photoUri: String? = null,
    val details: List<CustomerVisitDetail>
)

data class CustomerVisitDetail(
    val productName: String,
    val soldQuantity: Int,
    val unitPrice: Double,
    val subtotal: Double,
    val remainingStock: Int,
    val addedQuantity: Int,
    val returnedQuantity: Int
)

