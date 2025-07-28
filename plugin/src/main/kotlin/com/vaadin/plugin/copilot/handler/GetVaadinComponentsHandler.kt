package com.vaadin.plugin.copilot.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.Query
import io.netty.handler.codec.http.HttpResponseStatus

class GetVaadinComponentsHandler(project: Project) : AbstractHandler(project) {

    override fun run(): HandlerResponse {
        return ApplicationManager.getApplication().runReadAction<HandlerResponse> {
            val facade = JavaPsiFacade.getInstance(project)
            val scope = GlobalSearchScope.allScope(project)
            val componentClass =
                facade.findClass("com.vaadin.flow.component.Component", scope)
                    ?: return@runReadAction HandlerResponse(
                        HttpResponseStatus.NOT_FOUND, mapOf("error" to "Base class Component not found"))

            val index: ProjectFileIndex = ProjectRootManager.getInstance(project).fileIndex
            val query: Query<PsiClass> = ClassInheritorsSearch.search(componentClass, scope, true)

            val classesInfo =
                query.findAll().mapNotNull { psi ->
                    val fqName = psi.qualifiedName ?: return@mapNotNull null
                    val vfile = psi.containingFile?.virtualFile ?: return@mapNotNull null

                    val origin: String
                    val sourceName: String
                    val path: String

                    when {
                        index.isInLibraryClasses(vfile) -> {
                            origin = "library"
                            val entries: List<OrderEntry> = index.getOrderEntriesForFile(vfile)
                            // Try to get JAR path from orderEntries' CLASS roots
                            val jarPaths =
                                entries
                                    .flatMap { entry -> entry.getFiles(OrderRootType.CLASSES).toList() }
                                    .mapNotNull { it.path }
                                    .distinct()
                            path = jarPaths.firstOrNull() ?: "unknown.jar"
                            sourceName = entries.firstOrNull()?.presentableName ?: "unknown-library"
                        }
                        index.isInSourceContent(vfile) -> {
                            origin = "source"
                            // File path from project content
                            path = vfile.path
                            sourceName = psi.qualifiedName?.substringBeforeLast('.', "") ?: "unknown-source"
                        }
                        else -> {
                            origin = "unknown"
                            path = vfile.path
                            sourceName = "unknown"
                        }
                    }

                    mapOf("class" to fqName, "origin" to origin, "source" to sourceName, "path" to path)
                }
            val componentsFiltered =
                classesInfo.filter { it["origin"] != "library" && !(it["source"]?.contains("com.vaadin") ?: false) }
            HandlerResponse(status = HttpResponseStatus.OK, data = mapOf("classes" to componentsFiltered))
        }
    }
}
