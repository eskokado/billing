package com.eskcti.algashop.billing.domain.model.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PayerTest {

  @Test
  void shouldCreateValidPayer() {
    Payer payer = validPayer();

    assertThat(payer.getFullName()).isEqualTo("John Doe");
    assertThat(payer.getDocument()).isEqualTo("12345678901");
    assertThat(payer.getPhone()).isEqualTo("11999999999");
    assertThat(payer.getEmail()).isEqualTo("john@example.com");
    assertThat(payer.getAddress()).isNotNull();
  }

  @Test
  void shouldRejectInvalidEmail() {
    assertThatThrownBy(() -> Payer.builder()
        .fullName("John Doe")
        .document("12345678901")
        .phone("11999999999")
        .email("invalid-email")
        .address(AddressTest.validAddress())
        .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullAddress() {
    assertThatThrownBy(() -> Payer.builder()
        .fullName("John Doe")
        .document("12345678901")
        .phone("11999999999")
        .email("john@example.com")
        .address(null)
        .build())
        .isInstanceOf(NullPointerException.class);
  }

  static Payer validPayer() {
    return Payer.builder()
        .fullName("John Doe")
        .document("12345678901")
        .phone("11999999999")
        .email("john@example.com")
        .address(AddressTest.validAddress())
        .build();
  }
}
