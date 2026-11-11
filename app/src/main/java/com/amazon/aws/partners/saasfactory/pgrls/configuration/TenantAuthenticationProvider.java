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
package com.amazon.aws.partners.saasfactory.pgrls.configuration;

import com.amazon.aws.partners.saasfactory.pgrls.domain.Tenant;
import com.amazon.aws.partners.saasfactory.pgrls.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
public class TenantAuthenticationProvider implements AuthenticationProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(TenantAuthenticationProvider.class);

    @Autowired
    AdminService adminService;

    // Not a real auth experience here, but lets us create a session which we can use to bind
    // tenant context to our HTTP requests
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // The tenant picker submits the selected tenant's id as the credential. Anything that
        // is not a UUID did not come from the picker -- most often an administrator password
        // typed against the wrong username, because ProviderManager continues past a provider
        // that rejects a login and so reaches this one.
        //
        // Never pass the credential to UUID.fromString outside this guard and never log the
        // parse failure: UUID.fromString puts its own input into the exception message
        // ("Invalid UUID string: " + input) for any input of 36 characters or fewer, so
        // logging that exception would write the supplied password to the log in plaintext.
        UUID tenantId = parseTenantId(authentication.getCredentials());
        if (tenantId == null) {
            // Null is the AuthenticationProvider contract for "unable to support this
            // Authentication", which is exactly the case: a credential that is not a tenant
            // id did not come from the picker. ProviderManager then continues to any other
            // provider, and the credential never reaches the rest of this method.
            return null;
        }

        Tenant principal;
        try {
            principal = adminService.getTenant(tenantId);
        } catch (Exception e) {
            // Safe to keep the cause here: this is a backend failure looking up an
            // already-parsed UUID, so the message cannot echo the supplied credential.
            // InternalAuthenticationServiceException tells ProviderManager to fail fast
            // rather than poll further providers on what is a system fault, not a bad login.
            LOGGER.error("Tenant lookup failed during authentication", e);
            throw new InternalAuthenticationServiceException("Error authenticating");
        }

        if (principal == null || principal.getId() == null) {
            throw new BadCredentialsException("Bad credentials");
        }

        // ROLE_TENANT distinguishes a tenant session from an administrator one. Both are
        // "authenticated", but their principals are different types -- a Tenant here, a
        // UserDetails for the administrator -- and TenantController casts the principal to
        // Tenant. Without a distinct authority, /tenant/** would accept an administrator
        // session and then fail on that cast. The role lets the security configuration
        // deny it cleanly instead.
        return new UsernamePasswordAuthenticationToken(principal, principal.getId(),
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + SecurityConfiguration.TENANT_ROLE)));
    }

    private static UUID parseTenantId(Object credentials) {
        if (credentials == null) {
            return null;
        }
        try {
            return UUID.fromString(credentials.toString());
        } catch (IllegalArgumentException e) {
            // Deliberately swallowed without logging: e.getMessage() contains the credential.
            return null;
        }
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.equals(UsernamePasswordAuthenticationToken.class);
    }
}
