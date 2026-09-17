package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastpayPaymentModel {
    private String id;
    private String referenceCode;
    private String status;
    private String method;
    private BigDecimal totalAmount;
}
