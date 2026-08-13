package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import com.eskcti.algashop.billing.domain.model.invoice.PaymentMethod;
import com.eskcti.algashop.billing.domain.model.invoice.payment.PaymentStatus;

class FastpayEnumConverterTest {

    @Test
    void shouldInvokePrivateConstructorForCoverage() throws Exception {
        Constructor<FastpayEnumConverter> constructor = FastpayEnumConverter.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatCode(() -> {
            try {
                constructor.newInstance();
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldConvertCreditMethodToCreditCard() {
        PaymentMethod result = FastpayEnumConverter.convert(FastpayPaymentMethod.CREDIT);
        assertThat(result).isEqualTo(PaymentMethod.CREDIT_CARD);
    }

    @Test
    void shouldConvertGatewayBalanceMethod() {
        PaymentMethod result = FastpayEnumConverter.convert(FastpayPaymentMethod.GATEWAY_BALANCE);
        assertThat(result).isEqualTo(PaymentMethod.GATEWAY_BALANCE);
    }

    @Test
    void shouldConvertPendingStatus() {
        PaymentStatus result = FastpayEnumConverter.convert(FastpayPaymentStatus.PENDING);
        assertThat(result).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldConvertProcessingStatus() {
        PaymentStatus result = FastpayEnumConverter.convert(FastpayPaymentStatus.PROCESSING);
        assertThat(result).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    void shouldConvertFailedStatus() {
        PaymentStatus result = FastpayEnumConverter.convert(FastpayPaymentStatus.FAILED);
        assertThat(result).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldConvertPaidStatus() {
        PaymentStatus result = FastpayEnumConverter.convert(FastpayPaymentStatus.PAID);
        assertThat(result).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void shouldConvertCanceledStatusToFailed() {
        PaymentStatus result = FastpayEnumConverter.convert(FastpayPaymentStatus.CANCELED);
        assertThat(result).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void shouldConvertRefundedStatus() {
        PaymentStatus result = FastpayEnumConverter.convert(FastpayPaymentStatus.REFUNDED);
        assertThat(result).isEqualTo(PaymentStatus.REFUNDED);
    }
}
