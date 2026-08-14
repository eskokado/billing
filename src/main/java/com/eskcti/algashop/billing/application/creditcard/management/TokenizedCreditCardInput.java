package com.eskcti.algashop.billing.application.creditcard.management;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class TokenizedCreditCardInput {
    private UUID customerId;
    @NotBlank
    private String tokenizedCard;
}
