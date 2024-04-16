package com.vaadin.plugin.starter

data class StarterSupportMatrixElement(
    val framework: String,
    val languages: Collection<String>,
    val architectures: Collection<String>,
    val buildTools: Collection<String>,
    val javaMinVersion: Int
)
