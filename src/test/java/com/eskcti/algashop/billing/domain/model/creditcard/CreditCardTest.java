package com.eskcti.algashop.billing.domain.model.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CreditCardTest {

  @Test
  void shouldCreateValidCreditCard() {
    UUID customerId = UUID.randomUUID();

    CreditCard creditCard = CreditCard.brandNew(
        customerId,
        "1234",
        "visa",
        12,
        2030,
        "gateway-code");

    assertThat(creditCard.getId()).isNotNull();
    assertThat(creditCard.getCustomerId()).isEqualTo(customerId);
    assertThat(creditCard.getLastNumbers()).isEqualTo("1234");
    assertThat(creditCard.getBrand()).isEqualTo("visa");
    assertThat(creditCard.getExpMonth()).isEqualTo(12);
    assertThat(creditCard.getExpYear()).isEqualTo(2030);
    assertThat(creditCard.getGatewayCode()).isEqualTo("gateway-code");
    assertThat(creditCard.getCreatedAt()).isNotNull();
  }

  @Test
  void shouldRejectBlankFieldsOnBrandNew() {
    assertThatThrownBy(() -> CreditCard.brandNew(
        UUID.randomUUID(),
        " ",
        "visa",
        12,
        2030,
        "gateway-code"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullCustomerIdOnBrandNew() {
    assertThatThrownBy(() -> CreditCard.brandNew(
        null,
        "1234",
        "visa",
        12,
        2030,
        "gateway-code"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldUpdateGatewayCode() {
    CreditCard creditCard = CreditCard.brandNew(
        UUID.randomUUID(),
        "1234",
        "visa",
        12,
        2030,
        "gateway-code");

    creditCard.setGatewayCode("new-gateway-code");

    assertThat(creditCard.getGatewayCode()).isEqualTo("new-gateway-code");
  }

  @Test
  void shouldRejectBlankGatewayCodeOnUpdate() {
    CreditCard creditCard = CreditCard.brandNew(
        UUID.randomUUID(),
        "1234",
        "visa",
        12,
        2030,
        "gateway-code");

    assertThatThrownBy(() -> creditCard.setGatewayCode(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
