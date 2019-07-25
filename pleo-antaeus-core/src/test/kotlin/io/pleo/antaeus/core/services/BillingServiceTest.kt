package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.pleo.antaeus.core.external.CurrencyConvertor
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.tasks.BillServiceExecutor
import io.pleo.antaeus.core.utils.Constants
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.quartz.*
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class BillingServiceTest {

    @MockK
    lateinit var invoiceService: InvoiceService

    @MockK
    lateinit var customerService: CustomerService

    @MockK
    lateinit var paymentProvider: PaymentProvider

    @MockK
    lateinit var currencyConversionService: CurrencyConvertor

    @MockK(relaxed = true)
    lateinit var scheduler: Scheduler

    companion object {
        val sampleInvoice = Invoice(1,1, Money(BigDecimal.valueOf(100), currency = Currency.EUR), InvoiceStatus.PENDING)
        val sampleTrigger: CronTrigger = TriggerBuilder
                .newTrigger()
                .withDescription("Invoice Payment Trigger")
                .withIdentity("trigger_invoice_1")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 1 1/1 ? *"))
                .build()
        val sampleJobDetail: JobDetail = JobBuilder
                .newJob()
                .withDescription("Invoice Payment Job")
                .ofType(BillServiceExecutor::class.java)
                .withIdentity("job_invoice_1")
                .usingJobData(Constants.INVOICE_ID, 1)
                .build()
    }


    @Test
    fun `Check that schedule job starts properly for given scheduler`() {
        every { scheduler.isStarted } returns false
        every { scheduler.context } returns SchedulerContext()

        BillingService(paymentProvider, invoiceService, customerService, currencyConversionService, scheduler).startBillServiceExecutor(sampleInvoice)

        verify(exactly = 1) { scheduler.scheduleJob(sampleJobDetail, sampleTrigger) }
    }

    @Test
    fun `Check that schedule doesn't duplicate the job start`() {
        every { scheduler.isStarted } returns true
        every { scheduler.context } returns SchedulerContext()

        BillingService(paymentProvider, invoiceService, customerService, currencyConversionService)
        verify(exactly = 0) { scheduler.start() }
    }
}