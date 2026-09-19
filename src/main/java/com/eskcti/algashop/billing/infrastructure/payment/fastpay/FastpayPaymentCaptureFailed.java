package com.eskcti.algashop.billing.infrastructure.payment.fastpay;

public class FastpayPaymentCaptureFailed extends RuntimeException {
    public FastpayPaymentCaptureFailed() {
    }

    public FastpayPaymentCaptureFailed(String message) {
        super(message);
    }

    public FastpayPaymentCaptureFailed(String message, Throwable cause) {
        super(message, cause);
    }
}
