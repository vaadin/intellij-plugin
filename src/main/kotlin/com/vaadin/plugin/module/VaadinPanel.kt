package com.vaadin.plugin.module

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.util.first
import com.vaadin.plugin.starter.StarterModel
import com.vaadin.plugin.starter.StarterSupport
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JRadioButton

class VaadinPanel : JPanel() {

    private val model = StarterModel(
        StarterSupport.frameworks.keys.first(),
        StarterSupport.languages.keys.first(),
        StarterSupport.buildTools.keys.first(),
        StarterSupport.architectures.keys.first()
    )

    private val all = mapOf(
        "frameworks" to HashMap<JRadioButton, String>(),
        "languages" to HashMap(),
        "buildTools" to HashMap(),
        "architectures" to HashMap(),
    )

    private var dialogPanel: DialogPanel? = null

    private var kotlinInfo: JEditorPane? = null

    private var notAllArchitecturesSupportedMessage: JEditorPane? = null

    init {
        dialogPanel = panel {
            buttonsGroup {
                row("Framework") {
                    for (el in StarterSupport.frameworks) {
                        val r = radioButton(el.value, el.key).onChanged { refreshSupport() }
                        all["frameworks"]!![r.component] = el.key
                    }
                }
            }.bind(model::framework)
            buttonsGroup {
                row("Language") {
                    for (el in StarterSupport.languages) {
                        val r = radioButton(el.value, el.key).onChanged { refreshSupport() }
                        all["languages"]!![r.component] = el.key
                    }
                }
                row {
                    kotlinInfo = text("Kotlin support uses a community add-on.").component
                }
            }.bind(model::language)
            buttonsGroup {
                row("Build Tool") {
                    for (el in StarterSupport.buildTools) {
                        val r = radioButton(el.value, el.key).onChanged { refreshSupport() }
                        all["buildTools"]!![r.component] = el.key
                    }
                }
            }.bind(model::buildTool)
            buttonsGroup {
                row("Architecture") {
                    for (el in StarterSupport.architectures) {
                        val r = radioButton(el.value, el.key).onChanged { refreshSupport() }
                        all["architectures"]!![r.component] = el.key
                    }
                }
                row {
                    notAllArchitecturesSupportedMessage = text("").component
                }
            }.bind(model::architecture)
        }
        add(dialogPanel)
        refreshArchitecturesSupportedMessage()
        refreshKotlinMessage()
    }

    fun getModel(): StarterModel {
        return model
    }

    /**
     * Enable / disable radio buttons depending on support matrix
     */
    private fun refreshSupport() {
        // apply model updates
        dialogPanel?.apply() ?: return
        refreshGroup(all["frameworks"]!!, StarterSupport::isSupportedFramework)
        refreshGroup(all["languages"]!!, StarterSupport::isSupportedLanguage)
        refreshGroup(all["buildTools"]!!, StarterSupport::isSupportedBuildTool)
        refreshGroup(all["architectures"]!!, StarterSupport::isSupportedArchitecture)
        refreshKotlinMessage()
        refreshArchitecturesSupportedMessage()
    }

    private fun refreshArchitecturesSupportedMessage() {
        if (StarterSupport.supportsAllArchitectures(model)) {
            notAllArchitecturesSupportedMessage!!.isVisible = false
        } else {
            notAllArchitecturesSupportedMessage!!.isVisible = true
            val frameworkName = StarterSupport.frameworks[model.framework]
            notAllArchitecturesSupportedMessage!!.text = "$frameworkName does not support all architectures."
        }
    }

    private fun refreshKotlinMessage() {
        kotlinInfo!!.isVisible = model.language == "kotlin"
    }

    /**
     * Checks all JRadioButtons in given group if they should be disabled, fallbacks to first enabled
     */
    private fun refreshGroup(
        group: HashMap<JRadioButton, String>,
        supportCheck: (model: StarterModel, framework: String) -> Boolean
    ) {
        var selectFirstEnabled = false
        group.forEach {
            it.key.isEnabled = supportCheck(model, it.value)
            if (!it.key.isEnabled && it.key.isSelected) {
                selectFirstEnabled = true
            }
        }
        if (selectFirstEnabled) {
            group.filterKeys { it.isEnabled }.first().key.isSelected = true
        }
    }

}
