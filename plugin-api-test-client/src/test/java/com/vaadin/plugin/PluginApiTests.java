package com.vaadin.plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.function.Predicate;

@SpringBootTest(classes = {SpringBootApplication.class})
public class PluginApiTests {

    private static String projectBasePath;

    private static Client client;

    protected Path getTestResourcePath(String childPath) {
        return Path.of(projectBasePath)
                .resolve("src/test/resources")
                .resolve(childPath);
    }

    protected Path getSourcePath(String childPath) {
        return Path.of(projectBasePath)
                .resolve("src/main/java")
                .resolve(childPath);
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        projectBasePath = Path.of(System.getProperty("user.dir")).toString();
        var props = new Properties();
        props.load(new FileReader(projectBasePath + "/.idea/.copilot-plugin"));
        client = new Client(props.getProperty("endpoint"), projectBasePath);
    }

    @Test
    public void testWrite() throws IOException {
        var filePath = getTestResourcePath("test.txt");
        var response = client.write(filePath, "Hello World");
        assertHttpOk(response);
        assertNewFileCreated(filePath);
        Files.delete(filePath);
    }

    @Test
    public void testWriteBinary() throws IOException {
        var samplePath = getTestResourcePath("samples/image.png");
        var binaryContent = Files.readAllBytes(samplePath);
        var base64 = Base64.getEncoder().encodeToString(binaryContent);

        var filePath = getTestResourcePath("image.png");
        var response = client.writeBinary(filePath, base64);
        assertHttpOk(response);
        assertNewFileCreated(filePath);

        assertWithTimeout((originalContent) -> {
            try {
                byte[] fileContent = Files.readAllBytes(filePath);
                return originalContent.length == fileContent.length;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, binaryContent);
        Files.delete(filePath);
    }

    @Test
    public void testDelete() throws IOException {
        var filePath = getTestResourcePath("test.txt");
        var response = client.write(filePath, "Hello World");
        assertHttpOk(response);
        assertNewFileCreated(filePath);

        response = client.delete(filePath);
        assertHttpOk(response);
        assertFileDeleted(filePath);
    }

    @Test
    public void testUndoRedo() throws IOException, InterruptedException {

        var filePath = getTestResourcePath(UUID.randomUUID().toString());
        filePath.toFile().deleteOnExit();

        // nothing to undo at the beginning
        var response = client.undo(filePath);
        var performed = (Boolean) response.body(Map.class).get("performed");
        Assertions.assertFalse(performed);

        // write content
        response = client.write(filePath, "Hello");
        assertHttpOk(response);

        Thread.sleep(1000);

        // check undo has been performed
        response = client.undo(filePath);
        performed = (Boolean) response.body(Map.class).get("performed");
        Assertions.assertTrue(performed);

        Thread.sleep(1000);

        // do redo
        response = client.redo(filePath);
        performed = (Boolean) response.body(Map.class).get("performed");
        Assertions.assertTrue(performed);

        Thread.sleep(1000);

        // nothing more to redo
        response = client.redo(filePath);
        performed = (Boolean) response.body(Map.class).get("performed");
        Assertions.assertFalse(performed);
    }

    // Run using Debug to trigger compile on save
    @Test
    public void testHeartbeat() throws IOException, InterruptedException {
        var response = client.heartbeat();
        var body = response.body(Message.HeartbeatResponse.class);
        Assertions.assertFalse(body.hasCompilationError());
        Assertions.assertEquals(Collections.EMPTY_SET, body.filesContainCompilationError());

        var filePath = getSourcePath("com/vaadin/plugin/Test.java");
        filePath.toFile().deleteOnExit();

        response = client.write(filePath, "package com.vaadin.plugin;\n" +
                "\n" +
                "public class Test {\n" +
                "asdasdasd" +
                "}");
        assertHttpOk(response);
        Thread.sleep(2000);

        for (int i = 0 ; i < 10 ; ++i) {
            response = client.heartbeat();
            body = response.body(Message.HeartbeatResponse.class);
            if (body.hasCompilationError()) {
                Assertions.assertEquals(Collections.EMPTY_SET, body.filesContainCompilationError());
                return;
            }
            Thread.sleep(2000);
        }

        Assertions.fail("No compilation error received");
    }

    // add more tests when needed

    private void assertNewFileCreated(Path file) {
        assertWithTimeout(Files::exists, file);
    }

    private void assertFileDeleted(Path file) {
        assertWithTimeout(Files::notExists, file);
    }

    private <T> void assertWithTimeout(Predicate<T> predicate, T parameter) {
        for (int i = 0 ; i < 100 ; ++i) {
            try {
                if (predicate.test(parameter)) {
                    return;
                }
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Assertions.fail("Assertion failed for " + parameter);
    }

    private void assertHttpOk(RestClient.ResponseSpec response) {
        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.toBodilessEntity().getStatusCode());
    }

}
