package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.Invoice
import com.example.data.model.ShopOwner
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateInvoicePdf(
        context: Context,
        invoice: Invoice,
        owner: ShopOwner
    ): File? {
        val pdfDocument = PdfDocument()
        
        // Page width 595, height 842 (A4 Dimensions in points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Set up paints
        val titlePaint = Paint().apply {
            color = Color.rgb(24, 43, 73) // Deep Blue Navy
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 22f
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(33, 150, 243) // Accent Slate/Blue
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
        }

        val normalPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
        }

        val secondaryPaint = Paint().apply {
            color = Color.rgb(100, 100, 100)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 9f
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
        }

        val tableHeaderBgPaint = Paint().apply {
            color = Color.rgb(24, 43, 73) // Deep blue header bar
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(200, 200, 200)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val stampBorderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        val stampFillPaint = Paint().apply {
            style = Paint.Style.FILL
        }

        // Draw Frame Background Accent
        val sideAccentPaint = Paint().apply {
            color = Color.rgb(24, 43, 73)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 15f, 842f, sideAccentPaint)

        val leftMargin = 40f
        var currentY = 50f

        // Draw Invoice Details Title (Right Side)
        canvas.drawText("INVOICE", 420f, currentY + 15f, titlePaint)
        
        // Draw Shop Name (Left Side)
        canvas.drawText(owner.shopName.uppercase(Locale.ROOT), leftMargin, currentY + 15f, titlePaint)
        currentY += 35f

        // Draw Shop Meta Information
        canvas.drawText(owner.location, leftMargin, currentY, secondaryPaint)
        
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedDate = sdfDate.format(Date(invoice.timestamp))
        canvas.drawText("Invoice No: ${invoice.invoiceNumber}", 420f, currentY, boldPaint)
        currentY += 15f

        canvas.drawText("Contact: ${owner.contactNumber}", leftMargin, currentY, secondaryPaint)
        canvas.drawText("Date: $formattedDate", 420f, currentY, secondaryPaint)
        currentY += 15f

        canvas.drawText("Email: ${owner.email}", leftMargin, currentY, secondaryPaint)
        currentY += 30f

        // Draw a separator line
        canvas.drawLine(leftMargin, currentY, 555f, currentY, borderPaint)
        currentY += 20f

        // Draw Customer & Payment terms
        canvas.drawText("CUSTOMER DETAILS", leftMargin, currentY, headerPaint)
        canvas.drawText("BILL TO:", 420f, currentY, headerPaint)
        currentY += 20f

        canvas.drawText("Name: ${invoice.customerName}", leftMargin, currentY, boldPaint)
        canvas.drawText(invoice.customerName, 420f, currentY, normalPaint)
        currentY += 15f

        canvas.drawText("Contact: ${invoice.customerContact}", leftMargin, currentY, secondaryPaint)
        canvas.drawText("Tel: ${invoice.customerContact}", 420f, currentY, secondaryPaint)
        currentY += 30f

        // Draw Table Header Bar
        canvas.drawRect(leftMargin, currentY, 555f, currentY + 25f, tableHeaderBgPaint)
        canvas.drawText("ITEM DESCRIPTION", leftMargin + 10f, currentY + 16f, tableHeaderPaint)
        canvas.drawText("PRICE", 320f, currentY + 16f, tableHeaderPaint)
        canvas.drawText("QTY", 420f, currentY + 16f, tableHeaderPaint)
        canvas.drawText("TOTAL", 495f, currentY + 16f, tableHeaderPaint)
        currentY += 25f

        // Draw items
        val currency = owner.currencySymbol
        for (item in invoice.items) {
            // Draw a subtle border bottom for rows
            canvas.drawRect(leftMargin, currentY, 555f, currentY + 25f, Paint().apply {
                color = Color.rgb(250, 252, 255)
                style = Paint.Style.FILL
            })
            canvas.drawLine(leftMargin, currentY + 25f, 555f, currentY + 25f, borderPaint)

            // Safe text clipping for item names that are too long
            var itemName = item.name
            if (itemName.length > 35) {
                itemName = itemName.substring(0, 32) + "..."
            }
            canvas.drawText(itemName, leftMargin + 10f, currentY + 16f, normalPaint)
            canvas.drawText(String.format(Locale.getDefault(), "$currency %.2f", item.price), 320f, currentY + 16f, normalPaint)
            canvas.drawText(item.quantity.toString(), 420f, currentY + 16f, normalPaint)
            canvas.drawText(String.format(Locale.getDefault(), "$currency %.2f", item.totalPrice), 495f, currentY + 16f, boldPaint)
            currentY += 25f
        }

        currentY += 20f

        // Draw totals (Right aligned)
        val statsXLabel = 360f
        val statsXValue = 495f

        canvas.drawText("SUBTOTAL:", statsXLabel, currentY, normalPaint)
        canvas.drawText(String.format(Locale.getDefault(), "$currency %.2f", invoice.subtotal), statsXValue, currentY, boldPaint)
        currentY += 20f

        if (invoice.taxAmount > 0) {
            canvas.drawText("TAX (${owner.taxRate}%):", statsXLabel, currentY, normalPaint)
            canvas.drawText(String.format(Locale.getDefault(), "$currency %.2f", invoice.taxAmount), statsXValue, currentY, boldPaint)
            currentY += 20f
        }

        // Draw total accent line
        canvas.drawLine(statsXLabel, currentY, 555f, currentY, borderPaint)
        currentY += 15f

        val grandTotalPaint = Paint().apply {
            color = Color.rgb(24, 43, 73)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
        }
        canvas.drawText("GRAND TOTAL:", statsXLabel, currentY, grandTotalPaint)
        canvas.drawText(String.format(Locale.getDefault(), "$currency %.2f", invoice.grandTotal), statsXValue, currentY, grandTotalPaint)

        // Draw payment status badge/stamp on lower left
        var stampY = currentY - 20f
        if (stampY < 450f) {
            stampY = 550f
        }
        
        val stampText = invoice.paymentStatus.uppercase(Locale.ROOT)
        val stampColor = when (stampText) {
            "PAID" -> Color.rgb(46, 125, 50)     // Classic Emerald Green
            "PENDING" -> Color.rgb(239, 108, 0)   // Rich Amber Orange
            else -> Color.rgb(198, 40, 40)        // Crimson Red
        }

        stampBorderPaint.color = stampColor
        stampBorderPaint.alpha = 180
        
        // Draw Stamp Box
        canvas.drawRoundRect(leftMargin + 10f, stampY, leftMargin + 130f, stampY + 40f, 6f, 6f, stampBorderPaint)
        
        // Draw Stamp Text inside
        val stampTextPaint = Paint().apply {
            color = stampColor
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = 15f
            textAlign = Paint.Align.CENTER
            alpha = 200
        }
        canvas.drawText(stampText, leftMargin + 70f, stampY + 25f, stampTextPaint)

        // Draw invoice notes/custom footer
        if (owner.additionalNotes.isNotEmpty()) {
            val footerY = 780f
            canvas.drawLine(leftMargin, footerY - 10f, 555f, footerY - 10f, borderPaint)
            canvas.drawText(owner.additionalNotes, leftMargin, footerY, secondaryPaint)
            canvas.drawText("Generated on device via Mobile Billing Utility", leftMargin, footerY + 15f, secondaryPaint)
        }

        pdfDocument.finishPage(page)

        // Save PDF to cash directories
        val pdfDir = File(context.cacheDir, "generated_invoices")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        // Output file definition
        val cleanedInvcNo = invoice.invoiceNumber.replace("/", "_").replace("\\", "_")
        val outputFile = File(pdfDir, "Invoice_${cleanedInvcNo}.pdf")

        return try {
            val fileOutputStream = FileOutputStream(outputFile)
            pdfDocument.writeTo(fileOutputStream)
            pdfDocument.close()
            fileOutputStream.flush()
            fileOutputStream.close()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
