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
package com.amazon.aws.partners.saasfactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.*;

public class SaaSFactoryPgRLSBootstrap implements RequestHandler<Map<String, Object>, Object> {

	private SsmClient ssm;

	public SaaSFactoryPgRLSBootstrap() {
		this.ssm = SsmClient.builder()
				.httpClientBuilder(UrlConnectionHttpClient.builder())
				.credentialsProvider(EnvironmentVariableCredentialsProvider.create())
				.build();
	}

	@Override
	public Object handleRequest(Map<String, Object> event, Context context) {
		LambdaLogger logger = context.getLogger();

		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.log(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event));
			logger.log("\n");
		} catch (JsonProcessingException e) {
			logger.log("Could not log input\n");
		}

		final String requestType = (String) event.get("RequestType");
		Map<String, Object> resourceProperties = (Map<String, Object>) event.get("ResourceProperties");
		final String dbMasterUser = (String) resourceProperties.get("RDSMasterUsername");
		final String dbMasterPass = (String) resourceProperties.get("RDSMasterPassword");
		final String dbAppUser = (String) resourceProperties.get("RDSAppUsername");
		final String dbAppPass = (String) resourceProperties.get("RDSAppPassword");
		final String dbHost = (String) resourceProperties.get("RDSClusterEndpoint");
		final String dbDatabase = (String) resourceProperties.get("RDSDatabase");

		final String[] stackArn = ((String) event.get("StackId")).split(":");
		final String region = stackArn[3];

		ExecutorService service = Executors.newSingleThreadExecutor();

		ObjectNode responseData = JsonNodeFactory.instance.objectNode();
		try {
			if (requestType == null) {
				throw new RuntimeException();
			}

			Runnable r = () -> {
				GetParameterRequest ssmRequest = GetParameterRequest.builder()
						.name(dbMasterPass)
						.withDecryption(Boolean.TRUE)
						.build();

				GetParameterResponse ssmResult = ssm.getParameter(ssmRequest);
				Parameter ssmParam = ssmResult.parameter();
				String decryptedMasterPassword = ssmParam.value();

				ssmRequest = ssmRequest.toBuilder()
						.name(dbAppPass)
						.build();

				ssmResult = ssm.getParameter(ssmRequest);
				ssmParam = ssmResult.parameter();
				String decryptedAppPassword = ssmParam.value();

				Properties masterConnectionProperties = new Properties();
				masterConnectionProperties.put("user", dbMasterUser);
				masterConnectionProperties.put("password", decryptedMasterPassword);

				String jdbcUrl = "jdbc:postgresql://" + dbHost + ":5432/" + dbDatabase;

				if ("Create".equalsIgnoreCase(requestType)) {
					logger.log("CREATE\n");
					
					// Use the RDS master user to create a new read/write non-root
					// user for our app to access the database as. Also create the
					// tables and RLS policies. The RDS master user will be the owner
					// of these tables and by default will bypass RLS which is what
					// we need for new tenant on-boarding where INSERT statements
					// would otherwise fail
					try (Connection connection = DriverManager.getConnection(jdbcUrl, masterConnectionProperties);
							Statement sql = connection.createStatement()) {
						connection.setAutoCommit(false);
						InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("bootstrap.sql");
						Scanner scanner = new Scanner(is, "UTF-8");
						scanner.useDelimiter(";");
						while (scanner.hasNext()) {
							// Simple variable replacement in the SQL
							String userSql = scanner.next()
									.replace("{{DB_APP_USER}}", dbAppUser)
									.replace("{{DB_APP_PASS}}", decryptedAppPassword);
//							logger.log(userSql + ";\n");
							sql.addBatch(userSql);
						}
						int[] count = sql.executeBatch();
						connection.commit();
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
					sendResponse(event, context, "SUCCESS", responseData);
				} else if ("Update".equalsIgnoreCase(requestType)) {
					logger.log("UDPATE\n");
					// TODO: Is there really any logical update process here?
					sendResponse(event, context, "SUCCESS", responseData);
				} else if ("Delete".equalsIgnoreCase(requestType)) {
					logger.log("DELETE\n");
					// TODO: Do we dare drop the database here?
					sendResponse(event, context, "SUCCESS", responseData);
				} else {
					logger.log("FAILED unknown requestType " + requestType + "\n");
					responseData.put("Reason", "Unknown RequestType " + requestType);
					sendResponse(event, context, "FAILED", responseData);
				}
			};
			Future<?> f = service.submit(r);
			f.get(context.getRemainingTimeInMillis() - 1000, TimeUnit.MILLISECONDS);
		} catch (final TimeoutException | InterruptedException | ExecutionException e) {
			// Timed out or unexpected exception
			logger.log("FAILED unexpected error or request timed out " + e.getMessage() + "\n");
			logger.log(getFullStackTrace(e) + "\n");
			
			responseData.put("Reason", "Request timed out");
			sendResponse(event, context, "FAILED", responseData);
		} finally {
			service.shutdown();
		}

		return null;
	}

	/**
	 * Send a response to CloudFormation regarding progress in creating resource.
	 *
	 * @param event
	 * @param context
	 * @param responseStatus
	 * @param responseData
	 * @return
	 */
	public final Object sendResponse(final Map<String, Object> event, final Context context, final String responseStatus, ObjectNode responseData) {
		LambdaLogger logger = context.getLogger();
		String responseUrl = (String) event.get("ResponseURL");
		logger.log("ResponseURL: " + responseUrl + "\n");

		URL url;
		try {
			url = new URL(responseUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "");
			connection.setRequestMethod("PUT");

			ObjectNode responseBody = JsonNodeFactory.instance.objectNode();
			responseBody.put("Status", responseStatus);
			responseBody.put("RequestId", (String) event.get("RequestId"));
			responseBody.put("LogicalResourceId", (String) event.get("LogicalResourceId"));
			responseBody.put("StackId", (String) event.get("StackId"));
			responseBody.put("PhysicalResourceId", (String) event.get("LogicalResourceId"));
			if (!"FAILED".equals(responseStatus)) {
				responseBody.set("Data", responseData);
			} else {
				responseBody.put("Reason", responseData.get("Reason").asText());
			}
			logger.log("Response Body: " + responseBody.toString() + "\n");

			try (OutputStreamWriter response = new OutputStreamWriter(connection.getOutputStream())) {
				response.write(responseBody.toString());
			} catch (IOException ioe) {
				logger.log("Failed to call back to CFN response URL\n");
				logger.log(getFullStackTrace(ioe) + "\n");
			}

			logger.log("Response Code: " + connection.getResponseCode() + "\n");
			connection.disconnect();
		} catch (IOException e) {
			logger.log("Failed to open connection to CFN response URL\n");
			logger.log(getFullStackTrace(e) + "\n");
		}

		return null;
	}

	private static String getFullStackTrace(Exception e) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		e.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
}
