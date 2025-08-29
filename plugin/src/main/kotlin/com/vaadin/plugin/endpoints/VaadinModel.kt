package com.vaadin.plugin.endpoints

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnchor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.InheritanceUtil
import com.intellij.util.Processor
import com.intellij.util.Query
import org.jetbrains.uast.UClass
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElementOfType

internal const val VAADIN_ROUTE = "com.vaadin.flow.router.Route"
internal const val VAADIN_APP_SHELL_CONFIGURATOR = "com.vaadin.flow.component.page.AppShellConfigurator"
internal const val VAADIN_ID = "com.vaadin.flow.component.template.Id"
internal const val VAADIN_TAG = "com.vaadin.flow.component.Tag"
internal const val HILLA_BROWSER_CALLABLE = "com.vaadin.hilla.BrowserCallable"
internal const val PERSISTENCE_ENTITY = "jakarta.persistence.Entity"

/**
 * ModulesScope.moduleWithDependenciesAndLibrariesScope(targetModule) does not work so we need to manually filter
 * modules by name.
 */
internal fun findFlowRoutes(project: Project, scope: GlobalSearchScope, moduleName: String?): Collection<VaadinRoute> {
    return ApplicationManager.getApplication().runReadAction<Collection<VaadinRoute>> {
        val vaadinRouteClass =
            JavaPsiFacade.getInstance(project).findClass(VAADIN_ROUTE, ProjectScope.getLibrariesScope(project))
                ?: return@runReadAction emptyList()

        val routes = ArrayList<VaadinRoute>()

        AnnotatedElementsSearch.searchPsiClasses(vaadinRouteClass, scope)
            .forEach(
                Processor { psiClass ->
                    val uClass = psiClass.toUElementOfType<UClass>() ?: return@Processor true
                    val sourcePsi = uClass.sourcePsi ?: return@Processor true
                    val className = psiClass.name ?: return@Processor true

                    val uAnnotation = uClass.findAnnotation(VAADIN_ROUTE) ?: return@Processor true
                    val urlMapping = uAnnotation.findAttributeValue("value")?.evaluateString() ?: ""

                    if (moduleName != null) {
                        val module = ModuleUtilCore.findModuleForPsiElement(psiClass)
                        if (module != null && !module.name.startsWith(moduleName)) {
                            return@Processor true
                        }
                    }

                    val psiJavaFile = psiClass.containingFile as? PsiJavaFile
                    routes.add(
                        VaadinRoute(urlMapping, className, psiJavaFile?.packageName ?: "", PsiAnchor.create(sourcePsi)))
                    true
                })

        routes.toList()
    }
}

internal fun findHillaEndpoints(project: Project, scope: GlobalSearchScope): Collection<VaadinRoute> {
    return ApplicationManager.getApplication().runReadAction<Collection<VaadinRoute>> {
        val hillaBrowserCallableClass =
            JavaPsiFacade.getInstance(project)
                .findClass(HILLA_BROWSER_CALLABLE, ProjectScope.getLibrariesScope(project))
                ?: return@runReadAction emptyList()

        val endpoints = ArrayList<VaadinRoute>()

        AnnotatedElementsSearch.searchPsiClasses(hillaBrowserCallableClass, scope)
            .forEach(
                Processor { psiClass ->
                    val uClass = psiClass.toUElementOfType<UClass>()
                    val sourcePsi = uClass?.sourcePsi
                    val className = psiClass.name

                    if (sourcePsi == null || className == null) return@Processor true

                    val psiJavaFile = psiClass.containingFile as? PsiJavaFile

                    endpoints.add(
                        VaadinRoute(className, className, psiJavaFile?.packageName ?: "", PsiAnchor.create(sourcePsi)))

                    true
                })

        endpoints.toList()
    }
}

internal fun findComponents(project: Project, scope: GlobalSearchScope): Collection<VaadinComponent> {
    return ApplicationManager.getApplication().runReadAction<Collection<VaadinComponent>> {
        val facade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        val componentClass =
            facade.findClass("com.vaadin.flow.component.Component", scope) ?: return@runReadAction emptyList()

        val components = ArrayList<VaadinComponent>()

        val index: ProjectFileIndex = ProjectRootManager.getInstance(project).fileIndex
        val query: Query<PsiClass> = ClassInheritorsSearch.search(componentClass, scope, true)

        query
            .findAll()
            .forEach({ psi ->
                val fqName = psi.qualifiedName ?: return@forEach
                val vfile = psi.containingFile?.virtualFile ?: return@forEach

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

                components.add(
                    VaadinComponent(fqName, origin, sourceName, path, PsiAnchor.create(psi), psi.visibleSignatures))
            })
        val notVaadinComponentsFiltered =
            components
                .filterNot { it.origin == "library" && it.source.contains("com.vaadin") }
                .sortedBy { it.className }
        notVaadinComponentsFiltered
    }
}

internal fun findEntities(project: Project, scope: GlobalSearchScope): Collection<Entity> {
    return ApplicationManager.getApplication().runReadAction<Collection<Entity>> {
        val entityClass =
            JavaPsiFacade.getInstance(project).findClass(PERSISTENCE_ENTITY, ProjectScope.getLibrariesScope(project))
                ?: return@runReadAction emptyList()

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

        entities.toList()
    }
}

