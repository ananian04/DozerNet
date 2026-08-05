package com.dozernet.module6_payment.entity;

/**
 * Payment methods. CARD is a demo online portal method (no real gateway).
 */
public enum PaymentMethod {
    CASH("Cash"),
    BANK_TRANSFER("Bank transfer"),
    CARD("Card (demo)");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
