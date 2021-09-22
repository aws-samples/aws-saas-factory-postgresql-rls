/**
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazon.aws.partners.saasfactory.pgrls.controller;

import com.amazon.aws.partners.saasfactory.pgrls.domain.Status;
import com.amazon.aws.partners.saasfactory.pgrls.domain.Tenant;
import com.amazon.aws.partners.saasfactory.pgrls.domain.Tier;
import com.amazon.aws.partners.saasfactory.pgrls.repository.UniqueRecordException;
import com.amazon.aws.partners.saasfactory.pgrls.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
public class AdminController {

    private final static Logger LOGGER = LoggerFactory.getLogger(RootController.class);

    @Autowired
    private AdminService adminService;

    @GetMapping("/admin/cancel")
    public String cancel() {
        return "redirect:/admin";
    }

    @GetMapping("/admin")
    public String index(Model model) {
        // Load the list of tenants at the top of the view as the SaaS administrator (no RLS policies
        // applied because the database admin user is the table owner)
        List<Tenant> tenants = adminService.getTenants();
        model.addAttribute("tenants", tenants);
        return "admin";
    }

    @GetMapping("/admin/newTenant")
    public String newTenant(Model model) {
        Tenant tenant = new Tenant();
        tenant.setStatus(Status.Active);
        model.addAttribute("tenant", tenant);
        model.addAttribute("statuses", Status.values());
        model.addAttribute("tiers", Tier.values());
        return "editTenant";
    }

    @GetMapping("/admin/updateTenant")
    public String editTenant(@RequestParam("id") String id, Model model) {
        Tenant tenant = adminService.getTenant(UUID.fromString(id));
        if (tenant == null) {
            model.addAttribute("css", "danger");
            model.addAttribute("msg", "No tenant for id " + id);
        }
        model.addAttribute("tenant", tenant);
        model.addAttribute("statuses", Status.values());
        model.addAttribute("tiers", Tier.values());
        return "editTenant";
    }

    @PostMapping("/admin/editTenant")
    public String saveTenant(@ModelAttribute Tenant tenant, BindingResult binding, Model model, final RedirectAttributes redirectAttributes) {
        String view = null;
        if (tenant.getName() == null || tenant.getName().isEmpty()) {
            binding.addError(new FieldError("tenant", "name", "Tenant name is required"));
            view = "editTenant";
        } else {
            try {
                boolean isNew = (tenant.getId() == null);
                adminService.saveTenant(tenant);
                redirectAttributes.addFlashAttribute("css", "success");
                if (isNew) {
                    redirectAttributes.addFlashAttribute("msg", "New tenant added");
                } else {
                    redirectAttributes.addFlashAttribute("msg", "Tenant updated");
                }
                view = "redirect:/admin";
            } catch (UniqueRecordException e) {
                binding.addError(new FieldError("tenant", "name", "Tenant already exists"));
                view = "editTenant";
            }
        }
        model.addAttribute("statuses", Status.values());
        model.addAttribute("tiers", Tier.values());
        return view;
    }

    @GetMapping("/admin/deleteTenant")
    public String deleteTenantConfirm(@RequestParam("id") String id, Model model) {
        Tenant tenant = adminService.getTenant(UUID.fromString(id));
        if (tenant == null) {
            model.addAttribute("css", "danger");
            model.addAttribute("msg", "No tenant for id " + id);
        }
        model.addAttribute("tenant", tenant);
        return "deleteTenant";
    }

    @PostMapping("/admin/deleteTenant")
    public String deleteTenant(@ModelAttribute Tenant tenant, BindingResult binding, Model model, final RedirectAttributes redirectAttributes) {
        String view = null;
        try {
            adminService.deleteTenant(tenant);
            redirectAttributes.addFlashAttribute("css", "success");
            redirectAttributes.addFlashAttribute("msg", "Tenant deleted");
            view = "redirect:/admin";
        } catch (Exception e) {
            model.addAttribute("css", "danger");
            model.addAttribute("msg", "Failed to delete tenant: " + e.getMessage());
            view = "deleteTenant";
        }
        return view;
    }
}
