package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.CurrencyConvertor
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.utils.Constants.Companion.COUNTER
import io.pleo.antaeus.core.utils.Constants.Companion.CURRENCY_CONVERTOR
import io.pleo.antaeus.core.utils.Constants.Companion.CUSTOMER_SERVICE
import io.pleo.antaeus.core.utils.Constants.Companion.INVOICE_SERVICE
import io.pleo.antaeus.core.utils.Constants.Companion.PAYMENT_PROVIDER
import io.pleo.antaeus.core.utils.JobUtils
import io.pleo.antaeus.models.Invoice
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory
import kotlin.random.Random

class BillingService(
        paymentProvider: PaymentProvider,
        invoiceService: InvoiceService,
        customerService: CustomerService,
        currencyConvertor: CurrencyConvertor,
        private val scheduler: Scheduler = StdSchedulerFactory().scheduler
) {

    // TODO - Add code e.g. here

    init {
        scheduler.context[PAYMENT_PROVIDER] = paymentProvider
        scheduler.context[INVOICE_SERVICE] = invoiceService
        scheduler.context[CUSTOMER_SERVICE] = customerService
        scheduler.context[CURRENCY_CONVERTOR] = currencyConvertor
        scheduler.context[COUNTER] = 0

        if (!scheduler.isStarted) scheduler.start()
    }

    fun startBillServiceExecutor(invoice: Invoice) {

        // Here I am randomly distributing the scheduled tsk to scale the process so that overhead on database and payment provider is reduced
        // This can be further scaled depending on the size of the batch

        val hour = Random.nextInt(0, 12)
        val cronSchedule = "0 0 $hour 1 1/1 ? *"

        scheduler.scheduleJob(JobUtils().buildJobDetail(invoice), JobUtils().buildJobTrigger(invoice, cronSchedule))
    }
}