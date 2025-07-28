package com.vaadin.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

public class Client {

    private final String endpoint;

    private final String projectBasePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Client(String endpoint, String projectBasePath) {
        this.endpoint = endpoint;
        this.projectBasePath = projectBasePath;
    }

    public RestClient.ResponseSpec undo(Path path) throws IOException {
        return send("undo", new Message.UndoRedoMessage(Collections.singletonList(path.toString())));
    }

    public RestClient.ResponseSpec redo(Path path) throws IOException {
        return send("redo", new Message.UndoRedoMessage(Collections.singletonList(path.toString())));
    }

    public RestClient.ResponseSpec write(Path path, String content) throws IOException {
        return send("write", new Message.WriteFileMessage(path.toString(), null, content));
    }

    public RestClient.ResponseSpec restartApplication() throws IOException {
        return send("restartApplication", new Message.RestartApplicationMessage());
    }

    public RestClient.ResponseSpec writeBinary(Path path, String content) throws IOException {
        return send("writeBase64", new Message.WriteFileMessage(path.toString(), null, content));
    }

    public RestClient.ResponseSpec showInIde(Path path) throws IOException {
        return send("showInIde", new Message.ShowInIdeMessage(path.toString(), -10, -2));
    }

    public RestClient.ResponseSpec refresh() throws IOException {
        return send("refresh", new Message.RefreshMessage());
    }

    public RestClient.ResponseSpec delete(Path path) throws IOException {
        return send("delete", new Message.DeleteMessage(path.toString()));
    }

    public Optional<JsonNode> getVaadinRoutes() throws IOException {
        return sendRestSync("getVaadinRoutes", new Message.GetVaadinRoutesMessage());
    }

    public Optional<JsonNode> getVaadinVersion() throws IOException {
        return sendRestSync("getVaadinVersion", new Message.GetVaadinVersionMessage());
    }

    public Optional<JsonNode> getVaadinComponents() throws IOException {
        return sendRestSync("getVaadinComponents", new Message.GetVaadinComponentsMessage());
    }

    public Optional<JsonNode> getVaadinEntities() throws IOException {
        return sendRestSync("getVaadinPersistence", new Message.GetVaadinPersistenceMessage());
    }

    private RestClient.ResponseSpec send(String command, Object data) throws JsonProcessingException {
        Message.CopilotRestRequest message = new Message.CopilotRestRequest(command, projectBasePath, data);
        String body = new ObjectMapper().writeValueAsString(message);
        org.springframework.web.client.RestClient.ResponseSpec response = org.springframework.web.client.RestClient.create().post()
                .uri(endpoint).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve();
        return response;
    }

    // rest client
    private Optional<JsonNode> sendRestSync(String command, Object dataCommand) {
        try {
            Message.CopilotRestRequest message = new Message.CopilotRestRequest(command, projectBasePath, dataCommand);
            byte[] data = objectMapper.writeValueAsBytes(message);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofByteArray(data))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Unexpected response (" + response.statusCode()
                        + ") communicating with the IDE plugin: " + response.body());
            }
            if (response.body() != null && !response.body().isEmpty()) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                return Optional.of(responseJson);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return Optional.empty();
    }
}
