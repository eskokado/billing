package com.eskcti.algashop.billing.infrastructure.payment.fastpay.webhook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FastpayPaymentWebhookEvent {
    @NotBlank
    private String paymentId;
    @NotBlank
    private String referenceCode;
    @NotBlank
    private String status;
    @NotBlank
    private String method;
    @NotNull
    private OffsetDateTime notifiedAt;
}
