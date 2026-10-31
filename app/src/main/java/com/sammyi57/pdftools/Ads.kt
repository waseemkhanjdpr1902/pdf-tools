package com.sammyi57.pdftools

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object Advertising {
  var ready by mutableStateOf(false)
    private set
  var privacyOptionsRequired by mutableStateOf(false)
    private set
  private var initialized = false

  fun start(activity: Activity) {
    if (!BuildConfig.ADS_ENABLED || !Terms.accepted()) return
    val info = UserMessagingPlatform.getConsentInformation(activity)
    info.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
      UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { update(activity) }
    }, { update(activity) })
  }

  private fun update(activity: Activity) {
    val info = UserMessagingPlatform.getConsentInformation(activity)
    privacyOptionsRequired = info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    if (!info.canRequestAds()) { ready = false; return }
    if (!initialized) {
      initialized = true
      MobileAds.initialize(activity.applicationContext) { activity.runOnUiThread { ready = info.canRequestAds() } }
    } else ready = true
  }

  fun privacyOptions(activity: Activity) {
    ready = false // Dispose the old request while choices change.
    UserMessagingPlatform.showPrivacyOptionsForm(activity) { update(activity) }
  }
}

/** A labelled banner only on the tools home screen, never over scanning, files or editing controls. */
@Composable
fun HomeAdvertisement() {
  if (!BuildConfig.ADS_ENABLED || !Advertising.ready || Premium.active ||
    (Premium.configured && !Premium.resolved) || !Terms.accepted()) return
  val context = LocalContext.current
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  var loaded by remember { mutableStateOf(false) }
  val ad = remember(context) {
    AdView(context).apply {
      setAdSize(AdSize.BANNER)
      adUnitId = BuildConfig.BANNER_AD_UNIT_ID
      adListener = object : AdListener() { override fun onAdLoaded() { loaded = true } }
      loadAd(AdRequest.Builder().build())
    }
  }
  DisposableEffect(ad, lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> ad.pause()
        Lifecycle.Event.ON_RESUME -> ad.resume()
        else -> Unit
      }
    }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer); ad.destroy() }
  }
  Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (loaded) T(if (BuildConfig.DEBUG) "Advertisement · test" else "Advertisement", Type.small, c.sub)
    AndroidView(factory = { ad })
  }
}
