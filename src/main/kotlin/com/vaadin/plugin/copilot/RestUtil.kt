package com.vaadin.plugin.copilot

import org.jetbrains.ide.BuiltInServerManager
import java.util.*

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
