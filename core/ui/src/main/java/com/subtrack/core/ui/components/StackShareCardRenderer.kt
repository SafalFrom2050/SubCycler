package com.subtrack.core.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.R
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure JVM-friendly QR bit matrix produced offline via ZXing Core (zero network, zero camera).
 */
class QrMatrix(
    val width: Int,
    val height: Int,
    private val modules: BooleanArray
) {
    fun get(x: Int, y: Int): Boolean = modules[y * width + x]
}

fun generateQrMatrix(
    content: String,
    errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M
): QrMatrix? {
    if (content.isBlank()) return null
    return runCatching {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to errorCorrection,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val w = matrix.width
        val h = matrix.height
        val modules = BooleanArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                modules[y * w + x] = matrix.get(x, y)
            }
        }
        QrMatrix(w, h, modules)
    }.getOrNull()
}

/**
 * Decodes a QR code directly from ARGB pixel buffers (pure JVM testable, 100% offline).
 */
fun decodeQrFromArgb(pixels: IntArray, width: Int, height: Int): String? {
    if (width <= 0 || height <= 0 || pixels.size < width * height) return null
    val hints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )
    return runCatching {
        val source = RGBLuminanceSource(width, height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        MultiFormatReader().decode(bitmap, hints).text
    }.getOrNull()
}

/**
 * Offline QR decoder for shared Stack Card PNGs or screenshots.
 * Tries the full image first, then falls back to the bottom-half and bottom-left crops
 * (where SubCycler's Stack Card QR lives) for reliable offline recognition without MLKit or camera.
 */
fun decodeQrFromBitmap(bitmap: Bitmap): String? {
    fun decodeRegion(left: Int, top: Int, w: Int, h: Int): String? {
        if (w < 32 || h < 32) return null
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, left, top, w, h)
        return decodeQrFromArgb(pixels, w, h)
    }

    decodeRegion(0, 0, bitmap.width, bitmap.height)?.let { return it }

    // Bottom half (covers 4:5 Stack Cards and tall screenshots)
    val halfY = (bitmap.height * 0.48f).toInt().coerceIn(0, bitmap.height - 1)
    decodeRegion(0, halfY, bitmap.width, bitmap.height - halfY)?.let { return it }

    // Bottom-left quadrant (focused crop on the Stack Card QR slab)
    val cropW = (bitmap.width * 0.58f).toInt().coerceIn(32, bitmap.width)
    val cropTop = (bitmap.height * 0.56f).toInt().coerceIn(0, bitmap.height - 1)
    return decodeRegion(0, cropTop, cropW, bitmap.height - cropTop)
}

/**
 * Renders a high-contrast, crisp QR code [Bitmap] with subtle rounded data modules while
 * preserving razor-sharp 7x7 finder squares for fast scanning on every phone camera.
 */
fun renderQrBitmap(
    content: String,
    sizePx: Int,
    darkColor: Int = 0xFF02151B.toInt(),
    lightColor: Int = 0xFFFAFBFA.toInt(),
    quietZoneModules: Int = 2
): Bitmap? {
    val qr = generateQrMatrix(content) ?: return null
    val totalModules = qr.width + quietZoneModules * 2
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(lightColor)

    val cell = sizePx.toFloat() / totalModules.toFloat()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = darkColor
        style = Paint.Style.FILL
    }
    val rect = RectF()
    val dataRadius = cell * 0.24f

    fun isFinderZone(x: Int, y: Int): Boolean {
        val inTop = y < 7
        val inBottom = y >= qr.height - 7
        val inLeft = x < 7
        val inRight = x >= qr.width - 7
        return (inTop && inLeft) || (inTop && inRight) || (inBottom && inLeft)
    }

    for (y in 0 until qr.height) {
        for (x in 0 until qr.width) {
            if (!qr.get(x, y)) continue
            val left = (x + quietZoneModules) * cell
            val top = (y + quietZoneModules) * cell
            rect.set(left, top, left + cell + 0.35f, top + cell + 0.35f)
            if (isFinderZone(x, y)) {
                canvas.drawRect(rect, paint)
            } else {
                canvas.drawRoundRect(rect, dataRadius, dataRadius, paint)
            }
        }
    }
    return bmp
}

/**
 * Live Compose QR preview tile backed by [renderQrBitmap] — memoized on [content].
 */
