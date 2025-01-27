package com.vaadin.plugin.copilot.service

import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.lang.properties.psi.Property
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.vaadin.plugin.psi.TranslationPropertyReference
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HillaReferenceContributorTest : BasePlatformTestCase() {

    private val key = "button.label"
    private val value = "Click"

    @BeforeEach
    public override fun setUp() {
        super.setUp()
    }

    @AfterEach
    public override fun tearDown() {
        super.tearDown()
    }

    @Test
    fun hillaReferenceInjectedAndResolved() {
        val viewContent =
            """
            import { translate } from "@vaadin/hilla-react-i18n";
                
            myButton.label = translate("$key")
            """
                .trimIndent()

        val viewFile = myFixture.configureByText(TypeScriptFileType.INSTANCE, viewContent)
        myFixture.addFileToProject("vaadin-i18n/translations.properties", "$key=$value")

        runInEdtAndWait {
            myFixture.openFileInEditor(viewFile.virtualFile)
            myFixture.editor.caretModel.moveToOffset(viewContent.indexOf(key) + 1)

            val ref = myFixture.getReferenceAtCaretPosition()
            assertNotNull(ref)
            assertTrue(ref is TranslationPropertyReference)

            val property = ref!!.resolve() as Property
            assertNotNull(property)

            assertEquals(value, property.value)
        }
    }

    @Test
    fun hillaReferenceNotResolvedDueToMissingImport() {
        val viewContent =
            """               
            myButton.label = translate("$key")
            """
                .trimIndent()

        val viewFile = myFixture.configureByText(TypeScriptFileType.INSTANCE, viewContent)
        myFixture.addFileToProject("vaadin-i18n/translations.properties", "$key=$value")

        runInEdtAndWait {
            myFixture.openFileInEditor(viewFile.virtualFile)
            myFixture.editor.caretModel.moveToOffset(viewContent.indexOf(key) + 1)

            val ref = myFixture.getReferenceAtCaretPosition()
            assertTrue(ref == null || ref !is TranslationPropertyReference)
        }
    }
}
