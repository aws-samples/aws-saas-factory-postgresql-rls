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
import com.amazon.aws.partners.saasfactory.pgrls.UnauthorizedException;
import com.amazon.aws.partners.saasfactory.pgrls.domain.User;
import com.amazon.aws.partners.saasfactory.pgrls.repository.DataSourceRepository;
import com.amazon.aws.partners.saasfactory.pgrls.repository.UniqueRecordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In a more complete solution, you'd break up your business logic and error handling here and move
 * the data access code to another set of interfaces.
 * @author mibeard
 */
@Service
public class TenantServiceImpl implements TenantService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TenantServiceImpl.class);

	@Autowired
	private DataSourceRepository repo;

	// We have to "lazy load" the JDBC Template at runtime because there won't be an authenticated tenant
	// to map the connection pool to. Because we have a connection pool per-tenant, we have to ask the
	// repository for the data source each time to ensure that we get a connection back from the pool that
	// reflects the current tenant context. If you auto wired the JDBC Template, Spring would want to
	// inject a singleton data source.
	//
	// We could choose to reuse a JDBC Template instance. We'd still need to set the data source each time.
	// The savings would be in the JdbcTemplate not loading the exception translator (which it does when
	// it's instantiated). This would also expose a class member that could be mistakenly used below.
	private JdbcTemplate jdbc() {
		JdbcTemplate jdbc = new JdbcTemplate(repo.dataSource());

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			LOGGER.info("Spring current tenant = '{}'", ((Tenant) authentication.getPrincipal()).getId());
		}
		try (Connection conn = jdbc.getDataSource().getConnection(); Statement stmt = conn.createStatement()) {
			ResultSet rs = stmt.executeQuery("SHOW app.current_tenant");
			rs.next();
			String connectionCurrentTenant = rs.getString(1);
			rs.close();
			LOGGER.info("PostgreSQL current tenant = '{}' on {}", connectionCurrentTenant, jdbc.getDataSource().toString());
		} catch (SQLException e) {
			LOGGER.error("Error fetching PostgreSQL session variable app.current_tenant", e);
		}

		return jdbc;
	}

	@Override
	public Tenant getTenant(UUID tenantId) {
		Tenant tenant = null;
		try {
			tenant = jdbc().queryForObject("SELECT tenant_id, name, status, tier FROM tenant WHERE tenant_id = ?", new TenantRowMapper(), tenantId);
			tenant.setUsers(getUsers(tenant));
		} catch (EmptyResultDataAccessException e) {
			// If row level security policies aren't met, it's not
			// an exception from the database, it's just as if the
			// data didn't exist in the table.
		}
		return tenant;
	}

	@Override
	public Tenant saveTenant(Tenant tenant) {
		Tenant saved = null;
		int updated = jdbc().update("UPDATE tenant SET name = ?, status = ?, tier = ? WHERE tenant_id = ?", tenant.getName(), tenant.getStatus(), tenant.getTier(), tenant.getId());
		if (updated == 1) {
			saved = getTenant(tenant.getId());
		}
		return saved;
	}

	@Override
	public List<User> getUsers(Tenant tenant) {
		List<User> users = new ArrayList<>();
		try {
			users = jdbc().query("SELECT tenant_id, user_id, email, given_name, family_name FROM tenant_user WHERE tenant_id = ?", new UserRowMapper(), tenant.getId());
		} catch (EmptyResultDataAccessException e) {
			// If row level security policies aren't met, it's not
			// an exception from the database, it's just as if the
			// data didn't exist in the table.
		}
		return users;
	}

	/**
	 * Notice that there is nothing special about these queries. You don't have to add tenant_id = ? to your SQL.
	 * RLS protection is transparent to us because it's managed in the connection.
	 * @param userId
	 * @return the user with id userId
	 */
	@Override
	public User getUser(UUID userId) {
		User user = null;
		try {
			user = jdbc().queryForObject("SELECT tenant_id, user_id, email, given_name, family_name FROM tenant_user WHERE user_id = ?", new UserRowMapper(), userId);
		} catch (EmptyResultDataAccessException e) {
			// If row level security policies aren't met, it's not
			// an exception from the database, it's just as if the
			// data didn't exist in the table.
		}
		return user;
	}

	@Override
	public User saveUser(User user) {
		User saved = null;
		if (user.getId() == null) {
			saved = insertUser(user);
		} else {
			saved = updateUser(user);
		}
		return saved;
	}

	protected User insertUser(User user) {
		NamedParameterJdbcTemplate jdbc = new NamedParameterJdbcTemplate(jdbc());
		GeneratedKeyHolder generated = new GeneratedKeyHolder();
		StringBuilder sql = new StringBuilder("INSERT INTO tenant_user (tenant_id, email, given_name, family_name) VALUES (:tenant_id, :email, :given_name, :family_name)");
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("tenant_id", user.getTenant().getId())
				.addValue("email", user.getEmail())
				.addValue("given_name", user.getGivenName())
				.addValue("family_name", user.getFamilyName());
		try {
			int update = jdbc.update(sql.toString(), params, generated);
			if (update == 1) {
				UUID userId = (UUID) generated.getKeys().get("user_id");
				user.setId(userId);
				user.setTenant(getTenant(user.getTenant().getId()));
			}
		} catch (BadSqlGrammarException e) {
			// Postgres will throw an Access Rule Violation error with condition
			// insufficient_privilege if an INSERT fails to satisfy an RLS policy.
			// ERROR:  42501: new row violates row-level security policy for table...
			if ("42501".equals(e.getSQLException().getSQLState())) {
				throw new UnauthorizedException();
			} else {
				throw e;
			}
		} catch (DataAccessException e) {
			if (e.getRootCause() instanceof SQLException) {
				SQLException sqlError = (SQLException) e.getRootCause();
				if ("23505".equals(sqlError.getSQLState())) {
					throw new UniqueRecordException(user.getEmail() + " already exists", e);
				} else {
					throw e;
				}
			} else {
				throw e;
			}
		}
		return user;
	}

	/**
	 * Notice that there is nothing special about these queries. You don't have to add tenant_id = ? to your SQL.
	 * RLS protection is transparent to us because it's managed in the connection.
	 * @param user
	 * @return the updated user
	 */
	protected User updateUser(User user) {
		User updated = null;
		int rowsEffected = jdbc().update("UPDATE tenant_user SET email = ?, given_name = ?, family_name = ? WHERE user_id = ?", user.getEmail(), user.getGivenName(), user.getFamilyName(), user.getId());
		if (rowsEffected == 1) {
			updated = getUser(user.getId());
		}
		return updated;
	}

	@Override
	public void deleteUser(User user) {
		int rowsEffected = jdbc().update("DELETE FROM tenant_user WHERE user_id = ?", user.getId());
		LOGGER.info("Delete from tenant_user returned {} effected rows", rowsEffected);
	}
}
