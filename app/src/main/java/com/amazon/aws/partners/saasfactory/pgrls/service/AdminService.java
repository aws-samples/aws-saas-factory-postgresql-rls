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
 * The read methods are deliberately NOT annotated, and it is worth being precise about why,
 * because "they are only reached anonymously" would be wrong:
 *
 *  - getTenants() and getTenant() are called while the request is still anonymous, by the
 *    login page and by TenantAuthenticationProvider respectively, so requiring an authority
 *    would break the sample's own login.
 *  - getTenants(), tenantExists() and userExists() are ALSO called from TenantController,
 *    i.e. from an authenticated tenant session. So a tenant session does read tenant records
 *    over this RLS-bypassing connection, including rows belonging to other tenants.
 *
 * That second point is an accepted, deliberate property of the demonstration rather than an
 * oversight: the sample's tenant context is not authenticated in the first place (see the
 * security notice in README.md), so restricting these reads would not establish a boundary
 * that the login already fails to establish. If you are adapting this sample, fix the
 * identity layer first; annotating these methods on top of a tenant picker would only look
 * like a control.
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
