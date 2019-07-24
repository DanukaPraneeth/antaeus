package io.pleo.antaeus.core.utils

class Constants {

    companion object {
        const val PAYMENT_PROVIDER = "paymentProvider"
        const val INVOICE_SERVICE = "invoiceService"
        const val CUSTOMER_SERVICE = "customerService"
        const val CURRENCY_CONVERTOR = "conversionService"
        const val INVOICE_ID = "invoiceId"
        const val COUNTER = "retry"
        const val RETRY_LIMIT = 5
        const val RETRY_DELAY = 10      // delay for the retry attempt in minutes

    }
}