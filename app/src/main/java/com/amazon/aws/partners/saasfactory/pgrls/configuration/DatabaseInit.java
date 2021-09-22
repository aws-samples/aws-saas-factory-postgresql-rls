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
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

@Component
public class DatabaseInit implements ApplicationListener<ApplicationContextInitializedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseInit.class);

    // Execute this database bootstrap sequence after Spring has initialized its
    // context but before any Beans (data sources) are instantiated
    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();

        String dbAppUser = env.getRequiredProperty("spring.datasource.username");
        String dbAppPassword = env.getRequiredProperty("spring.datasource.password");
        String dbHost = env.getRequiredProperty("DB_HOST");
        String dbDatabase = env.getRequiredProperty("DB_NAME");
        String jdbcUrl = "jdbc:postgresql://" + dbHost + ":5432/" + dbDatabase;

        Properties masterConnectionProperties = new Properties();
        masterConnectionProperties.put("user", env.getRequiredProperty("admin.datasource.username"));
        masterConnectionProperties.put("password", env.getRequiredProperty("admin.datasource.password"));

        // Bootstrap the database objects. The SQL is written to be idempotent so it can run each time Spring Boot
        // launches. This bootstrapping creates the tables and RLS policies. The RDS master user will be the owner
        // of these tables and by default will bypass RLS which is what we need for new tenant on-boarding where
        // INSERT statements would otherwise fail.
        //
        // This also will create a non root user with full read/write privileges for our application code to connect
        // as. This user will not be the owner of tables or other objects and will be bound by the RLS policies.
        try (Connection connection = DriverManager.getConnection(jdbcUrl, masterConnectionProperties); Statement sql = connection.createStatement()) {
            connection.setAutoCommit(false);
            LOGGER.info("Executing bootstrap database");
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("bootstrap.sql")) {
                Scanner scanner = new Scanner(is, "UTF-8");
                // Break on blank newline so we can send the DO...END statements as a single statement
                scanner.useDelimiter("\n\n");
                while (scanner.hasNext()) {
                    String stmt = scanner.next()
                            .replace("{{DB_APP_USER}}", dbAppUser)
                            .replace("{{DB_APP_PASS}}", dbAppPassword)
                            .trim();
                    sql.addBatch(stmt);
                }
                int[] count = sql.executeBatch();
                connection.commit();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
