package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.CurrencyConvertor
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.utils.Constants.Companion.CURRENCY_CONVERTOR
import io.pleo.antaeus.core.utils.Constants.Companion.CUSTOMER_SERVICE
import io.pleo.antaeus.core.utils.Constants.Companion.INVOICE_SERVICE
import io.pleo.antaeus.core.utils.Constants.Companion.PAYMENT_PROVIDER
import io.pleo.antaeus.core.utils.JobUtils
import io.pleo.antaeus.models.Invoice
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory

class BillingService(
        paymentProvider: PaymentProvider,
        invoiceService: InvoiceService,
        customerService: CustomerService,
        currencyConvertor: CurrencyConvertor
) {

    // TODO - Add code e.g. here

    private val scheduler: Scheduler = StdSchedulerFactory().scheduler

    init {
        scheduler.context[PAYMENT_PROVIDER] = paymentProvider
        scheduler.context[INVOICE_SERVICE] = invoiceService
        scheduler.context[CUSTOMER_SERVICE] = customerService
        scheduler.context[CURRENCY_CONVERTOR] = currencyConvertor


        if (!scheduler.isStarted) scheduler.start()
    }

    fun startBillServiceExecutor(invoice: Invoice) {

        val cronSchedule = "0 0 0 1 1/1 ? *"
        scheduler.scheduleJob(JobUtils().buildJobDetail(invoice), JobUtils().buildJobTrigger(invoice, cronSchedule))
    }
}