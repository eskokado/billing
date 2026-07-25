package com.eskcti.algashop.billing.infrastructure.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.eskcti.algashop.billing.domain.model.invoice.InvoiceCanceledEvent;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceIssuedEvent;
import com.eskcti.algashop.billing.domain.model.invoice.InvoicePaidEvent;

@Component
public class InvoiceEventListener {

  @EventListener
  public void listen(InvoiceIssuedEvent event) {

  }

  @EventListener
  public void listen(InvoiceCanceledEvent event) {

  }

  @EventListener
  public void listen(InvoicePaidEvent event) {

  }

}