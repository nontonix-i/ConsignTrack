package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.ConsignmentStock
import com.example.data.local.entity.Customer
import com.example.data.local.entity.FinancialCategory
import com.example.data.local.entity.FinancialRecord
import com.example.data.local.entity.Product
import com.example.data.local.entity.TransactionDetail
import com.example.data.local.entity.TransactionHeader
import com.example.data.remote.ParsedToolCall
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

data class AgentToolExecutionSummary(
    val toolName: String,
    val actionType: String, // "ADD", "EDIT", "ERASE"
    val summary: String,
    val success: Boolean = true
)

data class AgentTurnResponse(
    val replyMarkdown: String,
    val executedTools: List<AgentToolExecutionSummary> = emptyList(),
    val updatedBusinessProfile: Triple<String, String, String>? = null
)

class AgentToolEngine(
    private val database: AppDatabase
) {
    private val customerDao = database.customerDao()
    private val productDao = database.productDao()
    private val stockDao = database.consignmentStockDao()
    private val transactionDao = database.transactionDao()
    private val financialDao = database.financialRecordDao()

    /**
     * Builds the Gemini native `functionDeclarations` JSONArray covering all CRUD operations.
     */
    fun buildFunctionDeclarations(): JSONArray {
        val declarations = JSONArray()

        fun prop(type: String, description: String): JSONObject =
            JSONObject().put("type", type).put("description", description)

        fun addTool(
            name: String,
            description: String,
            properties: Map<String, JSONObject>,
            required: List<String> = emptyList()
        ) {
            val propsObj = JSONObject()
            properties.forEach { (k, v) -> propsObj.put(k, v) }
            val params = JSONObject()
                .put("type", "OBJECT")
                .put("properties", propsObj)
            if (required.isNotEmpty()) {
                params.put("required", JSONArray(required))
            }
            declarations.put(
                JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put("parameters", params)
            )
        }

        // 1. Customers / Warung
        addTool(
            name = "add_customer",
            description = "Add a new customer store / warung / outlet to the database.",
            properties = mapOf(
                "name" to prop("STRING", "Store or warung name (e.g. 'Warung Makmur')"),
                "address" to prop("STRING", "Store address or location description"),
                "phone" to prop("STRING", "Phone or WhatsApp number"),
                "route_day" to prop("STRING", "Visit day: Senin, Selasa, Rabu, Kamis, Jumat, Sabtu, Minggu"),
                "route_order" to prop("INTEGER", "Route visit sequence number (default 1)"),
                "latitude" to prop("NUMBER", "Optional GPS latitude"),
                "longitude" to prop("NUMBER", "Optional GPS longitude")
            ),
            required = listOf("name")
        )

        addTool(
            name = "edit_customer",
            description = "Edit or update an existing customer store / warung by ID or name.",
            properties = mapOf(
                "customer_id" to prop("INTEGER", "Customer ID if known"),
                "target_name" to prop("STRING", "Current name of the customer/warung to edit"),
                "new_name" to prop("STRING", "New name for the customer/warung"),
                "address" to prop("STRING", "Updated address"),
                "phone" to prop("STRING", "Updated phone number"),
                "route_day" to prop("STRING", "Updated route day (Senin..Minggu)"),
                "route_order" to prop("INTEGER", "Updated route order"),
                "latitude" to prop("NUMBER", "Updated GPS latitude"),
                "longitude" to prop("NUMBER", "Updated GPS longitude")
            )
        )

        addTool(
            name = "delete_customer",
            description = "Erase/delete a customer store / warung by ID, name, or delete all customers.",
            properties = mapOf(
                "customer_id" to prop("INTEGER", "Customer ID to delete"),
                "name" to prop("STRING", "Name of the customer/warung to delete"),
                "delete_all" to prop("BOOLEAN", "Set true to erase ALL customers/warungs")
            )
        )

        // 2. Products / Katalog Produk
        addTool(
            name = "add_product",
            description = "Add a new product to the catalog with pack and unit pricing.",
            properties = mapOf(
                "name" to prop("STRING", "Product name (e.g. 'Keripik Tempe Pedas')"),
                "unit_small" to prop("STRING", "Smallest unit name (default 'Pcs' or 'Bungkus')"),
                "unit_big" to prop("STRING", "Pack/bundle unit name (default 'Pack' or 'Bal')"),
                "pieces_per_pack" to prop("INTEGER", "Number of small units per pack (default 10)"),
                "cost_price_pack" to prop("NUMBER", "Cost/modal price per pack in Rupiah"),
                "selling_price_pack" to prop("NUMBER", "Consignment selling price per pack to store in Rupiah"),
                "cost_price_unit" to prop("NUMBER", "Optional cost price per single small unit if pack price not given"),
                "selling_price_unit" to prop("NUMBER", "Optional selling price per single small unit if pack price not given")
            ),
            required = listOf("name")
        )

        addTool(
            name = "edit_product",
            description = "Edit or update an existing product's name, units, or prices by ID or name.",
            properties = mapOf(
                "product_id" to prop("INTEGER", "Product ID if known"),
                "target_name" to prop("STRING", "Current product name to find and edit"),
                "new_name" to prop("STRING", "New product name"),
                "unit_small" to prop("STRING", "Updated small unit name"),
                "unit_big" to prop("STRING", "Updated big unit name"),
                "pieces_per_pack" to prop("INTEGER", "Updated pieces per pack"),
                "cost_price_pack" to prop("NUMBER", "Updated cost price per pack"),
                "selling_price_pack" to prop("NUMBER", "Updated selling price per pack"),
                "cost_price_unit" to prop("NUMBER", "Updated cost price per single unit"),
                "selling_price_unit" to prop("NUMBER", "Updated selling price per single unit")
            )
        )

        addTool(
            name = "delete_product",
            description = "Erase/delete a product from the catalog by ID, name, or delete all products.",
            properties = mapOf(
                "product_id" to prop("INTEGER", "Product ID to delete"),
                "name" to prop("STRING", "Product name to delete"),
                "delete_all" to prop("BOOLEAN", "Set true to erase ALL products from catalog")
            )
        )

        // 3. Consignment Stock at Outlets / Stok Titipan & Harga Khusus Warung
        addTool(
            name = "set_consignment_stock",
            description = "Add, edit, or set consigned product stock (titip lalu/saat ini) and/or custom price at a specific customer warung.",
            properties = mapOf(
                "customer_id" to prop("INTEGER", "Customer ID if known"),
                "customer_name" to prop("STRING", "Customer/warung name"),
                "product_id" to prop("INTEGER", "Product ID if known"),
                "product_name" to prop("STRING", "Product name"),
                "quantity_pieces" to prop("INTEGER", "Quantity in small units (Pcs/Bungkus)"),
                "quantity_packs" to prop("INTEGER", "Optional quantity in packs (multiplied by pieces_per_pack)"),
                "custom_price_pack" to prop("NUMBER", "Optional custom selling price per pack for this specific warung in Rupiah"),
                "custom_price_unit" to prop("NUMBER", "Optional custom selling price per small unit (pcs) for this specific warung in Rupiah"),
                "mode" to prop("STRING", "Operation mode: 'SET' (replace stock), 'ADD' (increase stock), or 'SUBTRACT' (reduce stock)")
            )
        )

        addTool(
            name = "delete_consignment_stock",
            description = "Erase/remove consigned stock for a product at a store, all stock at a store, or all consigned stock everywhere.",
            properties = mapOf(
                "customer_id" to prop("INTEGER", "Customer ID"),
                "customer_name" to prop("STRING", "Customer/warung name"),
                "product_id" to prop("INTEGER", "Product ID"),
                "product_name" to prop("STRING", "Product name"),
                "delete_all_for_customer" to prop("BOOLEAN", "Set true to erase all consigned stock at the specified customer"),
                "delete_all" to prop("BOOLEAN", "Set true to erase ALL consigned stock across all stores")
            )
        )

        // 4. Reconciliation / Sales Transactions
        addTool(
            name = "record_reconciliation",
            description = "Record a store visit / sales reconciliation transaction and update consigned stock.",
            properties = mapOf(
                "customer_id" to prop("INTEGER", "Customer ID"),
                "customer_name" to prop("STRING", "Customer/warung name"),
                "product_name" to prop("STRING", "Product name being reconciled"),
                "remaining_stock" to prop("INTEGER", "Remaining stock counted at the store (in Pcs)"),
                "added_packs" to prop("INTEGER", "New packs dropped off/added during visit"),
                "added_pieces" to prop("INTEGER", "New extra pieces dropped off/added during visit"),
                "returned_quantity" to prop("INTEGER", "Damaged/returned pieces"),
                "amount_paid" to prop("NUMBER", "Cash paid by the store (if omitted, defaults to full sold amount)"),
                "notes" to prop("STRING", "Transaction notes")
            )
        )

        addTool(
            name = "edit_transaction",
            description = "Edit an existing reconciliation transaction header (amount paid, total sold, or notes).",
            properties = mapOf(
                "transaction_id" to prop("INTEGER", "Transaction header ID"),
                "customer_name" to prop("STRING", "Customer name (edits their latest transaction if transaction_id is omitted)"),
                "total_sold_amount" to prop("NUMBER", "Updated total sold amount in Rupiah"),
                "amount_paid" to prop("NUMBER", "Updated cash amount paid in Rupiah"),
                "notes" to prop("STRING", "Updated transaction notes")
            )
        )

        addTool(
            name = "delete_transaction",
            description = "Erase/delete a transaction by ID, latest transaction for a customer, or all transactions.",
            properties = mapOf(
                "transaction_id" to prop("INTEGER", "Transaction ID to delete"),
                "customer_name" to prop("STRING", "Delete latest transaction for this customer name"),
                "delete_all" to prop("BOOLEAN", "Set true to erase ALL transactions")
            )
        )

        // 5. Financial Records (Buku Kas Usaha & Pribadi)
        addTool(
            name = "add_financial_record",
            description = "Add a business or personal financial record (income or expense).",
            properties = mapOf(
                "category" to prop("STRING", "Category: 'BUSINESS_INCOME', 'BUSINESS_EXPENSE', 'PERSONAL_INCOME', or 'PERSONAL_EXPENSE'"),
                "amount" to prop("NUMBER", "Amount in Rupiah"),
                "description" to prop("STRING", "Description of the income or expense (e.g. 'Beli Bensin Motor', 'Beli Plastik Kemasan')")
            ),
            required = listOf("amount", "description")
        )

        addTool(
            name = "edit_financial_record",
            description = "Edit an existing financial record by ID or matching description.",
            properties = mapOf(
                "record_id" to prop("INTEGER", "Financial record ID"),
                "target_description" to prop("STRING", "Existing description keyword to find the record"),
                "category" to prop("STRING", "Updated category: 'BUSINESS_INCOME', 'BUSINESS_EXPENSE', 'PERSONAL_INCOME', 'PERSONAL_EXPENSE'"),
                "amount" to prop("NUMBER", "Updated amount in Rupiah"),
                "new_description" to prop("STRING", "Updated description")
            )
        )

        addTool(
            name = "delete_financial_record",
            description = "Erase/delete a financial record by ID, description keyword, category prefix, or delete all.",
            properties = mapOf(
                "record_id" to prop("INTEGER", "Financial record ID to delete"),
                "description" to prop("STRING", "Description keyword of record to delete"),
                "category_prefix" to prop("STRING", "Optional prefix 'BUSINESS' or 'PERSONAL' to delete all records in that book"),
                "delete_all" to prop("BOOLEAN", "Set true to erase ALL financial records")
            )
        )

        // 6. Business Profile & Master Reset
        addTool(
            name = "update_business_profile",
            description = "Update the business name, address, or phone number shown on thermal receipts.",
            properties = mapOf(
                "business_name" to prop("STRING", "New business/distributor name"),
                "business_address" to prop("STRING", "New business address or tagline"),
                "business_phone" to prop("STRING", "New business phone/WhatsApp")
            )
        )

        addTool(
            name = "erase_all_data",
            description = "Erase all data in a specific module or across the entire database.",
            properties = mapOf(
                "target" to prop("STRING", "Target to erase: 'ALL', 'CUSTOMERS', 'PRODUCTS', 'STOCKS', 'TRANSACTIONS', or 'FINANCIALS'")
            ),
            required = listOf("target")
        )

        return declarations
    }

    /**
     * System prompt instructions enabling any LLM to invoke tools via `<tool_call>` JSON blocks.
     */
    fun buildToolSystemInstructions(): String = buildString {
        appendLine("=== AGENT MODE (TOOL CALLING) AKTIF ===")
        appendLine("Anda memiliki akses PENUH untuk MENAMBAH (ADD), MENGUBAH (EDIT), dan MENGHAPUS (ERASE/DELETE) seluruh data di aplikasi ConsignTrack.")
        appendLine("Jika pengguna meminta untuk menambah, mengedit/mengubah, mengatur stok, mencatat transaksi/keuangan, atau menghapus data apa pun, Anda WAJIB memanggil tool dengan menyertakan blok `<tool_call>` berisi JSON valid dengan format:")
        appendLine("<tool_call>{\"name\": \"nama_tool\", \"arguments\": {\"param1\": \"nilai1\"}}</tool_call>")
        appendLine("Anda dapat memanggil lebih dari satu `<tool_call>` sekaligus jika pengguna meminta beberapa aksi.")
        appendLine("Daftar Tool yang Tersedia:")
        appendLine("1. add_customer(name, address, phone, route_day, route_order, latitude, longitude)")
        appendLine("2. edit_customer(customer_id, target_name, new_name, address, phone, route_day, route_order, latitude, longitude)")
        appendLine("3. delete_customer(customer_id, name, delete_all)")
        appendLine("4. add_product(name, unit_small, unit_big, pieces_per_pack, cost_price_pack, selling_price_pack, cost_price_unit, selling_price_unit)")
        appendLine("5. edit_product(product_id, target_name, new_name, unit_small, unit_big, pieces_per_pack, cost_price_pack, selling_price_pack, cost_price_unit, selling_price_unit)")
        appendLine("6. delete_product(product_id, name, delete_all)")
        appendLine("7. set_consignment_stock(customer_id, customer_name, product_id, product_name, quantity_pieces, quantity_packs, mode=['SET'|'ADD'|'SUBTRACT'])")
        appendLine("8. delete_consignment_stock(customer_id, customer_name, product_id, product_name, delete_all_for_customer, delete_all)")
        appendLine("9. record_reconciliation(customer_name, product_name, remaining_stock, added_packs, added_pieces, returned_quantity, amount_paid, notes)")
        appendLine("10. edit_transaction(transaction_id, customer_name, total_sold_amount, amount_paid, notes)")
        appendLine("11. delete_transaction(transaction_id, customer_name, delete_all)")
        appendLine("12. add_financial_record(category=['BUSINESS_INCOME'|'BUSINESS_EXPENSE'|'PERSONAL_INCOME'|'PERSONAL_EXPENSE'], amount, description)")
        appendLine("13. edit_financial_record(record_id, target_description, category, amount, new_description)")
        appendLine("14. delete_financial_record(record_id, description, category_prefix, delete_all)")
        appendLine("15. update_business_profile(business_name, business_address, business_phone)")
        appendLine("16. erase_all_data(target=['ALL'|'CUSTOMERS'|'PRODUCTS'|'STOCKS'|'TRANSACTIONS'|'FINANCIALS'])")
        appendLine("Setelah blok `<tool_call>`, tuliskan penjelasan singkat dan ramah dalam Bahasa Indonesia mengenai aksi yang dijalankan.")
    }

    /**
     * Executes a list of parsed tool calls against Room DB and returns their execution summaries.
     */
    suspend fun executeToolCalls(
        calls: List<ParsedToolCall>,
        currentBusinessProfile: Triple<String, String, String>
    ): Pair<List<AgentToolExecutionSummary>, Triple<String, String, String>?> {
        val summaries = mutableListOf<AgentToolExecutionSummary>()
        var updatedProfile: Triple<String, String, String>? = null

        for (call in calls) {
            val args = call.arguments
            try {
                when (call.name.lowercase().trim()) {
                    "add_customer" -> {
                        val name = args.optString("name").trim()
                        if (name.isBlank()) {
                            summaries.add(AgentToolExecutionSummary(call.name, "ADD", "Gagal: Nama warung kosong", false))
                            continue
                        }
                        val address = args.optString("address", "Alamat belum diisi").trim().ifBlank { "Alamat belum diisi" }
                        val phone = args.optString("phone", "-").trim().ifBlank { "-" }
                        val routeDay = normalizeDay(args.optString("route_day", "Senin"))
                        val existingCount = customerDao.getAllCustomersDirect().size
                        val routeOrder = args.optInt("route_order", existingCount + 1).coerceAtLeast(1)
                        val lat = if (args.has("latitude") && !args.isNull("latitude")) args.optDouble("latitude") else null
                        val lng = if (args.has("longitude") && !args.isNull("longitude")) args.optDouble("longitude") else null

                        val id = customerDao.insertCustomer(
                            Customer(
                                name = name,
                                address = address,
                                phone = phone,
                                route_day = routeDay,
                                route_order = routeOrder,
                                latitude = lat,
                                longitude = lng
                            )
                        )
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "add_customer",
                                actionType = "ADD",
                                summary = "Menambahkan warung **$name** (ID #$id, Rute $routeDay #$routeOrder, Alamat: $address)"
                            )
                        )
                    }

                    "edit_customer" -> {
                        val id = args.optLong("customer_id", 0L)
                        val targetName = args.optString("target_name").ifBlank { args.optString("name") }.trim()
                        val target = findCustomer(id, targetName)
                        if (target == null) {
                            summaries.add(
                                AgentToolExecutionSummary("edit_customer", "EDIT", "Warung '$targetName' tidak ditemukan", false)
                            )
                            continue
                        }
                        val newName = args.optString("new_name").trim().ifBlank { target.name }
                        val newAddr = args.optString("address").trim().ifBlank { target.address }
                        val newPhone = args.optString("phone").trim().ifBlank { target.phone }
                        val newDay = if (args.has("route_day") && args.optString("route_day").isNotBlank()) {
                            normalizeDay(args.optString("route_day"))
                        } else target.route_day
                        val newOrder = if (args.has("route_order")) args.optInt("route_order", target.route_order) else target.route_order
                        val newLat = if (args.has("latitude") && !args.isNull("latitude")) args.optDouble("latitude") else target.latitude
                        val newLng = if (args.has("longitude") && !args.isNull("longitude")) args.optDouble("longitude") else target.longitude

                        customerDao.updateCustomer(
                            target.copy(
                                name = newName,
                                address = newAddr,
                                phone = newPhone,
                                route_day = newDay,
                                route_order = newOrder,
                                latitude = newLat,
                                longitude = newLng
                            )
                        )
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "edit_customer",
                                actionType = "EDIT",
                                summary = "Memperbarui warung **${target.name}** → **$newName** (Rute $newDay #$newOrder, $newAddr)"
                            )
                        )
                    }

                    "delete_customer" -> {
                        val deleteAll = args.optBoolean("delete_all", false)
                        if (deleteAll) {
                            val count = customerDao.getAllCustomersDirect().size
                            stockDao.deleteAllStocks()
                            customerDao.deleteAllCustomers()
                            summaries.add(
                                AgentToolExecutionSummary(
                                    toolName = "delete_customer",
                                    actionType = "ERASE",
                                    summary = "Menghapus seluruh data warung ($count warung beserta stok terkait)"
                                )
                            )
                        } else {
                            val id = args.optLong("customer_id", 0L)
                            val name = args.optString("name").ifBlank { args.optString("target_name") }.trim()
                            val target = findCustomer(id, name)
                            if (target == null) {
                                summaries.add(
                                    AgentToolExecutionSummary("delete_customer", "ERASE", "Warung '$name' tidak ditemukan", false)
                                )
                            } else {
                                stockDao.deleteStocksForCustomer(target.id)
                                customerDao.deleteCustomer(target)
                                summaries.add(
                                    AgentToolExecutionSummary(
                                        toolName = "delete_customer",
                                        actionType = "ERASE",
                                        summary = "Menghapus warung **${target.name}** (ID #${target.id})"
                                    )
                                )
                            }
                        }
                    }

                    "add_product" -> {
                        val name = args.optString("name").trim()
                        if (name.isBlank()) {
                            summaries.add(AgentToolExecutionSummary("add_product", "ADD", "Gagal: Nama produk kosong", false))
                            continue
                        }
                        val unitSmall = args.optString("unit_small", "Pcs").trim().ifBlank { "Pcs" }
                        val unitBig = args.optString("unit_big", "Pack").trim().ifBlank { "Pack" }
                        val pcsPerPack = args.optInt("pieces_per_pack", 10).coerceAtLeast(1)

                        val sellingUnitArg = args.optDouble("selling_price_unit", 0.0)
                        val costUnitArg = args.optDouble("cost_price_unit", 0.0)

                        val sellingPack = when {
                            args.has("selling_price_pack") && args.optDouble("selling_price_pack", 0.0) > 0 ->
                                args.optDouble("selling_price_pack")
                            sellingUnitArg > 0 -> sellingUnitArg * pcsPerPack
                            else -> 16000.0
                        }
                        val costPack = when {
                            args.has("cost_price_pack") && args.optDouble("cost_price_pack", 0.0) > 0 ->
                                args.optDouble("cost_price_pack")
                            costUnitArg > 0 -> costUnitArg * pcsPerPack
                            else -> (sellingPack * 0.72).roundToInt().toDouble()
                        }

                        val costUnit = costPack / pcsPerPack
                        val sellingUnit = sellingPack / pcsPerPack

                        val id = productDao.insertProduct(
                            Product(
                                name = name,
                                unit = unitSmall,
                                unit_small = unitSmall,
                                unit_big = unitBig,
                                pieces_per_pack = pcsPerPack,
                                cost_price_pack = costPack,
                                selling_price_pack = sellingPack,
                                cost_price = costUnit,
                                selling_price = sellingUnit
                            )
                        )
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "add_product",
                                actionType = "ADD",
                                summary = "Menambahkan produk **$name** (ID #$id, 1 $unitBig = $pcsPerPack $unitSmall, Jual Rp %,.0f/%s, Modal Rp %,.0f/%s)".format(
                                    sellingPack, unitBig, costPack, unitBig
                                )
                            )
                        )
                    }

                    "edit_product" -> {
                        val id = args.optLong("product_id", 0L)
                        val targetName = args.optString("target_name").ifBlank { args.optString("name") }.trim()
                        val target = findProduct(id, targetName)
                        if (target == null) {
                            summaries.add(
                                AgentToolExecutionSummary("edit_product", "EDIT", "Produk '$targetName' tidak ditemukan", false)
                            )
                            continue
                        }
                        val newName = args.optString("new_name").trim().ifBlank { target.name }
                        val unitSmall = args.optString("unit_small").trim().ifBlank { target.unit_small }
                        val unitBig = args.optString("unit_big").trim().ifBlank { target.unit_big }
                        val pcsPerPack = if (args.has("pieces_per_pack")) {
                            args.optInt("pieces_per_pack", target.pieces_per_pack).coerceAtLeast(1)
                        } else target.pieces_per_pack

                        val sellingPack = when {
                            args.has("selling_price_pack") && args.optDouble("selling_price_pack", 0.0) > 0 ->
                                args.optDouble("selling_price_pack")
                            args.has("selling_price_unit") && args.optDouble("selling_price_unit", 0.0) > 0 ->
                                args.optDouble("selling_price_unit") * pcsPerPack
                            else -> target.selling_price_pack
                        }
                        val costPack = when {
                            args.has("cost_price_pack") && args.optDouble("cost_price_pack", 0.0) > 0 ->
                                args.optDouble("cost_price_pack")
                            args.has("cost_price_unit") && args.optDouble("cost_price_unit", 0.0) > 0 ->
                                args.optDouble("cost_price_unit") * pcsPerPack
                            else -> target.cost_price_pack
                        }

                        productDao.updateProduct(
                            target.copy(
                                name = newName,
                                unit = unitSmall,
                                unit_small = unitSmall,
                                unit_big = unitBig,
                                pieces_per_pack = pcsPerPack,
                                cost_price_pack = costPack,
                                selling_price_pack = sellingPack,
                                cost_price = costPack / pcsPerPack,
                                selling_price = sellingPack / pcsPerPack
                            )
                        )
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "edit_product",
                                actionType = "EDIT",
                                summary = "Memperbarui produk **${target.name}** → **$newName** (Jual Rp %,.0f/%s, Modal Rp %,.0f/%s)".format(
                                    sellingPack, unitBig, costPack, unitBig
                                )
                            )
                        )
                    }

                    "delete_product" -> {
                        val deleteAll = args.optBoolean("delete_all", false)
                        if (deleteAll) {
                            val count = productDao.getAllProductsDirect().size
                            stockDao.deleteAllStocks()
                            productDao.deleteAllProducts()
                            summaries.add(
                                AgentToolExecutionSummary(
                                    toolName = "delete_product",
                                    actionType = "ERASE",
                                    summary = "Menghapus seluruh produk di katalog ($count produk)"
                                )
                            )
                        } else {
                            val id = args.optLong("product_id", 0L)
                            val name = args.optString("name").ifBlank { args.optString("target_name") }.trim()
                            val target = findProduct(id, name)
                            if (target == null) {
                                summaries.add(
                                    AgentToolExecutionSummary("delete_product", "ERASE", "Produk '$name' tidak ditemukan", false)
                                )
                            } else {
                                stockDao.deleteStocksForProduct(target.id)
                                productDao.deleteProduct(target)
                                summaries.add(
                                    AgentToolExecutionSummary(
                                        toolName = "delete_product",
                                        actionType = "ERASE",
                                        summary = "Menghapus produk **${target.name}** dari katalog"
                                    )
                                )
                            }
                        }
                    }

                    "set_consignment_stock" -> {
                        val cust = findCustomer(args.optLong("customer_id", 0L), args.optString("customer_name"))
                        val prod = findProduct(args.optLong("product_id", 0L), args.optString("product_name"))
                        if (cust == null || prod == null) {
                            summaries.add(
                                AgentToolExecutionSummary(
                                    "set_consignment_stock",
                                    "EDIT",
                                    "Warung atau produk tidak ditemukan (Warung: '${args.optString("customer_name")}', Produk: '${args.optString("product_name")}')",
                                    false
                                )
                            )
                            continue
                        }
                        val packs = args.optInt("quantity_packs", 0)
                        val pieces = args.optInt("quantity_pieces", 0)
                        val deltaPieces = (packs * prod.pieces_per_pack) + pieces
                        val mode = args.optString("mode", "SET").uppercase()

                        val existing = stockDao.getStock(cust.id, prod.id)
                        val prevQty = existing?.current_quantity ?: 0
                        val hasQtyArgs = args.has("quantity_packs") || args.has("quantity_pieces")
                        val newQty = if (!hasQtyArgs && existing != null) {
                            prevQty
                        } else {
                            when (mode) {
                                "ADD" -> (prevQty + deltaPieces).coerceAtLeast(0)
                                "SUBTRACT" -> (prevQty - deltaPieces).coerceAtLeast(0)
                                else -> deltaPieces.coerceAtLeast(0)
                            }
                        }

                        val customPricePack: Double? = when {
                            args.has("custom_price_pack") && args.optDouble("custom_price_pack", 0.0) > 0 ->
                                args.optDouble("custom_price_pack")
                            args.has("custom_price_unit") && args.optDouble("custom_price_unit", 0.0) > 0 ->
                                args.optDouble("custom_price_unit") * prod.pieces_per_pack
                            else -> existing?.custom_price_pack
                        }

                        if (existing != null) {
                            stockDao.updateStock(
                                existing.copy(
                                    current_quantity = newQty,
                                    custom_price_pack = customPricePack,
                                    last_updated = System.currentTimeMillis()
                                )
                            )
                        } else {
                            stockDao.insertOrUpdateStock(
                                ConsignmentStock(
                                    customer_id = cust.id,
                                    product_id = prod.id,
                                    current_quantity = newQty,
                                    custom_price_pack = customPricePack,
                                    last_updated = System.currentTimeMillis()
                                )
                            )
                        }
                        val priceNote = if (customPricePack != null && customPricePack > 0) {
                            " • Harga Khusus Warung: **Rp %,.0f/%s**".format(customPricePack, prod.unit_big)
                        } else ""
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "set_consignment_stock",
                                actionType = if (existing == null) "ADD" else "EDIT",
                                summary = "Stok **${prod.name}** di **${cust.name}** diatur dari $prevQty menjadi **${prod.formatPackAndPieces(newQty)}**$priceNote"
                            )
                        )
                    }

                    "delete_consignment_stock" -> {
                        val deleteAll = args.optBoolean("delete_all", false)
                        val deleteAllCust = args.optBoolean("delete_all_for_customer", false)
                        if (deleteAll) {
                            stockDao.deleteAllStocks()
                            summaries.add(
                                AgentToolExecutionSummary(
                                    "delete_consignment_stock",
                                    "ERASE",
                                    "Menghapus seluruh stok titipan di semua warung"
                                )
                            )
                        } else {
                            val cust = findCustomer(args.optLong("customer_id", 0L), args.optString("customer_name"))
                            if (cust != null && (deleteAllCust || args.optString("product_name").isBlank())) {
                                stockDao.deleteStocksForCustomer(cust.id)
                                summaries.add(
                                    AgentToolExecutionSummary(
                                        "delete_consignment_stock",
                                        "ERASE",
                                        "Menghapus semua stok titipan di warung **${cust.name}**"
                                    )
                                )
                            } else {
                                val prod = findProduct(args.optLong("product_id", 0L), args.optString("product_name"))
                                if (cust != null && prod != null) {
                                    stockDao.deleteStock(cust.id, prod.id)
                                    summaries.add(
                                        AgentToolExecutionSummary(
                                            "delete_consignment_stock",
                                            "ERASE",
                                            "Menghapus stok **${prod.name}** di warung **${cust.name}**"
                                        )
                                    )
                                } else {
                                    summaries.add(
                                        AgentToolExecutionSummary(
                                            "delete_consignment_stock",
                                            "ERASE",
                                            "Warung atau produk untuk penghapusan stok tidak ditemukan",
                                            false
                                        )
                                    )
                                }
                            }
                        }
                    }

                    "record_reconciliation" -> {
                        val cust = findCustomer(args.optLong("customer_id", 0L), args.optString("customer_name"))
                        val prod = findProduct(args.optLong("product_id", 0L), args.optString("product_name"))
                            ?: productDao.getAllProductsDirect().firstOrNull()
                        if (cust == null || prod == null) {
                            summaries.add(
                                AgentToolExecutionSummary(
                                    "record_reconciliation",
                                    "ADD",
                                    "Warung atau produk untuk rekonsiliasi tidak ditemukan",
                                    false
                                )
                            )
                            continue
                        }
                        val existingStock = stockDao.getStock(cust.id, prod.id)?.current_quantity ?: 0
                        val remStock = args.optInt("remaining_stock", 0).coerceAtLeast(0)
                        val returnedQty = args.optInt("returned_quantity", 0).coerceAtLeast(0)
                        val soldQty = (existingStock - remStock - returnedQty).coerceAtLeast(0)
                        val addedPacks = args.optInt("added_packs", 0).coerceAtLeast(0)
                        val addedPieces = args.optInt("added_pieces", 0).coerceAtLeast(0)
                        val totalAdded = (addedPacks * prod.pieces_per_pack) + addedPieces
                        val finalStock = remStock + totalAdded
                        val totalSoldAmount = soldQty * prod.selling_price
                        val amountPaid = if (args.has("amount_paid")) args.optDouble("amount_paid", totalSoldAmount) else totalSoldAmount
                        val notes = args.optString("notes", "Dicatat via AI Agent").ifBlank { "Dicatat via AI Agent" }
                        val now = System.currentTimeMillis()

                        val headerId = transactionDao.insertHeader(
                            TransactionHeader(
                                customer_id = cust.id,
                                transaction_date = now,
                                total_sold_amount = totalSoldAmount,
                                amount_paid = amountPaid,
                                notes = notes
                            )
                        )
                        transactionDao.insertDetails(
                            listOf(
                                TransactionDetail(
                                    transaction_id = headerId,
                                    product_id = prod.id,
                                    previous_stock = existingStock,
                                    remaining_stock = remStock,
                                    sold_quantity = soldQty,
                                    returned_quantity = returnedQty,
                                    added_quantity = totalAdded,
                                    unit_price = prod.selling_price
                                )
                            )
                        )
                        stockDao.insertOrUpdateStock(
                            ConsignmentStock(
                                id = stockDao.getStock(cust.id, prod.id)?.id ?: 0L,
                                customer_id = cust.id,
                                product_id = prod.id,
                                current_quantity = finalStock,
                                last_updated = now
                            )
                        )
                        if (amountPaid > 0) {
                            financialDao.insertRecord(
                                FinancialRecord(
                                    category = FinancialCategory.BUSINESS_INCOME,
                                    amount = amountPaid,
                                    description = "Setoran Konsinyasi - ${cust.name}",
                                    transaction_date = now
                                )
                            )
                        }
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "record_reconciliation",
                                actionType = "ADD",
                                summary = "Mencatat rekonsiliasi **${cust.name}** (#$headerId): Terjual $soldQty ${prod.unit_small} (Rp %,.0f), Dibayar Rp %,.0f, Stok akhir $finalStock ${prod.unit_small}".format(
                                    totalSoldAmount, amountPaid
                                )
                            )
                        )
                    }

                    "edit_transaction" -> {
                        val txId = args.optLong("transaction_id", 0L)
                        val custName = args.optString("customer_name")
                        val headers = transactionDao.getAllHeadersDirect()
                        val targetHeader = if (txId > 0L) {
                            headers.find { it.id == txId }
                        } else {
                            val cust = findCustomer(0L, custName)
                            if (cust != null) headers.filter { it.customer_id == cust.id }.maxByOrNull { it.transaction_date }
                            else headers.maxByOrNull { it.transaction_date }
                        }

                        if (targetHeader == null) {
                            summaries.add(
                                AgentToolExecutionSummary("edit_transaction", "EDIT", "Transaksi tidak ditemukan", false)
                            )
                            continue
                        }

                        val newSold = if (args.has("total_sold_amount")) args.optDouble("total_sold_amount", targetHeader.total_sold_amount) else targetHeader.total_sold_amount
                        val newPaid = if (args.has("amount_paid")) args.optDouble("amount_paid", targetHeader.amount_paid) else targetHeader.amount_paid
                        val newNotes = if (args.has("notes")) args.optString("notes", targetHeader.notes ?: "") else targetHeader.notes

                        transactionDao.insertHeader(
                            targetHeader.copy(
                                total_sold_amount = newSold,
                                amount_paid = newPaid,
                                notes = newNotes
                            )
                        )
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "edit_transaction",
                                actionType = "EDIT",
                                summary = "Memperbarui transaksi #${targetHeader.id} (Total: Rp %,.0f, Dibayar: Rp %,.0f)".format(newSold, newPaid)
                            )
                        )
                    }

                    "delete_transaction" -> {
                        val deleteAll = args.optBoolean("delete_all", false)
                        if (deleteAll) {
                            val count = transactionDao.getAllHeadersDirect().size
                            transactionDao.deleteAllDetails()
                            transactionDao.deleteAllHeaders()
                            summaries.add(
                                AgentToolExecutionSummary(
                                    "delete_transaction",
                                    "ERASE",
                                    "Menghapus seluruh riwayat transaksi ($count transaksi)"
                                )
                            )
                        } else {
                            val txId = args.optLong("transaction_id", 0L)
                            val custName = args.optString("customer_name")
                            val headers = transactionDao.getAllHeadersDirect()
                            val targetHeader = if (txId > 0L) {
                                headers.find { it.id == txId }
                            } else {
                                val cust = findCustomer(0L, custName)
                                if (cust != null) headers.filter { it.customer_id == cust.id }.maxByOrNull { it.transaction_date }
                                else headers.firstOrNull()
                            }
                            if (targetHeader == null) {
                                summaries.add(
                                    AgentToolExecutionSummary("delete_transaction", "ERASE", "Transaksi yang akan dihapus tidak ditemukan", false)
                                )
                            } else {
                                transactionDao.deleteDetailsForHeader(targetHeader.id)
                                transactionDao.deleteHeaderById(targetHeader.id)
                                summaries.add(
                                    AgentToolExecutionSummary(
                                        "delete_transaction",
                                        "ERASE",
                                        "Menghapus transaksi #${targetHeader.id} (Rp %,.0f)".format(targetHeader.total_sold_amount)
                                    )
                                )
                            }
                        }
                    }

                    "add_financial_record" -> {
                        val rawCat = args.optString("category", FinancialCategory.BUSINESS_EXPENSE).uppercase()
                        val category = when {
                            rawCat.contains("PERSONAL") && rawCat.contains("INCOME") -> FinancialCategory.PERSONAL_INCOME
                            rawCat.contains("PERSONAL") -> FinancialCategory.PERSONAL_EXPENSE
                            rawCat.contains("INCOME") || rawCat.contains("MASUK") -> FinancialCategory.BUSINESS_INCOME
                            else -> FinancialCategory.BUSINESS_EXPENSE
                        }
                        val amount = args.optDouble("amount", 0.0)
                        val desc = args.optString("description", "Catatan Kas via AI").trim().ifBlank { "Catatan Kas via AI" }
                        if (amount <= 0) {
                            summaries.add(
                                AgentToolExecutionSummary("add_financial_record", "ADD", "Nominal keuangan harus lebih dari 0", false)
                            )
                            continue
                        }
                        val id = financialDao.insertRecord(
                            FinancialRecord(
                                category = category,
                                amount = amount,
                                description = desc
                            )
                        )
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "add_financial_record",
                                actionType = "ADD",
                                summary = "Mencatat kas **$desc** (#$id, Kategori: $category, Nominal: **Rp %,.0f**)".format(amount)
                            )
                        )
                    }

                    "edit_financial_record" -> {
                        val recId = args.optLong("record_id", 0L)
                        val targetDesc = args.optString("target_description").ifBlank { args.optString("description") }.trim()
                        val allRecords = financialDao.getAllRecordsDirect()
                        val target = if (recId > 0L) {
                            allRecords.find { it.id == recId }
                        } else if (targetDesc.isNotBlank()) {
                            allRecords.find { it.description.contains(targetDesc, ignoreCase = true) }
                        } else {
                            allRecords.firstOrNull()
                        }

                        if (target == null) {
                            summaries.add(
                                AgentToolExecutionSummary("edit_financial_record", "EDIT", "Catatan keuangan '$targetDesc' tidak ditemukan", false)
                            )
                            continue
                        }
                        val newAmount = if (args.has("amount") && args.optDouble("amount", 0.0) > 0) args.optDouble("amount") else target.amount
                        val newDesc = args.optString("new_description").trim().ifBlank { target.description }
                        val newCat = args.optString("category").trim().ifBlank { target.category }

                        financialDao.updateRecord(
                            target.copy(
                                category = newCat,
                                amount = newAmount,
                                description = newDesc
                            )
                        )
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "edit_financial_record",
                                actionType = "EDIT",
                                summary = "Memperbarui catatan kas **${target.description}** → **$newDesc** (**Rp %,.0f**)".format(newAmount)
                            )
                        )
                    }

                    "delete_financial_record" -> {
                        val deleteAll = args.optBoolean("delete_all", false)
                        val prefix = args.optString("category_prefix").uppercase().trim()
                        if (deleteAll) {
                            val count = financialDao.getAllRecordsDirect().size
                            financialDao.deleteAllRecords()
                            summaries.add(
                                AgentToolExecutionSummary("delete_financial_record", "ERASE", "Menghapus seluruh catatan buku kas ($count catatan)")
                            )
                        } else if (prefix == "BUSINESS" || prefix == "PERSONAL") {
                            financialDao.deleteRecordsByPrefix("${prefix}_")
                            summaries.add(
                                AgentToolExecutionSummary("delete_financial_record", "ERASE", "Menghapus semua catatan kas kategori $prefix")
                            )
                        } else {
                            val recId = args.optLong("record_id", 0L)
                            val desc = args.optString("description").ifBlank { args.optString("target_description") }.trim()
                            val allRecords = financialDao.getAllRecordsDirect()
                            val target = if (recId > 0L) {
                                allRecords.find { it.id == recId }
                            } else if (desc.isNotBlank()) {
                                allRecords.find { it.description.contains(desc, ignoreCase = true) }
                            } else {
                                allRecords.firstOrNull()
                            }
                            if (target == null) {
                                summaries.add(
                                    AgentToolExecutionSummary("delete_financial_record", "ERASE", "Catatan kas '$desc' tidak ditemukan", false)
                                )
                            } else {
                                financialDao.deleteRecord(target)
                                summaries.add(
                                    AgentToolExecutionSummary(
                                        "delete_financial_record",
                                        "ERASE",
                                        "Menghapus catatan kas **${target.description}** (Rp %,.0f)".format(target.amount)
                                    )
                                )
                            }
                        }
                    }

                    "update_business_profile" -> {
                        val newName = args.optString("business_name").trim().ifBlank { currentBusinessProfile.first }
                        val newAddr = args.optString("business_address").trim().ifBlank { currentBusinessProfile.second }
                        val newPhone = args.optString("business_phone").trim().ifBlank { currentBusinessProfile.third }
                        updatedProfile = Triple(newName, newAddr, newPhone)
                        summaries.add(
                            AgentToolExecutionSummary(
                                toolName = "update_business_profile",
                                actionType = "EDIT",
                                summary = "Memperbarui profil nota usaha menjadi **$newName** ($newAddr • $newPhone)"
                            )
                        )
                    }

                    "erase_all_data" -> {
                        val target = args.optString("target", "ALL").uppercase().trim()
                        when (target) {
                            "CUSTOMERS" -> {
                                stockDao.deleteAllStocks()
                                customerDao.deleteAllCustomers()
                                summaries.add(AgentToolExecutionSummary("erase_all_data", "ERASE", "Menghapus seluruh data warung & stok titipan"))
                            }
                            "PRODUCTS" -> {
                                stockDao.deleteAllStocks()
                                productDao.deleteAllProducts()
                                summaries.add(AgentToolExecutionSummary("erase_all_data", "ERASE", "Menghapus seluruh katalog produk & stok titipan"))
                            }
                            "STOCKS" -> {
                                stockDao.deleteAllStocks()
                                summaries.add(AgentToolExecutionSummary("erase_all_data", "ERASE", "Menghapus seluruh stok konsinyasi di semua warung"))
                            }
                            "TRANSACTIONS" -> {
                                transactionDao.deleteAllDetails()
                                transactionDao.deleteAllHeaders()
                                summaries.add(AgentToolExecutionSummary("erase_all_data", "ERASE", "Menghapus seluruh riwayat transaksi rekonsiliasi"))
                            }
                            "FINANCIALS" -> {
                                financialDao.deleteAllRecords()
                                summaries.add(AgentToolExecutionSummary("erase_all_data", "ERASE", "Menghapus seluruh catatan buku kas usaha & pribadi"))
                            }
                            else -> {
                                transactionDao.deleteAllDetails()
                                transactionDao.deleteAllHeaders()
                                stockDao.deleteAllStocks()
                                financialDao.deleteAllRecords()
                                customerDao.deleteAllCustomers()
                                productDao.deleteAllProducts()
                                summaries.add(
                                    AgentToolExecutionSummary(
                                        "erase_all_data",
                                        "ERASE",
                                        "Menghapus BERSIH seluruh data aplikasi (Warung, Produk, Stok, Transaksi & Buku Kas)"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                summaries.add(
                    AgentToolExecutionSummary(
                        toolName = call.name,
                        actionType = "EDIT",
                        summary = "Gagal menjalankan ${call.name}: ${e.message}",
                        success = false
                    )
                )
            }
        }

        return summaries to updatedProfile
    }

    /**
     * Local Natural Language Understanding (NLU) fallback parser for Indonesian & English CRUD commands.
     * Ensures Agent Mode tool calling works reliably even offline or if the LLM only returns text.
     */
    suspend fun parseLocalIntentToolCalls(query: String): List<ParsedToolCall> {
        val q = query.trim()
        val lower = q.lowercase()
        val calls = mutableListOf<ParsedToolCall>()

        // 1. Check for global erase / reset commands
        if ((lower.contains("hapus semua") || lower.contains("erase all") || lower.contains("delete all") || lower.contains("kosongkan semua") || lower.contains("bersihkan semua"))) {
            val target = when {
                lower.contains("warung") || lower.contains("toko") || lower.contains("customer") -> "CUSTOMERS"
                lower.contains("produk") || lower.contains("katalog") || lower.contains("barang") || lower.contains("product") -> "PRODUCTS"
                lower.contains("stok") || lower.contains("titipan") || lower.contains("stock") -> "STOCKS"
                lower.contains("transaksi") || lower.contains("nota") || lower.contains("transaction") -> "TRANSACTIONS"
                lower.contains("kas") || lower.contains("keuangan") || lower.contains("financial") -> "FINANCIALS"
                else -> "ALL"
            }
            calls.add(ParsedToolCall("erase_all_data", JSONObject().put("target", target)))
            return calls
        }

        val isAdd = lower.startsWith("tambah") || lower.startsWith("buat") || lower.startsWith("add") ||
                lower.startsWith("create") || lower.contains("tambah ") || lower.contains("add ") ||
                lower.startsWith("catat") || lower.contains("catat ")
        val isDelete = lower.startsWith("hapus") || lower.startsWith("delete") || lower.startsWith("erase") ||
                lower.startsWith("remove") || lower.contains("hapus ") || lower.contains("delete ") || lower.contains("erase ")
        val isEdit = lower.startsWith("ubah") || lower.startsWith("edit") || lower.startsWith("ganti") ||
                lower.startsWith("update") || lower.startsWith("set ") || lower.startsWith("atur ") ||
                lower.contains("ubah ") || lower.contains("edit ") || lower.contains("update ") || lower.contains("set stok")

        val allCustomers = customerDao.getAllCustomersDirect()
        val allProducts = productDao.getAllProductsDirect()

        // 2. Consignment Stock operations ("set stok ...", "tambah stok ...", "hapus stok ...")
        if (lower.contains("stok") || lower.contains("stock") || lower.contains("titipan")) {
            val matchedCust = allCustomers.firstOrNull { lower.contains(it.name.lowercase()) }
            val matchedProd = allProducts.firstOrNull { lower.contains(it.name.lowercase()) }
            val numbers = Regex("\\d+").findAll(q).mapNotNull { it.value.toIntOrNull() }.toList()

            if (isDelete && (matchedCust != null || matchedProd != null)) {
                val args = JSONObject()
                if (matchedCust != null) args.put("customer_id", matchedCust.id).put("customer_name", matchedCust.name)
                if (matchedProd != null) args.put("product_id", matchedProd.id).put("product_name", matchedProd.name)
                if (matchedCust != null && matchedProd == null) args.put("delete_all_for_customer", true)
                calls.add(ParsedToolCall("delete_consignment_stock", args))
                return calls
            }

            if ((isAdd || isEdit) && (matchedCust != null || allCustomers.isNotEmpty())) {
                val targetCust = matchedCust ?: allCustomers.first()
                val targetProd = matchedProd ?: allProducts.firstOrNull()
                if (targetProd != null) {
                    val qty = numbers.lastOrNull() ?: 20
                    val isPack = lower.contains("pack") || lower.contains("bal") || lower.contains("dus")
                    val mode = when {
                        lower.contains("tambah stok") || lower.contains("add stock") -> "ADD"
                        lower.contains("kurangi stok") || lower.contains("subtract") -> "SUBTRACT"
                        else -> "SET"
                    }
                    val args = JSONObject()
                        .put("customer_id", targetCust.id)
                        .put("customer_name", targetCust.name)
                        .put("product_id", targetProd.id)
                        .put("product_name", targetProd.name)
                        .put("mode", mode)
                    if (isPack) args.put("quantity_packs", qty) else args.put("quantity_pieces", qty)
                    calls.add(ParsedToolCall("set_consignment_stock", args))
                    return calls
                }
            }
        }

        // 3. Financial Records ("catat pengeluaran...", "tambah pemasukan...", "hapus kas...")
        if (lower.contains("pengeluaran") || lower.contains("pemasukan") || lower.contains("biaya") ||
            lower.contains("bensin") || lower.contains("kas ") || lower.contains("expense") || lower.contains("income")
        ) {
            val amount = parseRupiahAmount(q)
            if (isDelete) {
                val cleanedDesc = q.replace(Regex("(?i)(hapus|delete|erase|catatan|kas|keuangan|biaya|pengeluaran|pemasukan)"), "").trim()
                calls.add(ParsedToolCall("delete_financial_record", JSONObject().put("description", cleanedDesc)))
                return calls
            }
            if (isEdit && amount > 0) {
                val cleanedDesc = q.replace(Regex("(?i)(ubah|edit|update|catatan|kas|keuangan|biaya|pengeluaran|pemasukan|jadi|menjadi|rp|\\d+[.,]?\\d*\\s*(rb|ribu|jt|juta)?)"), "").trim()
                calls.add(
                    ParsedToolCall(
                        "edit_financial_record",
                        JSONObject()
                            .put("target_description", cleanedDesc)
                            .put("amount", amount)
                    )
                )
                return calls
            }
            if ((isAdd || amount > 0)) {
                val cat = when {
                    lower.contains("pribadi") && (lower.contains("masuk") || lower.contains("income") || lower.contains("gaji")) ->
                        FinancialCategory.PERSONAL_INCOME
                    lower.contains("pribadi") -> FinancialCategory.PERSONAL_EXPENSE
                    lower.contains("pemasukan") || lower.contains("income") || lower.contains("masuk") ->
                        FinancialCategory.BUSINESS_INCOME
                    else -> FinancialCategory.BUSINESS_EXPENSE
                }
                val desc = q.replace(
                    Regex("(?i)^(catat|tambah|buat|add|record)\\s+(pengeluaran|pemasukan|biaya|kas|usaha|pribadi)*\\s*"),
                    ""
                ).replace(Regex("(?i)(sebesar|senilai|rp\\.?\\s*\\d+[\\d.,]*\\s*(rb|ribu|jt|juta)?|\\d+\\s*(rb|ribu|jt|juta))"), "")
                    .trim()
                    .ifBlank { "Biaya Operasional" }

                calls.add(
                    ParsedToolCall(
                        "add_financial_record",
                        JSONObject()
                            .put("category", cat)
                            .put("amount", if (amount > 0) amount else 25000.0)
                            .put("description", desc)
                    )
                )
                return calls
            }
        }

        // 4. Customer / Warung CRUD ("tambah warung...", "edit warung...", "hapus warung...")
        if (lower.contains("warung") || lower.contains("toko") || lower.contains("outlet") || lower.contains("customer") || lower.contains("pelanggan")) {
            val matchedCust = allCustomers.firstOrNull { lower.contains(it.name.lowercase()) }
            if (isDelete) {
                val nameCandidate = matchedCust?.name ?: extractEntityName(q, listOf("hapus", "delete", "erase", "remove", "warung", "toko", "outlet", "customer", "pelanggan"))
                calls.add(
                    ParsedToolCall(
                        "delete_customer",
                        JSONObject().apply {
                            if (matchedCust != null) put("customer_id", matchedCust.id)
                            put("name", nameCandidate)
                        }
                    )
                )
                return calls
            }
            if (isEdit && matchedCust != null) {
                val args = JSONObject().put("customer_id", matchedCust.id).put("target_name", matchedCust.name)
                val dayMatch = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
                    .firstOrNull { lower.contains(it.lowercase()) }
                if (dayMatch != null) args.put("route_day", dayMatch)

                val addrMatch = Regex("(?i)(?:alamat|di)\\s+([^,]+)").find(q)?.groupValues?.get(1)?.trim()
                if (!addrMatch.isNullOrBlank()) args.put("address", addrMatch)

                val renameMatch = Regex("(?i)(?:nama\\s+jadi|menjadi|jadi|ke)\\s+([A-Za-z0-9\\s]+)").find(q)?.groupValues?.get(1)?.trim()
                if (!renameMatch.isNullOrBlank() && dayMatch == null) args.put("new_name", renameMatch)

                calls.add(ParsedToolCall("edit_customer", args))
                return calls
            }
            if (isAdd) {
                val dayMatch = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
                    .firstOrNull { lower.contains(it.lowercase()) } ?: "Senin"
                val addrMatch = Regex("(?i)(?:alamat|di)\\s+([^,]+)").find(q)?.groupValues?.get(1)?.trim() ?: "Jl. Raya Utama"
                val rawName = extractEntityName(q, listOf("tambah", "buat", "add", "create", "warung", "toko", "outlet", "customer", "pelanggan", "baru"))
                    .replace(Regex("(?i)\\s+(di|alamat|rute|hari)\\s+.*$"), "")
                    .trim()
                    .ifBlank { "Warung Mitra Baru" }
                calls.add(
                    ParsedToolCall(
                        "add_customer",
                        JSONObject()
                            .put("name", rawName)
                            .put("address", addrMatch)
                            .put("route_day", dayMatch)
                    )
                )
                return calls
            }
        }

        // 5. Product / Katalog CRUD ("tambah produk...", "ubah harga...", "hapus produk...")
        if (lower.contains("produk") || lower.contains("barang") || lower.contains("katalog") ||
            lower.contains("product") || lower.contains("harga") || lower.contains("price")
        ) {
            val matchedProd = allProducts.firstOrNull { lower.contains(it.name.lowercase()) }
            if (isDelete) {
                val prodName = matchedProd?.name ?: extractEntityName(q, listOf("hapus", "delete", "erase", "remove", "produk", "barang", "katalog", "product"))
                calls.add(
                    ParsedToolCall(
                        "delete_product",
                        JSONObject().apply {
                            if (matchedProd != null) put("product_id", matchedProd.id)
                            put("name", prodName)
                        }
                    )
                )
                return calls
            }
            if (isEdit && matchedProd != null) {
                val price = parseRupiahAmount(q)
                val args = JSONObject().put("product_id", matchedProd.id).put("target_name", matchedProd.name)
                if (price > 0) {
                    if (lower.contains("modal") || lower.contains("hpp") || lower.contains("cost")) {
                        args.put("cost_price_pack", if (price < 5000) price * matchedProd.pieces_per_pack else price)
                    } else {
                        args.put("selling_price_pack", if (price < 5000) price * matchedProd.pieces_per_pack else price)
                    }
                }
                calls.add(ParsedToolCall("edit_product", args))
                return calls
            }
            if (isAdd) {
                val price = parseRupiahAmount(q)
                val sellingPack = if (price > 0) (if (price < 5000) price * 10 else price) else 16000.0
                val costPack = (sellingPack * 0.72).roundToInt().toDouble()
                val rawName = extractEntityName(q, listOf("tambah", "buat", "add", "create", "produk", "barang", "katalog", "product", "baru"))
                    .replace(Regex("(?i)\\s+(harga|jual|modal|rp|\\d+).*$"), "")
                    .trim()
                    .ifBlank { "Produk Baru" }
                calls.add(
                    ParsedToolCall(
                        "add_product",
                        JSONObject()
                            .put("name", rawName)
                            .put("selling_price_pack", sellingPack)
                            .put("cost_price_pack", costPack)
                            .put("pieces_per_pack", 10)
                    )
                )
                return calls
            }
        }

        // 6. Delete or edit transaction by keyword
        if (lower.contains("transaksi") || lower.contains("nota") || lower.contains("rekonsiliasi")) {
            val matchedCust = allCustomers.firstOrNull { lower.contains(it.name.lowercase()) }
            if (isDelete) {
                val args = JSONObject()
                if (matchedCust != null) args.put("customer_name", matchedCust.name)
                calls.add(ParsedToolCall("delete_transaction", args))
                return calls
            }
        }

        return calls
    }

    private fun parseRupiahAmount(text: String): Double {
        val jutaMatch = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:jt|juta)", RegexOption.IGNORE_CASE).find(text)
        if (jutaMatch != null) {
            val num = jutaMatch.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            return num * 1_000_000.0
        }
        val ribuMatch = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:rb|ribu|k)\\b", RegexOption.IGNORE_CASE).find(text)
        if (ribuMatch != null) {
            val num = ribuMatch.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            return num * 1_000.0
        }
        val plainMatch = Regex("(?:rp\\.?\\s*)?(\\d{4,10})(?:\\b|$)", RegexOption.IGNORE_CASE).find(text.replace(".", "").replace(",", ""))
        if (plainMatch != null) {
            return plainMatch.groupValues[1].toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun extractEntityName(query: String, stopWords: List<String>): String {
        var result = query.trim()
        stopWords.forEach { word ->
            result = result.replace(Regex("(?i)^\\s*$word\\s+"), "")
            result = result.replace(Regex("(?i)\\b$word\\b"), "")
        }
        return result.trim().trim('"', '\'', ':', '-')
    }

    private suspend fun findCustomer(id: Long, nameQuery: String): Customer? {
        val all = customerDao.getAllCustomersDirect()
        if (id > 0L) {
            all.find { it.id == id }?.let { return it }
        }
        val clean = nameQuery.trim().lowercase()
        if (clean.isBlank()) return null
        return all.find { it.name.lowercase() == clean }
            ?: all.find { it.name.lowercase().contains(clean) || clean.contains(it.name.lowercase()) }
    }

    private suspend fun findProduct(id: Long, nameQuery: String): Product? {
        val all = productDao.getAllProductsDirect()
        if (id > 0L) {
            all.find { it.id == id }?.let { return it }
        }
        val clean = nameQuery.trim().lowercase()
        if (clean.isBlank()) return null
        return all.find { it.name.lowercase() == clean }
            ?: all.find { it.name.lowercase().contains(clean) || clean.contains(it.name.lowercase()) }
    }

    private fun normalizeDay(day: String): String {
        val lower = day.lowercase().trim()
        return when {
            lower.contains("sen") || lower.contains("mon") -> "Senin"
            lower.contains("sel") || lower.contains("tue") -> "Selasa"
            lower.contains("rab") || lower.contains("wed") -> "Rabu"
            lower.contains("kam") || lower.contains("thu") -> "Kamis"
            lower.contains("jum") || lower.contains("fri") -> "Jumat"
            lower.contains("sab") || lower.contains("sat") -> "Sabtu"
            lower.contains("min") || lower.contains("sun") -> "Minggu"
            else -> "Senin"
        }
    }
}
