package io.pleo.antaeus.core.utils

import io.pleo.antaeus.core.tasks.BillServiceExecutor
import io.pleo.antaeus.models.Invoice
import org.quartz.*

class JobUtils {

    fun buildJobTrigger(invoice: Invoice, cronSchedule: String): CronTrigger = TriggerBuilder
            .newTrigger()
            .withDescription("Invoice Payment Trigger")
            .withIdentity("trigger_invoice_" + invoice.id)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronSchedule))
            .build()


    fun buildJobDetail(invoice: Invoice): JobDetail = JobBuilder
            .newJob()
            .withDescription("Invoice Payment Job")
            .ofType(BillServiceExecutor::class.java)
            .withIdentity("job_invoice_" + invoice.id)
            .usingJobData(Constants.INVOICE_ID, invoice.id)
            .build()


    fun buildJobRetryTrigger(context: JobExecutionContext): SimpleTrigger = TriggerBuilder
            .newTrigger()
            .withDescription("Invoice Payment Retry Trigger")
            .withIdentity(context.jobDetail.key.name, context.jobDetail.key.group)
            .withSchedule(SimpleScheduleBuilder
                    .simpleSchedule()
                    .withRepeatCount(0)
                    .withIntervalInMinutes(Constants.RETRY_DELAY))
            .build()

}