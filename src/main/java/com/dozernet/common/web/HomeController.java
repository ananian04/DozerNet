package com.dozernet.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public marketing pages (the "wow-factor" area). These are open to everyone.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String landing() {
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
