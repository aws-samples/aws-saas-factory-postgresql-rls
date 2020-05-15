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
package com.amazon.aws.partners.saasfactory.pgrls;

import com.amazon.aws.partners.saasfactory.pgrls.service.AdminService;
import com.amazon.aws.partners.saasfactory.pgrls.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Simplistic example REST API to demonstrate PostgreSQL RLS policies.
 * No real authentication or authorization mechanism is in place. For
 * simplicity, pass an HTTP header of X-Tenant-ID with either "ADMIN"
 * to enable administrative functions that bypass RLS such as listing
 * all tenants or registering a new tenant, or a valid tenant id to
 * access RLS enforced functions.
 * @author mibeard
 */
@SpringBootApplication
@RestController
public class SaaSFactoryPgRLS {
	
	private final Logger logger = LoggerFactory.getLogger(SaaSFactoryPgRLS.class);

	@Autowired
	private TenantService tenantService;

	@Autowired
	private AdminService adminService;
	
	@GetMapping("/")
	public String home() {
		return "Hello World";
	}

	/**
	 * Endpoint for the ALB to call. Simply returns an HTTP 200.
	 * @return
	 */
	@GetMapping("/health")
	public ResponseEntity health() {
		return ResponseEntity.ok().build();
	}

	/**
	 * Cleans up resources no longer needed by a tenant.
	 * @param currentTenantId
	 * @return
	 */
	@GetMapping("/logout")
	public ResponseEntity logout(@RequestHeader("X-Tenant-ID") String currentTenantId) {
		TenantContext.setTenant(null);
		tenantService.logout(UUID.fromString(currentTenantId));
		return ResponseEntity.ok().build();
	}

	/**
	 * Get a listing of all tenants. Currently an admin API.
	 * Pass "ADMIN" as the X-Tenant-ID to circumvent RLS and
	 * return all tenants.
	 * @throws UnauthorizedException
	 * @param currentTenantId
	 * @return
	 */
	@GetMapping("/tenants")
	public List<Tenant> tenants(@RequestHeader("X-Tenant-ID") String currentTenantId) {
		if (!checkAdminAccess(currentTenantId)) {
			throw new UnauthorizedException();
		}
		List<Tenant> tenants = adminService.getTenants();
		if (tenants == null) {
			tenants = new ArrayList<>();
		}
		return tenants;
	}

	/**
	 * Create a new tenant. Currently an admin API.
	 * Pass "ADMIN" as the X-Tenant-ID to circumvent RLS
	 * and insert a new tenant record.
	 * @param tenant
	 * @param currentTenantId
	 * @return
	 */
	@PostMapping("/tenants")
	public Tenant registerTenant(@RequestBody Tenant tenant, @RequestHeader("X-Tenant-ID") String currentTenantId) {
		if (!checkAdminAccess(currentTenantId)) {
			throw new UnauthorizedException();
		}
		return adminService.registerTenant(tenant);
	}

	/**
	 * Returns a fully-hydrated tenant object from the database.
	 * Pass the id of the tenant to retrieve as X-Tenant-ID.
	 * @param tenantId
	 * @param currentTenantId
	 * @return
	 */
	@GetMapping("/tenants/{tenantId}")
	public Tenant getTenant(@PathVariable String tenantId, @RequestHeader("X-Tenant-ID") String currentTenantId) {
		if (!checkAdminAccess(currentTenantId)) {
			setTenantContext(currentTenantId);
		}
		return tenantService.getTenant(UUID.fromString(tenantId));
	}

	/**
	 * Updates a tenant. Does not cascade down to the tenant's users.
	 * Pass the entire tenant object. If the tenant id in X-Tenant-ID
	 * does not match the id of the tenant object to update, throws
	 * an Unauthorized error. Must use Content-Type: application/json.
	 * @param tenantId
	 * @param tenant
	 * @param currentTenantId
	 * @return
	 */
	@PutMapping("/tenants/{tenantId}")
	public Tenant saveTenant(@PathVariable String tenantId, @RequestBody Tenant tenant, @RequestHeader("X-Tenant-ID") String currentTenantId) {
		if (!checkAdminAccess(currentTenantId)) {
			setTenantContext(currentTenantId);
		}
		if (!tenantId.equals(tenant.getId().toString())) {
			throw new UnauthorizedException();
		}
		return tenantService.saveTenant(tenant);
	}

