package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.vaadin.plugin.endpoints.findEntities
import io.netty.handler.codec.http.HttpResponseStatus

class GetVaadinPersistenceHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        return ApplicationManager.getApplication().runReadAction<HandlerResponse> {
            val entities = findEntities(project, GlobalSearchScope.allScope(project))

            val mapEntities =
                entities.map { entity ->
                    mapOf(
                        "classname" to entity.locationString,
                        "methods" to entity.visibleMethods.joinToString(",") { signatureToString(it) })
                }

            LOG.info("Entities detected: $entities")

            HandlerResponse(status = HttpResponseStatus.OK, data = mapOf("entities" to mapEntities))
        }
    }

    fun signatureToString(sig: HierarchicalMethodSignature?): String {
        if (sig == null) return "<unknownMethod>"
        val method: PsiMethod = sig.method
        val returnType = method.returnType?.presentableText ?: "void"
        val params = method.parameterList.parameters.joinToString(", ") { p -> "${p.type.presentableText} ${p.name}" }
        val className = method.containingClass?.qualifiedName ?: "<unknownClass>"
        return "$returnType $className.${method.name}($params)"
    }
}
