package com.vaadin.plugin.copilot

data class CommandRequest(val command: String, val projectBasePath: String?, val data: Map<String, Any>)
