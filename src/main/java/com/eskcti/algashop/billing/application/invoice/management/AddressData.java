package com.eskcti.algashop.billing.application.invoice.management;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressData {
  @NotBlank(message = "Street is required")
  private String street;

  @NotBlank(message = "Number is required")
  private String number;

  private String complement;

  @NotBlank(message = "Neighborhood is required")
  private String neighborhood;

  @NotBlank(message = "City is required")
  private String city;

  @NotBlank(message = "State is required")
  private String state;

  @NotBlank(message = "Zip code is required")
  private String zipCode;
}
