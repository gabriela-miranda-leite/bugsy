package com.bugsy.desktop.brand

import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Path
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Surface
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Everything that draws the Bugsy mark comes from one file, `assets/brand/bugsy-mark.svg`. This reads it and writes the
 * platform versions: Android and Compose vector drawables, and the app icons. Change the mark in that file, run
 * `./gradlew :bugsy-desktop:generateBrandAssets`, and every place follows. A test ([BrandAssetsTest]) fails if a
 * generated file is out of date, so a copy can't quietly drift (DEB-68).
 */
class Brand(val svg: String) {
    val paths: List<String> = Regex("""<path\s+d="([^"]+)"""").findAll(svg).map { it.groupValues[1] }.toList()
    val color: String = attribute("data-brand-color")
    val background: String = attribute("data-brand-background")
    val strokeWidth: String = attribute("data-stroke-width")

    private val viewBox: List<Double> = Regex("""viewBox="([^"]+)"""").find(svg)!!.groupValues[1].trim().split(Regex("\\s+")).map(String::toDouble)
    val x get() = viewBox[0]
    val y get() = viewBox[1]
    val width get() = viewBox[2]
    val height get() = viewBox[3]
    val centerX get() = x + width / 2
    val centerY get() = y + height / 2

    init {
        require(paths.isNotEmpty()) { "no paths in the brand mark" }
    }

    private fun attribute(name: String) = Regex("""$name="([^"]+)"""").find(svg)?.groupValues?.get(1) ?: error("missing $name in the brand mark")

    private fun num(value: Double): String = if (value == value.toLong().toDouble()) value.toLong().toString() else "%.4f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')

    private fun argb(hex: String) = "#FF" + hex.removePrefix("#").uppercase()

    /** An Android vector drawable of the mark. [groupAttributes] place it inside the [viewport]. */
    fun androidVector(widthDp: Int, heightDp: Int, viewportWidth: Double, viewportHeight: Double, tint: String, groupAttributes: String, note: String): String = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("<!-- GENERATED from assets/brand/bugsy-mark.svg. Do not edit: change that file and run ./gradlew :bugsy-desktop:generateBrandAssets.")
        appendLine("     $note -->")
        appendLine("""<vector xmlns:android="http://schemas.android.com/apk/res/android"""")
        appendLine("""    android:width="${widthDp}dp"""")
        appendLine("""    android:height="${heightDp}dp"""")
        appendLine("""    android:viewportWidth="${num(viewportWidth)}"""")
        appendLine("""    android:viewportHeight="${num(viewportHeight)}">""")
        appendLine("""    <group $groupAttributes>""")
        for (d in paths) {
            appendLine("        <path")
            appendLine("""            android:pathData="$d"""")
            appendLine("""            android:strokeColor="$tint"""")
            appendLine("""            android:strokeWidth="$strokeWidth"""")
            appendLine("""            android:strokeLineCap="round"""")
            appendLine("""            android:strokeLineJoin="round" />""")
        }
        appendLine("    </group>")
        appendLine("</vector>")
    }

    /** The mark at its own size, in the brand colour: what Compose tints, and what the overlay draws. */
    fun composeDrawable() = androidVector(
        widthDp = width.toInt(), heightDp = height.toInt(), viewportWidth = width, viewportHeight = height, tint = argb(color),
        groupAttributes = """android:translateX="${num(-x)}" android:translateY="${num(-y)}"""",
        note = "The colour is a placeholder: the overlay tints it where it is used.",
    )

    /** The mark centred in a square box, for Android views (the floating button) and, in white, the status bar. */
    fun squareDrawable(sizeDp: Int, tint: String, note: String): String {
        val box = maxOf(width, height)
        return androidVector(
            widthDp = sizeDp, heightDp = sizeDp, viewportWidth = box, viewportHeight = box, tint = tint,
            groupAttributes = """android:translateX="${num(-x + (box - width) / 2)}" android:translateY="${num(-y + (box - height) / 2)}"""",
            note = note,
        )
    }

    /** The adaptive launcher icon foreground: the mark at half size, inside the 66dp safe zone of a 108dp canvas. */
    fun launcherForeground(): String = androidVector(
        widthDp = 108, heightDp = 108, viewportWidth = 108.0, viewportHeight = 108.0, tint = argb(color),
        groupAttributes = """android:pivotX="${num(centerX)}" android:pivotY="${num(centerY)}" android:scaleX="0.5" android:scaleY="0.5" android:translateX="${num(54 - centerX)}" android:translateY="${num(54 - centerY)}"""",
        note = "Adaptive launcher icon foreground, on the Bugsy surface colour.",
    )

    /** [margin] and [radius] are fractions of [size]; [markWidth] is the mark's width as a fraction of the shape. */
    fun png(size: Int, margin: Double, radius: Double, markWidth: Double = 0.56, opaque: Boolean = false): ByteArray {
        val surface = if (opaque) Surface.makeRaster(ImageInfo.makeN32(size, size, ColorAlphaType.OPAQUE)) else Surface.makeRasterN32Premul(size, size)
        val canvas: Canvas = surface.canvas
        val inset = (size * margin).toFloat()
        val side = size - 2 * inset
        canvas.drawRRect(RRect.makeXYWH(inset, inset, side, side, (size * radius).toFloat()), Paint().apply { color = parse(background); isAntiAlias = true })
        val scale = (side * markWidth / width).toFloat()
        canvas.save()
        canvas.translate(size / 2f, size / 2f)
        canvas.scale(scale, scale)
        canvas.translate(-centerX.toFloat(), -centerY.toFloat())
        val stroke = Paint().apply {
            color = parse(this@Brand.color)
            mode = PaintMode.STROKE
            strokeWidth = this@Brand.strokeWidth.toFloat()
            strokeCap = PaintStrokeCap.ROUND
            strokeJoin = PaintStrokeJoin.ROUND
            isAntiAlias = true
        }
        for (d in paths) canvas.drawPath(Path.makeFromSVGString(d)!!, stroke)
        canvas.restore()
        return surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes
    }

    private fun parse(hex: String): Int = (0xFF000000L or hex.removePrefix("#").toLong(16)).toInt()

    /** Windows `.ico` with PNG images inside, at the sizes Windows asks for. */
    fun ico(): ByteArray {
        val images = listOf(16, 32, 48, 64, 128, 256).map { it to png(it, margin = 0.0, radius = RADIUS) }
        val out = ByteBuffer.allocate(6 + 16 * images.size + images.sumOf { it.second.size }).order(ByteOrder.LITTLE_ENDIAN)
        out.putShort(0).putShort(1).putShort(images.size.toShort())
        var offset = 6 + 16 * images.size
        for ((size, data) in images) {
            out.put((if (size == 256) 0 else size).toByte()).put((if (size == 256) 0 else size).toByte()).put(0).put(0)
            out.putShort(1).putShort(32).putInt(data.size).putInt(offset)
            offset += data.size
        }
        images.forEach { out.put(it.second) }
        return out.array()
    }

    fun sha256(): String = MessageDigest.getInstance("SHA-256").digest(svg.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        const val RADIUS = 0.2237

        fun load(root: File) = Brand(File(root, "assets/brand/bugsy-mark.svg").readText())
    }
}

/** Where each generated file goes, relative to the repository root, and what it contains. */
fun generatedTextFiles(brand: Brand): Map<String, String> = mapOf(
    "ui/src/commonMain/composeResources/drawable/bugsy_icon.xml" to brand.composeDrawable(),
    "core/src/androidMain/res/drawable/bugsy_mark.xml" to brand.squareDrawable(104, "#FF${brand.color.removePrefix("#").uppercase()}", "The mark in the brand colour, for Android views (the floating button)."),
    "core/src/androidMain/res/drawable/bugsy_notification.xml" to brand.squareDrawable(24, "#FFFFFFFF", "Status bar icon: a white silhouette the system tints."),
    "sample-android/src/main/res/drawable/ic_launcher_foreground.xml" to brand.launcherForeground(),
    "assets/brand/generated.sha256" to brand.sha256() + "\n",
)

/** The binary files, which are written but not compared byte for byte (PNG output can differ between platforms). */
fun generatedBinaryFiles(brand: Brand): Map<String, ByteArray> = mapOf(
    "bugsy-desktop/src/main/resources/bugsy-app-icon.png" to brand.png(512, margin = 0.0, radius = Brand.RADIUS),
    "bugsy-desktop/packaging/icon.png" to brand.png(512, margin = 0.0, radius = Brand.RADIUS),
    "bugsy-desktop/packaging/icon.ico" to brand.ico(),
    "sample-ios/Sources/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png" to brand.png(1024, margin = 0.0, radius = 0.0, markWidth = 0.5, opaque = true),
)

fun main(args: Array<String>) {
    val root = File(args.firstOrNull() ?: ".").canonicalFile
    val brand = Brand.load(root)
    generatedTextFiles(brand).forEach { (path, text) -> File(root, path).apply { parentFile.mkdirs() }.writeText(text) }
    generatedBinaryFiles(brand).forEach { (path, bytes) -> File(root, path).apply { parentFile.mkdirs() }.writeBytes(bytes) }

    // The macOS icon is an .icns, which only macOS's `iconutil` makes. The Big Sur shape is 80.5% of the canvas, centred.
    val iconset = File(root, "bugsy-desktop/packaging/Bugsy.iconset").apply { deleteRecursively(); mkdirs() }
    for ((name, size) in listOf(
        "icon_16x16" to 16, "icon_16x16@2x" to 32, "icon_32x32" to 32, "icon_32x32@2x" to 64,
        "icon_128x128" to 128, "icon_128x128@2x" to 256, "icon_256x256" to 256, "icon_256x256@2x" to 512,
        "icon_512x512" to 512, "icon_512x512@2x" to 1024,
    )) File(iconset, "$name.png").writeBytes(brand.png(size, margin = 0.0975, radius = Brand.RADIUS * 0.805))
    val icns = File(root, "bugsy-desktop/packaging/icon.icns")
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        val result = ProcessBuilder("iconutil", "-c", "icns", iconset.path, "-o", icns.path).redirectErrorStream(true).start()
        val output = result.inputStream.readBytes().decodeToString()
        check(result.waitFor() == 0) { "iconutil failed: $output" }
    } else {
        println("Not on macOS: left ${icns.path} as it is. Run this on a Mac to regenerate the .icns.")
    }
    iconset.deleteRecursively()
    println("Brand assets generated from assets/brand/bugsy-mark.svg")
}
