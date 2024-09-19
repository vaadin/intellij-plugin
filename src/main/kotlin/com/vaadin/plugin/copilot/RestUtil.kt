package com.vaadin.plugin.copilot

import java.util.UUID
import org.jetbrains.ide.BuiltInServerManager

class RestUtil {

    companion object {

        private val serviceName = "copilot-" + UUID.randomUUID()

        fun getServiceName(): String = serviceName

        fun getEndpoint(): String {
            val port = BuiltInServerManager.getInstance().port
            return "http://127.0.0.1:$port/api/" + getServiceName()
        }
    }
}
