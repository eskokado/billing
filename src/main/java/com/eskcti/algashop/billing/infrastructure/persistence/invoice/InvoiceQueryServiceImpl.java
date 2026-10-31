package com.eskcti.algashop.billing.infrastructure.persistence.invoice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eskcti.algashop.billing.application.invoice.query.InvoiceOutput;
import com.eskcti.algashop.billing.application.invoice.query.InvoiceQueryService;
import com.eskcti.algashop.billing.application.utility.Mapper;
import com.eskcti.algashop.billing.domain.model.invoice.Invoice;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceRepository;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InvoiceQueryServiceImpl implements InvoiceQueryService {

  private final InvoiceRepository invoiceRepository;
  private final Mapper mapper;

  @Override
  public InvoiceOutput findByOrderId(String orderId) {
    Invoice invoice = invoiceRepository.findByOrderId(orderId).orElseThrow(() -> new InvoiceNotFoundException());
    return mapper.convert(invoice, InvoiceOutput.class);
  }

  @Override
  public InvoiceOutput findByOrderIdAndCustomerId(String orderId, UUID customerId) {
    Invoice invoice = invoiceRepository.findByOrderIdAndCustomerId(orderId, customerId)
        .orElseThrow(() -> new InvoiceNotFoundException());
    return mapper.convert(invoice, InvoiceOutput.class);
  }
}