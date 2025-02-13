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

    private static String projectBasePath;

    private static Client client;

    protected Path getTestResourcePath(String childPath) {
        return Path.of(projectBasePath).resolve("plugin-api-test-client/src/test/resources")
                .resolve(childPath);
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        if (Boolean.parseBoolean(System.getProperty("ghActions"))) {
            return;
        }
        projectBasePath = Path.of(System.getProperty("user.dir")).getParent().toString();
        var props = new Properties();
        props.load(new FileReader(projectBasePath + "/.idea/.copilot-plugin"));
        client = new Client(props.getProperty("endpoint"), projectBasePath);
    }

    @Test
    public void testWrite() throws IOException {
        if (client == null) {
            return;
        }
        var filePath = getTestResourcePath("test.txt");
        var response = client.write(filePath, "Hello World");
        assertHttpOk(response);
        assertNewFileCreated(filePath);
        Files.delete(filePath);
    }

    @Test
    public void testWriteBinary() throws IOException {
        if (client == null) {
            return;
        }
        var samplePath = getTestResourcePath("samples/image.png");
        var binaryContent = Files.readAllBytes(samplePath);
        var base64 = Base64.getEncoder().encodeToString(binaryContent);

        var filePath = getTestResourcePath("image.png");
        var response = client.writeBinary(filePath, base64);
        assertHttpOk(response);
        assertNewFileCreated(filePath);

        var fileContent = Files.readAllBytes(filePath);
        Assertions.assertEquals(binaryContent.length, fileContent.length);
        Files.delete(filePath);
    }

    @Test
    public void testDelete() throws IOException {
        if (client == null) {
            return;
        }
        var filePath = getTestResourcePath("test.txt");
        var response = client.write(filePath, "Hello World");
        assertHttpOk(response);
        assertNewFileCreated(filePath);

        response = client.delete(filePath);
        assertHttpOk(response);
        Assertions.assertFalse(Files.exists(filePath));
    }

    // add more tests when needed

    private void assertNewFileCreated(Path path) {
        for (int i = 0 ; i < 100 ; ++i) {
            try {
                if (Files.exists(path)) {
                    return;
                }
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Assertions.fail("File does not exist " + path);
    }

    private void assertHttpOk(RestClient.ResponseSpec response) {
        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.toBodilessEntity().getStatusCode());
    }

}
