package com.dozernet.module2_booking.web;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.module2_booking.entity.BookingStatus;
import com.dozernet.module2_booking.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin booking review: list requests and approve/reject them.
 */
@Controller
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/admin/bookings")
    public String list(@RequestParam(required = false) BookingStatus status, Model model) {
        model.addAttribute("bookings",
                status == null ? bookingService.all() : bookingService.byStatus(status));
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);
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
}
