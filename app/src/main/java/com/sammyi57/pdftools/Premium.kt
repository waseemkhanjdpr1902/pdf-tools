package com.sammyi57.pdftools

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith

/** Entitlements come only from RevenueCat, never a preference or a successful payment UI alone. */
object Premium {
  var active by mutableStateOf(false)
    private set
  var resolved by mutableStateOf(false)
    private set
  var busy by mutableStateOf(false)
    private set
  var packages by mutableStateOf<List<Package>>(emptyList())
    private set
  var message by mutableStateOf("")
    private set
  var configured by mutableStateOf(false)
    private set

  fun start(activity: Activity) {
    if (configured || BuildConfig.REVENUECAT_PUBLIC_KEY.isBlank()) return
    Purchases.configure(PurchasesConfiguration.Builder(activity.applicationContext, BuildConfig.REVENUECAT_PUBLIC_KEY).build())
    configured = true
    refresh()
  }

  private fun apply(info: CustomerInfo) {
    active = info.entitlements[BuildConfig.PREMIUM_ENTITLEMENT]?.isActive == true
    resolved = true
  }

  fun refresh() {
    if (!configured) return
    Purchases.sharedInstance.getCustomerInfoWith(
      onError = { /* Keep the last SDK-confirmed state. Unknown users see no ads until resolved. */ },
      onSuccess = { apply(it) },
    )
  }

  fun loadPlans() {
    if (!configured || busy) return
    busy = true
    message = ""
    Purchases.sharedInstance.getOfferingsWith(
      onError = { busy = false; message = "Could not load plans. Check your connection and try again." },
      onSuccess = { offerings ->
        packages = offerings.current?.availablePackages.orEmpty().filter {
          it.packageType == PackageType.MONTHLY || it.packageType == PackageType.ANNUAL
        }.sortedBy { if (it.packageType == PackageType.ANNUAL) 0 else 1 }
        busy = false
        if (packages.isEmpty()) message = "Premium plans are not available yet. All PDF tools remain available."
      },
    )
  }

  fun buy(activity: Activity, plan: Package) {
    if (!configured || busy) return
    busy = true
    message = ""
    Purchases.sharedInstance.purchaseWith(
      PurchaseParams.Builder(activity, plan).build(),
      onError = { _, cancelled ->
        busy = false
        message = if (cancelled) "Purchase cancelled. You have not been upgraded." else "Purchase not completed. If payment is pending, wait for confirmation or use Restore purchases."
      },
      onSuccess = { _, info ->
        apply(info)
        busy = false
        message = if (active) "Premium is active. Enjoy PDF Pocket without ads." else "Premium is not active yet. Pending payments unlock only after confirmation."
      },
    )
  }

  fun restore() {
    if (!configured || busy) return
    busy = true
    message = ""
    Purchases.sharedInstance.restorePurchasesWith(
      onError = { busy = false; message = "Could not restore purchases. Check your Play Store account and connection." },
      onSuccess = {
        apply(it)
        busy = false
        message = if (active) "Premium restored." else "No active Premium subscription was found for this store account."
      },
    )
  }
}

fun supportUrl(): String = if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) "mailto:${BuildConfig.SUPPORT_EMAIL}"
  else "https://github.com/waseemkhanjdpr1902/pdf-tools/issues"

fun policyUrl(page: String): String = if (BuildConfig.WEBSITE_URL.isNotBlank())
  "${BuildConfig.WEBSITE_URL.trimEnd('/')}/$page.html"
else "https://github.com/waseemkhanjdpr1902/pdf-tools/blob/feature/pdf-pocket-premium/docs/$page.html"

@Composable
fun PremiumPanel() {
  // Hide the offer until purchases are actually configured; the free tools still work.
  if (!Premium.configured) return
  LaunchedEffect(Premium.configured) { Premium.loadPlans() }
  GroupLabel("PDF Pocket Premium")
  Group {
    ListRow(
      if (Premium.active) "Premium is active" else "Go ad-free", first = true,
      subtitle = "Every PDF tool is included in the free version. Premium removes advertisements; it does not change your document storage.",
    )
  }
  Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (!Premium.configured) {
      T("Subscriptions are not available in this preview. No payment will be taken.", Type.small, c.sub)
    } else if (!Premium.active) {
      Premium.packages.forEach { plan ->
        val period = if (plan.packageType == PackageType.ANNUAL) "year" else "month"
        Btn("${plan.product.price.formatted} / $period", Modifier.fillMaxWidth(), enabled = !Premium.busy) {
          hostActivity?.let { Premium.buy(it, plan) }
        }
      }
      if (Premium.packages.isEmpty()) Btn("Load plans", Modifier.fillMaxWidth(), busy = Premium.busy, kind = BtnKind.Secondary) { Premium.loadPlans() }
    }
    if (Premium.configured) {
      Btn("Restore purchases", Modifier.fillMaxWidth(), kind = BtnKind.Secondary, enabled = !Premium.busy) { Premium.restore() }
      Btn("Manage subscriptions", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) {
        openUrl("https://play.google.com/store/account/subscriptions")
      }
    }
    if (Premium.message.isNotBlank()) T(Premium.message, Type.small, c.sub)
    T("Subscriptions renew automatically until cancelled in Google Play. The displayed price is charged each month or year, according to your selected plan. Uninstalling does not cancel a subscription. Refunds follow Google Play's applicable policy.", Type.small, c.sub)
    Btn("Privacy policy", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) { openUrl(policyUrl("privacy")) }
    Btn("Subscription terms", Modifier.fillMaxWidth(), kind = BtnKind.Secondary) { openUrl(policyUrl("terms")) }
  }
}
