package com.example

import com.example.data.local.entity.Product
import com.example.domain.model.ReceiptData
import com.example.domain.model.ReceiptItemData
import com.example.domain.model.ReconciliationItem
import com.example.util.thermal.EscPosHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsignmentBusinessLogicTest {

    @Test
    fun testReconciliationCalculations() {
        val product = Product(
            id = 1L,
            name = "Kerupuk Kaleng Putih",
            unit = "Bungkus",
            pieces_per_pack = 10,
            cost_price_pack = 12000.0,
            selling_price_pack = 20000.0
        )

        // Previous stock = 30, Remaining checked = 10, Auto swap returned = true, Added packs = 2 (20 pcs)
        val item = ReconciliationItem(
            product = product,
            previousStock = 30,
            remainingStock = 10,
            isAutoSwapReturned = true,
            addedPacks = 2
        )

        // Sold should be: 30 - 10 = 20
        assertEquals(20, item.soldQuantity)

        // Subtotal should be: 20 * 2000 = 40,000
        assertEquals(40000.0, item.subtotal, 0.01)

        // Cost total: 20 * 1200 = 24,000
        assertEquals(24000.0, item.costTotal, 0.01)

        // Final stock left at store: 0 (since auto swap pulled 10) + 20 added = 20
        assertEquals(20, item.finalStock)
    }

    @Test
    fun testEscPosReceiptGeneration() {
        val receipt = ReceiptData(
            headerId = 101,
            businessName = "CONSIGNTRACK DISTRIBUSI",
            businessSub = "Titip Jual Kerupuk",
            businessPhone = "0812-9988-7766",
            transactionDate = System.currentTimeMillis(),
            customerName = "Warung Bu Sri",
            customerAddress = "Pasar Minggu",
            items = listOf(
                ReceiptItemData(
                    productName = "Kerupuk Kaleng Putih",
                    unit = "Bungkus",
                    prevStock = 30,
                    remStock = 10,
                    returStock = 2,
                    soldQty = 18,
                    unitPrice = 2000.0,
                    subtotal = 36000.0,
                    addedQty = 20,
                    newTotalStock = 30
                )
            ),
            totalSoldQuantity = 18,
            totalAmount = 36000.0,
            amountPaid = 50000.0,
            changeOrDebt = 14000.0,
            notes = "Titipan baru rapi di toples"
        )

        val formattedText = EscPosHelper.generateMonospaceReceipt(receipt, is80mm = false)
        assertTrue(formattedText.contains("Warung Bu Sri"))
        assertTrue(formattedText.contains("36,000"))
        assertTrue(formattedText.contains("Kembalian"))

        val bytes = EscPosHelper.buildEscPosBytes(receipt, is80mm = false)
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testCustomerRouteDayAndPhoto() {
        val customer = com.example.data.local.entity.Customer(
            id = 5L,
            name = "Warung Bu Sri",
            address = "Pasar Minggu",
            phone = "0812-3456-7890",
            route_day = "Senin",
            route_order = 1,
            photo_uri = "file:///data/user/0/com.example/files/photos/photo_test.jpg"
        )

        assertEquals("Senin", customer.route_day)
        assertEquals(1, customer.route_order)
        assertTrue(customer.photo_uri!!.contains("photo_test.jpg"))
    }
}
