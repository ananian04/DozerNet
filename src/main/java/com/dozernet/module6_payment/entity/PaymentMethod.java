package com.dozernet.module6_payment.entity;

/**
 * Supported offline payment methods (no online gateway in this phase).
 */
public enum PaymentMethod {
    CASH("Cash"),
    BANK_TRANSFER("Bank transfer");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
