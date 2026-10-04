package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.ConsignmentStock
import com.example.data.local.entity.Customer
import com.example.data.local.entity.FinancialCategory
import com.example.data.local.entity.FinancialRecord
import com.example.data.local.entity.Product
import com.example.data.local.entity.TransactionDetail
import com.example.data.local.entity.TransactionHeader
import com.example.domain.model.BusinessFinancialSummary
import com.example.domain.model.CustomerPerformance
import com.example.domain.model.CustomerStockItemSummary
import com.example.domain.model.CustomerVisitDetail
import com.example.domain.model.CustomerVisitRecord
import com.example.domain.model.CustomerWithStatus
import com.example.domain.model.PersonalFinancialSummary
import com.example.domain.model.ReceiptData
import com.example.domain.model.ReceiptItemData
import com.example.domain.model.ReconciliationItem
import com.example.domain.model.TransactionWithDetails
import com.example.data.remote.OpenAiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class ConsignmentRepository(
    private val database: AppDatabase
) {
    private val customerDao = database.customerDao()
    private val productDao = database.productDao()
    private val stockDao = database.consignmentStockDao()
    private val transactionDao = database.transactionDao()
    private val financialDao = database.financialRecordDao()

    // --- Customers ---
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val customersWithStatus: Flow<List<CustomerWithStatus>> = combine(
        customerDao.getAllCustomers(),
        stockDao.getAllStocks(),
        transactionDao.getAllHeaders(),
        productDao.getAllProducts()
    ) { customers, stocks, headers, products ->
        val startOfDay = getStartOfDayMillis()
        val stocksByCustomer = stocks.groupBy { it.customer_id }
        val headersByCustomer = headers.groupBy { it.customer_id }
        val productsMap = products.associateBy { it.id }

        customers.map { customer ->
            val custStocks = stocksByCustomer[customer.id] ?: emptyList()
            val totalActive = custStocks.sumOf { it.current_quantity }
            val custHeaders = headersByCustomer[customer.id] ?: emptyList()
            val latestHeader = custHeaders.maxByOrNull { it.transaction_date }
            val hasVisitedToday = custHeaders.any { it.transaction_date >= startOfDay }

            val stockSummaries = custStocks.mapNotNull { st ->
                val prod = productsMap[st.product_id] ?: return@mapNotNull null
                CustomerStockItemSummary(
                    productId = prod.id,
                    productName = prod.name,
                    unitSmall = prod.unit_small,
                    unitBig = prod.unit_big,
                    piecesPerPack = prod.pieces_per_pack,
                    quantityPieces = st.current_quantity,
                    formattedStock = prod.formatPackAndPieces(st.current_quantity),
                    catalogPricePack = prod.selling_price_pack,
                    customPricePack = st.custom_price_pack,
                    effectivePricePack = st.effectivePricePack(prod),
                    effectivePriceUnit = st.effectivePriceUnit(prod)
                )
            }

            CustomerWithStatus(
                customer = customer,
                totalActiveStock = totalActive,
                stockItems = stockSummaries,
                hasVisitedToday = hasVisitedToday,
                lastVisitDate = latestHeader?.transaction_date,
                lastTransactionAmount = latestHeader?.total_sold_amount
            )
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getCustomer(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)
    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerByIdDirect(id)

    suspend fun saveCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun updateCustomerPhoto(customerId: Long, photoUri: String?) = withContext(Dispatchers.IO) {
        val cust = customerDao.getCustomerByIdDirect(customerId) ?: return@withContext
        customerDao.updateCustomer(cust.copy(photo_uri = photoUri))
    }

    suspend fun updateCustomerRoute(customerId: Long, routeDay: String, routeOrder: Int) = withContext(Dispatchers.IO) {
        val cust = customerDao.getCustomerByIdDirect(customerId) ?: return@withContext
        customerDao.updateCustomer(cust.copy(route_day = routeDay, route_order = routeOrder))
    }

    suspend fun updateCustomerLocation(
        customerId: Long,
        lat: Double?,
        lng: Double?,
        resolvedAddress: String? = null
    ) = withContext(Dispatchers.IO) {
        val cust = customerDao.getCustomerByIdDirect(customerId) ?: return@withContext
        val updatedAddress = if (!resolvedAddress.isNullOrBlank()) {
            resolvedAddress.trim()
        } else {
            cust.address
        }
        customerDao.updateCustomer(
            cust.copy(
                latitude = lat,
                longitude = lng,
                address = updatedAddress
            )
        )
    }

    /**
     * Automatically converts GPS coordinates (latitude, longitude) into street addresses
     * for any warung whose address is still blank or a temporary offline coordinate placeholder.
     */
    suspend fun syncMissingAddressesFromCoordinates(context: android.content.Context): Int = withContext(Dispatchers.IO) {
        val customers = customerDao.getAllCustomersDirect()
        var convertedCount = 0
        for (cust in customers) {
            val lat = cust.latitude
            val lng = cust.longitude
            if (com.example.util.LocationHelper.isValidCoordinate(lat, lng) &&
                com.example.util.LocationHelper.isAddressNeedingAutoConversion(cust.address)
            ) {
                val resolved = com.example.util.LocationHelper.reverseGeocodeAddress(context, lat!!, lng!!)
                if (!resolved.isNullOrBlank() && resolved != cust.address) {
                    customerDao.updateCustomer(cust.copy(address = resolved))
                    convertedCount++
                }
            }
        }
        convertedCount
    }

    // --- Products ---
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun saveProduct(product: Product): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    // --- Stocks & Reconciliation Prep ---
    val allStocks: Flow<List<ConsignmentStock>> = stockDao.getAllStocks()

    fun getStocksForCustomer(customerId: Long): Flow<List<ConsignmentStock>> =
        stockDao.getStocksForCustomer(customerId)

    suspend fun prepareReconciliationItems(customerId: Long): List<ReconciliationItem> = withContext(Dispatchers.IO) {
        val currentStocks = stockDao.getStocksForCustomerDirect(customerId)
        // If the customer has never had any consignment stock, start with an empty product list
        if (currentStocks.isEmpty()) {
            return@withContext emptyList()
        }

        val productsMap = productDao.getAllProductsDirect().associateBy { it.id }

        // Only include products that are active in this customer's consignment stock
        currentStocks.mapNotNull { stock ->
            val product = productsMap[stock.product_id] ?: return@mapNotNull null
            val prevStock = stock.current_quantity
            val defaultPacks = if (product.pieces_per_pack > 0) prevStock / product.pieces_per_pack else 0
            ReconciliationItem(
                product = product,
                previousStock = prevStock,
                remainingStock = 0,
                isAutoSwapReturned = true,
                manualReturnedQuantity = 0,
                addedPacks = defaultPacks,
                addedPiecesExtra = 0,
                customPricePack = stock.custom_price_pack
            )
        }
    }

    /**
     * Updates a customer's profile info AND their per-product custom prices + previous/active consigned stocks.
     * Each entry in [productConfigs] is Triple(productId, quantityPieces, customPricePack).
     */
    suspend fun saveCustomerWithCustomPricesAndStocks(
        customer: Customer,
        productConfigs: List<Triple<Long, Int, Double?>>
    ) = withContext(Dispatchers.IO) {
        val savedCustomerId = if (customer.id == 0L) {
            customerDao.insertCustomer(customer)
        } else {
            customerDao.updateCustomer(customer)
            customer.id
        }
        val now = System.currentTimeMillis()
        productConfigs.forEach { (productId, qtyPieces, customPricePack) ->
            val cleanQty = qtyPieces.coerceAtLeast(0)
            val cleanCustomPrice = if (customPricePack != null && customPricePack > 0.0) customPricePack else null
            val existing = stockDao.getStock(savedCustomerId, productId)
            if (existing != null) {
                if (cleanQty == 0 && cleanCustomPrice == null) {
                    stockDao.deleteStock(savedCustomerId, productId)
                } else {
                    stockDao.updateStock(
                        existing.copy(
                            current_quantity = cleanQty,
                            custom_price_pack = cleanCustomPrice,
                            last_updated = now
                        )
                    )
                }
            } else if (cleanQty > 0 || cleanCustomPrice != null) {
                stockDao.insertOrUpdateStock(
                    ConsignmentStock(
                        customer_id = savedCustomerId,
                        product_id = productId,
                        current_quantity = cleanQty,
                        custom_price_pack = cleanCustomPrice,
                        last_updated = now
                    )
                )
            }
        }
    }

    /**
     * Updates a single product's previous/current consigned stock and custom price for a customer.
     */
    suspend fun updateCustomerProductStockAndPrice(
        customerId: Long,
        productId: Long,
        quantityPieces: Int,
        customPricePack: Double?
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cleanQty = quantityPieces.coerceAtLeast(0)
        val cleanPrice = if (customPricePack != null && customPricePack > 0.0) customPricePack else null
        val existing = stockDao.getStock(customerId, productId)
        if (existing != null) {
            stockDao.updateStock(
                existing.copy(
                    current_quantity = cleanQty,
                    custom_price_pack = cleanPrice,
                    last_updated = now
                )
            )
        } else {
            stockDao.insertOrUpdateStock(
                ConsignmentStock(
                    customer_id = customerId,
                    product_id = productId,
                    current_quantity = cleanQty,
                    custom_price_pack = cleanPrice,
                    last_updated = now
                )
            )
        }
    }

    suspend fun updateProductCustomPriceForCustomer(
        customerId: Long,
        productId: Long,
        customPricePack: Double?
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cleanPrice = if (customPricePack != null && customPricePack > 0.0) customPricePack else null
        val existing = stockDao.getStock(customerId, productId)
        if (existing != null) {
            stockDao.updateStock(
                existing.copy(
                    custom_price_pack = cleanPrice,
                    last_updated = now
                )
            )
        } else if (cleanPrice != null) {
            stockDao.insertOrUpdateStock(
                ConsignmentStock(
                    customer_id = customerId,
                    product_id = productId,
                    current_quantity = 0,
                    custom_price_pack = cleanPrice,
                    last_updated = now
                )
            )
        }
    }

    suspend fun deleteStockForCustomerAndProduct(customerId: Long, productId: Long) = withContext(Dispatchers.IO) {
        stockDao.deleteStock(customerId, productId)
    }

    fun getCustomerPerformance(customerId: Long): Flow<CustomerPerformance?> = combine(
        customerDao.getAllCustomers(),
        transactionDao.getHeadersForCustomer(customerId),
        productDao.getAllProducts(),
        stockDao.getStocksForCustomer(customerId)
    ) { allCusts, headers, products, stocks ->
        val customer = allCusts.find { it.id == customerId } ?: return@combine null
        val productsMap = products.associateBy { it.id }

        val visitRecords = headers.map { h ->
            val details = transactionDao.getDetailsForHeaderDirect(h.id)
            val mappedDetails = details.map { d ->
                val p = productsMap[d.product_id]
                CustomerVisitDetail(
                    productName = p?.name ?: "Produk #${d.product_id}",
                    soldQuantity = d.sold_quantity,
                    unitPrice = d.unit_price,
                    subtotal = d.sold_quantity * d.unit_price,
                    remainingStock = d.remaining_stock,
                    addedQuantity = d.added_quantity,
                    returnedQuantity = d.returned_quantity
                )
            }
            CustomerVisitRecord(
                headerId = h.id,
                transactionDate = h.transaction_date,
                totalSoldAmount = h.total_sold_amount,
                amountPaid = h.amount_paid,
                notes = h.notes,
                photoUri = h.photo_uri,
                details = mappedDetails
            )
        }

        val totalVisits = headers.size
        val totalRevenue = headers.sumOf { it.total_sold_amount }
        val totalPaid = headers.sumOf { it.amount_paid }
        val debtOrOverpaid = totalPaid - totalRevenue

        val allSoldItems = visitRecords.flatMap { it.details }
        val totalSoldPieces = allSoldItems.sumOf { it.soldQuantity }
        val topProductEntry = allSoldItems
            .groupBy { it.productName }
            .mapValues { entry -> entry.value.sumOf { it.soldQuantity } }
            .maxByOrNull { it.value }

        val currentConsignedPieces = stocks.sumOf { it.current_quantity }
        val avgRevenue = if (totalVisits > 0) totalRevenue / totalVisits else 0.0

        CustomerPerformance(
            customer = customer,
            totalVisits = totalVisits,
            totalRevenue = totalRevenue,
            totalPaid = totalPaid,
            currentDebtOrOverpaid = debtOrOverpaid,
            totalSoldPieces = totalSoldPieces,
            averageRevenuePerVisit = avgRevenue,
            topSellingProduct = topProductEntry?.key,
            topSellingQuantity = topProductEntry?.value ?: 0,
            currentConsignedPieces = currentConsignedPieces,
            history = visitRecords
        )
    }.flowOn(Dispatchers.IO)

    // --- Core Workflow: Save Reconciliation & Update Consignment ---
    suspend fun saveReconciliation(
        customerId: Long,
        items: List<ReconciliationItem>,
        amountPaid: Double,
        notes: String?,
        photoUri: String? = null,
        businessName: String = "CONSIGNTRACK DISTRIBUSI",
        businessAddress: String = "Sentra Makanan Ringan",
        businessPhone: String = "0812-9988-7766"
    ): ReceiptData = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerByIdDirect(customerId)
            ?: throw IllegalArgumentException("Toko tidak ditemukan")

        val totalSoldAmount = items.sumOf { it.subtotal }
        val now = System.currentTimeMillis()

        // 1. Insert Transaction Header
        val headerId = transactionDao.insertHeader(
            TransactionHeader(
                customer_id = customerId,
                transaction_date = now,
                total_sold_amount = totalSoldAmount,
                amount_paid = amountPaid,
                notes = notes,
                photo_uri = photoUri
            )
        )

        // 2. Insert Transaction Details (uses warung's custom selling price if configured)
        val details = items.map { item ->
            TransactionDetail(
                transaction_id = headerId,
                product_id = item.product.id,
                previous_stock = item.previousStock,
                remaining_stock = item.remainingStock,
                sold_quantity = item.soldQuantity,
                returned_quantity = item.returnedQuantity,
                added_quantity = item.addedQuantity,
                unit_price = item.effectivePriceUnit
            )
        }
        transactionDao.insertDetails(details)

        // 3. Update Consignment Stock for Customer (preserving custom_price_pack)
        items.forEach { item ->
            val newQty = item.finalStock
            val existing = stockDao.getStock(customerId, item.product.id)
            val customPackPrice = item.customPricePack ?: existing?.custom_price_pack
            if (existing != null) {
                stockDao.updateStock(
                    existing.copy(
                        current_quantity = newQty,
                        custom_price_pack = customPackPrice,
                        last_updated = now
                    )
                )
            } else if (newQty > 0 || item.previousStock > 0 || customPackPrice != null) {
                stockDao.insertOrUpdateStock(
                    ConsignmentStock(
                        customer_id = customerId,
                        product_id = item.product.id,
                        current_quantity = newQty,
                        custom_price_pack = customPackPrice,
                        last_updated = now
                    )
                )
            }
        }

        // 4. Automatically record Business Income from cash collected
        if (amountPaid > 0) {
            financialDao.insertRecord(
                FinancialRecord(
                    category = FinancialCategory.BUSINESS_INCOME,
                    amount = amountPaid,
                    description = "Setoran Konsinyasi - ${customer.name}",
                    transaction_date = now
                )
            )
        }

        // 5. Build ReceiptData for thermal printer / preview
        val receiptItems = items.map { item ->
            ReceiptItemData(
                productName = item.product.name,
                unit = item.product.unit_small,
                unitBig = item.product.unit_big,
                piecesPerPack = item.product.pieces_per_pack,
                prevStock = item.previousStock,
                remStock = item.remainingStock,
                returStock = item.returnedQuantity,
                soldQty = item.soldQuantity,
                unitPrice = item.effectivePriceUnit,
                subtotal = item.subtotal,
                addedPacks = item.addedPacks,
                addedQty = item.addedQuantity,
                newTotalStock = item.finalStock
            )
        }

        val totalSoldQty = items.sumOf { it.soldQuantity }
        val changeOrDebt = amountPaid - totalSoldAmount

        ReceiptData(
            headerId = headerId,
            businessName = businessName,
            businessSub = "Distribusi & Titip Jual",
            businessPhone = businessPhone,
            transactionDate = now,
            customerName = customer.name,
            customerAddress = customer.address,
            items = receiptItems,
            totalSoldQuantity = totalSoldQty,
            totalAmount = totalSoldAmount,
            amountPaid = amountPaid,
            changeOrDebt = changeOrDebt,
            notes = notes,
            photoUri = photoUri
        )
    }

    // --- Transactions & History ---
    val allTransactionsWithDetails: Flow<List<TransactionWithDetails>> = combine(
        transactionDao.getAllHeaders(),
        customerDao.getAllCustomers(),
        productDao.getAllProducts()
    ) { headers, customers, products ->
        val custMap = customers.associateBy { it.id }
        val prodMap = products.associateBy { it.id }

        headers.map { header ->
            val details = transactionDao.getDetailsForHeaderDirect(header.id)
            TransactionWithDetails(
                header = header,
                customer = custMap[header.customer_id],
                details = details.map { d -> d to prodMap[d.product_id] }
            )
        }
    }.flowOn(Dispatchers.IO)

    val totalPiecesConsigned: Flow<Int> = stockDao.getTotalConsignedPieces()
    val todayTotalSoldAmount: Flow<Double> = transactionDao.getTodayTotalSoldAmount(getStartOfDayMillis())

    // --- Financial Records (Separate Business vs Personal) ---
    val businessRecords: Flow<List<FinancialRecord>> = financialDao.getRecordsByPrefix("BUSINESS_")
    val personalRecords: Flow<List<FinancialRecord>> = financialDao.getRecordsByPrefix("PERSONAL_")

    suspend fun addFinancialRecord(record: FinancialRecord): Long = withContext(Dispatchers.IO) {
        financialDao.insertRecord(record)
    }

    suspend fun deleteFinancialRecord(record: FinancialRecord) = withContext(Dispatchers.IO) {
        financialDao.deleteRecord(record)
    }

    // Summary calculations for Business & Personal
    val businessSummary: Flow<BusinessFinancialSummary> = combine(
        transactionDao.getAllHeaders(),
        database.transactionDao().getAllHeaders(),
        financialDao.getRecordsByPrefix("BUSINESS_")
    ) { _, _, bizRecords ->
        val details = transactionDao.getAllDetailsDirect()
        val products = productDao.getAllProductsDirect().associateBy { it.id }

        var totalRevenue = 0.0
        var totalHpp = 0.0

        details.forEach { d ->
            val p = products[d.product_id]
            val sold = d.sold_quantity
            if (sold > 0 && p != null) {
                totalRevenue += sold * d.unit_price
                totalHpp += sold * p.cost_price
            }
        }

        val operationalExpenses = bizRecords
            .filter { it.category == FinancialCategory.BUSINESS_EXPENSE }
            .sumOf { it.amount }

        val grossProfit = totalRevenue - totalHpp
        val netProfit = grossProfit - operationalExpenses

        BusinessFinancialSummary(
            totalSalesRevenue = totalRevenue,
            totalCostOfGoodsSold = totalHpp,
            grossProfit = grossProfit,
            operationalExpenses = operationalExpenses,
            netProfit = netProfit
        )
    }.flowOn(Dispatchers.IO)

    val personalSummary: Flow<PersonalFinancialSummary> = personalRecords.map { records ->
        val income = records.filter { it.category == FinancialCategory.PERSONAL_INCOME }.sumOf { it.amount }
        val expense = records.filter { it.category == FinancialCategory.PERSONAL_EXPENSE }.sumOf { it.amount }
        PersonalFinancialSummary(
            totalIncome = income,
            totalExpense = expense,
            balance = income - expense
        )
    }.flowOn(Dispatchers.IO)

    val agentToolEngine = AgentToolEngine(database)

    // --- Agent Mode with Tool Calling + Analytics Engine (Module E) ---
    suspend fun askLocalAgent(query: String): String {
        return askAgentWithTools(query = query, enableTools = true).replyMarkdown
    }

    suspend fun askAgentWithTools(
        query: String,
        enableTools: Boolean = true,
        currentBusinessProfile: Triple<String, String, String> = Triple(
            "CONSIGNTRACK DISTRIBUSI",
            "Sentra Makanan Ringan",
            "0812-9988-7766"
        )
    ): AgentTurnResponse = withContext(Dispatchers.IO) {
        val customers = customerDao.getAllCustomersDirect()
        val products = productDao.getAllProductsDirect().associateBy { it.id }
        val stocks = stockDao.getAllStocksDirect()
        val headers = transactionDao.getAllHeadersDirect()
        val details = transactionDao.getAllDetailsDirect()
        val allFinancials = financialDao.getAllRecordsDirect()
        val bizRecords = allFinancials.filter { it.category.startsWith("BUSINESS_") }

        val startOfDay = getStartOfDayMillis()
        val visitedCustIds = headers.filter { it.transaction_date >= startOfDay }.map { it.customer_id }.toSet()
        val unvisited = customers.filter { it.id !in visitedCustIds }

        var totalRev = 0.0
        var totalHpp = 0.0
        details.forEach { d ->
            val p = products[d.product_id]
            if (p != null) {
                totalRev += d.sold_quantity * d.unit_price
                totalHpp += d.sold_quantity * p.cost_price
            }
        }
        val opEx = bizRecords.filter { it.category == FinancialCategory.BUSINESS_EXPENSE }.sumOf { it.amount }
        val gross = totalRev - totalHpp
        val net = gross - opEx

        val systemContext = buildString {
            appendLine("Anda adalah AI Agent & Asisten Bisnis Cerdas untuk aplikasi distribusi konsinyasi 'ConsignTrack'.")
            appendLine("Jawab dengan singkat, jelas, dan ramah dalam Bahasa Indonesia.")
            appendLine("FORMATTING: Gunakan format Markdown lengkap (heading ###, **teks tebal** untuk nominal/angka penting, bullet points '-', dan tabel markdown jika membandingkan data).")
            if (enableTools) {
                appendLine()
                appendLine(agentToolEngine.buildToolSystemInstructions())
                appendLine()
            }
            appendLine("DATA DATABASE AKTUAL SAAT INI:")
            appendLine("- Profil Nota: ${currentBusinessProfile.first} | ${currentBusinessProfile.second} | ${currentBusinessProfile.third}")
            appendLine("- Total Warung: ${customers.size} (Selesai dikunjungi hari ini: ${visitedCustIds.size}, Belum: ${unvisited.size})")
            appendLine("- Daftar Warung (ID | Nama | Alamat | Rute | Stok Aktif):")
            customers.forEach { c ->
                val cStocks = stocks.filter { it.customer_id == c.id }
                val stockDesc = if (cStocks.isEmpty()) "0 pcs" else cStocks.joinToString { s ->
                    "${products[s.product_id]?.name ?: "Produk#${s.product_id}"}: ${s.current_quantity} pcs"
                }
                appendLine("  • ID#${c.id}: ${c.name} | ${c.address} | ${c.route_day} #${c.route_order} | Stok: $stockDesc")
            }
            appendLine("- Daftar Katalog Produk (ID | Nama | Satuan | Harga Jual/Pack | Modal/Pack):")
            products.values.forEach { p ->
                appendLine(
                    "  • ID#${p.id}: ${p.name} (1 ${p.unit_big} = ${p.pieces_per_pack} ${p.unit_small}) | Jual Rp %,.0f/%s (Rp %,.0f/%s) | Modal Rp %,.0f/%s".format(
                        p.selling_price_pack, p.unit_big, p.selling_price, p.unit_small, p.cost_price_pack, p.unit_big
                    )
                )
            }
            appendLine("- Ringkasan Keuangan: Omset Rp %,.0f | HPP Rp %,.0f | Biaya Operasional Rp %,.0f | Laba Bersih Rp %,.0f".format(totalRev, totalHpp, opEx, net))
            if (allFinancials.isNotEmpty()) {
                appendLine("- Catatan Kas Terbaru:")
                allFinancials.take(8).forEach { r ->
                    appendLine("  • ID#${r.id} [${r.category}]: ${r.description} = Rp %,.0f".format(r.amount))
                }
            }
        }

        val turnResult = try {
            OpenAiClient.generateAgentTurn(
                systemContext = systemContext,
                userQuery = query,
                functionDeclarations = agentToolEngine.buildFunctionDeclarations(),
                enableTools = enableTools
            )
        } catch (_: Exception) {
            null
        }

        val modelToolCalls = turnResult?.toolCalls.orEmpty()
        val effectiveToolCalls = if (enableTools && modelToolCalls.isEmpty()) {
            agentToolEngine.parseLocalIntentToolCalls(query)
        } else {
            modelToolCalls
        }

        var executedSummaries = emptyList<AgentToolExecutionSummary>()
        var updatedProfile: Triple<String, String, String>? = null

        if (effectiveToolCalls.isNotEmpty()) {
            val (summaries, newProfile) = agentToolEngine.executeToolCalls(
                calls = effectiveToolCalls,
                currentBusinessProfile = currentBusinessProfile
            )
            executedSummaries = summaries
            updatedProfile = newProfile
        }

        // If tools were executed, build a clear, helpful response combining AI reply + live database state
        if (executedSummaries.isNotEmpty()) {
            val updatedCustCount = customerDao.getAllCustomersDirect().size
            val updatedProdCount = productDao.getAllProductsDirect().size
            val updatedStockTotal = stockDao.getAllStocksDirect().sumOf { it.current_quantity }

            val replyText = buildString {
                if (!turnResult?.replyText.isNullOrBlank()) {
                    appendLine(turnResult?.replyText)
                    appendLine()
                } else {
                    appendLine("### ✅ Eksekusi Agent Mode Berhasil")
                    appendLine("Perintah Anda telah dijalankan secara langsung pada database aplikasi:")
                    executedSummaries.forEach { item ->
                        val icon = if (item.success) "✔" else "⚠️"
                        appendLine("- $icon **[${item.actionType}]** ${item.summary}")
                    }
                    appendLine()
                }
                appendLine("> **Status Data Terkini:** **$updatedCustCount Warung** • **$updatedProdCount Produk** • **$updatedStockTotal Unit** stok tersebar.")
            }.trim()

            return@withContext AgentTurnResponse(
                replyMarkdown = replyText,
                executedTools = executedSummaries,
                updatedBusinessProfile = updatedProfile
            )
        }

        // If no tools were called, return the remote AI answer if present
        if (!turnResult?.replyText.isNullOrBlank()) {
            return@withContext AgentTurnResponse(
                replyMarkdown = turnResult!!.replyText!!,
                executedTools = emptyList()
            )
        }

        // Offline local analytics fallback
        val q = query.lowercase().trim()
        val fallbackAnswer = when {
            q.contains("laku") || q.contains("terlaris") || q.contains("terbanyak") || q.contains("toko mana") -> {
                if (headers.isEmpty()) {
                    "Belum ada transaksi rekonsiliasi yang tercatat untuk dianalisis."
                } else {
                    val salesByCust = headers.groupBy { it.customer_id }
                        .mapValues { entry -> entry.value.sumOf { it.total_sold_amount } }
                    val best = salesByCust.maxByOrNull { it.value }
                    val bestCust = customers.find { it.id == best?.key }
                    if (bestCust != null && best != null) {
                        val formatted = "Rp %,.0f".format(best.value)
                        "### 🏆 Toko Terlaris\n\nToko paling laku saat ini adalah **${bestCust.name}** (${bestCust.address}) dengan total omset **$formatted**."
                    } else {
                        "Tidak ada data transaksi toko yang mencukupi."
                    }
                }
            }

            q.contains("stok") || q.contains("titipan") || q.contains("tersebar") || q.contains("kerupuk") -> {
                val totalPcs = stocks.sumOf { it.current_quantity }
                val stockRows = stocks.groupBy { it.product_id }.map { (pId, list) ->
                    val pName = products[pId]?.name ?: "Produk #$pId"
                    val unit = products[pId]?.unit ?: "Pcs"
                    val count = list.sumOf { it.current_quantity }
                    "| $pName | $count $unit |"
                }.joinToString("\n")

                "### 📦 Stok Konsinyasi di Lapangan\n\nTotal stok titipan: **$totalPcs unit**\n\n| Produk | Jumlah Titipan |\n|---|---|\n$stockRows"
            }

            q.contains("laba") || q.contains("rugi") || q.contains("profit") || q.contains("keuntungan") || q.contains("omset") -> {
                "### 📊 Ringkasan Laba / Rugi\n\n" +
                        "| Kategori | Nominal |\n" +
                        "|---|---|\n" +
                        "| Omset Penjualan | Rp %,.0f |\n".format(totalRev) +
                        "| HPP Modal Barang | Rp %,.0f |\n".format(totalHpp) +
                        "| Laba Kotor | Rp %,.0f |\n".format(gross) +
                        "| Biaya Operasional | Rp %,.0f |\n".format(opEx) +
                        "| **Laba Bersih** | **Rp %,.0f** |".format(net)
            }

            q.contains("belum") || q.contains("kunjungan") || q.contains("rute") -> {
                if (unvisited.isEmpty()) {
                    "### ✅ Semua Rute Selesai!\n\nSemua toko (**${customers.size} warung**) telah selesai dikunjungi hari ini."
                } else {
                    val listStr = unvisited.mapIndexed { i, c -> "${i + 1}. **${c.name}** (Rute #${c.route_order})" }.joinToString("\n")
                    "### 📍 Toko Belum Dikunjungi\n\nAda **${unvisited.size} toko** tersisa hari ini:\n\n$listStr"
                }
            }

            q.contains("margin") || q.contains("harga") || q.contains("produk") -> {
                val sortedByMargin = products.values.sortedByDescending { (it.selling_price - it.cost_price) / it.selling_price }
                val rows = sortedByMargin.joinToString("\n") { p ->
                    val profit = p.selling_price_pack - p.cost_price_pack
                    val pct = if (p.selling_price_pack > 0) (profit / p.selling_price_pack) * 100 else 0.0
                    "| ${p.name} | Rp %,.0f | %.1f%% |".format(profit, pct)
                }
                "### 💡 Margin Keuntungan Produk\n\n| Nama Produk | Laba/Pack | Margin % |\n|---|---|---|\n$rows"
            }

            else -> {
                "### 🤖 AI Agent Mode Aktif\n\n" +
                        "Tercatat **${customers.size} warung**, **${products.size} produk**, dan **${stocks.sumOf { it.current_quantity }} unit** stok di lapangan.\n\n" +
                        "Anda dapat meminta saya menganalisis data atau **menjalankan aksi langsung (Tool Calling)** seperti:\n" +
                        "- *\"Tambah warung Toko Maju di Jl. Sudirman rute Selasa\"*\n" +
                        "- *\"Tambah produk Keripik Singkong harga 18000\"*\n" +
                        "- *\"Set stok Kerupuk Udang di Warung Bu Siti 40 pcs\"*\n" +
                        "- *\"Catat pengeluaran bensin 25000\"*\n" +
                        "- *\"Hapus warung Toko Berkah\"*"
            }
        }

        AgentTurnResponse(
            replyMarkdown = fallbackAnswer,
            executedTools = emptyList()
        )
    }
}
