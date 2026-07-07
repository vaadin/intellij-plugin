package com.vaadin.plugin.endpoints

import com.intellij.microservices.url.UrlPath
import com.intellij.microservices.url.UrlPath.PathSegment
import com.intellij.microservices.url.references.UrlPksParser
import com.intellij.psi.util.PartiallyKnownString
import com.intellij.psi.util.SplitEscaper

internal val vaadinUrlPksParser: UrlPksParser =
    UrlPksParser(
        splitEscaper = { _, _ -> SplitEscaper.AcceptAll },
        customPathSegmentExtractor = { part ->
            if (part.startsWith(":")) {
                val varName = part.removePrefix(":").substringBefore("?").substringBefore("(")
                PathSegment.Variable(varName)
            } else {
                PathSegment.Exact(part)
            }
        })

internal fun parseVaadinUrlMapping(urlMapping: String): UrlPath {
    return vaadinUrlPksParser.parseUrlPath(PartiallyKnownString(urlMapping)).urlPath
}
