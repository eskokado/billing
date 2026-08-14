package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;

@Data
@Builder
public class FastpayPaymentModel {
    private String id;
    private String referenceCode;
    private String status;
    private String method;
    private BigDecimal totalAmount;
}
