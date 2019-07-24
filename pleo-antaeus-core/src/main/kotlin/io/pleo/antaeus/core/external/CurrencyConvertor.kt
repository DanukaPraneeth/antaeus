package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Currency
import java.math.BigDecimal

interface CurrencyConvertor {

    /*
   Currency Convertor Service is assumed as an external service that returns the BigDecimal values after currency conversion

   Below I have mocked the service to return the same input

 */

    fun convert(from: Currency, to: Currency, value: BigDecimal) = value
}