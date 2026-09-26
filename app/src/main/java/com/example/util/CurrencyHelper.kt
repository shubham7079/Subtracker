package com.example.util

import java.util.Locale

object CurrencyHelper {

    data class CountryInfo(
        val countryName: String,
        val flag: String,
        val currencyCode: String,
        val currencySymbol: String,
        val currencyName: String,
        val rateToUsd: Double
    )

    val supportedCountries = listOf(
        CountryInfo("India", "🇮🇳", "INR", "₹", "Indian Rupee", 0.012),
        CountryInfo("United States", "🇺🇸", "USD", "$", "US Dollar", 1.0),
        CountryInfo("United Kingdom", "🇬🇧", "GBP", "£", "British Pound", 1.28),
        CountryInfo("European Union", "🇪🇺", "EUR", "€", "Euro", 1.08),
        CountryInfo("Germany", "🇩🇪", "EUR", "€", "Euro", 1.08),
        CountryInfo("France", "🇫🇷", "EUR", "€", "Euro", 1.08),
        CountryInfo("Canada", "🇨🇦", "CAD", "CA$", "Canadian Dollar", 0.74),
        CountryInfo("Australia", "🇦🇺", "AUD", "AU$", "Australian Dollar", 0.66),
        CountryInfo("Japan", "🇯🇵", "JPY", "¥", "Japanese Yen", 0.0068),
        CountryInfo("Singapore", "🇸🇬", "SGD", "S$", "Singapore Dollar", 0.76),
        CountryInfo("United Arab Emirates", "🇦🇪", "AED", "AED", "UAE Dirham", 0.272),
        CountryInfo("Saudi Arabia", "🇸🇦", "SAR", "SAR", "Saudi Riyal", 0.266),
        CountryInfo("Brazil", "🇧🇷", "BRL", "R$", "Brazilian Real", 0.18),
        CountryInfo("Mexico", "🇲🇽", "MXN", "Mex$", "Mexican Peso", 0.051),
        CountryInfo("Switzerland", "🇨🇭", "CHF", "CHF", "Swiss Franc", 1.15),
        CountryInfo("South Korea", "🇰🇷", "KRW", "₩", "South Korean Won", 0.00075),
        CountryInfo("New Zealand", "🇳🇿", "NZD", "NZ$", "New Zealand Dollar", 0.61),
        CountryInfo("South Africa", "🇿🇦", "ZAR", "R", "South African Rand", 0.056),
        CountryInfo("Indonesia", "🇮🇩", "IDR", "Rp", "Indonesian Rupiah", 0.000063),
        CountryInfo("Philippines", "🇵🇭", "PHP", "₱", "Philippine Peso", 0.017),
        CountryInfo("Malaysia", "🇲🇾", "MYR", "RM", "Malaysian Ringgit", 0.23),
        CountryInfo("Thailand", "🇹🇭", "THB", "฿", "Thai Baht", 0.029),
        CountryInfo("Vietnam", "🇻🇳", "VND", "₫", "Vietnamese Dong", 0.00004),
        CountryInfo("Nigeria", "🇳🇬", "NGN", "₦", "Nigerian Naira", 0.00062),
        CountryInfo("Pakistan", "🇵🇰", "PKR", "₨", "Pakistani Rupee", 0.0036),
        CountryInfo("Bangladesh", "🇧🇩", "BDT", "৳", "Bangladeshi Taka", 0.0083)
    )

    // Deduplicated list of currencies for direct currency pickers
    val supportedCurrencies = supportedCountries
        .distinctBy { it.currencyCode }
        .map {
            CurrencyInfo(
                code = it.currencyCode,
                symbol = it.currencySymbol,
                name = it.currencyName,
                rateToUsd = it.rateToUsd
            )
        }

    data class CurrencyInfo(val code: String, val symbol: String, val name: String, val rateToUsd: Double)

    fun getCountryByCode(currencyCode: String): CountryInfo? {
        return supportedCountries.find { it.currencyCode.equals(currencyCode, ignoreCase = true) }
    }

    fun getSymbol(currencyCode: String): String {
        return supportedCountries.find { it.currencyCode.equals(currencyCode, ignoreCase = true) }?.currencySymbol
            ?: currencyCode
    }

    fun getFlag(currencyCode: String): String {
        return supportedCountries.find { it.currencyCode.equals(currencyCode, ignoreCase = true) }?.flag
            ?: "🌐"
    }

    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) return amount
        val fromRate = supportedCountries.find { it.currencyCode.equals(fromCurrency, ignoreCase = true) }?.rateToUsd ?: 1.0
        val toRate = supportedCountries.find { it.currencyCode.equals(toCurrency, ignoreCase = true) }?.rateToUsd ?: 1.0
        val amountInUsd = amount * fromRate
        return amountInUsd / toRate
    }

    fun format(amount: Double, currencyCode: String): String {
        val symbol = getSymbol(currencyCode)
        return if (currencyCode.equals("JPY", ignoreCase = true) ||
            currencyCode.equals("KRW", ignoreCase = true) ||
            currencyCode.equals("VND", ignoreCase = true) ||
            currencyCode.equals("IDR", ignoreCase = true)
        ) {
            "$symbol${String.format(Locale.US, "%,.0f", amount)}"
        } else {
            "$symbol${String.format(Locale.US, "%,.2f", amount)}"
        }
    }
}
