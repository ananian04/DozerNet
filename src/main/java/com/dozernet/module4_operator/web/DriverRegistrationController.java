package com.dozernet.module4_operator.web;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.module4_operator.dto.DriverRegisterForm;
import com.dozernet.module4_operator.service.OperatorService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Public self-registration for independent drivers/operators.
 */
@Controller
public class DriverRegistrationController {

    private final OperatorService operatorService;

    public DriverRegistrationController(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    @GetMapping("/register/driver")
    public String showForm(Model model) {
        if (!model.containsAttribute("driverForm")) {
            model.addAttribute("driverForm", new DriverRegisterForm());
        }
        return "operator/driver-register";
    }

    @PostMapping("/register/driver")
    public String register(@Valid @ModelAttribute("driverForm") DriverRegisterForm form,
                           BindingResult binding) {
        if (binding.hasErrors()) {
            return "operator/driver-register";
        }
        try {
            operatorService.registerDriver(form);
        } catch (BusinessRuleException ex) {
            binding.reject("registration", ex.getMessage());
            return "operator/driver-register";
        }
        return "redirect:/login?registered";
    }
}
