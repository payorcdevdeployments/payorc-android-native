package com.payorc.sdk.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.PayorcLanguage
import java.util.Locale

object PayorcLocalization {
    val currentLocale: Locale
        get() = when (PayOrcSdk.currentLanguage) {
            PayorcLanguage.english -> Locale.forLanguageTag("en")
            PayorcLanguage.arabic -> Locale.forLanguageTag("ar")
        }

    val currentLayoutDirection: LayoutDirection
        get() = when (PayOrcSdk.currentLanguage) {
            PayorcLanguage.english -> LayoutDirection.Ltr
            PayorcLanguage.arabic -> LayoutDirection.Rtl
        }

    fun localize(key: String): String {
        if (PayOrcSdk.currentLanguage != PayorcLanguage.arabic) return key
        if (key.isBlank()) return key

        arTranslations[key]?.let { return it }
        dynamicArabicTranslation(key)?.let { return it }

        return key
    }

    private fun dynamicArabicTranslation(key: String): String? {
        val transactionPrefix = "Transaction ID: "
        if (key.startsWith(transactionPrefix)) {
            return "معرّف المعاملة: ${key.removePrefix(transactionPrefix)}"
        }

        val expiresPrefix = "Expires "
        if (key.startsWith(expiresPrefix)) {
            return "تنتهي في ${key.removePrefix(expiresPrefix)}"
        }

        val errorPrefix = "Error: "
        if (key.startsWith(errorPrefix)) {
            return "خطأ: ${localize(key.removePrefix(errorPrefix))}"
        }

        return null
    }

