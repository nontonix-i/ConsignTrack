package com.example.domain.agent

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

enum class ToolActionType {
    ADD,
    EDIT,
    DELETE,
    SETTING
}

data class ExecutedToolCall(
    val toolName: String,
    val actionType: ToolActionType,
    val title: String,
    val detail: String,
    val success: Boolean = true
)

data class AgentSettingsUpdate(
    val businessName: String? = null,
    val businessAddress: String? = null,
    val businessPhone: String? = null,
    val themeMode: String? = null,
    val language: String? = null,
    val defaultPaper80mm: Boolean? = null
)

data class AgentResponseResult(
    val replyMarkdown: String,
    val executedTools: List<ExecutedToolCall> = emptyList(),
    val settingsUpdate: AgentSettingsUpdate? = null
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
     * Builds the Gemini / OpenAI Function Declarations JSON array for all CRUD tools.
     */
    fun buildFunctionDeclarations(): JSONArray {
        val arr = JSONArray()

        // 1. add_customer
        arr.put(
            fnDecl(
                name = "add_customer",
                description = "Add a new store / warung outlet to the distribution route.",
                props = mapOf(
                    "name" to prop("STRING", "Store / warung name, e.g. 'Warung Berkah'"),
                    "address" to prop("STRING", "Store address"),
                    "phone" to prop("STRING", "Phone or WhatsApp number"),
                    "route_day" to prop("STRING", "Route day: Senin, Selasa, Rabu, Kamis, Jumat, Sabtu, or Minggu"),
                    "route_order" to prop("INTEGER", "Route sequence order number"),
                    "latitude" to prop("NUMBER", "Optional GPS latitude"),
                    "longitude" to prop("NUMBER", "Optional GPS longitude")
                ),
                required = listOf("name")
            )
        )

        // 2. edit_customer
        arr.put(
            fnDecl(
                name = "edit_customer",
                description = "Edit or update an existing store / warung outlet by its name or ID.",
                props = mapOf(
                    "target" to prop("STRING", "Existing store name or ID to edit"),
                    "new_name" to prop("STRING", "New store name if renaming"),
                    "address" to prop("STRING", "Updated address"),
                    "phone" to prop("STRING", "Updated phone/WhatsApp number"),
                    "route_day" to prop("STRING", "Updated route day (Senin..Minggu)"),
                    "route_order" to prop("INTEGER", "Updated route order number"),
                    "latitude" to prop("NUMBER", "Updated GPS latitude"),
                    "longitude" to prop("NUMBER", "Updated GPS longitude")
                ),
                required = listOf("target")
            )
        )

        // 3. delete_customer
        arr.put(
            fnDecl(
                name = "delete_customer",
                description = "Delete / erase a store / warung outlet by name or ID, or 'ALL' to delete all stores.",
                props = mapOf(
                    "target" to prop("STRING", "Store name, store ID, or 'ALL'")
                ),
                required = listOf("target")
            )
        )

        // 4. add_product
        arr.put(
            fnDecl(
                name = "add_product",
                description = "Add a new product to the product catalog.",
                props = mapOf(
                    "name" to prop("STRING", "Product name, e.g. 'Makaroni Pedas'"),
                    "selling_price_pack" to prop("NUMBER", "Selling price per pack in Rupiah, e.g. 16000"),
                    "cost_price_pack" to prop("NUMBER", "Cost / modal price per pack in Rupiah, e.g. 12000"),
                    "pieces_per_pack" to prop("INTEGER", "Number of pieces per pack, default 10"),
                    "unit_big" to prop("STRING", "Big unit label, default 'Pack'"),
                    "unit_small" to prop("STRING", "Small unit label, default 'Pcs'")
                ),
                required = listOf("name", "selling_price_pack")
            )
        )

        // 5. edit_product
        arr.put(
            fnDecl(
                name = "edit_product",
                description = "Edit or update an existing product's name, selling price, cost price, or units.",
                props = mapOf(
                    "target" to prop("STRING", "Existing product name or ID to edit"),
                    "new_name" to prop("STRING", "Updated product name"),
                    "selling_price_pack" to prop("NUMBER", "Updated selling price per pack in Rupiah"),
                    "cost_price_pack" to prop("NUMBER", "Updated cost/modal price per pack in Rupiah"),
                    "pieces_per_pack" to prop("INTEGER", "Updated pieces per pack"),
                    "unit_big" to prop("STRING", "Updated big unit label"),
                    "unit_small" to prop("STRING", "Updated small unit label")
                ),
                required = listOf("target")
            )
        )

        // 6. delete_product
        arr.put(
            fnDecl(
                name = "delete_product",
                description = "Delete / erase a product from the catalog by name or ID, or 'ALL' to delete all products.",
                props = mapOf(
                    "target" to prop("STRING", "Product name, product ID, or 'ALL'")
                ),
                required = listOf("target")
            )
        )

        // 7. set_consignment_stock
        arr.put(
            fnDecl(
                name = "set_consignment_stock",
                description = "Add, set, or update consigned product stock at a specific store / warung.",
                props = mapOf(
                    "customer_target" to prop("STRING", "Store / warung name or ID"),
                    "product_target" to prop("STRING", "Product name or ID"),
                    "quantity_pcs" to prop("INTEGER", "Quantity in pieces (pcs)"),
                    "quantity_packs" to prop("INTEGER", "Quantity in packs (will be multiplied by pieces_per_pack if quantity_pcs not given)"),
                    "mode" to prop("STRING", "'SET' to overwrite total stock, 'ADD' to add to existing stock, 'SUBTRACT' to reduce stock")
                ),
                required = listOf("customer_target", "product_target")
            )
        )

        // 8. delete_consignment_stock
        arr.put(
            fnDecl(
                name = "delete_consignment_stock",
                description = "Delete / erase consigned stock of a product at a store, or all stock at a store.",
                props = mapOf(
                    "customer_target" to prop("STRING", "Store name, store ID, or 'ALL'"),
                    "product_target" to prop("STRING", "Product name, product ID, or 'ALL' (default 'ALL' for the store)")
                ),
                required = listOf("customer_target")
            )
        )

        // 9. record_visit_transaction
        arr.put(
            fnDecl(
                name = "record_visit_transaction",
                description = "Record a store visit / sales reconciliation transaction and update stock and cashbook.",
                props = mapOf(
                    "customer_target" to prop("STRING", "Store / warung name or ID"),
                    "product_target" to prop("STRING", "Product name or ID"),
                    "sold_quantity" to prop("INTEGER", "Number of pieces sold"),
                    "added_packs" to prop("INTEGER", "Number of new packs consigned during visit"),
                    "amount_paid" to prop("NUMBER", "Cash amount paid by the store in Rupiah"),
                    "notes" to prop("STRING", "Optional visit notes")
                ),
                required = listOf("customer_target", "product_target", "sold_quantity")
            )
        )

        // 10. delete_visit_transaction
        arr.put(
            fnDecl(
                name = "delete_visit_transaction",
                description = "Delete / erase a visit transaction by transaction ID, latest visit for a store, or 'ALL'.",
                props = mapOf(
                    "target" to prop("STRING", "Transaction ID, store name, 'LATEST', or 'ALL'")
                ),
                required = listOf("target")
            )
        )

        // 11. add_financial_record
        arr.put(
            fnDecl(
                name = "add_financial_record",
                description = "Add an income or expense record to the Business Cashbook or Personal Cashbook.",
                props = mapOf(
                    "category" to prop(
                        "STRING",
                        "One of: BUSINESS_EXPENSE, BUSINESS_INCOME, PERSONAL_EXPENSE, PERSONAL_INCOME"
                    ),
                    "amount" to prop("NUMBER", "Amount in Rupiah, e.g. 25000"),
                    "description" to prop("STRING", "Description of the income or expense, e.g. 'Beli Bensin Motor'")
                ),
                required = listOf("category", "amount", "description")
            )
        )

        // 12. edit_financial_record
        arr.put(
            fnDecl(
                name = "edit_financial_record",
                description = "Edit or update an existing financial cashbook record by ID, description, or 'LATEST'.",
                props = mapOf(
                    "target" to prop("STRING", "Record ID, description keyword, or 'LATEST'"),
                    "new_amount" to prop("NUMBER", "Updated amount in Rupiah"),
                    "new_description" to prop("STRING", "Updated description"),
                    "new_category" to prop("STRING", "Updated category (BUSINESS_EXPENSE, BUSINESS_INCOME, PERSONAL_EXPENSE, PERSONAL_INCOME)")
                ),
                required = listOf("target")
            )
        )

        // 13. delete_financial_record
        arr.put(
            fnDecl(
                name = "delete_financial_record",
                description = "Delete / erase a financial cashbook record by ID, description keyword, 'LATEST', 'ALL_BUSINESS', 'ALL_PERSONAL', or 'ALL'.",
                props = mapOf(
                    "target" to prop("STRING", "Record ID, description keyword, 'LATEST', 'ALL_BUSINESS', 'ALL_PERSONAL', or 'ALL'")
                ),
                required = listOf("target")
            )
        )

        // 14. update_app_settings
        arr.put(
            fnDecl(
                name = "update_app_settings",
                description = "Update business profile receipt header, theme mode, language, or thermal printer paper size.",
                props = mapOf(
                    "business_name" to prop("STRING", "Business / distributor name for receipt header"),
                    "business_address" to prop("STRING", "Business address or tagline"),
                    "business_phone" to prop("STRING", "Business phone or WhatsApp"),
                    "theme_mode" to prop("STRING", "DARK, LIGHT, or SYSTEM"),
                    "language" to prop("STRING", "ID or EN"),
                    "paper_size" to prop("STRING", "58mm or 80mm")
                ),
                required = emptyList()
            )
        )

        return arr
    }

    /**
     * Executes a single parsed tool call against Room DB and returns an ExecutedToolCall summary.
     */
    suspend fun executeToolCall(
        call: ParsedToolCall,
        onSettingsUpdated: (AgentSettingsUpdate) -> Unit
    ): ExecutedToolCall {
        val args = call.arguments
        return try {
            when (call.name.lowercase().trim()) {
                "add_customer" -> execAddCustomer(args)
                "edit_customer", "update_customer" -> execEditCustomer(args)
                "delete_customer", "remove_customer", "erase_customer" -> execDeleteCustomer(args)

                "add_product" -> execAddProduct(args)
                "edit_product", "update_product" -> execEditProduct(args)
                "delete_product", "remove_product", "erase_product" -> execDeleteProduct(args)

                "set_consignment_stock", "add_consignment_stock", "update_stock", "set_stock" -> execSetConsignmentStock(args)
                "delete_consignment_stock", "remove_consignment_stock", "erase_stock" -> execDeleteConsignmentStock(args)

                "record_visit_transaction", "add_transaction", "add_visit" -> execRecordVisitTransaction(args)
                "delete_visit_transaction", "delete_transaction", "erase_transaction" -> execDeleteVisitTransaction(args)

                "add_financial_record", "add_cashbook", "add_finance" -> execAddFinancialRecord(args)
                "edit_financial_record", "update_financial_record", "edit_finance" -> execEditFinancialRecord(args)
                "delete_financial_record", "remove_financial_record", "erase_finance" -> execDeleteFinancialRecord(args)

                "update_app_settings", "edit_settings", "update_business_profile" -> execUpdateSettings(args, onSettingsUpdated)

                else -> ExecutedToolCall(
                    toolName = call.name,
                    actionType = ToolActionType.EDIT,
                    title = "Tool Tidak Dikenal (${call.name})",
                    detail = "Nama fungsi tidak terdaftar.",
                    success = false
                )
            }
        } catch (e: Exception) {
            ExecutedToolCall(
                toolName = call.name,
                actionType = ToolActionType.EDIT,
                title = "Gagal Menjalankan ${call.name}",
                detail = e.message ?: "Terjadi kesalahan saat eksekusi tool.",
                success = false
            )
        }
    }

    // =========================================================================
    // 1. CUSTOMER (WARUNG / OUTLET) TOOLS
    // =========================================================================

    private suspend fun execAddCustomer(args: JSONObject): ExecutedToolCall {
        val name = args.optString("name").trim()
        if (name.isBlank()) {
            return ExecutedToolCall("add_customer", ToolActionType.ADD, "Gagal Tambah Warung", "Nama warung wajib diisi.", false)
        }
        val address = args.optString("address", "").trim()
        val phone = args.optString("phone", "").trim()
        val routeDay = normalizeDay(args.optString("route_day", "Senin"))
        val existingOnDay = customerDao.getCustomersByRouteDayDirect(routeDay)
        val nextOrder = (existingOnDay.maxOfOrNull { it.route_order } ?: 0) + 1
        val routeOrder = args.optInt("route_order", nextOrder).coerceAtLeast(1)
        val lat = if (args.has("latitude") && !args.isNull("latitude")) args.optDouble("latitude") else null
        val lng = if (args.has("longitude") && !args.isNull("longitude")) args.optDouble("longitude") else null

        val newId = customerDao.insertCustomer(
            Customer(
                name = name,
                address = address,
                phone = phone,
                route_day = routeDay,
                route_order = routeOrder,
                latitude = if (lat != null && !lat.isNaN()) lat else null,
                longitude = if (lng != null && !lng.isNaN()) lng else null
            )
        )
        return ExecutedToolCall(
            toolName = "add_customer",
            actionType = ToolActionType.ADD,
            title = "Tambah Warung: $name",
            detail = "ID #$newId • Rute $routeDay (Urutan #$routeOrder)${if (address.isNotBlank()) " • $address" else ""}",
            success = true
        )
    }

    private suspend fun execEditCustomer(args: JSONObject): ExecutedToolCall {
        val target = args.optString("target").ifBlank { args.optString("name") }.trim()
        val existing = findCustomer(target)
            ?: return ExecutedToolCall("edit_customer", ToolActionType.EDIT, "Warung Tidak Ditemukan", "Tidak ditemukan warung '$target'.", false)

        val newName = args.optString("new_name").trim().ifBlank { existing.name }
        val newAddress = if (args.has("address") && !args.isNull("address")) args.optString("address").trim() else existing.address
        val newPhone = if (args.has("phone") && !args.isNull("phone")) args.optString("phone").trim() else existing.phone
        val newDay = if (args.has("route_day") && args.optString("route_day").isNotBlank()) {
            normalizeDay(args.optString("route_day"))
        } else {
            existing.route_day
        }
        val newOrder = if (args.has("route_order") && args.optInt("route_order", -1) > 0) {
            args.optInt("route_order")
        } else {
            existing.route_order
        }
        val newLat = if (args.has("latitude") && !args.isNull("latitude")) args.optDouble("latitude") else existing.latitude
        val newLng = if (args.has("longitude") && !args.isNull("longitude")) args.optDouble("longitude") else existing.longitude

        val updated = existing.copy(
            name = newName,
            address = newAddress,
            phone = newPhone,
            route_day = newDay,
            route_order = newOrder,
            latitude = newLat,
            longitude = newLng
        )
        customerDao.updateCustomer(updated)

        return ExecutedToolCall(
            toolName = "edit_customer",
            actionType = ToolActionType.EDIT,
            title = "Edit Warung: ${existing.name} → $newName",
            detail = "Rute $newDay (#$newOrder) • Alamat: ${newAddress.ifBlank { "-" }} • Telp: ${newPhone.ifBlank { "-" }}",
            success = true
        )
    }

    private suspend fun execDeleteCustomer(args: JSONObject): ExecutedToolCall {
        val target = args.optString("target").ifBlank { args.optString("name") }.trim()
        if (target.equals("ALL", ignoreCase = true) || target.equals("SEMUA", ignoreCase = true)) {
            val count = customerDao.getAllCustomersDirect().size
            stockDao.deleteAllStocks()
            customerDao.deleteAllCustomers()
            return ExecutedToolCall(
                toolName = "delete_customer",
                actionType = ToolActionType.DELETE,
                title = "Hapus Semua Warung",
                detail = "$count warung beserta stok konsinyasinya telah dihapus.",
                success = true
            )
        }

        val existing = findCustomer(target)
            ?: return ExecutedToolCall("delete_customer", ToolActionType.DELETE, "Warung Tidak Ditemukan", "Tidak ditemukan warung '$target'.", false)

        stockDao.deleteStocksForCustomer(existing.id)
        customerDao.deleteCustomer(existing)

        return ExecutedToolCall(
            toolName = "delete_customer",
            actionType = ToolActionType.DELETE,
            title = "Hapus Warung: ${existing.name}",
            detail = "Warung ID #${existing.id} (${existing.route_day}) & stok titipannya berhasil dihapus.",
            success = true
        )
    }

    // =========================================================================
    // 2. PRODUCT (KATALOG PRODUK) TOOLS
    // =========================================================================

    private suspend fun execAddProduct(args: JSONObject): ExecutedToolCall {
        val name = args.optString("name").trim()
        if (name.isBlank()) {
            return ExecutedToolCall("add_product", ToolActionType.ADD, "Gagal Tambah Produk", "Nama produk wajib diisi.", false)
        }
        val sellPack = args.optDouble("selling_price_pack", 16000.0).coerceAtLeast(100.0)
        val costPack = if (args.has("cost_price_pack") && !args.isNull("cost_price_pack")) {
            args.optDouble("cost_price_pack").coerceAtLeast(0.0)
        } else {
            (sellPack * 0.72).roundToInt().toDouble()
        }
        val piecesPerPack = args.optInt("pieces_per_pack", 10).coerceAtLeast(1)
        val unitBig = args.optString("unit_big", "Pack").trim().ifBlank { "Pack" }
        val unitSmall = args.optString("unit_small", "Pcs").trim().ifBlank { "Pcs" }

        val sellPcs = sellPack / piecesPerPack
        val costPcs = costPack / piecesPerPack

        val newId = productDao.insertProduct(
            Product(
                name = name,
                unit = unitSmall,
                unit_big = unitBig,
                pieces_per_pack = piecesPerPack,
                selling_price_pack = sellPack,
                cost_price_pack = costPack,
                selling_price = sellPcs,
                cost_price = costPcs
            )
        )
        return ExecutedToolCall(
            toolName = "add_product",
            actionType = ToolActionType.ADD,
            title = "Tambah Produk: $name",
            detail = "ID #$newId • Jual Rp %,.0f/%s • Modal Rp %,.0f/%s (1 %s = %d %s)".format(
                sellPack, unitBig, costPack, unitBig, unitBig, piecesPerPack, unitSmall
            ),
            success = true
        )
    }

    private suspend fun execEditProduct(args: JSONObject): ExecutedToolCall {
        val target = args.optString("target").ifBlank { args.optString("name") }.trim()
        val existing = findProduct(target)
            ?: return ExecutedToolCall("edit_product", ToolActionType.EDIT, "Produk Tidak Ditemukan", "Tidak ditemukan produk '$target'.", false)

        val newName = args.optString("new_name").trim().ifBlank { existing.name }
        val newPieces = if (args.has("pieces_per_pack") && args.optInt("pieces_per_pack", -1) > 0) {
            args.optInt("pieces_per_pack")
        } else {
            existing.pieces_per_pack
        }
        val newSellPack = if (args.has("selling_price_pack") && !args.isNull("selling_price_pack")) {
            args.optDouble("selling_price_pack")
        } else {
            existing.selling_price_pack
        }
        val newCostPack = if (args.has("cost_price_pack") && !args.isNull("cost_price_pack")) {
            args.optDouble("cost_price_pack")
        } else {
            existing.cost_price_pack
        }
        val newUnitBig = args.optString("unit_big").trim().ifBlank { existing.unit_big }
        val newUnitSmall = args.optString("unit_small").trim().ifBlank { existing.unit_small }

        val updated = existing.copy(
            name = newName,
            unit = newUnitSmall,
            unit_big = newUnitBig,
            pieces_per_pack = newPieces,
            selling_price_pack = newSellPack,
            cost_price_pack = newCostPack,
            selling_price = if (newPieces > 0) newSellPack / newPieces else existing.selling_price,
            cost_price = if (newPieces > 0) newCostPack / newPieces else existing.cost_price
        )
        productDao.updateProduct(updated)

        return ExecutedToolCall(
            toolName = "edit_product",
            actionType = ToolActionType.EDIT,
            title = "Edit Produk: $newName",
            detail = "Jual Rp %,.0f/%s • Modal Rp %,.0f/%s • Isi %d %s".format(
                newSellPack, newUnitBig, newCostPack, newUnitBig, newPieces, newUnitSmall
            ),
            success = true
        )
    }

    private suspend fun execDeleteProduct(args: JSONObject): ExecutedToolCall {
        val target = args.optString("target").ifBlank { args.optString("name") }.trim()
        if (target.equals("ALL", ignoreCase = true) || target.equals("SEMUA", ignoreCase = true)) {
            val count = productDao.getAllProductsDirect().size
            stockDao.deleteAllStocks()
            productDao.deleteAllProducts()
            return ExecutedToolCall(
                toolName = "delete_product",
                actionType = ToolActionType.DELETE,
                title = "Hapus Semua Produk Katalog",
                detail = "$count produk katalog telah dihapus.",
                success = true
            )
        }

        val existing = findProduct(target)
            ?: return ExecutedToolCall("delete_product", ToolActionType.DELETE, "Produk Tidak Ditemukan", "Tidak ditemukan produk '$target'.", false)

        stockDao.deleteStocksForProduct(existing.id)
        productDao.deleteProduct(existing)

        return ExecutedToolCall(
            toolName = "delete_product",
            actionType = ToolActionType.DELETE,
            title = "Hapus Produk: ${existing.name}",
            detail = "Produk ID #${existing.id} beserta stok titipannya berhasil dihapus.",
            success = true
        )
    }

    // =========================================================================
    // 3. CONSIGNMENT STOCK (STOK TITIPAN WARUNG) TOOLS
    // =========================================================================

    private suspend fun execSetConsignmentStock(args: JSONObject): ExecutedToolCall {
        val custTarget = args.optString("customer_target").ifBlank { args.optString("customer") }.trim()
        val prodTarget = args.optString("product_target").ifBlank { args.optString("product") }.trim()
        val mode = args.optString("mode", "SET").uppercase().trim()

        val customer = findCustomer(custTarget)
            ?: return ExecutedToolCall("set_consignment_stock", ToolActionType.EDIT, "Warung Tidak Ditemukan", "Warung '$custTarget' tidak ditemukan.", false)
        val product = findProduct(prodTarget)
            ?: return ExecutedToolCall("set_consignment_stock", ToolActionType.EDIT, "Produk Tidak Ditemukan", "Produk '$prodTarget' tidak ditemukan.", false)

        val deltaPcs = when {
            args.has("quantity_pcs") && !args.isNull("quantity_pcs") -> args.optInt("quantity_pcs", 0)
            args.has("quantity_packs") && !args.isNull("quantity_packs") -> args.optInt("quantity_packs", 0) * product.pieces_per_pack
            args.has("quantity") && !args.isNull("quantity") -> args.optInt("quantity", 0)
            else -> product.pieces_per_pack
        }

        val existing = stockDao.getStock(customer.id, product.id)
        val prevQty = existing?.current_quantity ?: 0
        val newQty = when (mode) {
            "ADD", "TAMBAH" -> (prevQty + deltaPcs).coerceAtLeast(0)
            "SUBTRACT", "KURANG" -> (prevQty - deltaPcs).coerceAtLeast(0)
            else -> deltaPcs.coerceAtLeast(0)
        }

        stockDao.insertOrUpdateStock(
            ConsignmentStock(
                customer_id = customer.id,
                product_id = product.id,
                current_quantity = newQty,
                last_updated = System.currentTimeMillis()
            )
        )

        return ExecutedToolCall(
            toolName = "set_consignment_stock",
            actionType = if (existing == null) ToolActionType.ADD else ToolActionType.EDIT,
            title = "Stok Titipan: ${customer.name}",
            detail = "${product.name}: $prevQty → $newQty ${product.unit_small} (${product.formatPackAndPieces(newQty)})",
            success = true
        )
    }

    private suspend fun execDeleteConsignmentStock(args: JSONObject): ExecutedToolCall {
        val custTarget = args.optString("customer_target").ifBlank { args.optString("customer") }.trim()
        val prodTarget = args.optString("product_target", "ALL").trim()

        if (custTarget.equals("ALL", ignoreCase = true) || custTarget.equals("SEMUA", ignoreCase = true)) {
            stockDao.deleteAllStocks()
            return ExecutedToolCall(
                toolName = "delete_consignment_stock",
                actionType = ToolActionType.DELETE,
                title = "Reset Semua Stok Konsinyasi",
                detail = "Seluruh stok titipan di semua warung telah dikosongkan.",
                success = true
            )
        }

        val customer = findCustomer(custTarget)
            ?: return ExecutedToolCall("delete_consignment_stock", ToolActionType.DELETE, "Warung Tidak Ditemukan", "Warung '$custTarget' tidak ditemukan.", false)

        if (prodTarget.isBlank() || prodTarget.equals("ALL", ignoreCase = true) || prodTarget.equals("SEMUA", ignoreCase = true)) {
            stockDao.deleteStocksForCustomer(customer.id)
            return ExecutedToolCall(
                toolName = "delete_consignment_stock",
                actionType = ToolActionType.DELETE,
                title = "Hapus Semua Stok: ${customer.name}",
                detail = "Semua produk titipan di ${customer.name} telah dihapus.",
                success = true
            )
        }

        val product = findProduct(prodTarget)
            ?: return ExecutedToolCall("delete_consignment_stock", ToolActionType.DELETE, "Produk Tidak Ditemukan", "Produk '$prodTarget' tidak ditemukan.", false)

        stockDao.deleteStock(customer.id, product.id)
        return ExecutedToolCall(
            toolName = "delete_consignment_stock",
            actionType = ToolActionType.DELETE,
            title = "Hapus Produk dari ${customer.name}",
            detail = "Stok '${product.name}' dihapus dari daftar titipan ${customer.name}.",
            success = true
        )
    }

    // =========================================================================
    // 4. VISIT / RECONCILIATION TRANSACTION TOOLS
    // =========================================================================

    private suspend fun execRecordVisitTransaction(args: JSONObject): ExecutedToolCall {
        val custTarget = args.optString("customer_target").ifBlank { args.optString("customer") }.trim()
        val prodTarget = args.optString("product_target").ifBlank { args.optString("product") }.trim()

        val customer = findCustomer(custTarget)
            ?: return ExecutedToolCall("record_visit_transaction", ToolActionType.ADD, "Warung Tidak Ditemukan", "Warung '$custTarget' tidak ditemukan.", false)
        val product = findProduct(prodTarget) ?: productDao.getAllProductsDirect().firstOrNull()
            ?: return ExecutedToolCall("record_visit_transaction", ToolActionType.ADD, "Produk Tidak Ditemukan", "Produk '$prodTarget' tidak ditemukan.", false)

        val existingStock = stockDao.getStock(customer.id, product.id)?.current_quantity ?: (product.pieces_per_pack * 2)
        val soldQty = args.optInt("sold_quantity", 0).coerceAtLeast(0)
        val remStock = if (args.has("remaining_stock") && !args.isNull("remaining_stock")) {
            args.optInt("remaining_stock").coerceAtLeast(0)
        } else {
            (existingStock - soldQty).coerceAtLeast(0)
        }
        val addedPacks = args.optInt("added_packs", 0).coerceAtLeast(0)
        val addedQty = addedPacks * product.pieces_per_pack
        val totalSoldAmount = soldQty * product.selling_price
        val amountPaid = if (args.has("amount_paid") && !args.isNull("amount_paid")) {
            args.optDouble("amount_paid")
        } else {
            totalSoldAmount
        }
        val notes = args.optString("notes", "Dicatat via AI Agent").trim()
        val now = System.currentTimeMillis()

        val headerId = transactionDao.insertHeader(
            TransactionHeader(
                customer_id = customer.id,
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
                    product_id = product.id,
                    previous_stock = existingStock,
                    remaining_stock = remStock,
                    sold_quantity = soldQty,
                    returned_quantity = 0,
                    added_quantity = addedQty,
                    unit_price = product.selling_price
                )
            )
        )

        val finalStock = remStock + addedQty
        stockDao.insertOrUpdateStock(
            ConsignmentStock(
                customer_id = customer.id,
                product_id = product.id,
                current_quantity = finalStock,
                last_updated = now
            )
        )

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

        return ExecutedToolCall(
            toolName = "record_visit_transaction",
            actionType = ToolActionType.ADD,
            title = "Rekonsiliasi Kunjungan: ${customer.name}",
            detail = "Nota #TRX-$headerId • Laku $soldQty ${product.unit_small} (${product.name}) • Total Rp %,.0f • Setor Rp %,.0f".format(
                totalSoldAmount, amountPaid
            ),
            success = true
        )
    }

    private suspend fun execDeleteVisitTransaction(args: JSONObject): ExecutedToolCall {
        val target = args.optString("target").trim()
        if (target.equals("ALL", ignoreCase = true) || target.equals("SEMUA", ignoreCase = true)) {
            val count = transactionDao.getAllHeadersDirect().size
            transactionDao.deleteAllDetails()
            transactionDao.deleteAllHeaders()
            return ExecutedToolCall(
                toolName = "delete_visit_transaction",
                actionType = ToolActionType.DELETE,
                title = "Hapus Semua Riwayat Kunjungan",
                detail = "$count transaksi kunjungan berhasil dihapus.",
                success = true
            )
        }

        val headers = transactionDao.getAllHeadersDirect()
        if (headers.isEmpty()) {
            return ExecutedToolCall("delete_visit_transaction", ToolActionType.DELETE, "Tidak Ada Transaksi", "Belum ada transaksi kunjungan.", false)
        }

        val targetHeader = when {
            target.equals("LATEST", ignoreCase = true) || target.equals("TERAKHIR", ignoreCase = true) -> headers.firstOrNull()
            target.removePrefix("#").removePrefix("TRX-").toLongOrNull() != null -> {
                val id = target.removePrefix("#").removePrefix("TRX-").toLong()
                headers.find { it.id == id }
            }
            else -> {
                val customer = findCustomer(target)
                if (customer != null) headers.firstOrNull { it.customer_id == customer.id } else null
            }
        } ?: return ExecutedToolCall("delete_visit_transaction", ToolActionType.DELETE, "Transaksi Tidak Ditemukan", "Tidak ditemukan transaksi '$target'.", false)

        transactionDao.deleteDetailsForHeader(targetHeader.id)
        transactionDao.deleteHeaderById(targetHeader.id)

        return ExecutedToolCall(
            toolName = "delete_visit_transaction",
            actionType = ToolActionType.DELETE,
            title = "Hapus Transaksi #TRX-${targetHeader.id}",
            detail = "Transaksi senilai Rp %,.0f telah dihapus.".format(targetHeader.total_sold_amount),
            success = true
        )
    }

    // =========================================================================
    // 5. FINANCIAL CASHBOOK (BUKU KAS BISNIS & PRIBADI) TOOLS
    // =========================================================================

    private suspend fun execAddFinancialRecord(args: JSONObject): ExecutedToolCall {
        val rawCategory = args.optString("category", FinancialCategory.BUSINESS_EXPENSE).uppercase().trim()
        val category = when {
            rawCategory.contains("PERSONAL") && rawCategory.contains("INCOME") -> FinancialCategory.PERSONAL_INCOME
            rawCategory.contains("PERSONAL") || rawCategory.contains("PRIBADI") -> FinancialCategory.PERSONAL_EXPENSE
            rawCategory.contains("INCOME") || rawCategory.contains("MASUK") -> FinancialCategory.BUSINESS_INCOME
            else -> FinancialCategory.BUSINESS_EXPENSE
        }
        val amount = args.optDouble("amount", 0.0)
        val description = args.optString("description", "Catatan Kas").trim().ifBlank { "Catatan Kas" }

        if (amount <= 0) {
            return ExecutedToolCall("add_financial_record", ToolActionType.ADD, "Gagal Catat Kas", "Nominal harus lebih besar dari 0.", false)
        }

        val newId = financialDao.insertRecord(
            FinancialRecord(
                category = category,
                amount = amount,
                description = description,
                transaction_date = System.currentTimeMillis()
            )
        )

        val label = when (category) {
            FinancialCategory.BUSINESS_INCOME -> "Pemasukan Bisnis"
            FinancialCategory.BUSINESS_EXPENSE -> "Pengeluaran Bisnis"
            FinancialCategory.PERSONAL_INCOME -> "Pemasukan Pribadi"
            else -> "Pengeluaran Pribadi"
        }

        return ExecutedToolCall(
            toolName = "add_financial_record",
            actionType = ToolActionType.ADD,
            title = "Catat $label: $description",
            detail = "ID #$newId • Nominal Rp %,.0f".format(amount),
            success = true
        )
    }

    private suspend fun execEditFinancialRecord(args: JSONObject): ExecutedToolCall {
        val target = args.optString("target").trim()
        val allRecords = financialDao.getAllRecordsDirect()
        if (allRecords.isEmpty()) {
            return ExecutedToolCall("edit_financial_record", ToolActionType.EDIT, "Buku Kas Kosong", "Belum ada catatan buku kas.", false)
        }

        val record = findFinancialRecord(target, allRecords)
            ?: return ExecutedToolCall("edit_financial_record", ToolActionType.EDIT, "Catatan Kas Tidak Ditemukan", "Tidak ditemukan catatan kas '$target'.", false)

        val newAmount = if (args.has("new_amount") && !args.isNull("new_amount") && args.optDouble("new_amount") > 0) {
            args.optDouble("new_amount")
        } else {
            record.amount
        }
        val newDesc = args.optString("new_description").trim().ifBlank { record.description }
        val newCat = args.optString("new_category").trim().ifBlank { record.category }

        val updated = record.copy(
            amount = newAmount,
            description = newDesc,
            category = newCat
        )
        financialDao.updateRecord(updated)

        return ExecutedToolCall(
            toolName = "edit_financial_record",
            actionType = ToolActionType.EDIT,
            title = "Edit Kas #${record.id}: $newDesc",
            detail = "Nominal: Rp %,.0f → Rp %,.0f".format(record.amount, newAmount),
            success = true
        )
    }

    private suspend fun execDeleteFinancialRecord(args: JSONObject): ExecutedToolCall {
        val target = args.optString("target").trim()
        when (target.uppercase()) {
            "ALL", "SEMUA" -> {
                val count = financialDao.getAllRecordsDirect().size
                financialDao.deleteAllRecords()
                return ExecutedToolCall(
                    toolName = "delete_financial_record",
                    actionType = ToolActionType.DELETE,
                    title = "Hapus Semua Buku Kas",
                    detail = "$count catatan kas bisnis & pribadi berhasil dihapus.",
                    success = true
                )
            }
            "ALL_BUSINESS", "BISNIS" -> {
                financialDao.deleteRecordsByPrefix("BUSINESS_")
                return ExecutedToolCall(
                    toolName = "delete_financial_record",
                    actionType = ToolActionType.DELETE,
                    title = "Hapus Semua Kas Bisnis",
                    detail = "Seluruh riwayat buku kas bisnis telah dihapus.",
                    success = true
                )
            }
            "ALL_PERSONAL", "PRIBADI" -> {
                financialDao.deleteRecordsByPrefix("PERSONAL_")
                return ExecutedToolCall(
                    toolName = "delete_financial_record",
                    actionType = ToolActionType.DELETE,
                    title = "Hapus Semua Kas Pribadi",
                    detail = "Seluruh riwayat buku kas pribadi telah dihapus.",
                    success = true
                )
            }
        }

        val allRecords = financialDao.getAllRecordsDirect()
        val record = findFinancialRecord(target, allRecords)
            ?: return ExecutedToolCall("delete_financial_record", ToolActionType.DELETE, "Catatan Kas Tidak Ditemukan", "Tidak ditemukan catatan kas '$target'.", false)

        financialDao.deleteRecord(record)
        return ExecutedToolCall(
            toolName = "delete_financial_record",
            actionType = ToolActionType.DELETE,
            title = "Hapus Kas: ${record.description}",
            detail = "Catatan kas ID #${record.id} (Rp %,.0f) berhasil dihapus.".format(record.amount),
            success = true
        )
    }

    // =========================================================================
    // 6. APP SETTINGS & BUSINESS PROFILE TOOLS
    // =========================================================================

    private fun execUpdateSettings(
        args: JSONObject,
        onSettingsUpdated: (AgentSettingsUpdate) -> Unit
    ): ExecutedToolCall {
        val bizName = args.optString("business_name").trim().ifBlank { null }
        val bizAddr = args.optString("business_address").trim().ifBlank { null }
        val bizPhone = args.optString("business_phone").trim().ifBlank { null }
        val theme = args.optString("theme_mode").trim().uppercase().ifBlank { null }
        val lang = args.optString("language").trim().uppercase().ifBlank { null }
        val paperStr = args.optString("paper_size").trim().lowercase()
        val paper80mm = when {
            paperStr.contains("80") -> true
            paperStr.contains("58") -> false
            else -> null
        }

        val update = AgentSettingsUpdate(
            businessName = bizName,
            businessAddress = bizAddr,
            businessPhone = bizPhone,
            themeMode = theme,
            language = lang,
            defaultPaper80mm = paper80mm
        )
        onSettingsUpdated(update)

        val changes = listOfNotNull(
            bizName?.let { "Usaha: $it" },
            bizAddr?.let { "Alamat: $it" },
            bizPhone?.let { "Telp: $it" },
            theme?.let { "Tema: $it" },
            lang?.let { "Bahasa: $it" },
            paper80mm?.let { "Kertas: ${if (it) "80mm" else "58mm"}" }
        ).joinToString(" • ").ifBlank { "Pengaturan diperbarui" }

        return ExecutedToolCall(
            toolName = "update_app_settings",
            actionType = ToolActionType.SETTING,
            title = "Update Pengaturan Aplikasi",
            detail = changes,
            success = true
        )
    }

    // =========================================================================
    // SMART LOCAL NLU TOOL INFERRER (Fallback when offline or model skips JSON)
    // =========================================================================

    suspend fun inferLocalToolCalls(query: String): List<ParsedToolCall> {
        val q = query.trim()
        val lower = q.lowercase()
        val calls = mutableListOf<ParsedToolCall>()

        val isAdd = lower.contains("tambah") || lower.contains("buat") || lower.contains("add ") ||
                lower.contains("create ") || lower.contains("input ") || lower.contains("catat") ||
                lower.contains("masukkan") || lower.contains("titip")
        val isDelete = lower.contains("hapus") || lower.contains("delete") || lower.contains("erase") ||
                lower.contains("remove") || lower.contains("buang") || lower.contains("kosongkan") ||
                lower.contains("reset")
        val isEdit = lower.contains("ubah") || lower.contains("edit") || lower.contains("ganti") ||
                lower.contains("update") || lower.contains("set ") || lower.contains("naikkan") ||
                lower.contains("turunkan")

        if (!isAdd && !isDelete && !isEdit) return emptyList()

        val customers = customerDao.getAllCustomersDirect()
        val products = productDao.getAllProductsDirect()

        // 1. Detect Settings / Theme / Language / Business Profile
        if (lower.contains("tema") || lower.contains("theme") || lower.contains("dark mode") || lower.contains("light mode")) {
            val mode = when {
                lower.contains("terang") || lower.contains("light") -> "LIGHT"
                lower.contains("sistem") || lower.contains("system") -> "SYSTEM"
                else -> "DARK"
            }
            calls.add(ParsedToolCall("update_app_settings", JSONObject().put("theme_mode", mode)))
            return calls
        }
        if (lower.contains("bahasa") || lower.contains("language")) {
            val lang = if (lower.contains("inggris") || lower.contains("english") || lower.contains(" en")) "EN" else "ID"
            calls.add(ParsedToolCall("update_app_settings", JSONObject().put("language", lang)))
            return calls
        }
        if (lower.contains("nama usaha") || lower.contains("profil usaha") || lower.contains("header nota")) {
            val after = q.substringAfterLast("jadi ", q.substringAfterLast("ke ", "")).trim().trim('"', '\'')
            if (after.isNotBlank()) {
                calls.add(ParsedToolCall("update_app_settings", JSONObject().put("business_name", after)))
                return calls
            }
        }

        // 2. Detect Consignment Stock Operations (mentions both stok/titip AND warung/product)
        val matchedCustomer = customers.firstOrNull { lower.contains(it.name.lowercase()) }
        val matchedProduct = products.firstOrNull { lower.contains(it.name.lowercase()) }

        if ((lower.contains("stok") || lower.contains("titip")) && (matchedCustomer != null || matchedProduct != null)) {
            if (isDelete) {
                calls.add(
                    ParsedToolCall(
                        "delete_consignment_stock",
                        JSONObject().apply {
                            put("customer_target", matchedCustomer?.name ?: "ALL")
                            put("product_target", matchedProduct?.name ?: "ALL")
                        }
                    )
                )
                return calls
            } else {
                val number = extractFirstNumber(lower)?.toInt() ?: 10
                val isPack = lower.contains("pack") || lower.contains("pak") || lower.contains("bal")
                val custName = matchedCustomer?.name ?: customers.firstOrNull()?.name ?: ""
                val prodName = matchedProduct?.name ?: products.firstOrNull()?.name ?: ""
                if (custName.isNotBlank() && prodName.isNotBlank()) {
                    calls.add(
                        ParsedToolCall(
                            "set_consignment_stock",
                            JSONObject().apply {
                                put("customer_target", custName)
                                put("product_target", prodName)
                                if (isPack) put("quantity_packs", number) else put("quantity_pcs", number)
                                put("mode", if (lower.contains("tambah")) "ADD" else "SET")
                            }
                        )
                    )
                    return calls
                }
            }
        }

        // 3. Detect Financial / Cashbook Operations (pengeluaran, pemasukan, bensin, kas, biaya)
        if (lower.contains("pengeluaran") || lower.contains("pemasukan") || lower.contains("kas") ||
            lower.contains("bensin") || lower.contains("biaya") || lower.contains("expense") || lower.contains("income")
        ) {
            if (isDelete) {
                val target = when {
                    lower.contains("semua") || lower.contains("all") -> {
                        when {
                            lower.contains("pribadi") || lower.contains("personal") -> "ALL_PERSONAL"
                            lower.contains("bisnis") || lower.contains("usaha") -> "ALL_BUSINESS"
                            else -> "ALL"
                        }
                    }
                    else -> "LATEST"
                }
                calls.add(ParsedToolCall("delete_financial_record", JSONObject().put("target", target)))
                return calls
            }

            val amount = parseRupiahAmount(lower)
            if (amount != null && amount > 0) {
                if (isEdit && !isAdd) {
                    calls.add(
                        ParsedToolCall(
                            "edit_financial_record",
                            JSONObject().apply {
                                put("target", "LATEST")
                                put("new_amount", amount)
                            }
                        )
                    )
                    return calls
                }
                val isPersonal = lower.contains("pribadi") || lower.contains("personal")
                val isIncome = lower.contains("pemasukan") || lower.contains("income") || lower.contains("masuk")
                val category = when {
                    isPersonal && isIncome -> FinancialCategory.PERSONAL_INCOME
                    isPersonal -> FinancialCategory.PERSONAL_EXPENSE
                    isIncome -> FinancialCategory.BUSINESS_INCOME
                    else -> FinancialCategory.BUSINESS_EXPENSE
                }
                val desc = extractCleanDescription(q, listOf("catat", "tambah", "buat", "pengeluaran", "pemasukan", "kas", "bisnis", "pribadi", "sebesar", "senilai"))
                calls.add(
                    ParsedToolCall(
                        "add_financial_record",
                        JSONObject().apply {
                            put("category", category)
                            put("amount", amount)
                            put("description", desc.ifBlank { if (isIncome) "Pemasukan Kas" else "Biaya Operasional" })
                        }
                    )
                )
                return calls
            }
        }

        // 4. Detect Product Operations (produk, katalog, harga)
        if (lower.contains("produk") || lower.contains("product") || lower.contains("harga") || matchedProduct != null) {
            if (isDelete && (lower.contains("produk") || lower.contains("product") || matchedProduct != null)) {
                val target = when {
                    lower.contains("semua") || lower.contains("all") -> "ALL"
                    matchedProduct != null -> matchedProduct.name
                    else -> extractEntityNameAfterKeyword(q, listOf("produk", "product", "hapus", "delete", "erase"))
                }
                if (target.isNotBlank()) {
                    calls.add(ParsedToolCall("delete_product", JSONObject().put("target", target)))
                    return calls
                }
            }
            if (isEdit && matchedProduct != null) {
                val newPrice = parseRupiahAmount(lower)
                val newName = if (lower.contains("nama")) {
                    q.substringAfterLast("jadi ", q.substringAfterLast("ke ", "")).trim()
                } else ""
                calls.add(
                    ParsedToolCall(
                        "edit_product",
                        JSONObject().apply {
                            put("target", matchedProduct.name)
                            if (newPrice != null && newPrice > 0) put("selling_price_pack", newPrice)
                            if (newName.isNotBlank()) put("new_name", newName)
                        }
                    )
                )
                return calls
            }
            if (isAdd && (lower.contains("produk") || lower.contains("product"))) {
                val price = parseRupiahAmount(lower) ?: 16000.0
                val rawName = extractEntityNameAfterKeyword(q, listOf("produk baru", "produk", "product"))
                    .substringBefore(" harga")
                    .substringBefore(" jual")
                    .substringBefore(" rp")
                    .trim()
                if (rawName.isNotBlank()) {
                    calls.add(
                        ParsedToolCall(
                            "add_product",
                            JSONObject().apply {
                                put("name", rawName)
                                put("selling_price_pack", price)
                            }
                        )
                    )
                    return calls
                }
            }
        }

        // 5. Detect Customer / Warung / Toko Operations
        if (lower.contains("warung") || lower.contains("toko") || lower.contains("outlet") ||
            lower.contains("store") || lower.contains("customer") || matchedCustomer != null
        ) {
            if (isDelete) {
                val target = when {
                    lower.contains("semua") || lower.contains("all") -> "ALL"
                    matchedCustomer != null -> matchedCustomer.name
                    else -> extractEntityNameAfterKeyword(q, listOf("warung", "toko", "outlet", "store", "hapus", "delete", "erase"))
                }
                if (target.isNotBlank()) {
                    calls.add(ParsedToolCall("delete_customer", JSONObject().put("target", target)))
                    return calls
                }
            }
            if (isEdit && matchedCustomer != null) {
                val day = detectDayInText(lower)
                val newAddr = if (lower.contains("alamat")) {
                    q.substringAfter("alamat", "").trim().removePrefix(":").trim()
                } else null
                calls.add(
                    ParsedToolCall(
                        "edit_customer",
                        JSONObject().apply {
                            put("target", matchedCustomer.name)
                            if (day != null) put("route_day", day)
                            if (!newAddr.isNullOrBlank()) put("address", newAddr)
                        }
                    )
                )
                return calls
            }
            if (isAdd) {
                val day = detectDayInText(lower) ?: "Senin"
                val rawName = extractEntityNameAfterKeyword(q, listOf("warung baru", "toko baru", "outlet baru", "warung", "toko", "outlet", "store"))
                    .substringBefore(" alamat")
                    .substringBefore(" di jl")
                    .substringBefore(" hari ")
                    .substringBefore(" rute ")
                    .trim()
                val addr = if (lower.contains("alamat ")) {
                    q.substringAfter("alamat ", "").substringBefore(" hari ").trim()
                } else ""
                if (rawName.isNotBlank()) {
                    calls.add(
                        ParsedToolCall(
                            "add_customer",
                            JSONObject().apply {
                                put("name", if (rawName.lowercase().startsWith("warung") || rawName.lowercase().startsWith("toko")) rawName else "Warung $rawName")
                                put("address", addr)
                                put("route_day", day)
                            }
                        )
                    )
                    return calls
                }
            }
        }

        return calls
    }

    // =========================================================================
    // HELPER MATCHERS & BUILDERS
    // =========================================================================

    private suspend fun findCustomer(target: String): Customer? {
        if (target.isBlank()) return null
        val all = customerDao.getAllCustomersDirect()
        val id = target.removePrefix("#").toLongOrNull()
        if (id != null) {
            all.find { it.id == id }?.let { return it }
        }
        val clean = target.lowercase().trim()
        return all.find { it.name.lowercase() == clean }
            ?: all.find { it.name.lowercase().contains(clean) || clean.contains(it.name.lowercase()) }
            ?: all.find {
                val stripped = clean.removePrefix("warung").removePrefix("toko").trim()
                stripped.isNotBlank() && it.name.lowercase().contains(stripped)
            }
    }

    private suspend fun findProduct(target: String): Product? {
        if (target.isBlank()) return null
        val all = productDao.getAllProductsDirect()
        val id = target.removePrefix("#").toLongOrNull()
        if (id != null) {
            all.find { it.id == id }?.let { return it }
        }
        val clean = target.lowercase().trim()
        return all.find { it.name.lowercase() == clean }
            ?: all.find { it.name.lowercase().contains(clean) || clean.contains(it.name.lowercase()) }
    }

    private fun findFinancialRecord(target: String, records: List<FinancialRecord>): FinancialRecord? {
        if (records.isEmpty()) return null
        if (target.isBlank() || target.equals("LATEST", ignoreCase = true) || target.equals("TERAKHIR", ignoreCase = true)) {
            return records.firstOrNull()
        }
        val id = target.removePrefix("#").toLongOrNull()
        if (id != null) {
            records.find { it.id == id }?.let { return it }
        }
        val clean = target.lowercase().trim()
        return records.find { it.description.lowercase().contains(clean) } ?: records.firstOrNull()
    }

    private fun normalizeDay(raw: String): String {
        val lower = raw.lowercase().trim()
        return when {
            lower.contains("senin") || lower.contains("mon") -> "Senin"
            lower.contains("selasa") || lower.contains("tue") -> "Selasa"
            lower.contains("rabu") || lower.contains("wed") -> "Rabu"
            lower.contains("kamis") || lower.contains("thu") -> "Kamis"
            lower.contains("jumat") || lower.contains("jum'at") || lower.contains("fri") -> "Jumat"
            lower.contains("sabtu") || lower.contains("sat") -> "Sabtu"
            lower.contains("minggu") || lower.contains("ahad") || lower.contains("sun") -> "Minggu"
            else -> "Senin"
        }
    }

    private fun detectDayInText(lower: String): String? = when {
        lower.contains("senin") -> "Senin"
        lower.contains("selasa") -> "Selasa"
        lower.contains("rabu") -> "Rabu"
        lower.contains("kamis") -> "Kamis"
        lower.contains("jumat") -> "Jumat"
        lower.contains("sabtu") -> "Sabtu"
        lower.contains("minggu") -> "Minggu"
        else -> null
    }

    private fun parseRupiahAmount(text: String): Double? {
        // Match patterns like "25rb", "25 ribu", "25.000", "25000", "1.5jt"
        val rbRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:rb|ribu|k)\\b", RegexOption.IGNORE_CASE)
        rbRegex.find(text)?.let { m ->
            val v = m.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (v != null) return v * 1000.0
        }
        val jtRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:jt|juta)\\b", RegexOption.IGNORE_CASE)
        jtRegex.find(text)?.let { m ->
            val v = m.groupValues[1].replace(",", ".").toDoubleOrNull()
            if (v != null) return v * 1_000_000.0
        }
        val plainRegex = Regex("(?:rp\\.?\\s*)?(\\d{1,3}(?:[.,]\\d{3})+|[1-9]\\d{2,})", RegexOption.IGNORE_CASE)
        plainRegex.find(text)?.let { m ->
            val clean = m.groupValues[1].replace(".", "").replace(",", "")
            return clean.toDoubleOrNull()
        }
        return null
    }

    private fun extractFirstNumber(text: String): Double? {
        val m = Regex("(\\d+)").find(text) ?: return null
        return m.groupValues[1].toDoubleOrNull()
    }

    private fun extractEntityNameAfterKeyword(query: String, keywords: List<String>): String {
        val lower = query.lowercase()
        for (kw in keywords) {
            val idx = lower.indexOf(kw)
            if (idx >= 0) {
                val after = query.substring(idx + kw.length).trim().trim(':', '"', '\'')
                if (after.isNotBlank()) return after
            }
        }
        return ""
    }

    private fun extractCleanDescription(query: String, stopWords: List<String>): String {
        var working = query
        stopWords.forEach { w ->
            working = working.replace(Regex("\\b$w\\b", RegexOption.IGNORE_CASE), "")
        }
        working = working
            .replace(Regex("(?:rp\\.?\\s*)?\\d+(?:[.,]\\d+)*\\s*(?:rb|ribu|k|jt|juta)?", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return working.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun prop(type: String, description: String): JSONObject =
        JSONObject().apply {
            put("type", type)
            put("description", description)
        }

    private fun fnDecl(
        name: String,
        description: String,
        props: Map<String, JSONObject>,
        required: List<String>
    ): JSONObject = JSONObject().apply {
        put("name", name)
        put("description", description)
        put("parameters", JSONObject().apply {
            put("type", "OBJECT")
            put("properties", JSONObject().apply {
                props.forEach { (k, v) -> put(k, v) }
            })
            if (required.isNotEmpty()) {
                put("required", JSONArray().apply {
                    required.forEach { put(it) }
                })
            }
        })
    }
}
