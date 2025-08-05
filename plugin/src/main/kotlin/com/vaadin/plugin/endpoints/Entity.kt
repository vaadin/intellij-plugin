package com.vaadin.plugin.endpoints

import com.intellij.psi.HierarchicalMethodSignature

class Entity(val className: String, val visibleMethods: Collection<HierarchicalMethodSignature?>, val path: String) {}
