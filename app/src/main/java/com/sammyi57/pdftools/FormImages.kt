package com.sammyi57.pdftools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/** Bounded decode, EXIF orientation, exact dimensions, then the highest JPEG quality that fits. */
fun resizeFormImage(bytes: ByteArray, opts: Opts): ByteArray {
  val width = opts.str("width").toIntOrNull() ?: fail("Enter a whole-number width in pixels.")
  val height = opts.str("height").toIntOrNull() ?: fail("Enter a whole-number height in pixels.")
  if (width !in 16..4096 || height !in 16..4096 || width.toLong() * height > 8_000_000) {
    fail("Use dimensions from 16 to 4096 pixels, with at most 8 million pixels in total.")
  }
  val kb = opts.str("targetKB").toDoubleOrNull() ?: fail("Enter a maximum size in KB.")
  if (!kb.isFinite() || kb < 1 || kb > 20_480) fail("Use a maximum size between 1 and 20480 KB.")
  val target = (kb * 1024).toInt()
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
  if (bounds.outWidth <= 0 || bounds.outHeight <= 0) fail("This image cannot be read. Try a JPEG or PNG.")
  var sample = 1
  while (max(bounds.outWidth, bounds.outHeight) / sample > 4096) sample *= 2
  val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    ?: fail("This image cannot be read.")
  val orientation = runCatching {
    ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
  }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
  val matrix = Matrix().apply {
    when (orientation) {
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
      ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
      ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
      ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
      ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
    }
  }
  var oriented: Bitmap? = null
  var output: Bitmap? = null
  try {
    val image = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    oriented = image
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    output = result
    val scaleX = width.toFloat() / image.width
    val scaleY = height.toFloat() / image.height
    val scale = if (opts.str("fit") == "crop") max(scaleX, scaleY) else min(scaleX, scaleY)
    val left = (width - image.width * scale) / 2f
    val top = (height - image.height * scale) / 2f
    Canvas(result).apply {
      drawColor(Color.WHITE)
      drawBitmap(image, null, RectF(left, top, left + image.width * scale, top + image.height * scale), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }
    fun encode(quality: Int): ByteArray = ByteArrayOutputStream().use {
      if (!result.compress(Bitmap.CompressFormat.JPEG, quality, it)) fail("Could not encode this image.")
      it.toByteArray()
    }
    // Descending search ensures the highest fitting quality; JPEG sizes need not be perfectly monotonic.
    for (quality in 95 downTo 10) {
      val encoded = encode(quality)
      if (encoded.size <= target) return encoded
    }
    fail("Cannot fit these dimensions under $kb KB at usable quality. Reduce the dimensions or increase the size limit.")
  } finally {
    output?.recycle()
    if (oriented !== source) oriented?.recycle()
    source.recycle()
  }
}
