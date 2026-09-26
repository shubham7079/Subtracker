package com.example.util

import com.example.data.model.BillingCycle
import com.example.data.model.SubscriptionCategory
import java.util.Locale
import java.util.regex.Pattern

data class DetectedCharge(
    val serviceName: String,
    val amount: Double,
    val currency: String,
    val billingCycle: BillingCycle,
    val category: SubscriptionCategory,
    val paymentMethod: String,
    val rawText: String,
    val confidence: Float
)

object RecurringChargeDetector {

    data class SampleAlert(
        val title: String,
        val source: String,
        val text: String
    )

    val sampleAlerts = listOf(
        SampleAlert(
            title = "Bank SMS - Netflix",
            source = "SMS (HDFC / Chase)",
            text = "Your card ending in 4242 was debited for $15.99 on 24-Sep-2026 for monthly Netflix subscription auto-renewal. Available balance $2,450.00."
        ),
        SampleAlert(
            title = "Receipt Email - Spotify",
            source = "Email Invoice",
            text = "Spotify Premium Monthly: Thank you for your payment of $10.99 billed to Mastercard •• 8812. Next billing date: 24-Oct-2026."
        ),
        SampleAlert(
            title = "Google Play - ChatGPT Plus",
            source = "Play Billing",
            text = "Google Play Order: You were charged $20.00 for your OpenAI ChatGPT Plus monthly subscription renewal via Google Pay."
        ),
        SampleAlert(
            title = "Apple Receipt - iCloud+",
            source = "Apple StoreKit",
            text = "Apple Receipt: Your monthly subscription to iCloud+ 200GB for $2.99 renewed on 2026-09-22 using Apple Pay."
        ),
        SampleAlert(
            title = "Cloud Bill - AWS",
            source = "AWS Billing",
            text = "Amazon Web Services invoice notification: $45.50 charged to Visa ending in 1904 for recurring monthly cloud compute usage."
        ),
        SampleAlert(
            title = "Indian Bank SMS - Hotstar",
            source = "SMS Alert",
            text = "Acct XX5541 debited by INR 899.00 on 22-Sep-26 for Disney+ Hotstar Annual Super plan renewal. Ref: UPI/9082341."
        )
    )

    private val knownServices = listOf(
        ServiceRule("Netflix", SubscriptionCategory.ENTERTAINMENT, 15.99, BillingCycle.MONTHLY),
        ServiceRule("Spotify", SubscriptionCategory.ENTERTAINMENT, 10.99, BillingCycle.MONTHLY),
        ServiceRule("YouTube Premium", SubscriptionCategory.ENTERTAINMENT, 13.99, BillingCycle.MONTHLY),
        ServiceRule("ChatGPT", SubscriptionCategory.PRODUCTIVITY, 20.00, BillingCycle.MONTHLY, listOf("OpenAI", "ChatGPT Plus")),
        ServiceRule("Amazon Prime", SubscriptionCategory.ENTERTAINMENT, 14.99, BillingCycle.MONTHLY, listOf("Prime Video", "Amazon")),
        ServiceRule("iCloud", SubscriptionCategory.CLOUD_STORAGE, 2.99, BillingCycle.MONTHLY, listOf("iCloud+", "Apple Storage")),
        ServiceRule("Disney+", SubscriptionCategory.ENTERTAINMENT, 9.99, BillingCycle.MONTHLY, listOf("Hotstar", "Disney")),
        ServiceRule("Adobe", SubscriptionCategory.PRODUCTIVITY, 29.99, BillingCycle.MONTHLY, listOf("Creative Cloud", "Photoshop", "Lightroom")),
        ServiceRule("GitHub", SubscriptionCategory.PRODUCTIVITY, 4.00, BillingCycle.MONTHLY, listOf("Copilot", "GitHub Pro")),
        ServiceRule("Gym Membership", SubscriptionCategory.HEALTH_FITNESS, 39.99, BillingCycle.MONTHLY, listOf("Fitness", "Gym", "Equinox", "Gold's Gym")),
        ServiceRule("AWS", SubscriptionCategory.CLOUD_STORAGE, 45.00, BillingCycle.MONTHLY, listOf("Amazon Web Services", "CloudFront")),
        ServiceRule("Dropbox", SubscriptionCategory.CLOUD_STORAGE, 9.99, BillingCycle.MONTHLY),
        ServiceRule("Notion", SubscriptionCategory.PRODUCTIVITY, 10.00, BillingCycle.MONTHLY),
        ServiceRule("Slack", SubscriptionCategory.PRODUCTIVITY, 8.75, BillingCycle.MONTHLY),
        ServiceRule("Microsoft 365", SubscriptionCategory.PRODUCTIVITY, 6.99, BillingCycle.MONTHLY, listOf("Office 365", "Microsoft")),
        ServiceRule("PlayStation Plus", SubscriptionCategory.ENTERTAINMENT, 9.99, BillingCycle.MONTHLY, listOf("PS Plus", "Sony PlayStation")),
        ServiceRule("Xbox Game Pass", SubscriptionCategory.ENTERTAINMENT, 16.99, BillingCycle.MONTHLY, listOf("Game Pass", "Xbox")),
        ServiceRule("Duolingo", SubscriptionCategory.EDUCATION, 6.99, BillingCycle.MONTHLY, listOf("Super Duolingo", "Duolingo Plus"))
    )

