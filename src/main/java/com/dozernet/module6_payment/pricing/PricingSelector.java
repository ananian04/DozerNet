package com.dozernet.module6_payment.pricing;

import com.dozernet.module2_booking.entity.Booking;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chooses the applicable {@link PricingStrategy} for a booking. Spring injects
 * all strategies in @Order order, so the first that applies wins.
 */
@Component
public class PricingSelector {

    private final List<PricingStrategy> strategies;

    public PricingSelector(List<PricingStrategy> strategies) {
        this.strategies = strategies;
    }

    public PricingStrategy select(Booking booking) {
        return strategies.stream()
                .filter(s -> s.applies(booking))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No pricing strategy available"));
    }

    public BigDecimal price(Booking booking) {
        return select(booking).calculate(booking);
    }
}
