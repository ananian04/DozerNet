package com.dozernet.module6_payment.web;

import com.dozernet.module6_payment.service.AdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Booking / revenue reporting for the administrator.
 */
@Controller
public class AdminReportController {

    private final AdminService adminService;

    public AdminReportController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("report", adminService.revenueReport());
        return "payment/admin-reports";
    }
}
