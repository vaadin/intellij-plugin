package com.vaadin.plugin.endpoints

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnchor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.util.Processor
import org.jetbrains.uast.UClass
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElementOfType

internal const val VAADIN_ROUTE = "com.vaadin.flow.router.Route"
internal const val VAADIN_APP_SHELL_CONFIGURATOR = "com.vaadin.flow.component.page.AppShellConfigurator"
internal const val VAADIN_ID = "com.vaadin.flow.component.template.Id"
internal const val VAADIN_TAG = "com.vaadin.flow.component.Tag"
internal const val HILLA_BROWSER_CALLABLE = "com.vaadin.hilla.BrowserCallable"
internal const val PERSISTENCE_ENTITY = "jakarta.persistence.Entity"

fun findFlowRoutes(project: Project, scope: GlobalSearchScope): Collection<VaadinRoute> {
    val vaadinRouteClass =
        JavaPsiFacade.getInstance(project).findClass(VAADIN_ROUTE, ProjectScope.getLibrariesScope(project))
            ?: return emptyList()

    val routes = ArrayList<VaadinRoute>()

    AnnotatedElementsSearch.searchPsiClasses(vaadinRouteClass, scope)
        .forEach(
            Processor { psiClass ->
                val uClass = psiClass.toUElementOfType<UClass>()
                val sourcePsi = uClass?.sourcePsi
                val className = psiClass.name

                if (sourcePsi == null || className == null) return@Processor true
                val uAnnotation = uClass.findAnnotation(VAADIN_ROUTE) ?: return@Processor true

                val urlMapping = uAnnotation.findAttributeValue("value")?.evaluateString() ?: ""

                routes.add(VaadinRoute(urlMapping, className, PsiAnchor.create(sourcePsi)))

                true
            })

    return routes.toList()
}

fun findHillaEndpoints(project: Project, scope: GlobalSearchScope): Collection<VaadinRoute> {
    val hillaBrowserCallableClass =
        JavaPsiFacade.getInstance(project).findClass(HILLA_BROWSER_CALLABLE, ProjectScope.getLibrariesScope(project))
            ?: return emptyList()

    val endpoints = ArrayList<VaadinRoute>()

    AnnotatedElementsSearch.searchPsiClasses(hillaBrowserCallableClass, scope)
        .forEach(
            Processor { psiClass ->
                val uClass = psiClass.toUElementOfType<UClass>()
                val sourcePsi = uClass?.sourcePsi
                val className = psiClass.name

                if (sourcePsi == null || className == null) return@Processor true

                endpoints.add(VaadinRoute(className, className, PsiAnchor.create(sourcePsi)))

                true
            })

    return endpoints.toList()
}

fun findEntities(project: Project, scope: GlobalSearchScope): Collection<Entity> {
    val entityClass =
        JavaPsiFacade.getInstance(project).findClass(PERSISTENCE_ENTITY, ProjectScope.getLibrariesScope(project))
            ?: return emptyList()

    val entities = ArrayList<Entity>()

    AnnotatedElementsSearch.searchPsiClasses(entityClass, scope)
        .forEach(
            Processor { psiClass ->
                val uClass = psiClass.toUElementOfType<UClass>()
                val sourcePsi = uClass?.sourcePsi
                val className = psiClass.name
                val path = psiClass.containingFile?.virtualFile?.path
                if (sourcePsi == null || className == null) return@Processor true

                entities.add(Entity(className, psiClass.visibleSignatures, path ?: "unknown"))

                true
            })

    return entities.toList()
}
