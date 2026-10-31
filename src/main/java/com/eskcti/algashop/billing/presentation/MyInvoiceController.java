package com.eskcti.algashop.billing.presentation;

import com.eskcti.algashop.billing.application.invoice.query.InvoiceOutput;
import com.eskcti.algashop.billing.application.invoice.query.InvoiceQueryService;
import com.eskcti.algashop.billing.application.security.SecurityChecks;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.eskcti.algashop.billing.infrastructure.security.SecurityAnnotations.CanReadMyInvoices;

@RestController
@RequestMapping("/api/v1/customers/me/orders/{orderId}/invoice")
@RequiredArgsConstructor
public class MyInvoiceController {

    private final InvoiceQueryService invoiceQueryService;
    private final SecurityChecks securityChecks;

    @GetMapping
    @CanReadMyInvoices
    public InvoiceOutput findByOrder(@PathVariable String orderId) {
        return invoiceQueryService.findByOrderIdAndCustomerId(orderId, securityChecks.getAuthenticatedUserId());
    }

}
