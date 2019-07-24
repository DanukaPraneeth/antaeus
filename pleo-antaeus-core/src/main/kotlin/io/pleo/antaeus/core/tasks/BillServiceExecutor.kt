package io.pleo.antaeus.core.tasks

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory

class BillServiceExecutor : Job {

    companion object{
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
    }


    override fun execute(context: JobExecutionContext) {

        val taskContext = context.scheduler.context
        val paymentProvider = taskContext["PaymentProvider"] as PaymentProvider
        val invoiceService = taskContext["InvoiceService"] as InvoiceService


        val invoice = invoiceService.fetch(context.jobDetail.jobDataMap.getIntValue("invoiceId"))
        try {
            logger.debug("Executing the for ${invoice.id} and status ${invoice.status}")
            payCustomerInvoice(invoice, invoiceService, paymentProvider)

            if(invoice.status == InvoiceStatus.PAID)
                context.scheduler.deleteJob(context.jobDetail.key)

        } catch (ex: CustomerNotFoundException) {
            logger.error("Error during periodic bill service. Customer not found for customer Id: ${invoice.id}", ex)
        } catch (ex: CurrencyMismatchException) {
            logger.error("Error during periodic bill service. Currency mismatch for invoice Id: ${invoice.id} , customer Id : ${invoice.customerId}", ex)
        } catch (ex: NetworkException) {
            logger.error("Error during periodic bill service. Network failure ")
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

}