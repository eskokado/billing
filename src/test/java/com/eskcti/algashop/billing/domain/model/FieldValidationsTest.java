package com.eskcti.algashop.billing.domain.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FieldValidationsTest {

  @Test
  void shouldAcceptNonBlankValue() {
    assertThatCode(() -> FieldValidations.requiresNonBlank("value"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectNullValueOnRequiresNonBlank() {
    assertThatThrownBy(() -> FieldValidations.requiresNonBlank(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankValueOnRequiresNonBlank() {
    assertThatThrownBy(() -> FieldValidations.requiresNonBlank("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectBlankValueWithCustomMessage() {
    assertThatThrownBy(() -> FieldValidations.requiresNonBlank("   ", "custom error"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("custom error");
  }

  @Test
  void shouldAcceptValidEmail() {
    assertThatCode(() -> FieldValidations.requiresValidEmail("user@example.com"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectNullEmail() {
    assertThatThrownBy(() -> FieldValidations.requiresValidEmail(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRejectBlankEmail() {
    assertThatThrownBy(() -> FieldValidations.requiresValidEmail("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectInvalidEmail() {
    assertThatThrownBy(() -> FieldValidations.requiresValidEmail("invalid-email"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectInvalidEmailWithCustomMessage() {
    assertThatThrownBy(() -> FieldValidations.requiresValidEmail("invalid-email", "invalid email"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("invalid email");
  }
}
