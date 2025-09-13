package com.vaadin.plugin.utils

fun toArtifactId(name: String): String {
    return name
        .trim()
        .replace(Regex("([a-z])([A-Z])"), "$1-$2") // camelCase to kebab-case
        .replace(Regex("[\\s_]+"), "-") // spaces/underscores to hyphen
        .replace(Regex("[^a-zA-Z0-9-]"), "") // remove invalid chars
        .lowercase()
}
