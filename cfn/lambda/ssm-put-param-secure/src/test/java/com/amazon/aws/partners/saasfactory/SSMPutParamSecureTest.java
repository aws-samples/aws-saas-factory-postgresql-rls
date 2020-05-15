package com.amazon.aws.partners.saasfactory;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.Map;
import org.junit.Ignore;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class SSMPutParamSecureTest {

    private static Map<String, Object> input;

    @BeforeClass
    public static void createInput() throws IOException {
        // TODO: set up your sample input object here.
        input = null;
    }

    private Context createContext() {
        TestContext ctx = new TestContext();

        // TODO: customize your context here if needed.
        ctx.setFunctionName("Your Function Name");

        return ctx;
    }

    @Test
	@Ignore
    public void testSSMPutParamSecure() {
        SSMPutParamSecure handler = new SSMPutParamSecure();
        Context ctx = createContext();

        Object output = handler.handleRequest(input, ctx);

    }
}
