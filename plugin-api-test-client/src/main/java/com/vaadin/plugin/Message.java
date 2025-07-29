package com.vaadin.plugin;

import java.util.List;

public class Message {

    record Command(String command, Object data) {
    }

    record CopilotRestRequest(String command, String projectBasePath, Object data) {
    }

    record WriteFileMessage(String file, String undoLabel, String content) {
    }

    record UndoRedoMessage(List<String> files) {
    }

    record ShowInIdeMessage(String file, Integer line, Integer column) {
    }

    record RefreshMessage() {
    }

    record RestartApplicationMessage() {
    }

    record CompileMessage(List<String> files) {
    }

    record DeleteMessage(String file) {
    }

    record GetVaadinRoutesMessage() {
    }

    record GetVaadinVersionMessage() {
    }

    record GetVaadinComponentsMessage() {
    }

    record GetVaadinPersistenceMessage() {
    }

    record GetVaadinSecurityMessage() {
    }
}
