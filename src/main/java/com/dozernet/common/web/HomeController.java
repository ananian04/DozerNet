package com.dozernet.common.web;

import com.dozernet.module3_fleet.service.FleetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public marketing pages (the "wow-factor" area). These are open to everyone.
 */
@Controller
public class HomeController {

    private final FleetService fleetService;

    public HomeController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping("/")
    public String landing(Model model) {
        model.addAttribute("availableMachines", fleetService.search(null, null).size());
        return "public/landing";
    }

    @GetMapping("/about")
    public String about() {
        return "public/about";
    }

    @GetMapping("/how-it-works")
    public String howItWorks() {
        return "public/how-it-works";
    }
}
