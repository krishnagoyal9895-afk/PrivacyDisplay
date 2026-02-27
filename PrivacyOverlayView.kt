package com.privacy.display

import android.content.Context
import android.graphics.*
import android.view.View

/**
 * PrivacyOverlayView
 *
 * A full-screen transparent view drawn over other apps. It renders configurable pixel
 * patterns (checkerboard, stripes, dots) combined with an optional radial gradient mask
 * to simulate a privacy display effect. AMOLED-optimised: dark pixels cost near-zero power.
 */
class PrivacyOverlayView(context: Context) : View(context) {

    /* ---------- Configuration ---------- */
    var privacyLevel: Int = 50          // 0–100
    var opacity: Int = 60               // 0–100
    var patternSize: Int = 4            // pixels (2–8)
    var patternType: Int = 0            // 0=checker 1=horizontal 2=vertical 3=dots
    var centerClarityEnabled: Boolean = true
    var amoledMode: Boolean = true

    /* ---------- Paints ---------- */
    private val patternPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    /* ---------- Bitmaps (cached) ---------- */
    private var patternBitmap: Bitmap? = null
    private var patternCanvas: Canvas? = null
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastConfig = ""

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = false
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val configKey = "$privacyLevel-$opacity-$patternSize-$patternType-$centerClarityEnabled-$amoledMode"
        if (width != lastWidth || height != lastHeight || configKey != lastConfig) {
            rebuildBitmap(width, height)
            lastWidth = width
            lastHeight = height
            lastConfig = configKey
        }

        patternBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    private fun rebuildBitmap(w: Int, h: Int) {
        patternBitmap?.recycle()

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cvs = Canvas(bmp)

        // Compute effective alpha from privacy level + opacity sliders
        val effectiveAlpha = calculateAlpha()

        if (amoledMode) {
            // Pure black pattern — true black pixels on AMOLED = zero emission
            patternPaint.color = Color.argb(effectiveAlpha, 0, 0, 0)
        } else {
            patternPaint.color = Color.argb(effectiveAlpha, 20, 20, 20)
        }

        when (patternType) {
            0 -> drawCheckerboard(cvs, w, h)
            1 -> drawHorizontalStripes(cvs, w, h)
            2 -> drawVerticalStripes(cvs, w, h)
            3 -> drawDots(cvs, w, h)
        }

        // Apply radial gradient mask for center clarity
        if (centerClarityEnabled) {
            applyGradientMask(cvs, w, h, effectiveAlpha)
        }

        patternBitmap = bmp
        patternCanvas = cvs
    }

    /** Alpha = base from opacity slider, scaled by privacy level */
    private fun calculateAlpha(): Int {
        val baseAlpha = (opacity / 100f * 255).toInt().coerceIn(0, 255)
        val levelMultiplier = privacyLevel / 100f
        return (baseAlpha * levelMultiplier).toInt().coerceIn(30, 240)
    }

    /* ---------- Pattern Renderers ---------- */

    private fun drawCheckerboard(cvs: Canvas, w: Int, h: Int) {
        val ps = patternSize.coerceAtLeast(2)
        var row = 0
        var y = 0f
        while (y < h) {
            var col = 0
            var x = 0f
            while (x < w) {
                if ((row + col) % 2 == 0) {
                    cvs.drawRect(x, y, x + ps, y + ps, patternPaint)
                }
                x += ps
                col++
            }
            y += ps
            row++
        }
    }

    private fun drawHorizontalStripes(cvs: Canvas, w: Int, h: Int) {
        val ps = patternSize.coerceAtLeast(2)
        var y = 0f
        var row = 0
        while (y < h) {
            if (row % 2 == 0) {
                cvs.drawRect(0f, y, w.toFloat(), y + ps, patternPaint)
            }
            y += ps
            row++
        }
    }

    private fun drawVerticalStripes(cvs: Canvas, w: Int, h: Int) {
        val ps = patternSize.coerceAtLeast(2)
        var x = 0f
        var col = 0
        while (x < w) {
            if (col % 2 == 0) {
                cvs.drawRect(x, 0f, x + ps, h.toFloat(), patternPaint)
            }
            x += ps
            col++
        }
    }

    private fun drawDots(cvs: Canvas, w: Int, h: Int) {
        val spacing = (patternSize * 1.5f).coerceAtLeast(4f)
        val radius = spacing / 3f
        patternPaint.style = Paint.Style.FILL
        var y = spacing / 2f
        while (y < h) {
            var x = spacing / 2f
            while (x < w) {
                cvs.drawCircle(x, y, radius, patternPaint)
                x += spacing
            }
            y += spacing
        }
        patternPaint.style = Paint.Style.FILL // reset
    }

    /**
     * Overlay a radial gradient on top of the pattern:
     * - Centre = low alpha (user sees clearly)
     * - Edges  = high alpha (others see heavy pattern)
     *
     * We draw a gradient-masked black rectangle using PorterDuff.
     */
    private fun applyGradientMask(cvs: Canvas, w: Int, h: Int, peakAlpha: Int) {
        val cx = w / 2f
        val cy = h / 2f
        val radius = (minOf(w, h) / 2f) * 1.3f   // gradient extends slightly past screen edge

        // Centre is nearly transparent; edges are more opaque
        val centerAlpha = (peakAlpha * 0.15f).toInt()   // ~15% of peak at centre
        val edgeAlpha   = (peakAlpha * 0.55f).toInt()   // ~55% additional at edge

        val gradient = RadialGradient(
            cx, cy, radius,
            intArrayOf(
                Color.argb(centerAlpha, 0, 0, 0),   // centre — subtle
                Color.argb((peakAlpha * 0.25f).toInt(), 0, 0, 0), // mid
                Color.argb(edgeAlpha, 0, 0, 0)       // edge — stronger
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        gradientPaint.shader = gradient
        gradientPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        cvs.drawRect(0f, 0f, w.toFloat(), h.toFloat(), gradientPaint)
        gradientPaint.shader = null
    }

    fun applyConfig(bundle: android.os.Bundle) {
        privacyLevel   = bundle.getInt("privacy_level", 50)
        opacity        = bundle.getInt("opacity", 60)
        patternSize    = bundle.getInt("pattern_size", 4)
        patternType    = bundle.getInt("pattern_type", 0)
        centerClarityEnabled = bundle.getBoolean("center_clarity", true)
        amoledMode     = bundle.getBoolean("amoled_mode", true)
        // Invalidate cache so it redraws
        lastConfig = ""
        invalidate()
    }
}
