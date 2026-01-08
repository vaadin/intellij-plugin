package com.vaadin.plugin.endpoints

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*

/**
 * Custom PSI reference for @StyleSheet annotations that resolves file paths
 * in standard Spring Boot resource locations.
 *
 * This enables Cmd/Ctrl+click navigation from @StyleSheet annotation values
 * to their corresponding CSS/SCSS files.
 */
internal class VaadinStyleSheetReference(
    element: PsiElement,
    textRange: TextRange,
    private val pathString: String
) : PsiReferenceBase<PsiElement>(element, textRange), PsiPolyVariantReference {

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return if (results.size == 1) results[0].element else null
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return ResolveResult.EMPTY_ARRAY

        // Normalize path - remove leading "./" if present
        val normalizedPath = pathString.removePrefix("./")

        // Standard Spring Boot resource locations (matching VSCode implementation)
        val resourcePaths = listOf(
            "src/main/webapp/",
            "src/main/resources/META-INF/resources/",
            "src/main/resources/static/",
            "src/main/resources/public/",
            "src/main/resources/resources/"
        )

        val results = mutableListOf<ResolveResult>()
        val contentRoots = ModuleRootManager.getInstance(module).contentRoots

        for (contentRoot in contentRoots) {
            for (resourcePath in resourcePaths) {
                val fullPath = "$resourcePath$normalizedPath"
                val file = contentRoot.findFileByRelativePath(fullPath)

                if (file != null && file.exists()) {
                    val psiFile = PsiManager.getInstance(project).findFile(file)
                    if (psiFile != null) {
                        results.add(PsiElementResolveResult(psiFile))
                    }
                }
            }
        }

        return results.toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
