package com.dozernet.module2_booking.web;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.security.CurrentUserService;
import com.dozernet.module2_booking.dto.BookingForm;
import com.dozernet.module2_booking.service.BookingService;
import com.dozernet.module3_fleet.service.FleetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Customer-facing booking flow: request a booking, view history, cancel.
 */
@Controller
public class CustomerBookingController {

    private final BookingService bookingService;
    private final FleetService fleetService;
    private final CurrentUserService currentUserService;

    public CustomerBookingController(BookingService bookingService,
                                     FleetService fleetService,
                                     CurrentUserService currentUserService) {
        this.bookingService = bookingService;
        this.fleetService = fleetService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/bookings/new/{machineId}")
    public String newForm(@PathVariable Long machineId, Model model) {
        if (!model.containsAttribute("bookingForm")) {
            model.addAttribute("bookingForm", new BookingForm());
        }
        model.addAttribute("machine", fleetService.getById(machineId));
        return "booking/new";
    }

    @PostMapping("/bookings/new/{machineId}")
    public String create(@PathVariable Long machineId,
                         @Valid @ModelAttribute("bookingForm") BookingForm form,
                         BindingResult binding, Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("machine", fleetService.getById(machineId));
            return "booking/new";
        }
        try {
            bookingService.create(currentUserService.require(), machineId,
                    form.getStartDate(), form.getEndDate());
            ra.addFlashAttribute("success", "Booking request submitted. You will be notified once it is reviewed.");
            return "redirect:/customer/bookings";
        } catch (BusinessRuleException ex) {
            model.addAttribute("machine", fleetService.getById(machineId));
            model.addAttribute("error", ex.getMessage());
            return "booking/new";
        }
    }

    @GetMapping("/customer/bookings")
    public String myBookings(Model model) {
        model.addAttribute("bookings", bookingService.forCustomer(currentUserService.require()));
        return "booking/my-bookings";
    }

    @PostMapping("/bookings/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        try {
            bookingService.cancel(currentUserService.require(), id);
            ra.addFlashAttribute("success", "Booking cancelled.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/customer/bookings";
    }
}
