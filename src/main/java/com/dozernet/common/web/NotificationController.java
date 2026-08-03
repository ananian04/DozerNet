package com.dozernet.common.web;

import com.dozernet.common.notification.NotificationService;
import com.dozernet.common.security.CurrentUserService;
import com.dozernet.common.user.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Shared, authenticated notification centre available to every logged-in role.
 */
@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService,
                                  CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/notifications")
    public String list(Model model) {
        User user = currentUserService.require();
        model.addAttribute("notifications", notificationService.forUser(user));
        return "notifications/list";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead() {
        notificationService.markAllRead(currentUserService.require());
        return "redirect:/notifications";
    }
}