    data class ServiceRule(
        val name: String,
        val category: SubscriptionCategory,
        val defaultAmount: Double,
        val defaultCycle: BillingCycle,
        val aliases: List<String> = emptyList()
    )

    fun parse(text: String): DetectedCharge? {
        if (text.isBlank()) return null

        val lower = text.lowercase(Locale.ROOT)

        // 1. Detect Service Name & Category
        var matchedRule: ServiceRule? = null
        for (rule in knownServices) {
            if (lower.contains(rule.name.lowercase(Locale.ROOT))) {
                matchedRule = rule
                break
            }
            for (alias in rule.aliases) {
                if (lower.contains(alias.lowercase(Locale.ROOT))) {
                    matchedRule = rule
                    break
                }
            }
            if (matchedRule != null) break
        }

        // 2. Detect Amount & Currency
        val amountResult = extractAmountAndCurrency(text)
        val amount = amountResult?.first ?: matchedRule?.defaultAmount ?: 9.99
        val currency = amountResult?.second ?: "USD"

        // 3. Detect Billing Cycle
        val cycle = when {
            lower.contains("annual") || lower.contains("yearly") || lower.contains("/yr") -> BillingCycle.YEARLY
            lower.contains("quarter") || lower.contains("quarterly") -> BillingCycle.QUARTERLY
            lower.contains("week") || lower.contains("weekly") -> BillingCycle.WEEKLY
            else -> matchedRule?.defaultCycle ?: BillingCycle.MONTHLY
        }

        // 4. Detect Payment Method
        val paymentMethod = extractPaymentMethod(text)

        val serviceName = matchedRule?.name ?: extractFallbackServiceName(text)
        val category = matchedRule?.category ?: detectCategoryFromKeywords(lower)

        val confidence = if (matchedRule != null && amountResult != null) 0.95f
        else if (matchedRule != null || amountResult != null) 0.75f
        else 0.5f

        return DetectedCharge(
            serviceName = serviceName,
            amount = amount,
            currency = currency,
            billingCycle = cycle,
            category = category,
            paymentMethod = paymentMethod,
            rawText = text,
            confidence = confidence
        )
    }

