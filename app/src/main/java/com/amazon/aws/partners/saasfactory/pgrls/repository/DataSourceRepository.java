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
package com.amazon.aws.partners.saasfactory.pgrls.repository;

import com.amazon.aws.partners.saasfactory.pgrls.domain.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Generates a JDBC connection pool per authenticated tenant. These connections will be constrained by
 * RLS policies to prevent cross tenant data access.
 *
 * Most systems have multiple users per tenant. These connection pools are per tenant, not user.
 * @author mibeard
 */
@Repository
@Configuration
public class DataSourceRepository {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceRepository.class);

	// See DataSourcePropertiesConfiguration
	@Autowired
	@Qualifier("dataSourceProperties")
	private DataSourceProperties dataSourceProperties;
	
	// See DataSourceCacheConfiguration
	@Autowired
	private Map<Object, Object> dataSourceTargets;

	private final TenantAwareDataSource dataSource = new TenantAwareDataSource();

	public javax.sql.DataSource dataSource() {
		Object currentTenant = null;
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication !=  null && !(authentication instanceof AnonymousAuthenticationToken)) {
			currentTenant = ((Tenant) authentication.getPrincipal()).getId();
		}
		if (currentTenant == null) {
			throw new RuntimeException("Can't return data source. No authenticated tenant.");
		}

		// Each tenant gets its own Hikari connection pool
		if (!dataSourceTargets.containsKey(currentTenant) || dataSourceTargets.get(currentTenant) == null) {
			LOGGER.info("Creating new connection pool for tenant {}", currentTenant);
			dataSourceTargets.put(currentTenant, dataSourceProperties.initializeDataSourceBuilder().build());
		}

		// Tell our data source router where to find all the keys we want it to map pools to
		dataSource.setTargetDataSources(dataSourceTargets);

		// Tell Spring we're done configuring the data source so it can initialize the routing
		// functionality. We must call this each time, because internally AbstractRoutingDataSource
		// is keeping a map of resolved data sources per key and we may have just added a new
		// tenant to our list of targets or we may have removed (logged out) a tenant and want
		// to cleanup.
		dataSource.afterPropertiesSet();

		LOGGER.info("Returning dataSource with targets:");
		dataSourceTargets.keySet().forEach((key) -> {
			LOGGER.info(String.valueOf(key));
		});
		
		return dataSource;
	}

	public Map<Object, Object> getDataSourceTargets() {
		return dataSourceTargets;
	}
}
