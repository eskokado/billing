package com.eskcti.algashop.billing.application.invoice.management;

import com.eskcti.algashop.billing.domain.model.DomainException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardNotFoundException;
import com.eskcti.algashop.billing.domain.model.creditcard.CreditCardRepository;
import com.eskcti.algashop.billing.domain.model.invoice.Invoice;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceNotFoundException;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceRepository;
import com.eskcti.algashop.billing.domain.model.invoice.InvoiceStatus;
import com.eskcti.algashop.billing.domain.model.invoice.InvoicingService;
import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.Payment;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentGatewayService;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.eskcti.algashop.billing.application.invoice.management.GenerateInvoiceInputTestDataBuilder.DEFAULT_CREDIT_CARD_ID;
import static com.eskcti.algashop.billing.application.invoice.management.GenerateInvoiceInputTestDataBuilder.DEFAULT_CUSTOMER_ID;
import static com.eskcti.algashop.billing.application.invoice.management.GenerateInvoiceInputTestDataBuilder.SECOND_CUSTOMER_ID;
import static com.eskcti.algashop.billing.domain.model.invoice.InvoiceTestDataBuilder.aPayer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceManagementApplicationServiceTest {

        @Mock
        private PaymentGatewayService paymentGatewayService;

        @Mock
        private InvoicingService invoicingService;

        @Mock
        private InvoiceRepository invoiceRepository;

        @Mock
        private CreditCardRepository creditCardRepository;

        @InjectMocks
        private InvoiceManagementApplicationService service;

        @Test
        void shouldGenerateInvoiceWithGatewayBalance() {
                GenerateInvoiceInput input = new GenerateInvoiceInputTestDataBuilder()
                                .withGatewayBalancePayment()
                                .build();
                Invoice invoice = Invoice.issue(input.getOrderId(), input.getCustomerId(), aPayer(),
                                Set.of(com.eskcti.algashop.billing.domain.model.invoice.LineItem.builder()
                                                .number(1).name("Item 1").amount(new BigDecimal("100.00")).build()));

                when(invoicingService.issue(eq(input.getOrderId()), eq(input.getCustomerId()), any(), any()))
                                .thenReturn(invoice);
                when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

                UUID id = service.generate(input);

                assertThat(id).isEqualTo(invoice.getId());
                verify(invoiceRepository).saveAndFlush(any(Invoice.class));
        }

        @Test
        void shouldGenerateInvoiceWithCreditCard() {
                GenerateInvoiceInput input = new GenerateInvoiceInputTestDataBuilder()
                                .withCreditCardPayment()
                                .build();
                Invoice invoice = Invoice.issue(input.getOrderId(), input.getCustomerId(), aPayer(),
                                Set.of(com.eskcti.algashop.billing.domain.model.invoice.LineItem.builder()
                                                .number(1).name("Item 1").amount(new BigDecimal("100.00")).build()));

                when(creditCardRepository.existsByIdAndCustomerId(DEFAULT_CREDIT_CARD_ID, DEFAULT_CUSTOMER_ID))
                                .thenReturn(true);
                when(invoicingService.issue(eq(input.getOrderId()), eq(input.getCustomerId()), any(), any()))
                                .thenReturn(invoice);
                when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

                UUID id = service.generate(input);

                assertThat(id).isEqualTo(invoice.getId());
        }

        @Test
        void shouldThrowWhenCreditCardBelongsToAnotherCustomer() {
                GenerateInvoiceInput input = new GenerateInvoiceInputTestDataBuilder()
                                .withCustomerId(SECOND_CUSTOMER_ID)
                                .withCreditCardPayment()
                                .build();

                when(creditCardRepository.existsByIdAndCustomerId(DEFAULT_CREDIT_CARD_ID, SECOND_CUSTOMER_ID))
                                .thenReturn(false);

                assertThatThrownBy(() -> service.generate(input))
                                .isInstanceOf(CreditCardNotFoundException.class);

                verify(invoiceRepository, never()).saveAndFlush(any());
        }

        @Test
        void shouldProcessPaymentSuccessfully() {
                UUID invoiceId = UUID.randomUUID();
                Invoice invoice = Invoice.issue("order-1", DEFAULT_CUSTOMER_ID, aPayer(),
                                Set.of(com.eskcti.algashop.billing.domain.model.invoice.LineItem.builder()
                                                .number(1).name("Item").amount(new BigDecimal("50")).build()));
                invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
                org.springframework.test.util.ReflectionTestUtils.setField(invoice, "id", invoiceId);

                Payment payment = Payment.builder()
                                .gatewayCode("pay-abc")
                                .invoiceId(invoice.getId())
                                .status(PaymentStatus.PAID)
                                .method(PaymentMethod.GATEWAY_BALANCE)
                                .build();

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
                when(paymentGatewayService.capture(any())).thenReturn(payment);
                when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

                service.processPayment(invoiceId);

                verify(invoicingService).assignPayment(eq(invoice), eq(payment));
                verify(invoiceRepository).saveAndFlush(invoice);
        }

        @Test
        void shouldCancelInvoiceWhenPaymentCaptureFails() {
                UUID invoiceId = UUID.randomUUID();
                Invoice invoice = Invoice.issue("order-9", DEFAULT_CUSTOMER_ID, aPayer(),
                                Set.of(com.eskcti.algashop.billing.domain.model.invoice.LineItem.builder()
                                                .number(1).name("Item").amount(new BigDecimal("50")).build()));
                invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
                org.springframework.test.util.ReflectionTestUtils.setField(invoice, "id", invoiceId);

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
                when(paymentGatewayService.capture(any())).thenThrow(new RuntimeException("down"));
                when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

                service.processPayment(invoiceId);

                assertThat(invoice.isCanceled()).isTrue();
                verify(invoicingService, never()).assignPayment(any(), any());
        }

        @Test
        void shouldThrowWhenInvoiceNotFoundOnProcessPayment() {
                UUID invoiceId = UUID.randomUUID();
                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.processPayment(invoiceId))
                                .isInstanceOf(InvoiceNotFoundException.class);
        }

        @Test
        void shouldUpdatePaymentStatusToPaid() {
                UUID invoiceId = UUID.randomUUID();
                Invoice invoice = Invoice.issue("order-u", DEFAULT_CUSTOMER_ID, aPayer(),
                                Set.of(com.eskcti.algashop.billing.domain.model.invoice.LineItem.builder()
                                                .number(1).name("Item").amount(new BigDecimal("50")).build()));
                invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
                org.springframework.test.util.ReflectionTestUtils.setField(invoice, "id", invoiceId);

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
                when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

                service.updatePaymentStatus(invoiceId, PaymentStatus.PAID);

                assertThat(invoice.isPaid()).isTrue();
        }

        @Test
        void shouldCancelWhenPaymentStatusIsFailed() {
                UUID invoiceId = UUID.randomUUID();
                Invoice invoice = Invoice.issue("order-f", DEFAULT_CUSTOMER_ID, aPayer(),
                                Set.of(com.eskcti.algashop.billing.domain.model.invoice.LineItem.builder()
                                                .number(1).name("Item").amount(new BigDecimal("50")).build()));
                invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
                org.springframework.test.util.ReflectionTestUtils.setField(invoice, "id", invoiceId);

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
                when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

                service.updatePaymentStatus(invoiceId, PaymentStatus.FAILED);

                assertThat(invoice.isCanceled()).isTrue();
        }

        @Test
        void shouldCancelWhenPaymentStatusIsRefunded() {
                UUID invoiceId = UUID.randomUUID();
                Invoice invoice = Invoice.issue("order-r", DEFAULT_CUSTOMER_ID, aPayer(),
                                Set.of(com.eskcti.algashop.billing.domain.model.invoice.LineItem.builder()
                                                .number(1).name("Item").amount(new BigDecimal("50")).build()));
                invoice.changePaymentSettings(PaymentMethod.GATEWAY_BALANCE, null);
                org.springframework.test.util.ReflectionTestUtils.setField(invoice, "id", invoiceId);

                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
                when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

                service.updatePaymentStatus(invoiceId, PaymentStatus.REFUNDED);

                assertThat(invoice.isCanceled()).isTrue();
                assertThat(invoice.getCancelReason()).isEqualTo("Payment refunded");
        }

        @Test
        void shouldThrowWhenInvoiceNotFoundOnUpdateStatus() {
                UUID invoiceId = UUID.randomUUID();
                when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> service.updatePaymentStatus(invoiceId, PaymentStatus.PAID))
                                .isInstanceOf(InvoiceNotFoundException.class);
        }
}
