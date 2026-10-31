"""Generate matching in-app policy text and static support pages. No external dependencies."""
from pathlib import Path
import html
import json
import os

ROOT = Path(__file__).resolve().parents[1]
EMAIL = os.environ.get('SUPPORT_EMAIL', 'pdftoolsupport@gmail.com').strip()
CONTACT = EMAIL or 'Use the project support page: https://github.com/waseemkhanjdpr1902/pdf-tools/issues. Do not post documents or private payment details.'
PRIVACY = '''Effective date: 31 October 2026. PDF Pocket is operated by the PDF Pocket project (GitHub: waseemkhanjdpr1902). This preview policy must be reviewed with the final support contact and SDK configuration before a store release.

1. Documents and processing
Scanning, PDF tools and text recognition process the documents you select on your device. Our app does not send document contents to our servers, AdMob or RevenueCat. You choose when to share or export a document. Optional Google Drive sync sends documents directly to the Google account you select. Android system backup or device-transfer behavior may also depend on your device and system settings.

2. Advertising
The free version can display Google AdMob advertisements after the relevant privacy flow. Google's SDK may process device or advertising identifiers, IP address, app interactions and diagnostic information to deliver, measure and protect advertising. Relevant choices are available under Settings > Advertising privacy choices when required. Premium removes in-app ad placements. Advertising requires a network connection. Google explains its processing at https://policies.google.com/privacy and https://policies.google.com/technologies/partner-sites.

3. Purchases
If subscriptions are configured, Google Play processes payment; we do not receive card details. RevenueCat processes an app-generated customer identifier, purchase tokens, subscription status, product information and associated technical information to verify and restore Premium. It does not need your document contents. See https://www.revenuecat.com/privacy. Payment and transaction records may be retained by these providers for their service and legal obligations. Restore purchases is available in Settings. No app account is required.

4. Optional Google Drive
Drive sync is off unless you enable it. The app uses limited drive.file access to manage its own files in your chosen account. Disconnect in Settings to revoke access. Disconnecting does not automatically delete existing Drive copies. You may delete phone files, Drive copies or both through the app's storage controls. Google account and Drive processing follow Google's privacy policy.

5. Permissions and recognition
Camera access is used for scanning while you use that feature. System pickers provide access to files or images you choose. OCR is performed locally; the bundled recognition model currently supports Latin text. Keep original documents and check generated results.

6. Control and deletion
Delete local files in Settings or through the file list. Manage optional Drive copies separately. Uninstalling removes app-private local data but does not cancel a subscription or delete Drive copies. Manage or cancel subscriptions through Google Play. We cannot recover local documents. To ask about provider-held data or privacy rights, contact support below; verification may be needed. Do not send document contents in a support request.

7. Audience and changes
This is a general productivity app, not a service specifically directed at children. Policy changes will be published here; material in-app changes require renewed acceptance.

8. Contact
CONTACT_VALUE'''.replace('CONTACT_VALUE', CONTACT)
TERMS = '''Effective date: 31 October 2026. Preview terms for PDF Pocket.

1. Using PDF Pocket
Basic PDF tools are available without a subscription. You must have authority to process the documents you select. Do not forge signatures, misrepresent documents or use the app unlawfully. A drawn signature image is not a qualified digital-signature certificate.

2. Premium subscriptions
When available, Premium removes in-app advertisements. Monthly and yearly plans are displayed with prices and billing periods supplied by Google Play. The selected price is charged per stated billing period. Subscriptions automatically renew until cancelled through Google Play. No trial or discount is promised unless expressly shown by the store. Features not listed on the purchase screen are not included promises.

3. Cancellation, restoration and refunds
Cancel or manage renewal in Google Play's subscription settings. Uninstalling the app does not cancel billing. Cancellation normally leaves access until the end of the paid period, subject to store status, refunds and applicable law. Use Restore purchases with the same store account after reinstalling. Refund eligibility and requests follow Google Play's applicable policies; statutory consumer rights are unaffected.

4. Files and results
Keep backups and original files. Compression, cropping, OCR, redaction and conversions can alter or remove content. Check output before relying on it. We cannot recover files stored only on your phone. Availability can differ by Android version; some older phones use an external PDF viewer.

5. Third-party services
Optional Drive sync, Google advertising, Google Play billing and RevenueCat purchase verification follow their respective service terms. Offline document tools can work without a network; advertising, purchase checks and sync require connectivity. Temporary outages do not guarantee a particular service level.

6. Warranty and responsibility
The software is supplied as is, subject to mandatory rights under applicable law. No promise is made that every PDF will process perfectly or every scan will be recognized accurately. Nothing here excludes liability or consumer rights that cannot lawfully be excluded.

7. Contact and changes
CONTACT_VALUE
Material updates will be reflected in the app and on these pages.'''.replace('CONTACT_VALUE', CONTACT)

out = ROOT / 'app/src/main/java/com/sammyi57/pdftools/AppPolicy.kt'
out.write_text('package com.sammyi57.pdftools\n\n// Generated by tools/build_support.py; edit the generator, then regenerate.\nobject AppPolicy {\n  val privacy = ' + json.dumps(PRIVACY, ensure_ascii=False) + '\n  val terms = ' + json.dumps(TERMS, ensure_ascii=False) + '\n}\n')

