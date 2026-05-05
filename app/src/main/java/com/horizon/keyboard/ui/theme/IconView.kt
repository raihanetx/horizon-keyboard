package com.horizon.keyboard.ui.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View

/**
 * Custom vector-style icon view for the keyboard header.
 * Draws keyboard, translate, clipboard, voice, and settings icons.
 */
class IconView(context: Context, val iconType: IconType) : View(context) {

    enum class IconType { KEYBOARD, TRANSLATE, CLIPBOARD, VOICE, SETTINGS }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = width.toFloat()
        val h = height.toFloat()

        when (iconType) {
            IconType.KEYBOARD -> drawKeyboard(canvas, s, h)
            IconType.TRANSLATE -> drawTranslate(canvas, s, h)
            IconType.CLIPBOARD -> drawClipboard(canvas, s, h)
            IconType.VOICE -> drawVoice(canvas, s, h)
            IconType.SETTINGS -> drawSettings(canvas, s, h)
        }
    }

    private fun drawKeyboard(c: Canvas, s: Float, h: Float) {
        val color = Color.parseColor("#0A84FF")
        strokePaint.color = color
        fillPaint.color = color
        val sw = s * 0.07f
        strokePaint.strokeWidth = sw
        val left = s * 0.08f; val top = h * 0.18f; val right = s * 0.92f; val bottom = h * 0.82f
        c.drawRoundRect(left, top, right, bottom, s * 0.12f, s * 0.12f, strokePaint)
        val keyR = sw * 0.7f
        val padX = (right - left) * 0.12f; val padY = (bottom - top) * 0.15f
        val innerL = left + padX; val innerR = right - padX; val innerT = top + padY; val innerB = bottom - padY
        for (i in 0..2) {
            val x = innerL + (innerR - innerL) * (i + 0.5f) / 3f
            c.drawCircle(x, innerT + (innerB - innerT) * 0.25f, keyR, fillPaint)
            c.drawCircle(x, innerT + (innerB - innerT) * 0.55f, keyR, fillPaint)
        }
        val spaceY = innerT + (innerB - innerT) * 0.82f
        c.drawLine(innerL + (innerR - innerL) * 0.15f, spaceY, innerR - (innerR - innerL) * 0.15f, spaceY, strokePaint)
    }

    private fun drawTranslate(c: Canvas, s: Float, h: Float) {
        val color = Color.parseColor("#A0A0A8")
        strokePaint.color = color; val sw = s * 0.065f; strokePaint.strokeWidth = sw
        val off = s * 0.1f
        c.drawRect(s * 0.12f + off, h * 0.15f + off, s * 0.58f + off, h * 0.75f + off, strokePaint)
        c.drawRect(s * 0.12f, h * 0.15f, s * 0.58f, h * 0.75f, strokePaint)
        fillPaint.color = color; strokePaint.style = Paint.Style.FILL; strokePaint.textSize = h * 0.3f
        strokePaint.typeface = Typeface.DEFAULT_BOLD; strokePaint.textAlign = Paint.Align.CENTER
        c.drawText("A", (s * 0.12f + s * 0.58f) / 2f, h * 0.15f + h * 0.5f, strokePaint)
        strokePaint.style = Paint.Style.STROKE
        val ax = s * 0.72f; val ay = h * 0.72f; val arrowLen = s * 0.15f
        c.drawLine(ax, ay, ax + arrowLen, ay + arrowLen, strokePaint)
    }

    private fun drawClipboard(c: Canvas, s: Float, h: Float) {
        val color = Color.parseColor("#A0A0A8")
        strokePaint.color = color; fillPaint.color = color; val sw = s * 0.065f; strokePaint.strokeWidth = sw
        val left = s * 0.18f; val top = h * 0.1f; val right = s * 0.82f; val bottom = h * 0.9f
        c.drawRoundRect(left, top, right, bottom, s * 0.06f, s * 0.06f, strokePaint)
        val clipW = s * 0.3f; val clipH = h * 0.14f
        c.drawRoundRect((s - clipW) / 2f, top - clipH * 0.4f, (s + clipW) / 2f, top - clipH * 0.4f + clipH, s * 0.05f, s * 0.05f, strokePaint)
        val lineGap = (bottom - top) * 0.18f; val lineL = left + s * 0.15f; val lineR = right - s * 0.15f
        for (i in 0..2) { val y = top + h * 0.35f + lineGap * i; c.drawLine(lineL, y, lineR - (if (i == 2) s * 0.25f else 0f), y, strokePaint) }
    }

    private fun drawVoice(c: Canvas, s: Float, h: Float) {
        val color = Color.parseColor("#A0A0A8")
        strokePaint.color = color; val sw = s * 0.07f; strokePaint.strokeWidth = sw
        val cx = s / 2f; val cy = h * 0.35f; val micW = s * 0.18f; val micH = h * 0.28f
        c.drawRoundRect(cx - micW, cy - micH * 0.5f, cx + micW, cy + micH * 0.5f, micW * 0.6f, micW * 0.6f, strokePaint)
        val arcR = s * 0.22f; val arcTop = cy + micH * 0.3f
        c.drawArc(RectF(cx - arcR, arcTop, cx + arcR, arcTop + arcR * 2f), 0f, 180f, false, strokePaint)
        c.drawLine(cx, arcTop + arcR, cx, h * 0.82f, strokePaint)
        c.drawLine(cx - s * 0.14f, h * 0.82f, cx + s * 0.14f, h * 0.82f, strokePaint)
    }

    private fun drawSettings(c: Canvas, s: Float, h: Float) {
        val color = Color.parseColor("#A0A0A8")
        strokePaint.color = color; fillPaint.color = color; val sw = s * 0.065f; strokePaint.strokeWidth = sw
        val cx = s / 2f; val cy = h / 2f; val outerR = s * 0.34f; val innerR = s * 0.14f; val toothH = s * 0.09f
        for (i in 0 until 6) {
            val angle = Math.toRadians(i * 60.0 - 30.0)
            c.drawLine(cx + Math.cos(angle).toFloat() * (outerR - toothH), cy + Math.sin(angle).toFloat() * (outerR - toothH),
                cx + Math.cos(angle).toFloat() * outerR, cy + Math.sin(angle).toFloat() * outerR, strokePaint)
        }
        c.drawCircle(cx, cy, outerR - toothH, strokePaint)
        c.drawCircle(cx, cy, innerR, fillPaint)
    }
}
