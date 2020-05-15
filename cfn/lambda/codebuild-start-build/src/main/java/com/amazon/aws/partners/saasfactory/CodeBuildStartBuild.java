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
import software.amazon.awssdk.services.codebuild.CodeBuildClient;
import software.amazon.awssdk.services.codebuild.model.Build;
import software.amazon.awssdk.services.codebuild.model.StartBuildRequest;
import software.amazon.awssdk.services.codebuild.model.StartBuildResponse;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;

public class CodeBuildStartBuild implements RequestHandler<Map<String, Object>, Object> {

	private CodeBuildClient codebuild;

	public CodeBuildStartBuild() {
		this.codebuild = CodeBuildClient.builder()
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
		final String project = (String) resourceProperties.get("Project");

		ExecutorService service = Executors.newSingleThreadExecutor();
		ObjectNode responseData = JsonNodeFactory.instance.objectNode();
		try {
			if (requestType == null) {
				throw new RuntimeException();
			}
			Runnable r = () -> {
				StartBuildRequest request = StartBuildRequest.builder()
						.projectName(project)
						.build();
				if ("Create".equalsIgnoreCase(requestType)) {
					logger.log("CREATE\n");

					StartBuildResponse response = codebuild.startBuild(request);
					Build build = response.getValueForField("build", Build.class).get();
					responseData.put("BuildStatus", build.buildStatusAsString());

					sendResponse(event, context, "SUCCESS", responseData);
				} else if ("Update".equalsIgnoreCase(requestType)) {
					logger.log("UDPATE\n");
					sendResponse(event, context, "SUCCESS", responseData);
				} else if ("Delete".equalsIgnoreCase(requestType)) {
					logger.log("DELETE\n");
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
			// Timed out
			logger.log("FAILED unexpected error or request timed out " + e.getMessage() + "\n");
			logger.log(getFullStackTrace(e) + "\n");

			responseData.put("Reason", e.getMessage());
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