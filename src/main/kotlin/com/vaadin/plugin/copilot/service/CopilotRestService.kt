package com.vaadin.plugin.copilot.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.vaadin.plugin.copilot.CommandRequest
import com.vaadin.plugin.copilot.CopilotPluginUtil
import com.vaadin.plugin.copilot.RestUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.jetbrains.ide.RestService
import java.nio.charset.Charset
import java.nio.file.Path

class CopilotRestService : RestService() {

    private val LOG: Logger = Logger.getInstance(CopilotRestService::class.java)

    override fun getServiceName(): String {
        return RestUtil.getServiceName()
    }

    override fun execute(
        urlDecoder: QueryStringDecoder,
        request: FullHttpRequest,
        context: ChannelHandlerContext
    ): String? {
        val charset = HttpUtil.getCharset(request, Charset.defaultCharset())
        val copilotRequest: CommandRequest = jacksonObjectMapper()
            .readValue(request.content().toString(charset))

        if (copilotRequest.projectBasePath == null) {
            sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
            return null
        }

        val projectBasePath = Path.of(copilotRequest.projectBasePath).toRealPath()
        val project = ProjectManager.getInstance().openProjects.find {
            Path.of(it.basePath!!).toRealPath().equals(projectBasePath)
        }

        if (project == null) {
            LOG.error("Project location does not match any open project")
            sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
            return null
        }

        CopilotPluginUtil.createCommandHandler(copilotRequest.command, project, copilotRequest.data)?.run()

        sendOk(request, context)
        return null
    }

    override fun isMethodSupported(method: HttpMethod): Boolean {
        return method === HttpMethod.POST
    }

}
