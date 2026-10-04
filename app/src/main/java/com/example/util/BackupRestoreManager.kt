package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ConsignmentStock
import com.example.data.local.entity.Customer
import com.example.data.local.entity.FinancialRecord
import com.example.data.local.entity.Product
import com.example.data.local.entity.TransactionDetail
import com.example.data.local.entity.TransactionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupStats(
    val customerCount: Int = 0,
    val productCount: Int = 0,
    val stockCount: Int = 0,
    val transactionCount: Int = 0,
    val financialCount: Int = 0,
    val photoCount: Int = 0
)

data class BackupOperationResult(
    val success: Boolean,
    val message: String,
    val stats: BackupStats = BackupStats(),
    val shareFile: File? = null
)

object BackupRestoreManager {

    private const val BACKUP_JSON_NAME = "backup_data.json"
    private const val PHOTOS_FOLDER_PREFIX = "photos/"
    private const val PREFS_NAME = "consigntrack_settings"

    fun generateDefaultBackupFileName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        return "ConsignTrack_Backup_${sdf.format(Date())}.zip"
    }

    suspend fun getCurrentDatabaseStats(context: Context, db: AppDatabase): BackupStats =
        withContext(Dispatchers.IO) {
            val customers = db.customerDao().getAllCustomersDirect()
            val products = db.productDao().getAllProductsDirect()
            val stocks = db.consignmentStockDao().getAllStocksDirect()
            val headers = db.transactionDao().getAllHeadersDirect()
            val financials = db.financialRecordDao().getAllRecordsDirect()

            val photosDir = File(context.filesDir, "photos")
            val existingPhotoFiles = photosDir.listFiles()?.filter { it.isFile && it.length() > 0 } ?: emptyList()

            val referencedNames = mutableSetOf<String>()
            existingPhotoFiles.forEach { referencedNames.add(it.name) }
            customers.mapNotNull { extractFileNameFromUri(it.photo_uri) }.forEach { referencedNames.add(it) }
            headers.mapNotNull { extractFileNameFromUri(it.photo_uri) }.forEach { referencedNames.add(it) }

            BackupStats(
                customerCount = customers.size,
                productCount = products.size,
                stockCount = stocks.size,
                transactionCount = headers.size,
                financialCount = financials.size,
                photoCount = existingPhotoFiles.size.coerceAtLeast(
                    referencedNames.count { name -> File(photosDir, name).exists() }
                )
            )
        }

    /**
     * Exports the entire database, business profile settings, and all warung/visit photos into a single .zip Uri.
     */
    suspend fun exportBackupToUri(
        context: Context,
        db: AppDatabase,
        targetUri: Uri
    ): BackupOperationResult = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(targetUri)
                ?: return@withContext BackupOperationResult(
                    success = false,
                    message = "Gagal membuka lokasi penyimpanan file ZIP."
                )

            val stats = outputStream.use { out ->
                writeFullBackupZip(context, db, out)
            }

            BackupOperationResult(
                success = true,
                message = "Backup ZIP berhasil disimpan (${stats.customerCount} Warung, ${stats.productCount} Produk, ${stats.transactionCount} Transaksi, ${stats.photoCount} Foto).",
                stats = stats
            )
        } catch (e: Exception) {
            BackupOperationResult(
                success = false,
                message = "Gagal mengekspor backup ZIP: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Creates a temporary .zip file in cacheDir/backups/ and returns a Share Intent via FileProvider.
     */
    suspend fun createShareableBackupZip(
        context: Context,
        db: AppDatabase
    ): BackupOperationResult = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            // Clean old temp backup files
            backupDir.listFiles()?.forEach { runCatching { it.delete() } }

            val zipFile = File(backupDir, generateDefaultBackupFileName())
            val stats = FileOutputStream(zipFile).use { fos ->
                writeFullBackupZip(context, db, fos)
            }

            BackupOperationResult(
                success = true,
                message = "File Backup ZIP siap dibagikan (${stats.customerCount} Warung, ${stats.photoCount} Foto).",
                stats = stats,
                shareFile = zipFile
            )
        } catch (e: Exception) {
            BackupOperationResult(
                success = false,
                message = "Gagal membuat file Share ZIP: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    fun launchShareBackupIntent(context: Context, zipFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, zipFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, zipFile.nameWithoutExtension)
            putExtra(
                Intent.EXTRA_TEXT,
                "File Backup Data & Foto ConsignTrack (${zipFile.name})"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Bagikan File Backup ZIP").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Core routine that packs `backup_data.json` + all `photos/` files into [rawOutputStream].
     */
    private suspend fun writeFullBackupZip(
        context: Context,
        db: AppDatabase,
        rawOutputStream: OutputStream
    ): BackupStats {
        val customers = db.customerDao().getAllCustomersDirect()
        val products = db.productDao().getAllProductsDirect()
        val stocks = db.consignmentStockDao().getAllStocksDirect()
        val headers = db.transactionDao().getAllHeadersDirect()
        val details = db.transactionDao().getAllDetailsDirect()
        val financials = db.financialRecordDao().getAllRecordsDirect()

        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()

        // Map of filename inside `photos/` -> ByteArray or File to pack into ZIP
        val photoFilesToPack = linkedMapOf<String, File>()
        val extraPhotoBytesToPack = linkedMapOf<String, ByteArray>()

        // 1. Include all existing files in filesDir/photos
        photosDir.listFiles()?.forEach { file ->
            if (file.isFile && file.length() > 0) {
                photoFilesToPack[file.name] = file
            }
        }

        // Helper to resolve a photo URI from Customer or TransactionHeader into a filename inside `photos/`
        fun registerPhotoUri(uriString: String?, fallbackPrefix: String): String? {
            if (uriString.isNullOrBlank()) return null
            return try {
                val uri = Uri.parse(uriString)
                val pathSegment = uri.lastPathSegment ?: File(uri.path ?: "").name
                val cleanName = pathSegment.substringAfterLast('/').trim()

                // Case A: File inside filesDir/photos
                if (cleanName.isNotBlank()) {
                    val directFile = File(photosDir, cleanName)
                    if (directFile.exists() && directFile.isFile && directFile.length() > 0) {
                        photoFilesToPack[directFile.name] = directFile
                        return directFile.name
                    }
                }

                // Case B: Direct file:// path elsewhere on disk
                if (uri.scheme == "file" || uriString.startsWith("/")) {
                    val fileObj = File(uri.path ?: uriString)
                    if (fileObj.exists() && fileObj.isFile && fileObj.length() > 0) {
                        val entryName = fileObj.name.ifBlank { "${fallbackPrefix}_${System.currentTimeMillis()}.jpg" }
                        photoFilesToPack[entryName] = fileObj
                        return entryName
                    }
                }

                // Case C: content:// URI — read bytes via ContentResolver
                if (uri.scheme == "content") {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null && bytes.isNotEmpty()) {
                        val entryName = if (cleanName.endsWith(".jpg", true) || cleanName.endsWith(".png", true)) {
                            cleanName
                        } else {
                            "${fallbackPrefix}_${System.currentTimeMillis()}.jpg"
                        }
                        extraPhotoBytesToPack[entryName] = bytes
                        return entryName
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }

        // Build JSON payload
        val rootJson = JSONObject()

        // Metadata
        val metadataJson = JSONObject().apply {
            put("app", "ConsignTrack")
            put("backup_version", 1)
            put("created_at", System.currentTimeMillis())
            put(
                "created_at_formatted",
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
        }
        rootJson.put("metadata", metadataJson)

        // App & Business Profile Settings
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val settingsJson = JSONObject().apply {
            put("business_name", prefs.getString("business_name", "CONSIGNTRACK DISTRIBUSI") ?: "CONSIGNTRACK DISTRIBUSI")
            put("business_address", prefs.getString("business_address", "Sentra Makanan Ringan") ?: "Sentra Makanan Ringan")
            put("business_phone", prefs.getString("business_phone", "0812-9988-7766") ?: "0812-9988-7766")
            put("theme_mode", prefs.getString("theme_mode", "DARK") ?: "DARK")
            put("language", prefs.getString("language", "ID") ?: "ID")
            put("show_floating_ai", prefs.getBoolean("show_floating_ai", true))
            put("agent_mode_enabled", prefs.getBoolean("agent_mode_enabled", true))
            put("default_paper_80mm", prefs.getBoolean("default_paper_80mm", false))
            put("auto_sort_gps", prefs.getBoolean("auto_sort_gps", true))
            put("ai_base_url", prefs.getString("ai_base_url", com.example.data.remote.OpenAiClient.DEFAULT_BASE_URL) ?: com.example.data.remote.OpenAiClient.DEFAULT_BASE_URL)
            put("ai_api_key", prefs.getString("ai_api_key", com.example.data.remote.OpenAiClient.DEFAULT_API_KEY) ?: com.example.data.remote.OpenAiClient.DEFAULT_API_KEY)
            put("ai_model_name", prefs.getString("ai_model_name", com.example.data.remote.OpenAiClient.DEFAULT_MODEL) ?: com.example.data.remote.OpenAiClient.DEFAULT_MODEL)
            put("gemini_api_key", prefs.getString("gemini_api_key", "") ?: "")
        }
        rootJson.put("settings", settingsJson)

        // 1. Customers
        val customersArray = JSONArray()
        customers.forEach { c ->
            val photoFileName = registerPhotoUri(c.photo_uri, "warung_${c.id}")
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("address", c.address)
                put("phone", c.phone)
                put("route_day", c.route_day)
                put("route_order", c.route_order)
                put("photo_uri", c.photo_uri ?: JSONObject.NULL)
                put("photo_file", photoFileName ?: JSONObject.NULL)
                put("latitude", c.latitude ?: JSONObject.NULL)
                put("longitude", c.longitude ?: JSONObject.NULL)
                put("created_at", c.created_at)
            }
            customersArray.put(obj)
        }
        rootJson.put("customers", customersArray)

        // 2. Products
        val productsArray = JSONArray()
        products.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("unit", p.unit)
                put("unit_small", p.unit_small)
                put("unit_big", p.unit_big)
                put("pieces_per_pack", p.pieces_per_pack)
                put("cost_price_pack", p.cost_price_pack)
                put("selling_price_pack", p.selling_price_pack)
                put("cost_price", p.cost_price)
                put("selling_price", p.selling_price)
            }
            productsArray.put(obj)
        }
        rootJson.put("products", productsArray)

        // 3. Consignment Stocks
        val stocksArray = JSONArray()
        stocks.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("customer_id", s.customer_id)
                put("product_id", s.product_id)
                put("current_quantity", s.current_quantity)
                put("custom_price_pack", s.custom_price_pack ?: JSONObject.NULL)
                put("last_updated", s.last_updated)
            }
            stocksArray.put(obj)
        }
        rootJson.put("consignment_stocks", stocksArray)

        // 4. Transaction Headers
        val headersArray = JSONArray()
        headers.forEach { h ->
            val photoFileName = registerPhotoUri(h.photo_uri, "visit_${h.id}")
            val obj = JSONObject().apply {
                put("id", h.id)
                put("customer_id", h.customer_id)
                put("transaction_date", h.transaction_date)
                put("total_sold_amount", h.total_sold_amount)
                put("amount_paid", h.amount_paid)
                put("notes", h.notes ?: JSONObject.NULL)
                put("photo_uri", h.photo_uri ?: JSONObject.NULL)
                put("photo_file", photoFileName ?: JSONObject.NULL)
            }
            headersArray.put(obj)
        }
        rootJson.put("transaction_headers", headersArray)

        // 5. Transaction Details
        val detailsArray = JSONArray()
        details.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("transaction_id", d.transaction_id)
                put("product_id", d.product_id)
                put("previous_stock", d.previous_stock)
                put("remaining_stock", d.remaining_stock)
                put("sold_quantity", d.sold_quantity)
                put("returned_quantity", d.returned_quantity)
                put("added_quantity", d.added_quantity)
                put("unit_price", d.unit_price)
            }
            detailsArray.put(obj)
        }
        rootJson.put("transaction_details", detailsArray)

        // 6. Financial Records
        val financialsArray = JSONArray()
        financials.forEach { f ->
            val obj = JSONObject().apply {
                put("id", f.id)
                put("category", f.category)
                put("amount", f.amount)
                put("description", f.description)
                put("transaction_date", f.transaction_date)
            }
            financialsArray.put(obj)
        }
        rootJson.put("financial_records", financialsArray)

        val totalPhotos = photoFilesToPack.size + extraPhotoBytesToPack.size
        metadataJson.put("customer_count", customers.size)
        metadataJson.put("product_count", products.size)
        metadataJson.put("stock_count", stocks.size)
        metadataJson.put("transaction_count", headers.size)
        metadataJson.put("financial_count", financials.size)
        metadataJson.put("photo_count", totalPhotos)

        // Write ZIP Archive
        ZipOutputStream(BufferedOutputStream(rawOutputStream)).use { zos ->
            // Entry 1: backup_data.json
            val jsonBytes = rootJson.toString(2).toByteArray(Charsets.UTF_8)
            val jsonEntry = ZipEntry(BACKUP_JSON_NAME)
            zos.putNextEntry(jsonEntry)
            zos.write(jsonBytes)
            zos.closeEntry()

            // Entry 2..N: photos/<filename>
            photoFilesToPack.forEach { (fileName, file) ->
                if (file.exists() && file.isFile) {
                    val entry = ZipEntry("$PHOTOS_FOLDER_PREFIX$fileName")
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }

            extraPhotoBytesToPack.forEach { (fileName, bytes) ->
                if (!photoFilesToPack.containsKey(fileName)) {
                    val entry = ZipEntry("$PHOTOS_FOLDER_PREFIX$fileName")
                    zos.putNextEntry(entry)
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
        }

        return BackupStats(
            customerCount = customers.size,
            productCount = products.size,
            stockCount = stocks.size,
            transactionCount = headers.size,
            financialCount = financials.size,
            photoCount = totalPhotos
        )
    }

    /**
     * Imports & restores database entities, photos, and business profile from a backup .zip (or .json) Uri.
     */
    suspend fun importBackupFromUri(
        context: Context,
        db: AppDatabase,
        sourceUri: Uri,
        replaceExisting: Boolean = true
    ): BackupOperationResult = withContext(Dispatchers.IO) {
        try {
            val photosDir = File(context.filesDir, "photos")
            if (!photosDir.exists()) photosDir.mkdirs()

            var backupJsonString: String? = null
            var restoredPhotoCount = 0

            // First pass: try reading as a ZIP stream
            context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val currentEntry = entry
                        if (!currentEntry.isDirectory) {
                            val entryName = currentEntry.name.replace('\\', '/')
                            val safeFileName = File(entryName).name

                            if (entryName == BACKUP_JSON_NAME || safeFileName.equals(BACKUP_JSON_NAME, ignoreCase = true) || safeFileName.endsWith(".json", ignoreCase = true)) {
                                val baos = ByteArrayOutputStream()
                                zis.copyTo(baos)
                                backupJsonString = baos.toString(Charsets.UTF_8.name())
                            } else if (
                                entryName.startsWith(PHOTOS_FOLDER_PREFIX, ignoreCase = true) ||
                                safeFileName.endsWith(".jpg", ignoreCase = true) ||
                                safeFileName.endsWith(".jpeg", ignoreCase = true) ||
                                safeFileName.endsWith(".png", ignoreCase = true) ||
                                safeFileName.endsWith(".webp", ignoreCase = true)
                            ) {
                                if (safeFileName.isNotBlank()) {
                                    val targetPhotoFile = File(photosDir, safeFileName)
                                    FileOutputStream(targetPhotoFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                    restoredPhotoCount++
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // Fallback: if user picked a raw .json backup file directly instead of .zip
            if (backupJsonString.isNullOrBlank()) {
                val rawText = context.contentResolver.openInputStream(sourceUri)?.use {
                    it.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!rawText.isNullOrBlank() && rawText.trimStart().startsWith("{")) {
                    backupJsonString = rawText
                }
            }

            val jsonText = backupJsonString
                ?: return@withContext BackupOperationResult(
                    success = false,
                    message = "File ZIP tidak valid atau tidak berisi data backup ConsignTrack (backup_data.json)."
                )

            val rootJson = JSONObject(jsonText)

            // Restore Settings / Business Profile if present
            rootJson.optJSONObject("settings")?.let { s ->
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().apply {
                    if (s.has("business_name")) putString("business_name", s.optString("business_name", "CONSIGNTRACK DISTRIBUSI"))
                    if (s.has("business_address")) putString("business_address", s.optString("business_address", "Sentra Makanan Ringan"))
                    if (s.has("business_phone")) putString("business_phone", s.optString("business_phone", "0812-9988-7766"))
                    if (s.has("theme_mode")) putString("theme_mode", s.optString("theme_mode", "DARK"))
                    if (s.has("language")) putString("language", s.optString("language", "ID"))
                    if (s.has("show_floating_ai")) putBoolean("show_floating_ai", s.optBoolean("show_floating_ai", true))
                    if (s.has("agent_mode_enabled")) putBoolean("agent_mode_enabled", s.optBoolean("agent_mode_enabled", true))
                    if (s.has("default_paper_80mm")) putBoolean("default_paper_80mm", s.optBoolean("default_paper_80mm", false))
                    if (s.has("auto_sort_gps")) putBoolean("auto_sort_gps", s.optBoolean("auto_sort_gps", true))
                    if (s.has("ai_base_url")) putString("ai_base_url", s.optString("ai_base_url", com.example.data.remote.OpenAiClient.DEFAULT_BASE_URL))
                    if (s.has("ai_api_key")) putString("ai_api_key", s.optString("ai_api_key", com.example.data.remote.OpenAiClient.DEFAULT_API_KEY))
                    if (s.has("ai_model_name")) putString("ai_model_name", s.optString("ai_model_name", com.example.data.remote.OpenAiClient.DEFAULT_MODEL))
                    if (s.has("gemini_api_key")) putString("gemini_api_key", s.optString("gemini_api_key", ""))
                }.apply()
                com.example.data.remote.OpenAiClient.syncFromPreferences(context)
            }

            // Helper to resolve restored local photo URI on this device
            fun resolveRestoredPhotoUri(photoFileField: String?, originalPhotoUri: String?): String? {
                val candidateName = photoFileField?.takeIf { it.isNotBlank() && it != "null" }
                    ?: extractFileNameFromUri(originalPhotoUri)
                if (!candidateName.isNullOrBlank()) {
                    val localFile = File(photosDir, candidateName)
                    if (localFile.exists() && localFile.length() > 0) {
                        return Uri.fromFile(localFile).toString()
                    }
                }
                return originalPhotoUri?.takeIf { it.isNotBlank() && it != "null" }
            }

            // Parse Customers
            val customersList = mutableListOf<Customer>()
            val customersArray = rootJson.optJSONArray("customers") ?: JSONArray()
            for (i in 0 until customersArray.length()) {
                val c = customersArray.optJSONObject(i) ?: continue
                val photoFile = if (c.isNull("photo_file")) null else c.optString("photo_file")
                val origUri = if (c.isNull("photo_uri")) null else c.optString("photo_uri")
                val resolvedPhotoUri = resolveRestoredPhotoUri(photoFile, origUri)

                val lat = if (c.has("latitude") && !c.isNull("latitude")) c.optDouble("latitude") else null
                val lng = if (c.has("longitude") && !c.isNull("longitude")) c.optDouble("longitude") else null

                customersList.add(
                    Customer(
                        id = c.optLong("id", 0L),
                        name = c.optString("name", "Warung"),
                        address = c.optString("address", ""),
                        phone = c.optString("phone", ""),
                        route_day = c.optString("route_day", "Senin"),
                        route_order = c.optInt("route_order", 1),
                        photo_uri = resolvedPhotoUri,
                        latitude = lat?.takeIf { !it.isNaN() },
                        longitude = lng?.takeIf { !it.isNaN() },
                        created_at = c.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }

            // Parse Products
            val productsList = mutableListOf<Product>()
            val productsArray = rootJson.optJSONArray("products") ?: JSONArray()
            for (i in 0 until productsArray.length()) {
                val p = productsArray.optJSONObject(i) ?: continue
                val pcsPerPack = p.optInt("pieces_per_pack", 10).coerceAtLeast(1)
                val costPack = p.optDouble("cost_price_pack", 11500.0)
                val sellPack = p.optDouble("selling_price_pack", 16000.0)
                val costUnit = p.optDouble("cost_price", costPack / pcsPerPack)
                val sellUnit = p.optDouble("selling_price", sellPack / pcsPerPack)

                productsList.add(
                    Product(
                        id = p.optLong("id", 0L),
                        name = p.optString("name", "Produk"),
                        unit = p.optString("unit", "Pcs"),
                        unit_small = p.optString("unit_small", "Pcs"),
                        unit_big = p.optString("unit_big", "Pack"),
                        pieces_per_pack = pcsPerPack,
                        cost_price_pack = costPack,
                        selling_price_pack = sellPack,
                        cost_price = costUnit,
                        selling_price = sellUnit
                    )
                )
            }

            // Parse Consignment Stocks
            val stocksList = mutableListOf<ConsignmentStock>()
            val stocksArray = rootJson.optJSONArray("consignment_stocks") ?: JSONArray()
            for (i in 0 until stocksArray.length()) {
                val s = stocksArray.optJSONObject(i) ?: continue
                val customPrice = if (s.has("custom_price_pack") && !s.isNull("custom_price_pack")) {
                    s.optDouble("custom_price_pack").takeIf { !it.isNaN() && it > 0.0 }
                } else null

                stocksList.add(
                    ConsignmentStock(
                        id = s.optLong("id", 0L),
                        customer_id = s.optLong("customer_id", 0L),
                        product_id = s.optLong("product_id", 0L),
                        current_quantity = s.optInt("current_quantity", 0),
                        custom_price_pack = customPrice,
                        last_updated = s.optLong("last_updated", System.currentTimeMillis())
                    )
                )
            }

            // Parse Transaction Headers
            val headersList = mutableListOf<TransactionHeader>()
            val headersArray = rootJson.optJSONArray("transaction_headers") ?: JSONArray()
            for (i in 0 until headersArray.length()) {
                val h = headersArray.optJSONObject(i) ?: continue
                val photoFile = if (h.isNull("photo_file")) null else h.optString("photo_file")
                val origUri = if (h.isNull("photo_uri")) null else h.optString("photo_uri")
                val resolvedPhotoUri = resolveRestoredPhotoUri(photoFile, origUri)
                val notes = if (h.isNull("notes")) null else h.optString("notes")

                headersList.add(
                    TransactionHeader(
                        id = h.optLong("id", 0L),
                        customer_id = h.optLong("customer_id", 0L),
                        transaction_date = h.optLong("transaction_date", System.currentTimeMillis()),
                        total_sold_amount = h.optDouble("total_sold_amount", 0.0),
                        amount_paid = h.optDouble("amount_paid", 0.0),
                        notes = notes,
                        photo_uri = resolvedPhotoUri
                    )
                )
            }

            // Parse Transaction Details
            val detailsList = mutableListOf<TransactionDetail>()
            val detailsArray = rootJson.optJSONArray("transaction_details") ?: JSONArray()
            for (i in 0 until detailsArray.length()) {
                val d = detailsArray.optJSONObject(i) ?: continue
                detailsList.add(
                    TransactionDetail(
                        id = d.optLong("id", 0L),
                        transaction_id = d.optLong("transaction_id", 0L),
                        product_id = d.optLong("product_id", 0L),
                        previous_stock = d.optInt("previous_stock", 0),
                        remaining_stock = d.optInt("remaining_stock", 0),
                        sold_quantity = d.optInt("sold_quantity", 0),
                        returned_quantity = d.optInt("returned_quantity", 0),
                        added_quantity = d.optInt("added_quantity", 0),
                        unit_price = d.optDouble("unit_price", 0.0)
                    )
                )
            }

            // Parse Financial Records
            val financialsList = mutableListOf<FinancialRecord>()
            val financialsArray = rootJson.optJSONArray("financial_records") ?: JSONArray()
            for (i in 0 until financialsArray.length()) {
                val f = financialsArray.optJSONObject(i) ?: continue
                financialsList.add(
                    FinancialRecord(
                        id = f.optLong("id", 0L),
                        category = f.optString("category", "BUSINESS_EXPENSE"),
                        amount = f.optDouble("amount", 0.0),
                        description = f.optString("description", ""),
                        transaction_date = f.optLong("transaction_date", System.currentTimeMillis())
                    )
                )
            }

            // Execute database restore atomically inside a transaction
            db.withTransaction {
                if (replaceExisting) {
                    db.transactionDao().deleteAllDetails()
                    db.transactionDao().deleteAllHeaders()
                    db.consignmentStockDao().deleteAllStocks()
                    db.financialRecordDao().deleteAllRecords()
                    db.customerDao().deleteAllCustomers()
                    db.productDao().deleteAllProducts()
                }

                // 1. Insert parent tables (Customers & Products)
                if (customersList.isNotEmpty()) {
                    db.customerDao().insertCustomers(customersList)
                }
                if (productsList.isNotEmpty()) {
                    db.productDao().insertProducts(productsList)
                }

                val validCustomerIds = db.customerDao().getAllCustomersDirect().map { it.id }.toSet()
                val validProductIds = db.productDao().getAllProductsDirect().map { it.id }.toSet()

                // 2. Insert Consignment Stocks with valid foreign keys
                val validStocks = stocksList.filter {
                    it.customer_id in validCustomerIds && it.product_id in validProductIds
                }
                if (validStocks.isNotEmpty()) {
                    db.consignmentStockDao().insertOrUpdateStocks(validStocks)
                }

                // 3. Insert Transaction Headers with valid customer foreign keys
                val validHeaders = headersList.filter { it.customer_id in validCustomerIds }
                validHeaders.forEach { header ->
                    db.transactionDao().insertHeader(header)
                }

                val validHeaderIds = db.transactionDao().getAllHeadersDirect().map { it.id }.toSet()

                // 4. Insert Transaction Details with valid header & product foreign keys
                val validDetails = detailsList.filter {
                    it.transaction_id in validHeaderIds && it.product_id in validProductIds
                }
                if (validDetails.isNotEmpty()) {
                    db.transactionDao().insertDetails(validDetails)
                }

                // 5. Insert Financial Records
                financialsList.forEach { record ->
                    db.financialRecordDao().insertRecord(record)
                }
            }

            val stats = BackupStats(
                customerCount = customersList.size,
                productCount = productsList.size,
                stockCount = stocksList.size,
                transactionCount = headersList.size,
                financialCount = financialsList.size,
                photoCount = restoredPhotoCount
            )

            BackupOperationResult(
                success = true,
                message = "Data berhasil dipulihkan: ${stats.customerCount} Warung, ${stats.productCount} Produk, ${stats.stockCount} Stok, ${stats.transactionCount} Transaksi, ${stats.financialCount} Kas, dan ${stats.photoCount} Foto.",
                stats = stats
            )
        } catch (e: Exception) {
            BackupOperationResult(
                success = false,
                message = "Gagal mengimpor backup ZIP: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Completely clears all database tables and local warung/visit photos for a clean preproduction slate.
     */
    suspend fun clearAllDataAndPhotos(
        context: Context,
        db: AppDatabase
    ): BackupOperationResult = withContext(Dispatchers.IO) {
        try {
            db.withTransaction {
                db.transactionDao().deleteAllDetails()
                db.transactionDao().deleteAllHeaders()
                db.consignmentStockDao().deleteAllStocks()
                db.financialRecordDao().deleteAllRecords()
                db.customerDao().deleteAllCustomers()
                db.productDao().deleteAllProducts()
            }

            val photosDir = File(context.filesDir, "photos")
            photosDir.listFiles()?.forEach { file ->
                runCatching { file.delete() }
            }

            BackupOperationResult(
                success = true,
                message = "Seluruh data & foto telah dihapus bersih (Pre-Production Ready).",
                stats = BackupStats()
            )
        } catch (e: Exception) {
            BackupOperationResult(
                success = false,
                message = "Gagal menghapus data: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    private fun extractFileNameFromUri(uriString: String?): String? {
        if (uriString.isNullOrBlank() || uriString == "null") return null
        return try {
            val uri = Uri.parse(uriString)
            val name = (uri.lastPathSegment ?: File(uri.path ?: "").name).substringAfterLast('/')
            name.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
