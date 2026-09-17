package com.eskcti.algashop.billing.application.creditcard.management;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenizedCreditCardInput {
    private UUID customerId;
    @NotBlank
    private String tokenizedCard;
}
