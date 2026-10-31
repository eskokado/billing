package com.eskcti.algashop.billing.application.invoice.query;

import java.util.UUID;

public interface InvoiceQueryService {
  InvoiceOutput findByOrderId(String orderId);

  InvoiceOutput findByOrderIdAndCustomerId(String orderId, UUID customerId);
}
