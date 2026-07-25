package com.eskcti.algashop.billing.domain.model.invoice;

import java.util.Objects;

import com.eskcti.algashop.billing.domain.model.FieldValidations;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payer {
  private String fullName;
  private String document;
  private String phone;
  private String email;
  private Address address;

  @Builder
  public Payer(String fullName, String document, String phone, String email, Address address) {
    FieldValidations.requiresNonBlank(fullName);
    FieldValidations.requiresNonBlank(document);
    FieldValidations.requiresNonBlank(phone);
    FieldValidations.requiresValidEmail(email);
    Objects.requireNonNull(address);
    this.fullName = fullName;
    this.document = document;
    this.phone = phone;
    this.email = email;
    this.address = address;
  }
}