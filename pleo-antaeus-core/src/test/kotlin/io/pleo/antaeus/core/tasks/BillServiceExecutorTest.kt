package io.pleo.antaeus.core.tasks

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.CurrencyConvertor
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.utils.Constants.Companion.COUNTER
import io.pleo.antaeus.core.utils.Constants.Companion.CURRENCY_CONVERTOR
import io.pleo.antaeus.core.utils.Constants.Companion.CUSTOMER_SERVICE
import io.pleo.antaeus.core.utils.Constants.Companion.INVOICE_ID
import io.pleo.antaeus.core.utils.Constants.Companion.INVOICE_SERVICE
import io.pleo.antaeus.core.utils.Constants.Companion.PAYMENT_PROVIDER
import io.pleo.antaeus.core.utils.Constants.Companion.RETRY_LIMIT
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.quartz.*
import org.quartz.impl.JobDetailImpl
import java.math.BigDecimal


@ExtendWith(MockKExtension::class)
internal class BillServiceExecutorTest{

    @MockK
    lateinit var invoiceService: InvoiceService

    @MockK
    lateinit var customerService: CustomerService

    @MockK
    lateinit var paymentProvider: PaymentProvider

    @MockK
    lateinit var currencyConvetor: CurrencyConvertor

    @MockK
    lateinit var context: JobExecutionContext

    @MockK(relaxed = true)
    lateinit var scheduler: Scheduler

    lateinit var schedulerContext: SchedulerContext

    lateinit var jobdetails: JobDetail

    @BeforeEach
    internal fun setUp() {
        schedulerContext = SchedulerContext().apply {
            this[PAYMENT_PROVIDER] = paymentProvider
            this[INVOICE_SERVICE] = invoiceService
            this[CUSTOMER_SERVICE] = customerService
            this[CURRENCY_CONVERTOR] = currencyConvetor
            this[COUNTER] = 1
        }

        jobdetails = JobDetailImpl("name", "group", BillServiceExecutor::class.java).apply {
            jobDataMap[COUNTER] = 0
            jobDataMap[INVOICE_ID] = 1
        }

        every { context.scheduler } returns scheduler
        every { context.jobDetail } returns jobdetails
        every { scheduler.context } returns schedulerContext
        every { invoiceService.fetch(any()) } returns Invoice(1,1, Money(BigDecimal.valueOf(100), currency = Currency.EUR), InvoiceStatus.PENDING)

    }

    companion object {
        val sampleInvoice = Invoice(1,1, Money(BigDecimal.valueOf(100), currency = Currency.EUR), InvoiceStatus.PENDING)
        val sampleCustomer = Customer(1, Currency.USD)
    }

    @Test
    fun `check that Invoice payment works properly`() {

        val paidInvoice = sampleInvoice.copy(status = InvoiceStatus.PAID)

        every { paymentProvider.charge(sampleInvoice) } returns true
        every { invoiceService.update(paidInvoice) } returns paidInvoice

        BillServiceExecutor().execute(context)

        verify { paymentProvider.charge(sampleInvoice) }
        verify { invoiceService.update(paidInvoice) }
    }

    @Test
    fun `check if Invoice payment fails on network error and repeat the schedule`() {

        every { paymentProvider.charge(sampleInvoice) } throws NetworkException()

        BillServiceExecutor().execute(context)

        verify { paymentProvider.charge(sampleInvoice) }
        verify { scheduler.scheduleJob(any()) }
        assertEquals(1, jobdetails.jobDataMap.getIntValue(COUNTER))
    }

    @Test
    fun `check if Invoice payment fails on CurrencyMismatchException and currency conversion works properly `() {

        val updatedInvoice =  sampleInvoice.copy(amount = Money(sampleInvoice.amount.value, sampleCustomer.currency))
        val inputCurrency = sampleInvoice.amount.currency
        val outputCurrency = sampleCustomer.currency
        val value = sampleInvoice.amount.value

        every { paymentProvider.charge(sampleInvoice) } throws CurrencyMismatchException(sampleInvoice.id, sampleCustomer.id)
        every { customerService.fetch(sampleCustomer.id) } returns sampleCustomer
        every { invoiceService.update(updatedInvoice) } returns updatedInvoice
        every { currencyConvetor.convert(inputCurrency, outputCurrency, value) } returns value

        org.junit.jupiter.api.assertThrows<JobExecutionException> { BillServiceExecutor().execute(context) }

        verify { paymentProvider.charge(sampleInvoice) }
        verify { currencyConvetor.convert(inputCurrency, outputCurrency, value) }
        verify { invoiceService.update(updatedInvoice) }
    }

    @Test
    fun `check if Invoice payment fails after maximum retry attempts and raise exception properly`() {

        jobdetails.jobDataMap[COUNTER] = RETRY_LIMIT

        every { paymentProvider.charge(sampleInvoice) } throws NetworkException()
        org.junit.jupiter.api.assertThrows<JobExecutionException> { BillServiceExecutor().execute(context) }
        verify { paymentProvider.charge(sampleInvoice) }
    }

}