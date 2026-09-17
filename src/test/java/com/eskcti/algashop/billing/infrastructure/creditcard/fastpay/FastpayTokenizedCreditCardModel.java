package com.eskcti.algashop.billing.infrastructure.creditcard.fastpay;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FastpayTokenizedCreditCardModel {
    private String tokenizedCard;
    private OffsetDateTime expiresAt;
}
