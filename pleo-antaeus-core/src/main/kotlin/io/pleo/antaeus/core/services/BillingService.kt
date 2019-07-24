package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.utils.JobUtils
import io.pleo.antaeus.models.Invoice
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory

class BillingService(
        paymentProvider: PaymentProvider,
        invoiceService: InvoiceService,
        customerService: CustomerService
) {

    // TODO - Add code e.g. here

    private val scheduler: Scheduler = StdSchedulerFactory().scheduler

    init {
        scheduler.context["paymentProvider"] = paymentProvider
        scheduler.context["invoiceService"] = invoiceService
        scheduler.context["customerService"] = customerService

        if (!scheduler.isStarted) scheduler.start()
    }

    fun startBillServiceExecutor(invoice: Invoice) {

        val cronSchedule = "0 0 0 1 1/1 ? *"
        scheduler.scheduleJob(JobUtils().buildJobDetail(invoice), JobUtils().buildJobTrigger(invoice, cronSchedule))
    }
}