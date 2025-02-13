package com.vaadin.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

public class Client {

    private final String endpoint;

    private final String projectBasePath;

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

    private RestClient.ResponseSpec send(String command, Object data) throws JsonProcessingException {
        Message.CopilotRestRequest message = new Message.CopilotRestRequest(command, projectBasePath, data);
        String body = new ObjectMapper().writeValueAsString(message);
        org.springframework.web.client.RestClient.ResponseSpec response = org.springframework.web.client.RestClient.create().post()
                .uri(endpoint).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve();
        return response;
    }

}
