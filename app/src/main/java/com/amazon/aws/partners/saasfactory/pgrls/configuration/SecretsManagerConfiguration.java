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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Until ECS allows for secret ParameterStore environment variables
 * to be transparently set on task definitions, we have to decrypt
 * them ourselves. You must set a Task Role with the proper IAM
 * policies. ECS will generate a temporary set of AWS credentials
 * based on this role to execute SDK actions.
 *
 * This class will execute early as Spring boots up prior to the dispatcher
 * servlet coming online. You must list any post processors in spring.factories
 * in META-INF for it to be picked up.
 * @author mibeard
 */
public class SecretsManagerConfiguration implements EnvironmentPostProcessor {

	private static final Logger logger = LoggerFactory.getLogger(SecretsManagerConfiguration.class);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String awsRegion = environment.getProperty("AWS_REGION");
		String appPwParam = environment.getProperty("spring.datasource.password");
		String adminPwParam = environment.getProperty("admin.datasource.password");

		// Fetch the secret value for the application user database
		// password from parameter store.
		AWSSimpleSystemsManagement ssm = new AWSSimpleSystemsManagementClient();
		ssm.setRegion(Region.getRegion(Regions.fromName(awsRegion)));
		GetParameterRequest appPwRequest = new GetParameterRequest()
				.withName(appPwParam)
				.withWithDecryption(Boolean.TRUE);
		GetParameterResult appPwResult = ssm.getParameter(appPwRequest);
		String decryptedAppPassword = appPwResult.getParameter().getValue();

		// Fetch the secret value for the admin user database
		// password from parameter store.
		GetParameterRequest adminPwRequest = new GetParameterRequest()
				.withName(adminPwParam)
				.withWithDecryption(Boolean.TRUE);
		GetParameterResult adminPwResult = ssm.getParameter(adminPwRequest);
		String decryptedAdminPassword = adminPwResult.getParameter().getValue();

		// Create properties with the same names as we launched Spring with
		Properties decryptedProps = new Properties();
		decryptedProps.put("spring.datasource.password", decryptedAppPassword);
		decryptedProps.put("admin.datasource.password", decryptedAdminPassword);

		// Now replace the existing environment variables with our new values
		environment.getPropertySources().addFirst(new PropertiesPropertySource("myProps", decryptedProps));
	}
	
}
