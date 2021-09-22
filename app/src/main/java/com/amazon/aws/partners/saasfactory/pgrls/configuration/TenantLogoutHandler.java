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
import com.amazon.aws.partners.saasfactory.pgrls.repository.DataSourceRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.UUID;

public class TenantLogoutHandler extends SimpleUrlLogoutSuccessHandler implements LogoutSuccessHandler, ApplicationContextAware {

    private final static Logger LOGGER = LoggerFactory.getLogger(TenantLogoutHandler.class);

    private DataSourceRepository databaseConnectionPools;

    // Custom clean up of the database connection pool when we logout a tenant
    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        UUID tenantId = ((Tenant) authentication.getPrincipal()).getId();
        LOGGER.info("Tenant logout: removing database connection pool for tenant {}", tenantId);
        // Can't just call databaseConnectionPools.dataSource() because we just logged out the security context
        // principal and we'll get a null pointer when it tries to resolve the pool from it's target map.
        DataSource connectionPool = (DataSource) databaseConnectionPools.getDataSourceTargets().get(tenantId);
        // Explicitly close down the connection pool
        if (connectionPool != null) {
            ((HikariDataSource) connectionPool).close();
        }
        // And remove it from the list of targets
        databaseConnectionPools.getDataSourceTargets().remove(tenantId);
        super.onLogoutSuccess(httpServletRequest, httpServletResponse, authentication);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        databaseConnectionPools = applicationContext.getBean("dataSourceRepository", DataSourceRepository.class);
    }
}
