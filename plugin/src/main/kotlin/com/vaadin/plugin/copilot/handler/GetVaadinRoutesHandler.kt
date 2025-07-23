package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.util.Query
import io.netty.handler.codec.http.HttpResponseStatus

class GetVaadinRoutesHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        return ApplicationManager.getApplication().runReadAction<HandlerResponse> {
            val facade = JavaPsiFacade.getInstance(project)
            val scope = GlobalSearchScope.allScope(project)

            val annotationClass =
                facade.findClass("com.vaadin.flow.router.Route", scope)
                    ?: return@runReadAction HandlerResponse(
                        HttpResponseStatus.NOT_FOUND, mapOf("error" to "Annotation class @Route not found"))

            val query: Query<PsiClass> = AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope)
            val classNames = query.findAll().mapNotNull { it.qualifiedName }

            HandlerResponse(status = HttpResponseStatus.OK, data = mapOf("classes" to classNames))
        }
    }
}
