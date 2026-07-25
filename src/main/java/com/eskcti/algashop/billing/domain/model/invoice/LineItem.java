package com.eskcti.algashop.billing.domain.model.invoice;

import java.math.BigDecimal;
import java.util.Objects;

import com.eskcti.algashop.billing.domain.model.FieldValidations;

import jakarta.persistence.Embeddable;
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
@Embeddable
public class LineItem {
  private Integer number;
  private String name;
  private BigDecimal amount;

  @Builder
  public LineItem(Integer number, String name, BigDecimal amount) {
    FieldValidations.requiresNonBlank(name);
    Objects.requireNonNull(number);
    Objects.requireNonNull(amount);

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException();
    }

    if (number <= 0) {
      throw new IllegalArgumentException();
    }

    this.number = number;
    this.name = name;
    this.amount = amount;
  }
}