	/**
	 * Deletes a tenant and its users. Currently an admin API.
	 * Pass "ADMIN" as the X-Tenant-ID to circumvent RLS
	 * and delete a tenant record.
	 * @param tenantId
	 * @param currentTenantId
	 * @return
	 */
	@DeleteMapping("/tenants/{tenantId}")
	public Tenant deleteTenant(@PathVariable String tenantId, @RequestHeader("X-Tenant-ID") String currentTenantId) {
		if (!checkAdminAccess(currentTenantId)) {
			throw new UnauthorizedException();
		}
		Tenant toDelete = new Tenant(UUID.fromString(tenantId));
		adminService.deleteTenantUsers(toDelete);
		adminService.deleteTenant(toDelete);
		return toDelete;
	}

	/**
	 * Get a listing of all users for a given tenant. If the tenant id
	 * in X-Tenant-ID does not match the path parameter tenant id, RLS
	 * will prevent cross tenant data access.
	 * @param currentTenantId
	 * @return
	 */
	@GetMapping("/tenants/{tenantId}/users")
	public List<User> users(@PathVariable String tenantId, @RequestHeader("X-Tenant-ID") String currentTenantId) {
		if (!checkAdminAccess(currentTenantId)) {
			setTenantContext(currentTenantId);
		}
		Tenant owner = new Tenant(UUID.fromString(tenantId));
		List<User> users = tenantService.getUsers(owner);
		if (users == null) {
			users = new ArrayList<>();
		}
		return users;
	}

	/**
	 * Creates a new user object for a given tenant. If the X-Tenant-ID
	 * value does not match the path parameter tenant id, RLS will prevent
	 * the insert and an Unauthorized error will bubble up. INSERTS, unlike
	 * SELECT, UPDATE and DELETE that violate RLS policies are errors in PG.
	 * @param tenantId
	 * @param user
	 * @param currentTenantId
	 * @return
	 */
	@PostMapping("/tenants/{tenantId}/users")
	public User saveUser(@PathVariable String tenantId, @RequestBody User user, @RequestHeader("X-Tenant-ID") String currentTenantId) {
		if (!checkAdminAccess(currentTenantId)) {
			setTenantContext(currentTenantId);
		}
		Tenant owner = new Tenant(UUID.fromString(tenantId));
		user.setTenant(owner);
		return tenantService.saveUser(user);
	}

	@GetMapping("/tenants/{tenantId}/users/{userId}")
	public User getUser(@PathVariable String tenantId, @PathVariable String userId, @RequestHeader("X-Tenant-ID") String currentTenantId) {
		if (!checkAdminAccess(currentTenantId)) {
			setTenantContext(currentTenantId);
		}
		User user = tenantService.getUser(UUID.fromString(userId));
		return user;
	}

	/**
	 * Fake authorization mechanism. Pass "ADMIN" as the X-Tenant-ID
	 * header value to authn/z as an administrator.
	 * @param tenantId
	 * @return
	 */
	private boolean checkAdminAccess(String tenantId) {
		boolean admin = "ADMIN".equals(tenantId);
		if (admin) {
			TenantContext.setTenant(null);
		}
		return admin;
	}

	/**
	 * Fake authentication mechanism. Pass the desired tenant id to
	 * "log in" as in the X-Tenant-ID header.
	 * @param tenantId
	 */
	private void setTenantContext(String tenantId) {
		UUID id = null;
		if (tenantId != null) {
			id = UUID.fromString(tenantId);
			TenantContext.setTenant(id);
			logger.info("Set current request tenant context to " + id.toString());
		}
	}

	public static void main(String[] args) throws Exception {
		SpringApplication app = new SpringApplication(SaaSFactoryPgRLS.class);
		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
	}
	
	public void run(String... args) throws Exception {
		logger.info("SaaSFactoryPgRLS app started...");
	}
}