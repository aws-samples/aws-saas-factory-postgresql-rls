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

import com.amazon.aws.partners.saasfactory.pgrls.TenantContext;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;

/**
 * Generates a JDBC connection pool per authenticated tenant. These
 * connections will be constrained by RLS policies to prevent cross
 * tenant data access. In a more complete solution, when a tenant
 * logs out, reference to that tenant in the data source should be
 * removed so the pool of connections can be garbage collected.
 *
 * Most systems have multiple users per tenant. These connection
 * pools are per tenant, not user.
 * @author mibeard
 */
@Repository
@Configuration
public class DataSourceRepository {
	
	private static final Logger logger = LoggerFactory.getLogger(DataSourceRepository.class);

	// See DataSourcePropertiesConfiguration
	@Autowired
	@Qualifier("dataSourceProperties")
	private DataSourceProperties dataSourceProperties;
	
	// See DataSourceCacheConfiguration
	@Autowired
	private Map<Object, Object> dataSourceTargets;

	private final TenantAwareDataSource dataSource = new TenantAwareDataSource();

	public javax.sql.DataSource dataSource() {
		Object currentTenant = TenantContext.getTenant();
		if (currentTenant == null) {
			throw new RuntimeException("No current tenant");
		}

		// Each tenant gets its own Hikari connection pool
		if (!dataSourceTargets.containsKey(currentTenant) || dataSourceTargets.get(currentTenant) == null) {
			dataSourceTargets.put(currentTenant, dataSourceProperties.initializeDataSourceBuilder().build());
		}

		// Tell our data source router where to find all the keys
		// we want it to map pools to
		dataSource.setTargetDataSources(dataSourceTargets);

		// Tell Spring we're done configuring the data source so
		// it can initialize the routing functionality. We must call
		// this each time, because internally AbstractRoutingDataSource
		// is keeping a map of resolved data sources per key and we
		// may have just added a new tenant to our list of targets or
		// we may have removed (logged out) a tenant and want to cleanup.
		dataSource.afterPropertiesSet();

//		logger.info("Returning dataSource with targets:");
//		dataSourceTargets.keySet().forEach((key) -> {
//			logger.info(String.valueOf(key));
//		});
		
		return dataSource;
	}

	public Map<Object, Object> getDataSourceTargets() {
		return dataSourceTargets;
	}
}
