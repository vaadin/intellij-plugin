package com.vaadin.plugin.module

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.joinCanonicalPath
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withPathToTextConvertor
import com.intellij.openapi.ui.BrowseFolderDescriptor.Companion.withTextToPathConvertor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.getCanonicalPath
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.UIBundle
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindText
import com.vaadin.plugin.starter.HelloWorldModel
import com.vaadin.plugin.starter.StarterProjectModel
import com.vaadin.plugin.utils.VaadinProjectUtil.Companion.PROJECT_MODEL_PROP_KEY
import java.io.File
import org.jetbrains.annotations.Nls

class VaadinPanel(propertyGraph: PropertyGraph, private val wizardContext: WizardContext, builder: Panel) {

    private val MAX_LINE_LENGTH = 60

    private val entityNameProperty = propertyGraph.lazyProperty(::suggestName)
    private val locationProperty = propertyGraph.lazyProperty(::suggestLocationByName)
    private val canonicalPathProperty = locationProperty.joinCanonicalPath(entityNameProperty)

    private var starterProjectGroup: CollapsibleRow? = null
    private var helloWorldGroup: CollapsibleRow? = null

    private val starterProjectModel = StarterProjectModel()
    private val starterProjectPanel = StarterProjectPanel(starterProjectModel)

    private val helloWorldModel = HelloWorldModel()
    private val helloWorldPanel = HelloWorldPanel(helloWorldModel)

    init {
        builder.panel {
            row("Name:") { textField().bindText(entityNameProperty) }
            row("Location:") {
                val commentLabel =
                    projectLocationField(locationProperty, wizardContext)
                        .align(AlignX.FILL)
                        .comment(getLocationComment(), 100)
                        .comment!!
                entityNameProperty.afterChange {
                    commentLabel.text = getLocationComment()
                    updateModel()
                }
                locationProperty.afterChange {
                    commentLabel.text = getLocationComment()
                    entityNameProperty.set(suggestName(entityNameProperty.get()))
                    updateModel()
                }
            }

            starterProjectGroup = collapsibleGroup("Starter Project") { row {}.cell(starterProjectPanel.root) }

            helloWorldGroup = collapsibleGroup("Hello World Projects") { row {}.cell(helloWorldPanel.root) }
            row { text("Getting Started").bold() }
            row {
                text(
                    "The <a href=\"https://vaadin.com/docs/latest/getting-started\">Getting Started</a> guide will " +
                        "quickly familiarize you with your new Walking Skeleton " +
                        "implementation. You'll learn how to set up your development environment, " +
                        "understand the project structure, and find resources to help you add " +
                        "muscles to your skeleton—transforming it into a fully-featured application.",
                    MAX_LINE_LENGTH)
            }
            row { text("Flow and Hilla").bold() }
            row {
                text(
                    "<a href=\"https://vaadin.com/flow\">Flow framework</a> is the most productive" +
                        " choice, allowing 100% of the user interface to be coded in server-side Java.",
                    MAX_LINE_LENGTH)
            }
            row {
                text(
                    "<a href=\"https://hilla.dev/\">Hilla framework</a>, on the other hand, enables" +
                        " implementation of your user interface with React while automatically connecting it to your" +
                        " Java backend.",
                    MAX_LINE_LENGTH)
            }
        }

        starterProjectGroup!!.expanded = true
        starterProjectGroup!!.addExpandedListener {
            if (it) helloWorldGroup!!.expanded = false
            updateModel()
        }
        helloWorldGroup!!.addExpandedListener {
            if (it) starterProjectGroup!!.expanded = false
            updateModel()
        }

        updateModel()
    }

    private fun suggestName(): String {
        return suggestName("untitled")
    }

    private fun suggestName(prefix: String): String {
        val projectFileDirectory = File(wizardContext.projectFileDirectory)
        return FileUtil.createSequentFileName(projectFileDirectory, prefix, "")
    }

    private fun suggestLocationByName(): String {
        return wizardContext.projectFileDirectory
    }

    private fun getLocationComment(): @Nls String {
        val shortPath = StringUtil.shortenPathWithEllipsis(getPresentablePath(canonicalPathProperty.get()), 60)
        return UIBundle.message(
            "label.project.wizard.new.project.path.description",
            wizardContext.isCreatingNewProjectInt,
            shortPath,
        )
    }

    private fun updateModel() {
        wizardContext.setProjectFileDirectory(canonicalPathProperty.get())
        wizardContext.projectName = entityNameProperty.get()
        wizardContext.defaultModuleName = entityNameProperty.get()
        val projectModel = if (starterProjectGroup!!.expanded) starterProjectModel else helloWorldModel
        wizardContext.getUserData(PROJECT_MODEL_PROP_KEY)?.set(projectModel)
    }

    private fun Row.projectLocationField(
        locationProperty: GraphProperty<String>,
        wizardContext: WizardContext,
    ): Cell<TextFieldWithBrowseButton> {
        val fileChooserDescriptor =
            FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
                .withFileFilter { it.isDirectory }
                .withPathToTextConvertor(::getPresentablePath)
                .withTextToPathConvertor(::getCanonicalPath)
        val property = locationProperty.transform(::getPresentablePath, ::getCanonicalPath)
        return textFieldWithBrowseButton(fileChooserDescriptor, wizardContext.project).bindText(property)
    }
}