@Composable
fun ExpressiveQrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    containerCorner: Dp = 18.dp
) {
    val qrBitmap = remember(content) {
        renderQrBitmap(
            content = content,
            sizePx = 420,
            darkColor = 0xFF02151B.toInt(),
            lightColor = 0xFFFAFBFA.toInt(),
            quietZoneModules = 2
        )?.asImageBitmap()
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(containerCorner))
            .background(Color(0xFFFAFBFA))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap,
                contentDescription = "Scannable SubCycler transfer QR code",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Renders a branded `1080x1350` (4:5 portrait) Botanical Emerald Stack Card PNG completely offline
 * for sharing to WhatsApp, Instagram, Telegram, LINE, Discord, X, or Quick Share.
 * Embeds the new SubCycler swirling logo, service monogram pills, cycle headline, and a crisp
 * scannable QR code encoding the short `subcycler://s/...` transfer link.
 */
fun renderStackShareCardBitmap(
    context: Context,
    subscriptions: List<Subscription>,
    transferLink: String,
    includePersonalHistory: Boolean
): Bitmap {
    val w = 1080
    val h = 1350
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Load bundled OFL fonts gracefully with crisp sans-serif fallbacks
    val displayTypeface = runCatching {
        ResourcesCompat.getFont(context, R.font.bricolage_grotesque_variable)
    }.getOrNull() ?: Typeface.DEFAULT_BOLD
    val bodyTypeface = runCatching {
        ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_variable)
    }.getOrNull() ?: Typeface.SANS_SERIF

    val displayBold = Typeface.create(displayTypeface, Typeface.BOLD)
    val bodyBold = Typeface.create(bodyTypeface, Typeface.BOLD)
    val bodyRegular = Typeface.create(bodyTypeface, Typeface.NORMAL)

    // 1. Deep Botanical Obsidian-Emerald vertical gradient backdrop
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, w * 0.25f, h.toFloat(),
            intArrayOf(0xFF02151B.toInt(), 0xFF08261E.toInt(), 0xFF0D3A2D.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    // Soft ambient botanical glow orbs behind header & chips
    val orb1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            w * 0.88f, h * 0.14f, 440f,
            intArrayOf(0x305CE8AE, 0x00000000),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(w * 0.88f, h * 0.14f, 440f, orb1Paint)

    val orb2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            w * 0.12f, h * 0.56f, 380f,
            intArrayOf(0x207CE6C1, 0x00000000),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(w * 0.12f, h * 0.56f, 380f, orb2Paint)

    val padX = 72f

    // 2. Top Brand Row: New SubCycler swirling logo + two-tone wordmark + 100% offline badge
    val logoSize = 86f
    val headerTop = 76f
    val logoBmp = runCatching {
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_subcycler_brand_mark)
    }.getOrNull()
    if (logoBmp != null) {
        val logoRect = RectF(padX, headerTop, padX + logoSize, headerTop + logoSize)
        val clipPath = android.graphics.Path().apply {
            addRoundRect(logoRect, 24f, 24f, android.graphics.Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(logoBmp, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
        logoBmp.recycle()
    }

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayBold
        textSize = 54f
        color = 0xFFF2FBF7.toInt()
        letterSpacing = -0.03f
    }
    val cyclerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayBold
        textSize = 54f
        color = 0xFF7CE6C1.toInt()
        letterSpacing = -0.02f
    }
    val wordX = padX + logoSize + 24f
    val wordBaseline = headerTop + 60f
    canvas.drawText("Sub", wordX, wordBaseline, subPaint)
    val subW = subPaint.measureText("Sub")
    canvas.drawText("Cycler", wordX + subW, wordBaseline, cyclerPaint)

    // Right-aligned pill badge
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x267CE6C1
        style = Paint.Style.FILL
    }
    val pillStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x557CE6C1
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = bodyBold
        textSize = 24f
        color = 0xFFBAFCE0.toInt()
        letterSpacing = 0.08f
    }
    val pillLabel = "100% OFFLINE"
    val pillTextW = pillTextPaint.measureText(pillLabel)
    val pillRect = RectF(w - padX - pillTextW - 44f, headerTop + 18f, w - padX, headerTop + 68f)
    canvas.drawRoundRect(pillRect, 25f, 25f, pillPaint)
    canvas.drawRoundRect(pillRect, 25f, 25f, pillStroke)
    canvas.drawText(pillLabel, pillRect.left + 22f, pillRect.top + 33f, pillTextPaint)

    // 3. Editorial Hero Headline
    val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = bodyBold
        textSize = 26f
        color = 0xFF7CE6C1.toInt()
        letterSpacing = 0.14f
    }
    val eyebrowText = if (includePersonalHistory) "MY ROTATION STACK" else "SUBSCRIPTION STACK TEMPLATE"
    canvas.drawText(eyebrowText, padX, 244f, eyebrowPaint)

    val count = subscriptions.size
    val rotatingCount = subscriptions.count { it.isRotating }
    val heroTitle = "$count Subscription${if (count == 1) "" else "s"}"
    val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayBold
        textSize = 84f
        color = AndroidColor.WHITE
        letterSpacing = -0.03f
    }
    canvas.drawText(heroTitle, padX, 336f, heroPaint)

    val subtitleText = when {
        rotatingCount > 0 -> "$rotatingCount rotating on smart cycles · pause, binge, save"
        else -> "Tracked privately on-device · zero internet permission"
    }
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = bodyRegular
        textSize = 31f
        color = 0xFFB8DCD0.toInt()
    }
    canvas.drawText(subtitleText, padX, 390f, subtitlePaint)

    // 4. Service Pills 2-Column Grid (up to 6 visible + "+N more" badge)
    val gridTop = 442f
    val colGap = 24f
    val rowGap = 22f
    val colWidth = (w - padX * 2f - colGap) / 2f
    val tileHeight = 128f
    val maxTiles = 6
    val displaySubs = subscriptions.take(maxTiles)

    val tileBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0E2C24.toInt()
        style = Paint.Style.FILL
    }
    val tileBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1F5243.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    val tileTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayBold
        textSize = 32f
        color = 0xFFF2FBF7.toInt()
    }
    val tileMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = bodyRegular
        textSize = 24f
        color = 0xFF9AC5B7.toInt()
    }
    val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayBold
        textSize = 28f
        color = 0xFFBAFCE0.toInt()
        textAlign = Paint.Align.CENTER
    }

    displaySubs.forEachIndexed { idx, sub ->
        val col = idx % 2
        val row = idx / 2
        val left = padX + col * (colWidth + colGap)
        val top = gridTop + row * (tileHeight + rowGap)
        val cardRect = RectF(left, top, left + colWidth, top + tileHeight)
        val tonal = com.subtrack.core.ui.theme.BrandColorHarmonizer.harmonizedTonalPalette(
            serviceName = sub.name,
            storedColorHex = sub.colorHex,
            isDark = true
        )
        val badgeBgArgb = 0xFF000000.toInt() or com.subtrack.core.ui.theme.BrandColorHarmonizer.composeColorToRgb(tonal.container)
        val badgeInkArgb = 0xFF000000.toInt() or com.subtrack.core.ui.theme.BrandColorHarmonizer.composeColorToRgb(tonal.content)
        val badgeBorderArgb = 0x55000000 or com.subtrack.core.ui.theme.BrandColorHarmonizer.composeColorToRgb(tonal.accent)

        canvas.drawRoundRect(cardRect, 34f, 34f, tileBgPaint)
        canvas.drawRoundRect(cardRect, 34f, 34f, tileBorderPaint)

        // Harmonized brand monogram squircle badge
        val badgeSize = 72f
        val badgeLeft = left + 26f
        val badgeTop = top + (tileHeight - badgeSize) / 2f
        val badgeRect = RectF(badgeLeft, badgeTop, badgeLeft + badgeSize, badgeTop + badgeSize)
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeBgArgb
        }
        val badgeRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = badgeBorderArgb
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(badgeRect, 24f, 24f, badgePaint)
        canvas.drawRoundRect(badgeRect, 24f, 24f, badgeRimPaint)
        val initials = ExpressivePalettes.initialsForService(sub.name)
        badgeTextPaint.color = badgeInkArgb
        val textMetrics = badgeTextPaint.fontMetrics
        val badgeCenterY = badgeRect.centerY() - (textMetrics.ascent + textMetrics.descent) / 2f
        canvas.drawText(initials, badgeRect.centerX(), badgeCenterY, badgeTextPaint)

        // Service name + price/cycle
        val textLeft = badgeRect.right + 22f
        val maxTextW = cardRect.right - textLeft - 22f
        val truncatedName = ellipsizeCanvasText(sub.name, tileTitlePaint, maxTextW)
        canvas.drawText(truncatedName, textLeft, top + 56f, tileTitlePaint)

        val cyclePart = when {
            sub.billingPeriod == BillingPeriod.YEARLY -> "Yearly"
            sub.strategy == RotationStrategy.CONTINUOUS -> "Always on"
            else -> "${sub.activeDays}d/${sub.blackoutDays}d"
        }
        val pricePart = if (sub.cost > 0.0) {
            "${MoneyFormatter.format(sub.cost, sub.currency)}${sub.billingPeriod.shortSuffix} · $cyclePart"
        } else {
            cyclePart
        }
        val truncatedMeta = ellipsizeCanvasText(pricePart, tileMetaPaint, maxTextW)
        canvas.drawText(truncatedMeta, textLeft, top + 93f, tileMetaPaint)
    }

    if (subscriptions.size > maxTiles) {
        val extraText = "+ ${subscriptions.size - maxTiles} more included in this card"
        val extraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bodyBold
            textSize = 25f
            color = 0xFF7CE6C1.toInt()
        }
        canvas.drawText(extraText, padX + 8f, gridTop + 3 * (tileHeight + rowGap) + 12f, extraPaint)
    }

    // 5. Bottom Alabaster QR Pass Slab
    val slabTop = 918f
    val slabBottom = h - 68f
    val slabRect = RectF(padX, slabTop, w - padX, slabBottom)
    val slabPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF5FAF7.toInt()
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(slabRect, 46f, 46f, slabPaint)

    val qrSide = min(296, (slabRect.height() - 68f).roundToInt())
    val qrBitmap = renderQrBitmap(
        content = transferLink,
        sizePx = qrSide,
        darkColor = 0xFF02151B.toInt(),
        lightColor = 0xFFFFFFFF.toInt(),
        quietZoneModules = 2
    )
    val qrLeft = slabRect.left + 34f
    val qrTop = slabRect.centerY() - qrSide / 2f
    if (qrBitmap != null) {
        val qrFrame = RectF(qrLeft - 8f, qrTop - 8f, qrLeft + qrSide + 8f, qrTop + qrSide + 8f)
        val qrFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE }
        canvas.drawRoundRect(qrFrame, 26f, 26f, qrFramePaint)
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, Paint(Paint.ANTI_ALIAS_FLAG))
        qrBitmap.recycle()
    }

    val infoX = qrLeft + qrSide + 42f
    val infoMaxW = slabRect.right - infoX - 34f
    val qrEyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = bodyBold
        textSize = 22f
        color = 0xFF0C6B4E.toInt()
        letterSpacing = 0.12f
    }
    val qrHeadlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = displayBold
        textSize = 40f
        color = 0xFF02151B.toInt()
        letterSpacing = -0.02f
    }
    val qrDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = bodyRegular
        textSize = 25f
        color = 0xFF3E554F.toInt()
    }

    canvas.drawText("SCAN OR SHARE", infoX, slabTop + 82f, qrEyebrowPaint)
    canvas.drawText("Import in 1 tap", infoX, slabTop + 134f, qrHeadlinePaint)
    canvas.drawText(
        ellipsizeCanvasText("Scan with your camera, or", qrDescPaint, infoMaxW),
        infoX,
        slabTop + 184f,
        qrDescPaint
    )
    canvas.drawText(
        ellipsizeCanvasText("share this image straight", qrDescPaint, infoMaxW),
        infoX,
        slabTop + 218f,
        qrDescPaint
    )
    canvas.drawText(
        ellipsizeCanvasText("into SubCycler to import.", qrDescPaint, infoMaxW),
        infoX,
        slabTop + 252f,
        qrDescPaint
    )

    // Bottom badge chip inside QR slab
    val footerPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD6F3E5.toInt() }
    val footerPillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = bodyBold
        textSize = 21f
        color = 0xFF0C6B4E.toInt()
    }
    val footerLabel = "No account · Works offline"
    val footerPillRect = RectF(
        infoX,
        slabBottom - 84f,
        min(slabRect.right - 34f, infoX + footerPillTextPaint.measureText(footerLabel) + 38f),
        slabBottom - 36f
    )
    canvas.drawRoundRect(footerPillRect, 24f, 24f, footerPillPaint)
    canvas.drawText(footerLabel, footerPillRect.left + 19f, footerPillRect.top + 31f, footerPillTextPaint)

    return bitmap
}

private fun ellipsizeCanvasText(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    val ellipsis = "…"
    val ellipsisWidth = paint.measureText(ellipsis)
    var end = text.length
    while (end > 1 && paint.measureText(text, 0, end) + ellipsisWidth > maxWidth) {
        end--
    }
    return text.substring(0, end).trimEnd() + ellipsis
}
