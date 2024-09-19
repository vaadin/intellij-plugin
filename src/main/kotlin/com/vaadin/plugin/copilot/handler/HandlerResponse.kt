package com.vaadin.plugin.copilot.handler

import io.netty.handler.codec.http.HttpResponseStatus

data class HandlerResponse(val status: HttpResponseStatus, val data: Map<String, String>? = null)
