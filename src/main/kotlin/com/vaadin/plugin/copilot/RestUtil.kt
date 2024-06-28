package com.vaadin.plugin.copilot

import org.jetbrains.ide.BuiltInServerManager

class RestUtil {

    companion object {

        fun getServiceName(): String = "copilot"

        fun getEndpoint(): String {
            val port = BuiltInServerManager.getInstance().port
            return "http://127.0.0.1:$port/api/" + getServiceName()
        }

    }

}