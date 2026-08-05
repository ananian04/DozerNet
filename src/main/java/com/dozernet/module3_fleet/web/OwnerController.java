package com.dozernet.module3_fleet.web;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.model.Role;
import com.dozernet.common.security.CurrentUserService;
import com.dozernet.common.user.AccountService;
import com.dozernet.common.user.User;
import com.dozernet.module3_fleet.dto.MachineForm;
import com.dozernet.module3_fleet.dto.OwnerRegisterForm;
import com.dozernet.module3_fleet.entity.Machine;
import com.dozernet.module3_fleet.entity.MachineType;
import com.dozernet.module3_fleet.service.FleetService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Private owner area: self-registration plus listing management, and the admin
 * verifies these listings before they go public.
 */
@Controller
public class OwnerController {

    private final FleetService fleetService;
    private final AccountService accountService;
    private final CurrentUserService currentUserService;

    public OwnerController(FleetService fleetService,
                           AccountService accountService,
                           CurrentUserService currentUserService) {
        this.fleetService = fleetService;
        this.accountService = accountService;
        this.currentUserService = currentUserService;
    }

    // ---------- Public owner registration ----------

    @GetMapping("/register/owner")
    public String showRegister(Model model) {
        if (!model.containsAttribute("ownerForm")) {
            model.addAttribute("ownerForm", new OwnerRegisterForm());
        }
        return "fleet/owner-register";
    }

    @PostMapping("/register/owner")
    public String register(@Valid @ModelAttribute("ownerForm") OwnerRegisterForm form,
                           BindingResult binding) {
        if (binding.hasErrors()) {
            return "fleet/owner-register";
        }
        try {
            accountService.createAccount(form.getFullName(), form.getEmail(), form.getPhone(),
                    form.getIdentityCardNumber(),
                    form.getPassword(), form.getConfirmPassword(), Role.OWNER, true);
        } catch (BusinessRuleException ex) {
            binding.reject("registration", ex.getMessage());
            return "fleet/owner-register";
        }
        return "redirect:/login?registered";
    }

    // ---------- Owner dashboard / listings ----------

    @GetMapping("/owner/dashboard")
    public String dashboard(Model model) {
        User owner = currentUserService.require();
        model.addAttribute("machines", fleetService.findByOwner(owner));
        model.addAttribute("owner", owner);
        return "fleet/owner-dashboard";
    }

    @GetMapping("/owner/machines/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("machineForm")) {
            model.addAttribute("machineForm", new MachineForm());
        }
        model.addAttribute("types", MachineType.values());
        model.addAttribute("mode", "new");
        return "fleet/owner-machine-form";
    }

    @PostMapping("/owner/machines/new")
    public String create(@Valid @ModelAttribute("machineForm") MachineForm form,
                         BindingResult binding, Model model, RedirectAttributes ra) {
        if (binding.hasErrors()) {
            model.addAttribute("types", MachineType.values());
            model.addAttribute("mode", "new");
            return "fleet/owner-machine-form";
        }
        try {
            fleetService.createOwnerListing(currentUserService.require(), form);
            ra.addFlashAttribute("success", "Listing submitted. It will be visible once an admin approves it.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/owner/dashboard";
    }

    @GetMapping("/owner/machines/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Machine m = fleetService.requireOwnedBy(id, currentUserService.require());
        if (!model.containsAttribute("machineForm")) {
            model.addAttribute("machineForm", MachineForm.from(m));
        }
        model.addAttribute("types", MachineType.values());
        model.addAttribute("mode", "edit");
        model.addAttribute("machineId", id);
        return "fleet/owner-machine-form";
    }

    @PostMapping("/owner/machines/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("machineForm") MachineForm form,
                         BindingResult binding, Model model, RedirectAttributes ra) {
        fleetService.requireOwnedBy(id, currentUserService.require());
        if (binding.hasErrors()) {
            model.addAttribute("types", MachineType.values());
            model.addAttribute("mode", "edit");
            model.addAttribute("machineId", id);
            return "fleet/owner-machine-form";
        }
        try {
            fleetService.update(id, form);
            ra.addFlashAttribute("success", "Listing updated.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/owner/dashboard";
    }

    @PostMapping("/owner/machines/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            fleetService.requireOwnedBy(id, currentUserService.require());
            fleetService.delete(id);
            ra.addFlashAttribute("success", "Listing removed.");
        } catch (BusinessRuleException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/owner/dashboard";
    }
}
