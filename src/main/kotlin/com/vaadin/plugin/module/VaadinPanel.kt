package com.vaadin.plugin.module

import com.intellij.ide.IdeBundle
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
import com.intellij.ui.dsl.builder.*
import com.vaadin.plugin.utils.VaadinProjectUtil.Companion.PROJECT_MODEL_PROP_KEY
import org.jetbrains.annotations.Nls
import java.io.File

class VaadinPanel(propertyGraph: PropertyGraph, private val wizardContext: WizardContext, builder: Panel) {

    private val entityNameProperty = propertyGraph.lazyProperty(::suggestName)
    private val locationProperty = propertyGraph.lazyProperty(::suggestLocationByName)
    private val canonicalPathProperty = locationProperty.joinCanonicalPath(entityNameProperty)

    private var quickStarterGroup: CollapsibleRow? = null
    private var skeletonStarterGroup: CollapsibleRow? = null

    private val quickStarterPanel = QuickStarterPanel()
    private val skeletonStarterPanel = SkeletonStarterPanel()

    init {
        builder.panel {
            row("Name:") {
                textField().bindText(entityNameProperty)
            }
            row("Location:") {
                val commentLabel = projectLocationField(locationProperty, wizardContext)
                    .align(AlignX.FILL)
                    .comment(getLocationComment(), 100).comment!!
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

            quickStarterGroup = collapsibleGroup("Project Settings") {
                row {}.cell(quickStarterPanel.root)
            }

            skeletonStarterGroup = collapsibleGroup("Hello World Projects") {
                row {}.cell(skeletonStarterPanel.root)
            }
            row {
                text(
                    "<a href=\"https://vaadin.com/flow\">Flow framework</a> is the most productive" +
                            " choice, allowing 100% of the user<br>interface to be coded in server-side Java."
                )
            }
            row {
                text(
                    "<a href=\"https://hilla.dev/\">Hilla framework</a>, on the other hand, enables" +
                            " implementation of your user<br>interface with React while automatically connecting it to your" +
                            " Java backend."
                )
            }
            row {
                text("For more configuration options, visit <a href=\"https://start.vaadin.com\">start.vaadin.com</a>")
            }
        }

        quickStarterGroup!!.expanded = true
        quickStarterGroup!!.addExpandedListener { if (it) skeletonStarterGroup!!.expanded = false; updateModel() }
        skeletonStarterGroup!!.addExpandedListener { if (it) quickStarterGroup!!.expanded = false; updateModel() }

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
            shortPath
        )
    }

    private fun updateModel() {
        wizardContext.setProjectFileDirectory(canonicalPathProperty.get())
        wizardContext.projectName = entityNameProperty.get()
        wizardContext.defaultModuleName = entityNameProperty.get()
        val projectModel = if (quickStarterGroup!!.expanded) quickStarterPanel.model else skeletonStarterPanel.model
        wizardContext.getUserData(PROJECT_MODEL_PROP_KEY)?.set(projectModel)
    }

    private fun Row.projectLocationField(
        locationProperty: GraphProperty<String>,
        wizardContext: WizardContext
    ): Cell<TextFieldWithBrowseButton> {
        val fileChooserDescriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
            .withFileFilter { it.isDirectory }
            .withPathToTextConvertor(::getPresentablePath)
            .withTextToPathConvertor(::getCanonicalPath)
        val title = IdeBundle.message("title.select.project.file.directory", wizardContext.presentationName)
        val property = locationProperty.transform(::getPresentablePath, ::getCanonicalPath)
        return textFieldWithBrowseButton(title, wizardContext.project, fileChooserDescriptor)
            .bindText(property)
    }

}
