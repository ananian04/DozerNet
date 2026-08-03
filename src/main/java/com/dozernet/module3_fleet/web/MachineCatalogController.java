package com.dozernet.module3_fleet.web;

import com.dozernet.module3_fleet.entity.MachineType;
import com.dozernet.module3_fleet.service.FleetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Public machine catalogue: browse, search and view a machine.
 */
@Controller
public class MachineCatalogController {

    private final FleetService fleetService;

    public MachineCatalogController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    @GetMapping("/machines")
    public String browse(@RequestParam(required = false) MachineType type,
                         @RequestParam(required = false) String keyword,
                         Model model) {
        model.addAttribute("machines", fleetService.search(type, keyword));
        model.addAttribute("types", MachineType.values());
        model.addAttribute("selectedType", type);
        model.addAttribute("keyword", keyword);
        return "fleet/catalog";
    }

    @GetMapping("/machines/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("machine", fleetService.getById(id));
        return "fleet/detail";
    }
}
