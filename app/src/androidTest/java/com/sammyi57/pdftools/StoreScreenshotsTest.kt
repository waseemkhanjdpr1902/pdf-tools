package com.sammyi57.pdftools

import android.content.Intent
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

/** Captures the real app UI on an isolated emulator; all document contents are synthetic fixtures. */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotsTest {
  private val instrumentation = InstrumentationRegistry.getInstrumentation()
  private val device = UiDevice.getInstance(instrumentation)
  private val context = instrumentation.targetContext
  private val output get() = File(context.getExternalFilesDir(null), "store-screenshots").apply { mkdirs() }

  @Test
  fun captureStoreScreenshots() {
    check(BuildConfig.APPLICATION_ID.endsWith(".test")) { "Screenshots must use the isolated -PsideBySide build." }
    check(!BuildConfig.ADS_ENABLED) { "Use -PstoreScreenshots to match the ad-disabled internal release." }
    appContext = context
    PDFBoxResourceLoader.init(context)
    rootFolder().deleteRecursively()
    Terms.accept()
    ThemeState.set(ThemePref.Light)
    Drafts.set(null)
    val samples = listOf(
      document("Travel checklist", 3), document("Meeting notes", 2), document("Project overview", 1),
    )
    File(rootFolder(), "Work").mkdirs()
    val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    assertNotNull(device.wait(Until.findObject(By.text("Tools")), 15000))
    capture("01-home")
    checkNotNull(device.wait(Until.findObject(By.desc("Files")), 8000)).click()
    assertNotNull(device.wait(Until.findObject(By.text("Files")), 8000))
    capture("02-files")
    checkNotNull(device.wait(Until.findObject(By.desc("Tools")), 8000)).click()

    val shots = listOf("merge" to "03-merge", "compress" to "04-compress", "split" to "05-split", "organize" to "06-organise", "fillSign" to "07-fill-and-sign")
    shots.forEach { (id, filename) ->
      val tool = checkNotNull(toolById(id))
      instrumentation.runOnMainSync {
        val chosen = if (id == "merge") samples.take(2) else samples.take(1)
        Drafts.set(ToolDraft(id, chosen.map { DraftFile(it.name, it.path, it.length()) }, defaults(tool.opts)))
      }
      val open = checkNotNull(device.wait(Until.findObject(By.text("Open")), 10000))
      assertNotNull("Resume control for $id", open)
      open.click()
      assertNotNull("Tool title for $id", device.wait(Until.findObject(By.text(tool.title)), 10000))
      capture(filename)
      device.pressBack()
      assertNotNull(device.wait(Until.findObject(By.desc("Tools")), 10000))
      Thread.sleep(700)
      instrumentation.runOnMainSync { Drafts.set(null) }
      Thread.sleep(500)
    }
    instrumentation.runOnMainSync { ThemeState.set(ThemePref.Dark) }
    capture("08-dark-mode")
    assertEquals(8, output.listFiles()?.count { it.extension == "png" })
    instrumentation.runOnMainSync { activity.finish() }
  }

  private fun capture(name: String) {
    device.waitForIdle(3000)
    Thread.sleep(2200) // Let document thumbnails and navigation transitions finish before capture.
    val frame = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
    assertEquals(1080, frame.width)
    assertEquals(1920, frame.height)
    val rgb = checkNotNull(frame.copy(Bitmap.Config.RGB_565, false))
    File(output, "$name.png").outputStream().use { rgb.compress(Bitmap.CompressFormat.PNG, 100, it) }
    rgb.recycle()
    frame.recycle()
  }

  private fun document(title: String, pages: Int): File {
    val bytes = PDDocument().use { doc ->
      repeat(pages) { index ->
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        PDPageContentStream(doc, page).use { cs ->
          cs.setNonStrokingColor(84, 37, 59)
          cs.addRect(0f, 685f, 595f, 157f)
          cs.fill()
          fun line(text: String, x: Float, y: Float, size: Float, light: Boolean = false) {
            if (light) cs.setNonStrokingColor(255, 249, 242) else cs.setNonStrokingColor(50, 43, 48)
            cs.beginText()
            cs.setFont(PDType1Font.HELVETICA, size)
            cs.newLineAtOffset(x, y)
            cs.showText(text)
            cs.endText()
          }
          line(title.uppercase(), 42f, 760f, 27f, true)
          line("Sample document for PDF Pocket", 42f, 719f, 13f, true)
          line("YOUR EVERYDAY DOCUMENTS, ORGANISED", 42f, 632f, 17f)
          listOf("Keep important details together.", "Review your notes and prepare the next steps.", "Save an original copy before making changes.", "Share only the pages you choose.").forEachIndexed { i, text ->
            line("${i + 1}. $text", 42f, 580f - i * 49f, 14f)
          }
          line("SAMPLE DATA - No personal information", 42f, 93f, 11f)
          line("Page ${index + 1} of $pages", 42f, 60f, 11f)
        }
      }
      ByteArrayOutputStream().use { doc.save(it); it.toByteArray() }
    }
    return File(rootFolder(), "$title.pdf").apply { writeBytes(bytes) }
  }
}
