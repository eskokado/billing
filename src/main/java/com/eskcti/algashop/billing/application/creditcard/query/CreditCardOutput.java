package com.eskcti.algashop.billing.application.creditcard.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardOutput {
    private UUID id;
    private String lastNumbers;
    private Integer expMonth;
    private Integer expYear;
    private String brand;
}