    private val arTranslations = mapOf(
        "Apple Pay" to "آبل باي",
        "Google Pay" to "جوجل باي",
        "Tabby" to "تابي",
        "Samsung Pay" to "سامسونج باي",
        "Sadad" to "سداد",
        "Urpay" to "يور باي",
        "Pay with Card" to "الدفع بالبطاقة",
        "Pay with Tabby" to "الدفع عبر تابي",
        "Use Stored Card" to "استخدام بطاقة محفوظة",
        "Confirm" to "تأكيد",
        "Confirm Payment" to "تأكيد الدفع",
        "Cancel" to "إلغاء",
        "Done" to "تم",
        "Close" to "إغلاق",
        "Error" to "خطأ",
        "Card Verified" to "تم التحقق من البطاقة",
        "Initiating 3D Secure" to "بدء التحقق ثلاثي الأبعاد",
        "Connecting to your bank..." to "جارٍ الاتصال بالبنك...",
        "Payment Failed" to "فشل الدفع",
        "Payment failed" to "فشل الدفع",
        "Payment Successful" to "تم الدفع بنجاح",
        "Retry Payment" to "إعادة المحاولة",
        "Add New Card" to "إضافة بطاقة جديدة",
        "Add a new card" to "إضافة بطاقة جديدة",
        "OR TRY ANOTHER METHOD" to "أو جرّب طريقة أخرى",
        "TRY ANOTHER METHOD" to "جرّب طريقة أخرى",
        "YOUR SAVED CARDS" to "بطاقاتك المحفوظة",
        "Select Card" to "اختر البطاقة",
        "Declined" to "مرفوض",
        "Pay Now" to "ادفع الآن",
        "Make Payment" to "إجراء الدفع",
        "Submitting your order" to "جارٍ إرسال طلبك",
        "Please don't leave this page before the end of the order submission." to "يرجى عدم مغادرة هذه الصفحة قبل اكتمال إرسال الطلب.",
        "Do not close or refresh this page during payment processing." to "لا تقم بإغلاق أو تحديث الصفحة أثناء معالجة الدفع.",
        "Secure" to "آمن",
        "Processing payment..." to "جارٍ معالجة الدفع...",
        "Processing..." to "جارٍ المعالجة...",
        "Opening..." to "جارٍ الفتح...",
        "Edit Card" to "تعديل البطاقة",
        "Use New Card" to "استخدام بطاقة جديدة",
        "Enter your card details securely" to "أدخل بيانات بطاقتك بشكل آمن",
        "Customer Details" to "بيانات العميل",
        "Card Details" to "بيانات البطاقة",
        "Billing Address" to "عنوان الفوترة",
        "Shipping Address" to "عنوان الشحن",
        "CARD HOLDER NAME" to "اسم حامل البطاقة",
        "Card Holder Name" to "اسم حامل البطاقة",
        "Cardholder Name" to "اسم حامل البطاقة",
        "Cardholder name" to "اسم حامل البطاقة",
        "Enter name on card" to "أدخل الاسم الموجود على البطاقة",
        "CARD NUMBER" to "رقم البطاقة",
        "Card Number" to "رقم البطاقة",
        "Card number" to "رقم البطاقة",
        "EXPIRY (MM/YY)" to "تاريخ الانتهاء (MM/YY)",
        "Expiry (MM/YY)" to "تاريخ الانتهاء (MM/YY)",
        "CVV" to "رمز CVV",
        "Enter 3-digit CVV" to "أدخل رمز CVV المكون من 3 أرقام",
        "EMAIL" to "البريد الإلكتروني",
        "Email" to "البريد الإلكتروني",
        "MOBILE NUMBER" to "رقم الهاتف",
        "Mobile number" to "رقم الهاتف",
        "Name" to "الاسم",
        "Code" to "الرمز",
        "Phone" to "الهاتف",
        "Save changes" to "حفظ التغييرات",
        "Verify" to "تحقق",
        "256-bit SSL Encrypted" to "تشفير SSL ‏256-بت",
        "Enter cardholder name" to "أدخل اسم حامل البطاقة",
        "Enter first and last name" to "أدخل الاسم الأول والأخير",
        "Enter a valid card number" to "أدخل رقم بطاقة صحيح",
        "Required" to "مطلوب",
        "Use MM/YY" to "استخدم MM/YY",
        "Invalid month" to "شهر غير صحيح",
        "Invalid year" to "سنة غير صحيحة",
        "Enter a valid expiry date" to "أدخل تاريخ انتهاء صالح",
        "CVV required" to "رمز CVV مطلوب",
        "3 or 4 digits" to "3 أو 4 أرقام",
        "Enter a valid email" to "أدخل بريدًا إلكترونيًا صالحًا",
        "Search country" to "ابحث عن دولة",
        "Payment Confirmed" to "تم تأكيد الدفع",
        "Your order has been submitted successfully" to "تم إرسال طلبك بنجاح",
        "Approve" to "موافقة",
        "3D Secure" to "التحقق ثلاثي الأبعاد",
        "Verified by Visa" to "موثق بواسطة فيزا",
        "Enter authentication code" to "أدخل رمز التحقق",
        "We've sent a 6-digit code to ....9999" to "أرسلنا رمزًا من 6 أرقام إلى ....9999",
        "Code expires in " to "تنتهي صلاحية الرمز خلال ",
        "Resend" to "إعادة الإرسال",
        "Secured by your issuing bank" to "محمي بواسطة البنك المُصدِر",
        "Merchant" to "التاجر",
        "Card" to "البطاقة",
        "Amount" to "المبلغ",
        "Date" to "التاريخ",
        "Card Verification" to "التحقق من البطاقة",
        "Apple Pay isn't available on this device. Use Pay with Card." to "آبل باي غير متاح على هذا الجهاز. استخدم الدفع بالبطاقة.",
        "Waiting to continue..." to "بانتظار المتابعة...",
        "Network error" to "خطأ في الشبكة",
        "Please enter CVV" to "يرجى إدخال رمز CVV",
        "Payment status:" to "حالة الدفع:",
        "Enter CVV" to "أدخل رمز CVV",
        "Your transaction could not be completed. Please try again or use a different payment method." to "تعذّر إتمام معاملتك. يرجى المحاولة مرة أخرى أو استخدام طريقة دفع مختلفة.",
        "No internet connection" to "لا يوجد اتصال بالإنترنت",
        "No payment methods available." to "لا توجد طرق دفع متاحة.",
        "Address Line 1" to "سطر العنوان 1",
        "Address Line 2 (Optional)" to "سطر العنوان 2 (اختياري)",
        "City" to "المدينة",
        "State/Province" to "الولاية/المحافظة",
        "ZIP/PIN" to "الرمز البريدي",
        "Country" to "الدولة",
        "Powered by" to "مدعوم من",
        "Card Added Successfully!" to "تمت إضافة البطاقة بنجاح!",
        "Your card has been verified. Tap the button below to complete the payment." to "تم التحقق من بطاقتك. اضغط الزر أدناه لإكمال الدفع.",
        "Please enter your CVV to complete the payment" to "يرجى إدخال رمز CVV لإكمال الدفع",
        "By providing your card information, you allow us to charge your card for future payments in accordance with their terms." to "بتقديم معلومات بطاقتك، فإنك تسمح لنا بخصم المدفوعات المستقبلية من بطاقتك وفقًا للشروط.",
        "Loading payment config..." to "جارٍ تحميل إعدادات الدفع...",
        "Initializing..." to "جارٍ التهيئة...",
        "Initializing Tabby..." to "جارٍ تهيئة تابي...",
        "Setting up Tabby session..." to "جارٍ إعداد جلسة تابي...",
        "Creating Tabby session..." to "جارٍ إنشاء جلسة تابي...",
        "Confirming payment..." to "جارٍ تأكيد الدفع...",
        "Payment authorized" to "تمت الموافقة على الدفع",
        "Payment authorized successfully" to "تمت الموافقة على الدفع بنجاح",
        "Failed to load configuration" to "فشل تحميل الإعدادات",
        "An unknown error occurred" to "حدث خطأ غير معروف",
        "Unsupported card network" to "شبكة البطاقة غير مدعومة",
        "Card has expired" to "انتهت صلاحية البطاقة",
        "User cancelled" to "ألغى المستخدم العملية",
        "Tabby payment cancelled" to "تم إلغاء دفع تابي",
        "Tabby payment failed" to "فشل دفع تابي",
        "Transaction failed" to "فشلت المعاملة",
        "Cardholder name required" to "اسم حامل البطاقة مطلوب",
        "Name required" to "الاسم مطلوب",
        "Card number required" to "رقم البطاقة مطلوب",
        "MM required" to "الشهر مطلوب",
        "YY required" to "السنة مطلوبة",
        "Email required" to "البريد الإلكتروني مطلوب",
        "Mobile number required" to "رقم الهاتف مطلوب",
        "Billing address required" to "عنوان الفوترة مطلوب",
        "Shipping address required" to "عنوان الشحن مطلوب",
        "Invalid Cardholder name" to "اسم حامل البطاقة غير صحيح",
        "Invalid Name" to "الاسم غير صحيح",
        "Invalid Card number" to "رقم البطاقة غير صحيح",
        "Invalid MM" to "الشهر غير صحيح",
        "Invalid YY" to "السنة غير صحيحة",
        "Invalid CVV" to "رمز CVV غير صحيح",
        "Invalid Email" to "البريد الإلكتروني غير صحيح",
        "Invalid Mobile number" to "رقم الهاتف غير صحيح",
        "Invalid Billing address" to "عنوان الفوترة غير صحيح",
        "Invalid Shipping address" to "عنوان الشحن غير صحيح"
    )
}

@Composable
fun PayorcLanguageProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLayoutDirection provides PayorcLocalization.currentLayoutDirection,
        content = content
    )
}
