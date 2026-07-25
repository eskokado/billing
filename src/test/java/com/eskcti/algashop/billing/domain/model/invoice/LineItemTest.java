package com.eskcti.algashop.billing.domain.model.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class LineItemTest {

  @Test
  void shouldCreateValidLineItem() {
    LineItem item = validLineItem();

    assertThat(item.getNumber()).isEqualTo(1);
    assertThat(item.getName()).isEqualTo("Product");
    assertThat(item.getAmount()).isEqualByComparingTo("100.00");
  }

  @Test
  void shouldRejectBlankName() {
    assertThatThrownBy(() -> LineItem.builder()
        .number(1)
        .name(" ")
        .amount(new BigDecimal("100.00"))
        .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNonPositiveAmount() {
    assertThatThrownBy(() -> LineItem.builder()
        .number(1)
        .name("Product")
        .amount(BigDecimal.ZERO)
        .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNonPositiveNumber() {
    assertThatThrownBy(() -> LineItem.builder()
        .number(0)
        .name("Product")
        .amount(new BigDecimal("100.00"))
        .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  static LineItem validLineItem() {
    return LineItem.builder()
        .number(1)
        .name("Product")
        .amount(new BigDecimal("100.00"))
        .build();
  }
}
