package com.sammyi57.pdftools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class FormImagesTest {
  private fun image(): ByteArray {
    val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.RED)
    return ByteArrayOutputStream().use {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
      bitmap.recycle()
      it.toByteArray()
    }
  }

  @Test fun fitPreservesWholeImageAndMeetsLimit() {
    val out = resizeFormImage(image(), NoOpts.with("width", "300").with("height", "400").with("targetKB", "50").with("fit", "fit"))
    assertTrue(out.size <= 50 * 1024)
    val bitmap = BitmapFactory.decodeByteArray(out, 0, out.size)
    assertEquals(300, bitmap.width)
    assertEquals(400, bitmap.height)
    assertTrue(Color.green(bitmap.getPixel(150, 10)) > 230) // white margin
    assertTrue(Color.red(bitmap.getPixel(150, 200)) > 230)
    assertTrue(Color.green(bitmap.getPixel(150, 200)) < 30)
    bitmap.recycle()
  }

  @Test fun cropFillsDimensionsWithoutMargins() {
    val out = resizeFormImage(image(), NoOpts.with("width", "600").with("height", "200").with("targetKB", "20").with("fit", "crop"))
    assertTrue(out.size <= 20 * 1024)
    val bitmap = BitmapFactory.decodeByteArray(out, 0, out.size)
    assertEquals(600, bitmap.width)
    assertEquals(200, bitmap.height)
    assertTrue(Color.green(bitmap.getPixel(300, 5)) < 30)
    bitmap.recycle()
  }

  @Test fun rejectsImpossibleSizeRatherThanExportingOversizeImage() {
    assertThrows(UserError::class.java) {
      resizeFormImage(image(), NoOpts.with("width", "4096").with("height", "1900").with("targetKB", "1"))
    }
  }
}
