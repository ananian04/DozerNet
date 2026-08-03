package com.dozernet.module2_booking.entity;

/**
 * Lifecycle of a booking request.
 * PENDING -> APPROVED/REJECTED (admin) ; PENDING -> CANCELLED (customer) ;
 * APPROVED -> COMPLETED (set when the assigned operator finishes the job).
 */
public enum BookingStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    private final String displayName;

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Statuses that occupy a machine's calendar (used for overlap checks). */
    public boolean blocksCalendar() {
        return this == PENDING || this == APPROVED;
    }
}
