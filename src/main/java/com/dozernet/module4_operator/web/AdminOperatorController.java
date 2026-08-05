package com.dozernet.module4_operator.web;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.exception.ResourceNotFoundException;
import com.dozernet.common.user.User;
import com.dozernet.common.user.UserRepository;
import com.dozernet.module4_operator.dto.CompanyOperatorForm;
import com.dozernet.module4_operator.service.OperatorService;
import com.dozernet.module6_payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin operator management: list operators, add company operators, verify
 * driver licences, and assign operators to approved bookings.
 */
@Controller
public class AdminOperatorController {

    private final OperatorService operatorService;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public AdminOperatorController(OperatorService operatorService,
                                   UserRepository userRepository,
                                   PaymentService paymentService) {
        this.operatorService = operatorService;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    // ---------- Operator list + add ----------

    @GetMapping("/admin/operators")
    public String list(Model model) {
        model.addAttribute("operators", operatorService.allOperators());
        model.addAttribute("pending", operatorService.pendingVerifications());
        if (!model.containsAttribute("operatorForm")) {
            model.addAttribute("operatorForm", new CompanyOperatorForm());
        }
        return "operator/admin-operators";
    }

    @PostMapping("/admin/operators/new")
    public String addCompanyOperator(@Valid @ModelAttribute("operatorForm") CompanyOperatorForm form,
                                     BindingResult binding, Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("operators", operatorService.allOperators());
            model.addAttribute("pending", operatorService.pendingVerifications());
            return "operator/admin-operators";
        }
        try {
            operatorService.addCompanyOperator(form);
            ra.addFlashAttribute("success", "Company operator added.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/operators";
    }

    @PostMapping("/admin/operators/{id}/verify")
    public String verify(@PathVariable Long id, RedirectAttributes ra) {
        operatorService.verify(id);
        ra.addFlashAttribute("success", "Operator verified.");
        return "redirect:/admin/operators";
    }

    // ---------- Assignments ----------

    @GetMapping("/admin/assignments")
    public String assignments(Model model) {
        var bookings = operatorService.unassignedApprovedBookings();
        var ready = operatorService.paidBookingsAwaitingOperator();
        model.addAttribute("bookings", bookings);
        model.addAttribute("readyToAssign", ready);
        model.addAttribute("operators", operatorService.verifiedOperators());
        model.addAttribute("paymentLabels", paymentService.paymentLabelsForBookings(bookings));
        return "operator/admin-assignments";
    }

    @PostMapping("/admin/assignments/assign")
    public String assign(@RequestParam Long bookingId,
                         @RequestParam Long operatorId,
                         RedirectAttributes ra) {
        try {
            User operator = userRepository.findById(operatorId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Operator", operatorId));
            operatorService.assign(bookingId, operator);
            ra.addFlashAttribute("success", "Operator assigned to the booking.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/assignments";
    }
}
