package com.example.util.thermal

import com.example.domain.model.ReceiptData
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EscPosHelper {

    // ESC/POS Commands
    private val ESC: Byte = 0x1B
    private val GS: Byte = 0x1D
    private val LF: Byte = 0x0A

    private val CMD_INIT = byteArrayOf(ESC, 0x40)
    private val CMD_ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
    private val CMD_ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
    private val CMD_ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02)
    private val CMD_BOLD_ON = byteArrayOf(ESC, 0x45, 0x01)
    private val CMD_BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)
    private val CMD_DOUBLE_SIZE = byteArrayOf(GS, 0x21, 0x11)
    private val CMD_NORMAL_SIZE = byteArrayOf(GS, 0x21, 0x00)
    private val CMD_FEED_CUT = byteArrayOf(LF, LF, LF, GS, 0x56, 0x42, 0x00)

    fun generateMonospaceReceipt(receipt: ReceiptData, is80mm: Boolean = false): String {
        val colWidth = if (is80mm) 48 else 32
        val divider = "-".repeat(colWidth)
        val doubleDivider = "=".repeat(colWidth)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        sb.append(centerText(receipt.businessName, colWidth)).append("\n")
        sb.append(centerText(receipt.businessSub, colWidth)).append("\n")
        sb.append(centerText("Telp: ${receipt.businessPhone}", colWidth)).append("\n")
        sb.append(doubleDivider).append("\n")

        sb.append("Nota : #TRX-${receipt.headerId}\n")
        sb.append("Tgl  : ${dateFormat.format(Date(receipt.transactionDate))}\n")
        sb.append("Toko : ${receipt.customerName}\n")
        if (receipt.customerAddress.isNotBlank()) {
            sb.append("Almt : ${receipt.customerAddress}\n")
        }
        sb.append(divider).append("\n")

        // Items table
        if (is80mm) {
            sb.append(padBetween("BARANG [AWAL->SISA|RET] LAKU", "SUBTOTAL", colWidth)).append("\n")
        } else {
            sb.append(padBetween("ITEM [AWAL->SISA] LAKU", "TOTAL", colWidth)).append("\n")
        }
        sb.append(divider).append("\n")

        receipt.items.forEach { item ->
            val name = if (item.productName.length > colWidth) item.productName.take(colWidth - 3) + "..." else item.productName
            sb.append(name).append("\n")

            val qtyDesc = "Sisa:${item.remStock} | Laku:${item.soldQty} x %,.0f".format(item.unitPrice)
            val subtotalStr = "Rp %,.0f".format(item.subtotal)
            sb.append(padBetween("  $qtyDesc", subtotalStr, colWidth)).append("\n")

            val extras = mutableListOf<String>()
            if (item.returStock > 0) extras.add("Tarik:${item.returStock} ${item.unit}")
            if (item.addedPacks > 0) extras.add("+Ganti:${item.addedPacks} ${item.unitBig}")
            else if (item.addedQty > 0) extras.add("+Titip:${item.addedQty} ${item.unit}")
            extras.add("Stok Kini:${item.newTotalStock} ${item.unit}")
            sb.append("  [${extras.joinToString(" | ")}]\n")
        }

        sb.append(divider).append("\n")
        sb.append(padBetween("Total Laku (${receipt.totalSoldQuantity} pcs):", "Rp %,.0f".format(receipt.totalAmount), colWidth)).append("\n")
        sb.append(padBetween("Uang Diterima:", "Rp %,.0f".format(receipt.amountPaid), colWidth)).append("\n")

        if (receipt.changeOrDebt >= 0) {
            sb.append(padBetween("Kembalian:", "Rp %,.0f".format(receipt.changeOrDebt), colWidth)).append("\n")
        } else {
            sb.append(padBetween("Kurang / Sisa Hutang:", "Rp %,.0f".format(-receipt.changeOrDebt), colWidth)).append("\n")
        }

        if (!receipt.notes.isNullOrBlank()) {
            sb.append(divider).append("\n")
            sb.append("Catatan: ${receipt.notes}\n")
        }

        sb.append(doubleDivider).append("\n")
        sb.append(centerText("Barang titipan baru telah diterima", colWidth)).append("\n")
        sb.append(centerText("dalam kondisi baik & lengkap.", colWidth)).append("\n")
        sb.append(centerText("~ Terima Kasih Atas Kerjasamanya ~", colWidth)).append("\n")
        sb.append("\n\n")

        return sb.toString()
    }

    fun buildEscPosBytes(receipt: ReceiptData, is80mm: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        val charset = Charset.forName("CP437")
        val colWidth = if (is80mm) 48 else 32
        val divider = ("-".repeat(colWidth) + "\n").toByteArray(charset)
        val doubleDivider = ("=".repeat(colWidth) + "\n").toByteArray(charset)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // Init
        out.write(CMD_INIT)

        // Header Title
        out.write(CMD_ALIGN_CENTER)
        out.write(CMD_BOLD_ON)
        out.write(CMD_DOUBLE_SIZE)
        out.write("${receipt.businessName}\n".toByteArray(charset))
        out.write(CMD_NORMAL_SIZE)
        out.write(CMD_BOLD_OFF)
        out.write("${receipt.businessSub}\n".toByteArray(charset))
        out.write("Telp: ${receipt.businessPhone}\n".toByteArray(charset))
        out.write(doubleDivider)

        // Metadata
        out.write(CMD_ALIGN_LEFT)
        out.write("Nota : #TRX-${receipt.headerId}\n".toByteArray(charset))
        out.write("Tgl  : ${dateFormat.format(Date(receipt.transactionDate))}\n".toByteArray(charset))
        out.write("Toko : ${receipt.customerName}\n".toByteArray(charset))
        if (receipt.customerAddress.isNotBlank()) {
            out.write("Almt : ${receipt.customerAddress}\n".toByteArray(charset))
        }
        out.write(divider)

        // Table Header
        out.write(CMD_BOLD_ON)
        val tableHeader = padBetween("ITEM [AWAL->SISA] LAKU", "TOTAL", colWidth) + "\n"
        out.write(tableHeader.toByteArray(charset))
        out.write(CMD_BOLD_OFF)
        out.write(divider)

        // Items
        receipt.items.forEach { item ->
            out.write(CMD_BOLD_ON)
            out.write("${item.productName}\n".toByteArray(charset))
            out.write(CMD_BOLD_OFF)

            val qtyDesc = "  ${item.prevStock}->${item.remStock} | Laku ${item.soldQty} x %,.0f".format(item.unitPrice)
            val subtotalStr = "Rp %,.0f".format(item.subtotal)
            out.write((padBetween(qtyDesc, subtotalStr, colWidth) + "\n").toByteArray(charset))

            if (item.returStock > 0 || item.addedQty > 0) {
                val extras = mutableListOf<String>()
                if (item.returStock > 0) extras.add("Retur:${item.returStock}")
                if (item.addedQty > 0) extras.add("+Titip:${item.addedQty}")
                extras.add("Stok Akhir:${item.newTotalStock}")
                out.write(("  [" + extras.joinToString(" | ") + "]\n").toByteArray(charset))
            }
        }

        out.write(divider)

        // Summary
        out.write(CMD_BOLD_ON)
        out.write((padBetween("Total Laku (${receipt.totalSoldQuantity} pcs):", "Rp %,.0f".format(receipt.totalAmount), colWidth) + "\n").toByteArray(charset))
        out.write((padBetween("Uang Diterima:", "Rp %,.0f".format(receipt.amountPaid), colWidth) + "\n").toByteArray(charset))

        if (receipt.changeOrDebt >= 0) {
            out.write((padBetween("Kembalian:", "Rp %,.0f".format(receipt.changeOrDebt), colWidth) + "\n").toByteArray(charset))
        } else {
            out.write((padBetween("Sisa Hutang:", "Rp %,.0f".format(-receipt.changeOrDebt), colWidth) + "\n").toByteArray(charset))
        }
        out.write(CMD_BOLD_OFF)

        if (!receipt.notes.isNullOrBlank()) {
            out.write(divider)
            out.write("Catatan: ${receipt.notes}\n".toByteArray(charset))
        }

        out.write(doubleDivider)
        out.write(CMD_ALIGN_CENTER)
        out.write("Barang titipan baru telah diterima\n".toByteArray(charset))
        out.write("dalam kondisi baik & lengkap.\n".toByteArray(charset))
        out.write("~ Terima Kasih ~\n".toByteArray(charset))

        // Feed & cut
        out.write(CMD_FEED_CUT)

        return out.toByteArray()
    }

    private fun padBetween(left: String, right: String, totalWidth: Int): String {
        val available = totalWidth - left.length - right.length
        return if (available > 0) {
            left + " ".repeat(available) + right
        } else {
            // Truncate left if needed
            val trimmedLeft = left.take(maxOf(0, totalWidth - right.length - 1))
            val spaceCount = maxOf(1, totalWidth - trimmedLeft.length - right.length)
            trimmedLeft + " ".repeat(spaceCount) + right
        }
    }

    private fun centerText(text: String, totalWidth: Int): String {
        if (text.length >= totalWidth) return text.take(totalWidth)
        val pad = (totalWidth - text.length) / 2
        return " ".repeat(pad) + text
    }
}
