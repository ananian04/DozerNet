package com.dozernet.module2_booking.web;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.module2_booking.entity.Booking;
import com.dozernet.module2_booking.entity.BookingStatus;
import com.dozernet.module2_booking.service.BookingService;
import com.dozernet.module6_payment.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Admin booking review: list requests and approve/reject them.
 */
@Controller
public class AdminBookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    public AdminBookingController(BookingService bookingService, PaymentService paymentService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    @GetMapping("/admin/bookings")
    public String list(@RequestParam(required = false) BookingStatus status, Model model) {
        List<Booking> bookings = status == null ? bookingService.all() : bookingService.byStatus(status);
        Set<Long> cancellableUnpaid = new HashSet<>();
        for (Booking b : bookings) {
            if (b.getStatus() == BookingStatus.APPROVED && !paymentService.isInvoicePaidForBooking(b)) {
                cancellableUnpaid.add(b.getId());
            }
        }
        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("cancellableUnpaid", cancellableUnpaid);
        return "booking/admin-bookings";
    }

    @PostMapping("/admin/bookings/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        try {
            bookingService.approve(id);
            ra.addFlashAttribute("success", "Booking approved. Invoice issued — customer notified to pay.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/admin/bookings/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes ra) {
        try {
            bookingService.reject(id);
            ra.addFlashAttribute("success", "Booking rejected.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/admin/bookings/{id}/cancel")
    public String cancelUnpaid(@PathVariable Long id, RedirectAttributes ra) {
        try {
            bookingService.cancelUnpaidApproved(id);
            ra.addFlashAttribute("success", "Unpaid booking cancelled. Machine dates are free again.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }
}
