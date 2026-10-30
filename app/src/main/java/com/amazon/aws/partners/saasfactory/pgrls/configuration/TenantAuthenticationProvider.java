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
import org.springframework.security.authentication.AuthenticationServiceException;
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
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();
        UsernamePasswordAuthenticationToken token = null;
        try {
            Tenant principal = adminService.getTenant(UUID.fromString(password));
            // ROLE_TENANT distinguishes a tenant session from an administrator one. Both are
            // "authenticated", but their principals are different types -- a Tenant here, a
            // UserDetails for the administrator -- and TenantController casts the principal to
            // Tenant. Without a distinct authority, /tenant/** would accept an administrator
            // session and then fail on that cast. The role lets the security configuration
            // deny it cleanly instead.
            token = new UsernamePasswordAuthenticationToken(principal, principal.getId(),
                    Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + SecurityConfiguration.TENANT_ROLE)));
        } catch (Exception e) {
            LOGGER.error("Error authenticating", e);
            throw new AuthenticationServiceException("Error authenticating");
        }
        return token;
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.equals(UsernamePasswordAuthenticationToken.class);
    }
}
