package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.entity.InvoiceEntity
import com.example.data.entity.InvoiceItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val itemType = Types.newParameterizedType(List::class.java, InvoiceItem::class.java)
    private val jsonAdapter = moshi.adapter<List<InvoiceItem>>(itemType)

    fun parseItemsJson(json: String): List<InvoiceItem> {
        return try {
            jsonAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun itemsToJson(items: List<InvoiceItem>): String {
        return jsonAdapter.toJson(items)
    }

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    fun generateInvoicePdf(context: Context, invoice: InvoiceEntity): PdfDocument {
        val pdfDocument = PdfDocument()
        
        // 1240 x 1754 pixels represents A4 at 300 DPI (approx) for extreme crispness
        val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Palette Colors
        val primaryColor = Color.parseColor("#1E3A8A") // Soft Corporate Blue
        val accentColor = Color.parseColor("#0D9488")  // Light Teal Accent
        val darkGray = Color.parseColor("#1F2937")
        val lightGray = Color.parseColor("#9CA3AF")
        val borderGray = Color.parseColor("#E5E7EB")
        val bgHeaderGray = Color.parseColor("#F3F4F6")

        var currentY = 100f
        val leftMargin = 80f
        val rightMargin = 1240f - 80f

        // Draw Business Logo (if available)
        var textStartX = leftMargin
        invoice.businessLogoPath?.let { path ->
            val logoFile = File(path)
            if (logoFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                    if (bitmap != null) {
                        val size = 120
                        val rect = RectF(leftMargin, currentY, leftMargin + size, currentY + size)
                        canvas.drawBitmap(bitmap, null, rect, paint)
                        textStartX = leftMargin + size + 30f
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Business Name and Address
        paint.color = darkGray
        paint.textSize = 32f
        paint.isFakeBoldText = true
        canvas.drawText(invoice.businessName, textStartX, currentY + 35f, paint)

        paint.textSize = 18f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#4B5563")
        
        val addressLines = invoice.businessAddress.split("\n")
        var addrY = currentY + 70f
        for (line in addressLines) {
            canvas.drawText(line, textStartX, addrY, paint)
            addrY += 25f
        }

        // Draw INVOICE title on top right
        paint.color = primaryColor
        paint.textSize = 48f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("INVOICE", rightMargin, currentY + 50f, paint)

        // Draw Invoice Meta Info on top right
        paint.textSize = 18f
        paint.isFakeBoldText = false
        paint.color = darkGray
        
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
        val dateStr = dateFormat.format(Date(invoice.createdAt))
        val dueStr = dateFormat.format(Date(invoice.dueDate))

        var metaY = currentY + 90f
        canvas.drawText("No: ${invoice.invoiceNumber}", rightMargin, metaY, paint)
        metaY += 25f
        canvas.drawText("Tanggal: $dateStr", rightMargin, metaY, paint)
        metaY += 25f
        canvas.drawText("Jatuh Tempo: $dueStr", rightMargin, metaY, paint)

        // Payment status badge
        metaY += 35f
        paint.textAlign = Paint.Align.RIGHT
        if (invoice.isPaid) {
            paint.color = Color.parseColor("#10B981") // Success Green
            paint.isFakeBoldText = true
            canvas.drawText("STATUS: LUNAS", rightMargin, metaY, paint)
        } else {
            paint.color = Color.parseColor("#EF4444") // Red
            paint.isFakeBoldText = true
            canvas.drawText("STATUS: BELUM LUNAS", rightMargin, metaY, paint)
        }

        // Divider
        currentY = Math.max(addrY, metaY) + 50f
        paint.color = borderGray
        paint.strokeWidth = 2f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)

        // Client Info Block
        currentY += 40f
        paint.color = primaryColor
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("DITAGIHKAN KEPADA:", leftMargin, currentY, paint)

        currentY += 35f
        paint.color = darkGray
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText(invoice.clientName, leftMargin, currentY, paint)

        currentY += 25f
        paint.color = Color.parseColor("#4B5563")
        paint.textSize = 18f
        paint.isFakeBoldText = false
        canvas.drawText(invoice.clientContact, leftMargin, currentY, paint)

        // Ledger Table
        currentY += 60f
        
        // Table Header
        paint.color = bgHeaderGray
        val tableHeaderRect = RectF(leftMargin, currentY, rightMargin, currentY + 50f)
        canvas.drawRect(tableHeaderRect, paint)

        paint.color = darkGray
        paint.isFakeBoldText = true
        paint.textSize = 16f
        
        val colDescX = leftMargin + 20f
        val colQtyX = leftMargin + 480f
        val colHargaX = leftMargin + 600f
        val colDiskonX = leftMargin + 800f
        val colPpnX = leftMargin + 940f
        val colTotalX = rightMargin - 20f

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("DESKRIPSI", colDescX, currentY + 32f, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("QTY", colQtyX, currentY + 32f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("HARGA", colHargaX, currentY + 32f, paint)
        canvas.drawText("POTONGAN", colDiskonX, currentY + 32f, paint)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("PPN", colPpnX, currentY + 32f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TOTAL", colTotalX, currentY + 32f, paint)

        currentY += 50f

        // Draw items
        val items = parseItemsJson(invoice.itemsJson)
        var totalSubtotal = 0.0
        var totalDiscount = 0.0
        var totalTax = 0.0

        paint.isFakeBoldText = false
        paint.color = darkGray
        paint.textSize = 16f

        for (item in items) {
            val itemSubtotal = item.quantity * item.price
            val itemDiscount = itemSubtotal * (item.discountPercent / 100.0)
            val itemNet = itemSubtotal - itemDiscount
            val itemPajak = if (item.hasPpn) itemNet * 0.11 else 0.0
            val itemTotal = itemNet + itemPajak

            totalSubtotal += itemSubtotal
            totalDiscount += itemDiscount
            totalTax += itemPajak

            // Background line separator
            paint.color = borderGray
            canvas.drawLine(leftMargin, currentY + 60f, rightMargin, currentY + 60f, paint)

            paint.color = darkGray
            paint.textAlign = Paint.Align.LEFT
            // Item Name
            paint.isFakeBoldText = true
            canvas.drawText(item.name, colDescX, currentY + 28f, paint)
            // Item Desc
            if (item.description.isNotEmpty()) {
                paint.isFakeBoldText = false
                paint.color = Color.parseColor("#6B7280")
                paint.textSize = 13f
                canvas.drawText(item.description, colDescX, currentY + 48f, paint)
            }

            paint.textSize = 16f
            paint.color = darkGray
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(item.quantity.toString().replace(".0", ""), colQtyX, currentY + 35f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(formatRupiah(item.price), colHargaX, currentY + 35f, paint)
            canvas.drawText(if (item.discountPercent > 0) "${item.discountPercent}%" else "-", colDiskonX, currentY + 35f, paint)
            
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(if (item.hasPpn) "11%" else "-", colPpnX, currentY + 35f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.isFakeBoldText = true
            canvas.drawText(formatRupiah(itemTotal), colTotalX, currentY + 35f, paint)

            currentY += 60f
        }

        // Totals Calculations
        currentY += 30f
        val totalsStartX = rightMargin - 400f
        
        paint.color = darkGray
        paint.textSize = 16f
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        
        canvas.drawText("Subtotal", totalsStartX, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatRupiah(totalSubtotal), rightMargin - 20f, currentY, paint)

        currentY += 30f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Total Diskon", totalsStartX, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("- " + formatRupiah(totalDiscount), rightMargin - 20f, currentY, paint)

        currentY += 30f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("PPN 11%", totalsStartX, currentY, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatRupiah(totalTax), rightMargin - 20f, currentY, paint)

        // Bold Total Block
        currentY += 20f
        paint.color = bgHeaderGray
        val finalTotalRect = RectF(totalsStartX - 20f, currentY, rightMargin, currentY + 60f)
        canvas.drawRect(finalTotalRect, paint)

        paint.color = primaryColor
        paint.textSize = 20f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Total Tagihan", totalsStartX, currentY + 38f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatRupiah(totalSubtotal - totalDiscount + totalTax), rightMargin - 20f, currentY + 38f, paint)

        // Reset text alignment
        paint.textAlign = Paint.Align.LEFT

        // Payment instructions at the bottom left
        var instructionY = currentY + 120f
        paint.color = primaryColor
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("PETUNJUK PEMBAYARAN:", leftMargin, instructionY, paint)

        paint.color = darkGray
        paint.textSize = 16f
        paint.isFakeBoldText = false
        val payInstructions = invoice.paymentInstructions.split("\n")
        var payY = instructionY + 30f
        for (line in payInstructions) {
            canvas.drawText(line, leftMargin, payY, paint)
            payY += 25f
        }

        // Draw Stamp and Signatures bottom right
        var sigY = currentY + 120f
        val sigAreaStartX = rightMargin - 450f

        paint.color = primaryColor
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("AUTHORIZED SIGNATURE:", sigAreaStartX, sigY, paint)

        // Draw stamp & signature box side-by-side
        sigY += 30f

        // 1. Stamp Image (left of signature area)
        invoice.companyStampPath?.let { stampPath ->
            val stampFile = File(stampPath)
            if (stampFile.exists()) {
                try {
                    val stampBitmap = BitmapFactory.decodeFile(stampFile.absolutePath)
                    if (stampBitmap != null) {
                        val stampRect = RectF(sigAreaStartX, sigY, sigAreaStartX + 180f, sigY + 150f)
                        canvas.drawBitmap(stampBitmap, null, stampRect, paint)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. Hand-drawn Signature (right of signature area)
        invoice.signaturePath?.let { sigPath ->
            val sigFile = File(sigPath)
            if (sigFile.exists()) {
                try {
                    val sigBitmap = BitmapFactory.decodeFile(sigFile.absolutePath)
                    if (sigBitmap != null) {
                        val sigRect = RectF(sigAreaStartX + 200f, sigY, sigAreaStartX + 380f, sigY + 150f)
                        canvas.drawBitmap(sigBitmap, null, sigRect, paint)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        pdfDocument.finishPage(page)
        return pdfDocument
    }
}
