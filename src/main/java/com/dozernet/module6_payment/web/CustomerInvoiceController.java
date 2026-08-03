package com.dozernet.module6_payment.web;

import com.dozernet.common.exception.BusinessRuleException;
import com.dozernet.common.security.CurrentUserService;
import com.dozernet.module6_payment.entity.Invoice;
import com.dozernet.module6_payment.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Customer-facing invoices: list and view own invoices with payment history.
 */
@Controller
public class CustomerInvoiceController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public CustomerInvoiceController(PaymentService paymentService,
                                     CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/customer/invoices")
    public String list(Model model) {
        model.addAttribute("invoices",
                paymentService.invoicesForCustomer(currentUserService.require()));
        return "payment/customer-invoices";
    }

    @GetMapping("/customer/invoices/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Invoice invoice = paymentService.getInvoice(id);
        if (!invoice.getCustomer().getId().equals(currentUserService.require().getId())) {
            throw new BusinessRuleException("You can only view your own invoices.");
        }
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", paymentService.paymentsFor(invoice));
        return "payment/customer-invoice-detail";
    }
}
