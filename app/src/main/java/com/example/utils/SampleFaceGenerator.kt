package com.example.utils

import android.graphics.*
import java.io.ByteArrayOutputStream

object SampleFaceGenerator {
    fun generateFace(
        name: String,
        themeColor: Int,
        hasGlasses: Boolean,
        hasHat: Boolean,
        hasMustache: Boolean,
        isQueryImage: Boolean = false
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Background
        canvas.drawColor(themeColor)

        // Face outline
        paint.color = Color.parseColor("#FFE0BD") // Skin tone
        paint.isAntiAlias = true
        canvas.drawCircle(200f, 200f, 130f, paint)

        // Eyes
        paint.color = Color.BLACK
        if (isQueryImage) {
            // Give them slightly playful wink or smaller eyes to simulate query variation
            canvas.drawCircle(150f, 180f, 12f, paint)
            canvas.drawCircle(250f, 180f, 12f, paint)
        } else {
            canvas.drawCircle(150f, 180f, 15f, paint)
            canvas.drawCircle(250f, 180f, 15f, paint)
        }

        // Smile
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.color = Color.parseColor("#D32F2F")
        val rectF = RectF(140f, 180f, 260f, 260f)
        canvas.drawArc(rectF, 30f, 120f, false, paint)

        // Nose
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F5C396")
        canvas.drawCircle(200f, 205f, 12f, paint)

        // Glasses
        if (hasGlasses) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 8f
            paint.color = Color.DKGRAY
            // Left lens
            canvas.drawCircle(150f, 180f, 28f, paint)
            // Right lens
            canvas.drawCircle(250f, 180f, 28f, paint)
            // Bridge
            canvas.drawLine(178f, 180f, 222f, 180f, paint)
        }

        // Hat
        if (hasHat) {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#3E2723")
            // Hat base
            canvas.drawRect(90f, 100f, 310f, 120f, paint)
            // Hat top
            canvas.drawRect(120f, 50f, 280f, 100f, paint)
        }

        // Mustache
        if (hasMustache) {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#4E342E")
            val path = Path()
            path.moveTo(160f, 225f)
            path.quadTo(200f, 210f, 240f, 225f)
            path.quadTo(200f, 240f, 160f, 225f)
            canvas.drawPath(path, paint)
        }

        // Label
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val labelText = if (isQueryImage) "Sorgu: $name" else name
        canvas.drawText(labelText.uppercase(), 200f, 360f, paint)

        // Convert to ByteArray
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }
}
