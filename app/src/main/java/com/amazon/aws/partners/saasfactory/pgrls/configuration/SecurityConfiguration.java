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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final static Logger LOGGER = LoggerFactory.getLogger(SecurityConfiguration.class);

    public final static String ADMIN_ROLE = "ADMIN";

    @Autowired
    private TenantAuthenticationProvider authenticationProvider;

    /**
     * Credentials for the SaaS administration pages under /admin. There is no default:
     * if either is unset, no administrator account is registered, nothing can satisfy
     * the ROLE_ADMIN requirement on /admin/**, and the whole administration surface is
     * unreachable. That is deliberate -- an unconfigured deployment should expose
     * nothing rather than expose everything.
     *
     * Supply them as environment variables; do not commit values here.
     */
    @Value("${admin.ui.username:}")
    private String adminUsername;

    @Value("${admin.ui.password:}")
    private String adminPassword;

    private boolean administratorConfigured() {
        return adminUsername != null && !adminUsername.trim().isEmpty()
                && adminPassword != null && !adminPassword.trim().isEmpty();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // The administrator is registered first so an administrator login is resolved by
        // this provider rather than falling through to the tenant provider below.
        if (administratorConfigured()) {
            PasswordEncoder encoder = passwordEncoder();
            auth.inMemoryAuthentication()
                    .passwordEncoder(encoder)
                    .withUser(adminUsername.trim())
                    .password(encoder.encode(adminPassword))
                    .roles(ADMIN_ROLE);
        } else {
            LOGGER.warn("ADMIN_UI_USERNAME/ADMIN_UI_PASSWORD are not set, so no "
                    + "administrator account has been registered and the /admin pages "
                    + "are unreachable. Set both to enable tenant administration.");
        }
        auth.authenticationProvider(authenticationProvider);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/webjars/**");
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new TenantLogoutHandler();
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/", "/health").permitAll() // landing page and ALB health check
                // The SaaS administration pages create, edit and delete any tenant, and
                // they run over the table-owner connection in AdminDataSourceRepository,
                // which is not bound by row level security. They therefore need a real
                // administrator, not merely a session: the tenant login below establishes
                // tenant context and grants no authorities, so a tenant session cannot
                // satisfy this.
                .antMatchers("/admin/**").hasRole(ADMIN_ROLE)
                .antMatchers("/tenant/**").authenticated() // tenant user management is authenticated
            .and() // custom login form
                .formLogin()
                    .loginPage("/login")
                    .permitAll() // anyone can access login
            .and() // custom logout redirect
                .logout()
                    .logoutSuccessHandler(logoutSuccessHandler())
                    .permitAll(); // anyone can access logout
    }

}
