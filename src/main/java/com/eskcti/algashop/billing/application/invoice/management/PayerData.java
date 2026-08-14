package com.eskcti.algashop.billing.application.invoice.management;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayerData {
  @NotBlank(message = "Full name is required")
  private String fullName;

  @NotBlank(message = "Document is required")
  private String document;

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  private String email;

  @NotBlank(message = "Phone is required")
  private String phone;

  @NotNull(message = "Address is required")
  @Valid
  private AddressData address;
}