internal fun signatureToString(sig: HierarchicalMethodSignature?): String {
    if (sig == null) return "<unknownMethod>"
    val method: PsiMethod = sig.method
    val returnType = method.returnType?.presentableText ?: "void"
    val params = method.parameterList.parameters.joinToString(", ") { p -> "${p.type.presentableText} ${p.name}" }
    val className = method.containingClass?.qualifiedName ?: "<unknownClass>"
    return "$returnType $className.${method.name}($params)"
}

internal fun findSecurityConfig(project: Project, scope: GlobalSearchScope): Collection<VaadinSecurity> {
    return ApplicationManager.getApplication().runReadAction<Collection<VaadinSecurity>> {
        val facade = JavaPsiFacade.getInstance(project)
        val securityClass =
            facade.findClass("com.vaadin.flow.spring.security.VaadinWebSecurity", scope)
                ?: return@runReadAction emptyList()

        val security = ArrayList<VaadinSecurity>()
        val index: ProjectFileIndex = ProjectRootManager.getInstance(project).fileIndex
        val query: Query<PsiClass> = ClassInheritorsSearch.search(securityClass, scope, true)

        query.findAll().forEach { psi ->
            val fqName = psi.qualifiedName ?: return@forEach
            val vfile = psi.containingFile?.virtualFile ?: return@forEach

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

            // Look for setLoginView call
            var loginViewClassName: String? = null
            psi.methods.forEach { method ->
                method.body?.statements?.forEach { stmt ->
                    val callExpr = stmt as? com.intellij.psi.PsiExpressionStatement ?: return@forEach
                    val expr = callExpr.expression as? com.intellij.psi.PsiMethodCallExpression ?: return@forEach
                    val methodName = expr.methodExpression.referenceName

                    if (methodName == "setLoginView") {
                        val args = expr.argumentList.expressions
                        if (args.size == 2) {
                            val arg = args[1]
                            if (arg is com.intellij.psi.PsiClassObjectAccessExpression) {
                                val refClass = arg.operand?.type as? com.intellij.psi.PsiClassType
                                loginViewClassName = refClass?.resolve()?.qualifiedName
                            }
                        }
                    }
                }
            }

            security.add(
                VaadinSecurity(
                    fqName, origin, sourceName, path, PsiAnchor.create(psi), loginViewClassName // added parameter
                    ))
        }

        security.filterNot { it.origin == "library" && it.source.contains("com.vaadin") }.sortedBy { it.className }
    }
}

internal fun findUserDetails(project: Project, scope: GlobalSearchScope): Collection<VaadinUserDetails> {
    return ApplicationManager.getApplication().runReadAction<Collection<VaadinUserDetails>> {
        val facade = JavaPsiFacade.getInstance(project)
        val userDetailsClass =
            facade.findClass("org.springframework.security.core.userdetails.UserDetailsService", scope)
                ?: return@runReadAction emptyList()

        val userDetails = ArrayList<VaadinUserDetails>()
        val index: ProjectFileIndex = ProjectRootManager.getInstance(project).fileIndex
        val query: Query<PsiClass> = ClassInheritorsSearch.search(userDetailsClass, scope, true)

        query.findAll().forEach { psi ->
            val fqName = psi.qualifiedName ?: return@forEach
            val vfile = psi.containingFile?.virtualFile ?: return@forEach

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

            // Extract JPA repository entity types from fields
            val jpaEntities = psi.fields.mapNotNull { field -> extractJpaEntityFromField(field) }
            userDetails.add(VaadinUserDetails(fqName, origin, sourceName, path, PsiAnchor.create(psi), jpaEntities))
        }

        userDetails.filterNot { it.origin == "library" && it.source.contains("com.vaadin") }.sortedBy { it.className }
    }
}

private fun extractJpaEntityFromField(field: PsiField): String? {
    val type = field.type
    if (type !is PsiClassType) return null

    // Resolve the class and its generic substitutor
    val resolveResult: PsiClassType.ClassResolveResult = type.resolveGenerics()
    val resolvedCls: PsiClass? = resolveResult.element
    val substitutor = resolveResult.substitutor
    if (resolvedCls == null) return null

    // Check if type is JpaRepository or subclass
    val jpaRepoFqn = "org.springframework.data.jpa.repository.JpaRepository"
    if (resolvedCls.qualifiedName == jpaRepoFqn || InheritanceUtil.isInheritor(resolvedCls, jpaRepoFqn)) {
        // Get actual type argument for T
        val paramIndexMap = resolvedCls.interfaceTypes
        if (paramIndexMap.isNotEmpty()) {
            // lookup mapping for first parameter
            val interfaceImplemented = paramIndexMap[0]
            if (interfaceImplemented is PsiClassType) {
                val paramsClasses = extractTypeArgumentClasses(interfaceImplemented)
                if (paramsClasses.isNotEmpty()) {
                    return paramsClasses[0].qualifiedName
                }
            }
        }
    }
    return null
}

/**
 * Given a PsiClassType like JpaRepository<User, Long>, returns actual [PsiClass]s for the type arguments in order:
 * [User], [Long].
 */
private fun extractTypeArgumentClasses(type: PsiClassType): List<PsiClass> {
    val result: PsiClassType.ClassResolveResult = type.resolveGenerics()
    val resolvedCls: PsiClass? = result.element
    val substitutor = result.substitutor

    val paramClasses = mutableListOf<PsiClass>()
    if (resolvedCls != null && type.hasParameters()) {
        type.parameters.forEach { param ->
            val substituted: PsiType? = substitutor.substitute(param)
            if (substituted is PsiClassType) {
                substituted.resolve()?.let { paramClasses.add(it) }
            }
        }
    }
    return paramClasses
}
