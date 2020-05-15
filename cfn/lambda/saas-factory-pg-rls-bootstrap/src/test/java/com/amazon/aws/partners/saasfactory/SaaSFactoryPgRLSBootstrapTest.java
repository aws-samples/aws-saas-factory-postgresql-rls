package com.amazon.aws.partners.saasfactory;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.junit.Ignore;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class SaaSFactoryPgRLSBootstrapTest {

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
	public void testNioPaths() throws Exception {
		Path p = Paths.get(Thread.currentThread().getContextClassLoader().getResource("bootstrap.sql").toURI());
		List<String> lines = Files.readAllLines(p, Charset.forName("UTF-8"));
		StringBuilder buffer = new StringBuilder();
		lines.forEach(line -> buffer.append(line));
		Assert.assertEquals("-- sql file", buffer.toString());
	}
	
	@Test
	public void testSemicolonScanner() throws Exception {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("bootstrap.sql");
		Scanner scanner = new Scanner(is, "UTF-8");
		scanner.useDelimiter(";");
		while (scanner.hasNext()) {
			System.out.println(scanner.nextLine());
		}
	}

    @Test
	@Ignore
    public void testSaaSFactoryPgRLSBootstrap() {
        SaaSFactoryPgRLSBootstrap handler = new SaaSFactoryPgRLSBootstrap();
        Context ctx = createContext();

        Object output = handler.handleRequest(input, ctx);

        // TODO: validate output here if needed.
        Assert.assertEquals("Hello from Lambda!", output);
    }
}