style = '''*{box-sizing:border-box}body{margin:0;background:#f6f2ee;color:#251d26;font:17px/1.7 system-ui,sans-serif}header,main,footer{max-width:940px;margin:auto;padding:24px}header{display:flex;align-items:center;justify-content:space-between;gap:20px}a{color:#763344}nav{display:flex;gap:18px;flex-wrap:wrap}h1{font-size:clamp(36px,7vw,64px);line-height:1.12;letter-spacing:-2px}h2{line-height:1.3}.badge{display:inline-block;border:1px solid #c6b8bd;border-radius:24px;padding:5px 14px;font-size:14px}.hero{padding:36px 0}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:18px}.card{padding:24px;background:white;border:1px solid #e0d6d9;border-radius:20px}.note{border-left:4px solid #c05436;padding:12px 18px;background:#fff8ed}.button{display:inline-block;padding:12px 22px;background:#54253b;color:white;border-radius:30px;text-decoration:none}footer{font-size:14px;border-top:1px solid #e0d6d9}section{margin:28px 0}a:focus-visible{outline:3px solid #c05436;outline-offset:4px}@media(max-width:560px){header{align-items:flex-start;flex-direction:column}header,main,footer{padding:20px}}'''

def page(title, body):
    return f'''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex"><meta name="description" content="PDF Pocket scanner and PDF tools: product information, privacy and support."><title>{html.escape(title)} | PDF Pocket</title><style>{style}</style></head><body><header><a href="index.html"><strong>PDF Pocket</strong></a><nav aria-label="Main navigation"><a href="privacy.html">Privacy</a><a href="terms.html">Terms</a><a href="support.html">Support</a></nav></header><main>{body}</main><footer>PDF Pocket · Scanner &amp; Tools · Built on <a href="https://github.com/sammyZi/pdf-tools">PDF Tools</a>, MIT licensed. <a href="https://github.com/waseemkhanjdpr1902/pdf-tools">Source</a></footer></body></html>'''

def sections(text):
    return ''.join('<section><p>' + html.escape(x).replace('\n', '<br>') + '</p></section>' for x in text.split('\n\n'))

docs = ROOT / 'docs'
docs.mkdir(exist_ok=True)
notice = '<p class="note">Development preview. Not yet available to download. Live payments and production advertising are not activated. Support email is pending.</p>' if not EMAIL else '<p class="note">Development preview. Store availability will be announced here.</p>'
for slug, title, text in [('privacy', 'Privacy policy', PRIVACY), ('terms', 'Terms & subscriptions', TERMS)]:
    (docs / f'{slug}.html').write_text(page(title, f'<h1>{html.escape(title)}</h1>{notice}{sections(text)}'))
contact_html = f'<a href="mailto:{html.escape(EMAIL, quote=True)}">{html.escape(EMAIL)}</a>' if EMAIL else '<a href="https://github.com/waseemkhanjdpr1902/pdf-tools/issues">Project support page</a> (temporary; public posts)'
(docs / 'support.html').write_text(page('Support', '<h1>Here to help.</h1>' + notice + f'<p>Contact: {contact_html}</p><p>Include your Android version, app version, tool name and steps to reproduce. Do not attach private documents, passwords, card details or purchase tokens.</p><h2>Restore Premium</h2><p>Open Settings → Restore purchases using the same Play Store account. Manage or cancel renewal in Google Play → Payments &amp; subscriptions → Subscriptions.</p><h2>Delete your files</h2><p>Use Settings → Delete all saved files. Optional Drive copies can be deleted separately. No app account is created; Google account settings and provider records are managed with their respective providers.</p>'))
(docs / 'index.html').write_text(page('Scanner & PDF Tools', '''<section class="hero"><span class="badge">Android · Document productivity</span><h1>Your documents.<br>A little more organised.</h1><p>Scan paperwork, merge PDFs, shrink files and sign forms. PDF processing runs on your phone.</p>''' + notice + '''</section><div class="grid"><article class="card"><h2>Scan &amp; search</h2><p>Capture pages, crop edges and make scans searchable with local Latin-text OCR.</p></article><article class="card"><h2>Organise &amp; share</h2><p>Merge, split, reorder, compress and share the files you choose.</p></article><article class="card"><h2>Your choice</h2><p>Free document tools. Optional Premium removes ads. Google Drive sync is optional.</p></article></div><section><h2>Simple tools. Clear choices.</h2><p>No app account is required. Review how ads, purchases and optional backup work before you start.</p><a class="button" href="privacy.html">Read the privacy policy</a></section>'''))
(docs / 'app-ads.txt').write_text('# Production advertising is not enabled. Add the exact authorised-seller line from your AdMob account before launch.\n')
config = {'headers': [{'source': '/(.*)', 'headers': [{'key': 'X-Content-Type-Options', 'value': 'nosniff'}, {'key': 'Referrer-Policy', 'value': 'strict-origin-when-cross-origin'}]}], 'framework': None, 'buildCommand': '', 'installCommand': '', 'outputDirectory': '.'}
(docs / 'vercel.json').write_text(json.dumps(config, indent=2) + '\n')
(ROOT / 'vercel.json').write_text(json.dumps(dict(config, outputDirectory='docs'), indent=2) + '\n')
print('Generated AppPolicy.kt and 4 static support pages.')