    private fun extractAmountAndCurrency(text: String): Pair<Double, String>? {
        // Look for patterns like $15.99, USD 19.99, INR 649.00, €9.99, £10, 899.00 INR
        val currencyPatterns = listOf(
            Pattern.compile("""(?i)(?:USD|\$)\s*([0-9]+(?:[.,][0-9]{2})?)""") to "USD",
            Pattern.compile("""(?i)(?:EUR|€)\s*([0-9]+(?:[.,][0-9]{2})?)""") to "EUR",
            Pattern.compile("""(?i)(?:GBP|£)\s*([0-9]+(?:[.,][0-9]{2})?)""") to "GBP",
            Pattern.compile("""(?i)(?:INR|₹|Rs\.?)\s*([0-9]+(?:[.,][0-9]{2})?)""") to "INR",
            Pattern.compile("""(?i)(?:JPY|¥)\s*([0-9]+)""") to "JPY",
            Pattern.compile("""(?i)(?:CAD|CA\$)\s*([0-9]+(?:[.,][0-9]{2})?)""") to "CAD",
            Pattern.compile("""(?i)(?:AUD|AU\$)\s*([0-9]+(?:[.,][0-9]{2})?)""") to "AUD",
            Pattern.compile("""([0-9]+(?:[.,][0-9]{2})?)\s*(?i)(?:USD|\$)""") to "USD",
            Pattern.compile("""([0-9]+(?:[.,][0-9]{2})?)\s*(?i)(?:INR|₹)""") to "INR",
            Pattern.compile("""([0-9]+(?:[.,][0-9]{2})?)\s*(?i)(?:EUR|€)""") to "EUR"
        )

        for ((pattern, code) in currencyPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val numStr = matcher.group(1)?.replace(",", ".")
                val parsed = numStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return Pair(parsed, code)
                }
            }
        }

        // Generic decimal number fallback if preceded by debited / charged / payment of / bill
        val genericPattern = Pattern.compile("""(?i)(?:debited|charged|payment of|amount of|fee of|total)\s*(?:of)?\s*[:$€£₹]?\s*([0-9]+(?:\.[0-9]{1,2})?)""")
        val m = genericPattern.matcher(text)
        if (m.find()) {
            val num = m.group(1)?.toDoubleOrNull()
            if (num != null && num > 0.0) {
                return Pair(num, "USD")
            }
        }

        return null
    }

    private fun extractPaymentMethod(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val cardEndingPattern = Pattern.compile("""(?i)(?:ending (?:in|with)|card|acct|a/c|ending)\s*(?:in|with|no\.?)?\s*[*•X]{0,4}([0-9]{4})""")
        val m = cardEndingPattern.matcher(text)
        val cardLast4 = if (m.find()) m.group(1) else null

        return when {
            lower.contains("apple pay") -> "Apple Pay"
            lower.contains("google pay") || lower.contains("gpay") -> "Google Pay"
            lower.contains("paypal") -> "PayPal"
            lower.contains("upi") -> "UPI"
            lower.contains("mastercard") -> if (cardLast4 != null) "Mastercard •• $cardLast4" else "Mastercard"
            lower.contains("visa") -> if (cardLast4 != null) "Visa •• $cardLast4" else "Visa"
            lower.contains("amex") || lower.contains("american express") -> if (cardLast4 != null) "Amex •• $cardLast4" else "American Express"
            cardLast4 != null -> "Card •• $cardLast4"
            else -> "Credit Card"
        }
    }

    private fun extractFallbackServiceName(text: String): String {
        val forPattern = Pattern.compile("""(?i)(?:for|towards|to|merchant)\s+([A-Za-z0-9+.\- ]{3,25})(?:\s+(?:subscription|renewal|monthly|annual|plan))?""")
        val m = forPattern.matcher(text)
        if (m.find()) {
            val match = m.group(1)?.trim()
            if (!match.isNullOrBlank() && match.length in 3..25) {
                return match.capitalize(Locale.ROOT)
            }
        }
        return "New Subscription"
    }

    private fun detectCategoryFromKeywords(lower: String): SubscriptionCategory {
        return when {
            lower.contains("stream") || lower.contains("movie") || lower.contains("music") || lower.contains("game") -> SubscriptionCategory.ENTERTAINMENT
            lower.contains("cloud") || lower.contains("hosting") || lower.contains("server") || lower.contains("domain") -> SubscriptionCategory.CLOUD_STORAGE
            lower.contains("gym") || lower.contains("fitness") || lower.contains("workout") || lower.contains("yoga") -> SubscriptionCategory.HEALTH_FITNESS
            lower.contains("course") || lower.contains("learn") || lower.contains("class") || lower.contains("tutor") -> SubscriptionCategory.EDUCATION
            lower.contains("software") || lower.contains("ai") || lower.contains("code") || lower.contains("tool") -> SubscriptionCategory.PRODUCTIVITY
            lower.contains("electricity") || lower.contains("internet") || lower.contains("broadband") || lower.contains("mobile") -> SubscriptionCategory.UTILITIES
            lower.contains("bank") || lower.contains("card") || lower.contains("insurance") -> SubscriptionCategory.FINANCE
            else -> SubscriptionCategory.OTHER
        }
    }
}
