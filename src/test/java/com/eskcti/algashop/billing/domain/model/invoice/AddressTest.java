package com.eskcti.algashop.billing.domain.model.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AddressTest {

  @Test
  void shouldCreateValidAddress() {
    Address address = validAddress();

    assertThat(address.getStreet()).isEqualTo("Rua A");
    assertThat(address.getNumber()).isEqualTo("123");
    assertThat(address.getCity()).isEqualTo("São Paulo");
    assertThat(address.getState()).isEqualTo("SP");
    assertThat(address.getZipCode()).isEqualTo("01000-000");
  }

  @Test
  void shouldRejectBlankRequiredFields() {
    assertThatThrownBy(() -> Address.builder()
        .street(" ")
        .number("123")
        .neighborhood("Centro")
        .city("São Paulo")
        .state("SP")
        .zipCode("01000-000")
        .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  static Address validAddress() {
    return Address.builder()
        .street("Rua A")
        .number("123")
        .neighborhood("Centro")
        .city("São Paulo")
        .state("SP")
        .zipCode("01000-000")
        .build();
  }
}
