package com.vaadin.plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;

@SpringBootTest(classes = {SpringBootApplication.class})
public class PluginApiTests {

    private static final String PROJECT_BASE_PATH = "/Users/vaadin/src/intellij-plugin";

    private static final String TEST_RESOURCES_PATH = PROJECT_BASE_PATH + "/plugin-api-test-client/src/test/resources";

    private static Client client;

    @BeforeAll
    public static void beforeAll() throws IOException {
        if (System.getProperty("ghActions") != null) {
            return;
        }
        var props = new Properties();
        props.load(new FileReader(PROJECT_BASE_PATH + "/.idea/.copilot-plugin"));
        client = new Client(props.getProperty("endpoint"), PROJECT_BASE_PATH);
    }

    @Test
    public void testWrite() throws IOException {
        if (client == null) {
            return;
        }
        var filePath = Path.of(TEST_RESOURCES_PATH + "/test.txt");
        var response = client.write(filePath, "Hello World");
        assertHttpOk(response);
        Assertions.assertTrue(Files.exists(filePath));
        Files.delete(filePath);
    }

    @Test
    public void testWriteBinary() throws IOException {
        if (client == null) {
            return;
        }
        var samplePath = Path.of(TEST_RESOURCES_PATH + "/samples/image.png");
        var binaryContent = Files.readAllBytes(samplePath);
        var base64 = Base64.getEncoder().encodeToString(binaryContent);

        var filePath = Path.of(TEST_RESOURCES_PATH + "/image.png");
        var response = client.writeBinary(filePath, base64);
        assertHttpOk(response);
        Assertions.assertTrue(Files.exists(filePath));

        var fileContent = Files.readAllBytes(filePath);
        Assertions.assertEquals(binaryContent.length, fileContent.length);
        Files.delete(filePath);
    }

    @Test
    public void testDelete() throws IOException {
        if (client == null) {
            return;
        }
        var filePath = Path.of(TEST_RESOURCES_PATH + "/test.txt");
        var response = client.write(filePath, "Hello World");
        assertHttpOk(response);
        Assertions.assertTrue(Files.exists(filePath));

        response = client.delete(filePath);
        assertHttpOk(response);
        Assertions.assertFalse(Files.exists(filePath));
    }

    // add more tests when needed

    private void assertHttpOk(RestClient.ResponseSpec response) {
        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.toBodilessEntity().getStatusCode());
    }

}
