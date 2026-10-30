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
package com.amazon.aws.partners.saasfactory.pgrls.service;

import com.amazon.aws.partners.saasfactory.pgrls.domain.Tenant;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.UUID;

/**
 * Simplistic CRUD API
 * @author mibeard
 *
 * Every method here runs over the table-owner connection in AdminDataSourceRepository,
 * which is deliberately not bound by row level security. The two methods that modify
 * tenants therefore carry an explicit ROLE_ADMIN requirement as well as sitting behind
 * the URL rule on /admin/** -- so a route added later without that rule cannot reach
 * them unauthenticated.
 *
 * getTenants() and getTenant() are deliberately NOT annotated: the login page lists
 * tenants to choose from and TenantAuthenticationProvider resolves one while the request
 * is still anonymous, so requiring an authority here would break the sample's own login.
 * That those reads are reachable anonymously is a property of the sample's simplified
 * authentication, which README.md now documents.
 */
public interface AdminService {

	@PreAuthorize("hasRole('ADMIN')")
	public Tenant saveTenant(Tenant tenant);

	public List<Tenant> getTenants();

	public Tenant getTenant(UUID tenantId);

	@PreAuthorize("hasRole('ADMIN')")
	public void deleteTenant(Tenant tenant);

	public boolean tenantExists(UUID tenantId);

	public boolean userExists(UUID userId);

}
