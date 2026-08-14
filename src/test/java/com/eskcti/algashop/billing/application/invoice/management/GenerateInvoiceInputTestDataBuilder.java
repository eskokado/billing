package com.eskcti.algashop.billing.application.invoice.management;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;

public class GenerateInvoiceInputTestDataBuilder {

  public static final UUID DEFAULT_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  public static final UUID SECOND_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  public static final UUID DEFAULT_CREDIT_CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
  public static final String DEFAULT_ORDER_ID = "ORD-0001";
  public static final String SECOND_ORDER_ID = "ORD-0002";
  public static final String DEFAULT_FULL_NAME = "John Doe";
  public static final String DEFAULT_DOCUMENT = "111.222.333-44";
  public static final String DEFAULT_EMAIL = "john.doe@email.com";
  public static final String DEFAULT_PHONE = "11-99999-8888";
  public static final String DEFAULT_STREET = "Street Name";
  public static final String DEFAULT_NUMBER = "123";
  public static final String DEFAULT_NEIGHBORHOOD = "Neighborhood";
  public static final String DEFAULT_CITY = "City";
  public static final String DEFAULT_STATE = "State";
  public static final String DEFAULT_ZIP_CODE = "12345-678";

  private final GenerateInvoiceInput.GenerateInvoiceInputBuilder builder;

  public GenerateInvoiceInputTestDataBuilder() {
    this.builder = anInput();
  }

  public GenerateInvoiceInputTestDataBuilder withCustomerId(UUID customerId) {
    builder.customerId(customerId);
    return this;
  }

  public GenerateInvoiceInputTestDataBuilder withOrderId(String orderId) {
    builder.orderId(orderId);
    return this;
  }

  public GenerateInvoiceInputTestDataBuilder withCreditCardPayment() {
    builder.paymentSettings(PaymentSettingsInput.builder()
        .method(PaymentMethod.CREDIT_CARD)
        .creditCardId(DEFAULT_CREDIT_CARD_ID)
        .build());
    return this;
  }

  public GenerateInvoiceInputTestDataBuilder withGatewayBalancePayment() {
    builder.paymentSettings(PaymentSettingsInput.builder()
        .method(PaymentMethod.GATEWAY_BALANCE)
        .build());
    return this;
  }

  public GenerateInvoiceInputTestDataBuilder withCreditCardId(UUID creditCardId) {
    PaymentSettingsInput current = builder.build().getPaymentSettings();
    builder.paymentSettings(PaymentSettingsInput.builder()
        .method(PaymentMethod.CREDIT_CARD)
        .creditCardId(creditCardId)
        .build());
    return this;
  }

  public GenerateInvoiceInput build() {
    return builder.build();
  }

  public static GenerateInvoiceInput.GenerateInvoiceInputBuilder anInput() {
    return GenerateInvoiceInput.builder()
        .orderId(DEFAULT_ORDER_ID)
        .customerId(DEFAULT_CUSTOMER_ID)
        .paymentSettings(PaymentSettingsInput.builder()
            .method(PaymentMethod.CREDIT_CARD)
            .creditCardId(DEFAULT_CREDIT_CARD_ID)
            .build())
        .payer(PayerData.builder()
            .fullName(DEFAULT_FULL_NAME)
            .document(DEFAULT_DOCUMENT)
            .phone(DEFAULT_PHONE)
            .email(DEFAULT_EMAIL)
            .address(AddressData.builder()
                .street(DEFAULT_STREET)
                .number(DEFAULT_NUMBER)
                .neighborhood(DEFAULT_NEIGHBORHOOD)
                .city(DEFAULT_CITY)
                .state(DEFAULT_STATE)
                .zipCode(DEFAULT_ZIP_CODE)
                .build())
            .build())
        .items(Set.of(LineItemInput.builder()
            .name("Product 1")
            .amount(new BigDecimal("200.00"))
            .build()));
  }
}
