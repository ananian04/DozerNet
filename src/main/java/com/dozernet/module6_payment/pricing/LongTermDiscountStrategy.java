package com.dozernet.module6_payment.pricing;

import com.dozernet.module2_booking.entity.Booking;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Rewards longer rentals: a 10% discount for bookings of 7 days or more.
 * Ordered before the standard strategy so it is chosen when it applies.
 */
@Component
@Order(10)
public class LongTermDiscountStrategy implements PricingStrategy {

    private static final long MIN_DAYS = 7;
    private static final BigDecimal RATE_AFTER_DISCOUNT = new BigDecimal("0.90");

    @Override
    public boolean applies(Booking booking) {
        return booking.getDays() >= MIN_DAYS;
    }

    @Override
    public BigDecimal calculate(Booking booking) {
        BigDecimal base = booking.getMachine().getDailyRate().multiply(BigDecimal.valueOf(booking.getDays()));
        return base.multiply(RATE_AFTER_DISCOUNT).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String label() {
        return "Long-term (10% off, 7+ days)";
    }
}
