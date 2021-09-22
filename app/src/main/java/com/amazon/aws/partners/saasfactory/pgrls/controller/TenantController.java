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

import com.amazon.aws.partners.saasfactory.pgrls.UnauthorizedException;
import com.amazon.aws.partners.saasfactory.pgrls.domain.Tenant;
import com.amazon.aws.partners.saasfactory.pgrls.domain.User;
import com.amazon.aws.partners.saasfactory.pgrls.repository.UniqueRecordException;
import com.amazon.aws.partners.saasfactory.pgrls.service.AdminService;
import com.amazon.aws.partners.saasfactory.pgrls.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.util.UUID;

@Controller
public class TenantController {

    private final static Logger LOGGER = LoggerFactory.getLogger(TenantController.class);

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AdminService adminService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Tenant.class, new TenantEditor());
    }

    @GetMapping("/tenant/cancel")
    public String cancel() {
        return "redirect:/tenant";
    }

    @GetMapping("/tenant")
    public String index(Authentication authentication, Model model) {
        LOGGER.info("Authenticated tenant {}", ((Tenant) authentication.getPrincipal()).getId());
        Tenant tenant = new Tenant();
        if (model.containsAttribute("selectedTenant")) {
            String selectedTenantId = String.valueOf(model.getAttribute("selectedTenant"));
            LOGGER.info("Redirected with existing selected tenant {}", selectedTenantId);
            tenant.setId(selectedTenantId);
        }
        // Provide a list of all tenants so the demo can force cross-tenant access
        model.addAttribute("tenants", adminService.getTenants());
        model.addAttribute("selectedTenant", tenant);
        return "tenant";
    }

    @PostMapping("/tenant")
    public String listUsers(Authentication authentication, @RequestParam String tenantId, Model model) {
        Tenant authenticatedTenant = (Tenant) authentication.getPrincipal();
        Tenant tenant = new Tenant();
        try {
            tenant.setId(UUID.fromString(tenantId));
            try {
                // Load the list of tenant users as the currently logged in tenant.
                // But, ask for the users for a specific tenant id. If the 2 ids don't match,
                // RLS will prevent cross tenant access to the other tenant's resources without
                // having to specify ...WHERE tenant_id = ? in the SQL queries.
                Tenant tenantForEdit = tenantService.getTenant(tenant.getId());
                if (tenantForEdit == null) {
                    LOGGER.info("Database security policies prevented cross tenant access");
                    model.addAttribute("css", "danger");
                    model.addAttribute("msg", "Row Level Security policies prevented " + authenticatedTenant.getId().toString() + " from accessing data for " + tenantId);
                } else {
                    tenant = tenantForEdit;
                }
            } catch (Exception e) {
                model.addAttribute("css", "danger");
                model.addAttribute("msg", e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            model.addAttribute("css", "danger");
            model.addAttribute("msg", "Invalid tenant id");
        }

        model.addAttribute("tenants", adminService.getTenants());
        model.addAttribute("selectedTenant", tenant);
        return "tenant";
    }

    @GetMapping("/tenant/newUser")
    public String newUser(@RequestParam String tenantId, Model model) {
        User user = new User();
        user.setTenant(new Tenant(UUID.fromString(tenantId)));
        model.addAttribute("user", user);
        return "editUser";
    }

    @GetMapping("/tenant/updateUser")
    public String editUser(Authentication authentication, @RequestParam("id") String id, Model model) {
        UUID userId = UUID.fromString(id);
        User user = tenantService.getUser(userId);
        if (user == null) {
            // For this demo, just to show RLS in action, see if the user exists
            if (adminService.userExists(userId)) {
                Tenant authenticatedTenant = (Tenant) authentication.getPrincipal();
                model.addAttribute("css", "danger");
                model.addAttribute("msg", "Row Level Security policies prevented " + authenticatedTenant.getId().toString() + " from accessing user " + userId);
            } else {
                model.addAttribute("css", "danger");
                model.addAttribute("msg", "No tenant for id " + id);
            }
        }
        model.addAttribute("user", user);
        return "editUser";
    }

    @PostMapping("/tenant/editUser")
    public String saveTenant(Authentication authentication, @ModelAttribute User user, BindingResult binding, Model model, final RedirectAttributes redirectAttributes, WebRequest request) {
        String view = null;
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            binding.addError(new FieldError("user", "email", "User email is required"));
            view = "editUser";
        } else if (user.getGivenName() == null || user.getGivenName().isEmpty()) {
            binding.addError(new FieldError("user", "giveName", "User first name is required"));
            view = "editUser";
        } else if (user.getFamilyName() == null || user.getFamilyName().isEmpty()) {
            binding.addError(new FieldError("user", "familyName", "User last name is required"));
            view = "editUser";
        } else if (user.getTenant() == null || user.getTenant().getId() == null) {
            String requestedTenantId = request.getParameter("tenant");
            if (requestedTenantId != null && !requestedTenantId.isEmpty() && adminService.tenantExists(UUID.fromString(requestedTenantId))) {
                Tenant authenticatedTenant = (Tenant) authentication.getPrincipal();
                LOGGER.warn("Row Level Security policies prevented " + authenticatedTenant.getIdAsString() + " from accessing data for tenant " + requestedTenantId);
                redirectAttributes.addFlashAttribute("css", "danger");
                redirectAttributes.addFlashAttribute("msg", "Row Level Security policies prevented " + authenticatedTenant.getIdAsString() + " from accessing data for tenant " + requestedTenantId);
                view = "redirect:/tenant";
            } else {
                LOGGER.error("Unable to load tenant for user from input " + requestedTenantId);
                redirectAttributes.addFlashAttribute("css", "danger");
                redirectAttributes.addFlashAttribute("msg", "Unable to load tenant for user from input");
                view = "redirect:/tenant";
            }
        } else {
            try {
                boolean isNew = (user.getId() == null);
                LOGGER.info("Saving {}user {}", isNew ? "new " : "", user.getEmail());
                user = tenantService.saveUser(user);
                redirectAttributes.addFlashAttribute("css", "success");
                if (isNew) {
                    redirectAttributes.addFlashAttribute("msg", "New user added");
                } else {
                    redirectAttributes.addFlashAttribute("msg", "User updated");
                }
                // Add the tenant back into model for the redirect
                redirectAttributes.addFlashAttribute("selectedTenant", user.getTenant().getId());
                view = "redirect:/tenant";
            } catch (UnauthorizedException e) {
                LOGGER.warn("Authenticated tenant is not authorized to save user for current tenant");
                Tenant authenticatedTenant = (Tenant) authentication.getPrincipal();
                redirectAttributes.addFlashAttribute("css", "danger");
                redirectAttributes.addFlashAttribute("msg", "Row Level Security policies prevented " + authenticatedTenant.getIdAsString() + " from creating a user");
                view = "editUser";
            } catch (UniqueRecordException e) {
                LOGGER.warn("Duplicate user email error");
                binding.addError(new FieldError("user", "email", "User already exists"));
                view = "editUser";
            }
        }
        return view;
    }

    @GetMapping("/tenant/deleteUser")
    public String deleteUserConfirm(Authentication authentication, @RequestParam("id") String id, Model model, final RedirectAttributes redirectAttributes) {
        String view = null;
        UUID userId = UUID.fromString(id);
        User user = tenantService.getUser(userId);
        if (user == null) {
            user = new User();
            if (adminService.userExists(userId)) {
                LOGGER.warn("Authenticated tenant is not authorized to save user for current tenant");
                Tenant authenticatedTenant = (Tenant) authentication.getPrincipal();
                redirectAttributes.addFlashAttribute("css", "danger");
                redirectAttributes.addFlashAttribute("msg", "Row Level Security policies prevented " + authenticatedTenant.getIdAsString() + " from deleting user " + id);
                view = "redirect:/tenant";
            } else {
                model.addAttribute("css", "danger");
                model.addAttribute("msg", "No user for id " + id);
                view = "deleteUser";
            }
        } else {
            view = "deleteUser";
        }
        model.addAttribute("user", user);
        return view;
    }

    @PostMapping("/tenant/deleteUser")
    public String deleteUser(Authentication authentication, @ModelAttribute User user, BindingResult binding, Model model, final RedirectAttributes redirectAttributes) {
        LOGGER.info("Deleting user " + user.getId());
        String view = null;
        try {
            Tenant authenticatedTenant = (Tenant) authentication.getPrincipal();
            tenantService.deleteUser(user);
            // For this demo, just to show RLS in action, see if the user exists
            if (adminService.userExists(user.getId())) {
                LOGGER.warn("Row Level Security policies prevented " + authenticatedTenant.getIdAsString() + " from deleting user " + user.getId().toString());
                redirectAttributes.addFlashAttribute("css", "danger");
                redirectAttributes.addFlashAttribute("msg", "Row Level Security policies prevented " + authenticatedTenant.getIdAsString() + " from deleting user " + user.getId().toString());
                view = "redirect:/tenant";
            } else {
                LOGGER.info("User delete succeeded");
                redirectAttributes.addFlashAttribute("css", "success");
                redirectAttributes.addFlashAttribute("msg", "User deleted");
                // Add the tenant back into model for the redirect
                redirectAttributes.addFlashAttribute("selectedTenant", authenticatedTenant.getId());
                view = "redirect:/tenant";
            }
        } catch (Exception e) {
            LOGGER.error("Error deleting user", e);
            model.addAttribute("css", "danger");
            model.addAttribute("msg", "Failed to delete user: " + e.getMessage());
            view = "deleteUser";
        }
        return view;
    }

    private final class TenantEditor extends PropertyEditorSupport {
        @Override
        public String getAsText() {
            Tenant tenant = (Tenant) getValue();
            return tenant == null ? "" : tenant.getId().toString();
        }

        @Override
        public void setAsText(String text) throws IllegalArgumentException {
            Tenant tenant = null;
            try {
                tenant = tenantService.getTenant(UUID.fromString(text));
            } catch (Exception e) {
                LOGGER.error("Can't look up tenant by id {}", text, e);
            }
            setValue(tenant);
        }
    }

}
