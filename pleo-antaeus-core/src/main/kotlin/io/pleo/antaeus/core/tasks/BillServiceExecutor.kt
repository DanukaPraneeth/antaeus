package io.pleo.antaeus.core.tasks

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.CurrencyConvertor
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.utils.Constants
import io.pleo.antaeus.core.utils.JobUtils
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.slf4j.LoggerFactory

class BillServiceExecutor : Job {

    companion object{
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }


    override fun execute(context: JobExecutionContext) {

        val taskContext = context.scheduler.context
        val paymentProvider = taskContext[Constants.PAYMENT_PROVIDER] as PaymentProvider
        val invoiceService = taskContext[Constants.INVOICE_SERVICE] as InvoiceService

        val invoice = invoiceService.fetch(context.jobDetail.jobDataMap.getIntValue(Constants.INVOICE_ID))

        try {
            logger.debug("Executing the for ${invoice.id} and status ${invoice.status}")
            payCustomerInvoice(invoice, invoiceService, paymentProvider)

            if(invoice.status == InvoiceStatus.PAID)
                context.scheduler.deleteJob(context.jobDetail.key)

        } catch (ex: CustomerNotFoundException) {
            logger.error("Error during periodic bill service. Customer not found for customer Id: ${invoice.id}", ex)
            context.scheduler.deleteJob(context.jobDetail.key)

        } catch (ex: CurrencyMismatchException) {
            logger.error("Error during periodic bill service. Currency mismatch for invoice Id: ${invoice.id} , customer Id : ${invoice.customerId}", ex)
            handleCurrencyMismatchException(context, ex, invoiceService)

        } catch (ex: NetworkException) {
            logger.error("Error during periodic bill service. Network failure ")
            repeatTheSchedule(context, ex)

        }
    }


    private fun payCustomerInvoice(invoice: Invoice, invoiceService: InvoiceService, paymentProvider: PaymentProvider) {

        if (invoice.status != InvoiceStatus.PENDING) {
            logger.info("Charging aborted for invoice Id ${invoice.id}. Status of the invoice is : ${invoice.status}")

        } else {
            if (paymentProvider.charge(invoice)) {
                invoiceService.update(invoice.copy(status = InvoiceStatus.PAID))
                var sta = invoiceService.fetch(invoice.id).status
                logger.info("Invoice payment completed for : ${invoice.id}, Invoice status updated to : $sta")
            } else {

                // Can implement some logic to handle this scenario

            }
        }
    }


    private fun repeatTheSchedule(context: JobExecutionContext, ex: NetworkException) {

        val jobDataMap = context.jobDetail.jobDataMap
        var counter = jobDataMap.getIntValue(Constants.COUNTER)
        if ( Constants.RETRY_LIMIT > counter) {
            jobDataMap[Constants.COUNTER] = ++counter
            context.scheduler.scheduleJob(JobUtils().buildJobRetryTrigger(context))

        } else {
            throw JobExecutionException("Retry Counter Exceeded", ex).apply { setUnscheduleAllTriggers(true) }

        }
    }


    private fun handleCurrencyMismatchException(context: JobExecutionContext,
                                                ex: CurrencyMismatchException,
                                                invoiceService: InvoiceService) {

        val customerService = context.scheduler.context[Constants.CUSTOMER_SERVICE] as CustomerService
        val currencyConvertor = context.scheduler.context[Constants.CURRENCY_CONVERTOR] as CurrencyConvertor
        val invoice = invoiceService.fetch(context.jobDetail.jobDataMap.getIntValue(Constants.INVOICE_ID))

        val customer = customerService.fetch(invoice.customerId)
        val convertedValue = currencyConvertor.convert( invoice.amount.currency, customer.currency, invoice.amount.value)
        invoiceService.update(  invoice.copy(amount = Money(convertedValue, customer.currency)))

        throw JobExecutionException("Error due to currency mismatch. $ex").apply { setRefireImmediately(true) }
    }
}