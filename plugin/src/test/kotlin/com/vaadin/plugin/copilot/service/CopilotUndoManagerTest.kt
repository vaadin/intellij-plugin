package com.vaadin.plugin.copilot.service

import ai.grazie.utils.mpp.UUID
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.vaadin.plugin.copilot.handler.RedoHandler
import com.vaadin.plugin.copilot.handler.UndoHandler
import com.vaadin.plugin.copilot.handler.WriteFileHandler
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class CopilotUndoManagerTest : BasePlatformTestCase() {

    private lateinit var tempFile: File

    private lateinit var undoManager: CopilotUndoManager

    @BeforeEach
    fun setup() {
        super.setUp()
        DumbService.getInstance(project).waitForSmartMode()
        tempFile = File("${project.basePath}/${UUID.random().text}.tmp")
        tempFile.deleteOnExit()
        Files.createDirectories(Path.of(project.basePath))
        undoManager = project.getService(CopilotUndoManager::class.java)
    }

    @AfterEach
    fun teardown() {
        super.tearDown()
    }

    @Test
    fun test_newFile_createUndoRedo() {
        callFileWriteHandler(tempFile.path, "Some changes")

        var vfsFile = VfsUtil.findFileByIoFile(tempFile, false)
        assertTrue(vfsFile?.exists() == true)

        assertEquals(1, undoManager.getUndoCount(vfsFile!!))

        // simulate save action
        writeFile(vfsFile, "Updated Content", "Format On Save")
        assertEquals(2, undoManager.getUndoCount(vfsFile))
        assertEquals(0, undoManager.getRedoCount(vfsFile))

        // undo should remove file
        callUndoHandler(tempFile.path)
        assertEquals(0, undoManager.getUndoCount(vfsFile))
        assertEquals(2, undoManager.getRedoCount(vfsFile))
        vfsFile = VfsUtil.findFileByIoFile(tempFile, false)
        assertTrue(vfsFile == null)

        // redo should recreate it
        callRedoHandler(tempFile.path)

        vfsFile = VfsUtil.findFileByIoFile(tempFile, false)
        assertTrue(vfsFile?.exists() == true)
        assertEquals(2, undoManager.getUndoCount(vfsFile!!))
        assertEquals(0, undoManager.getRedoCount(vfsFile))
    }

    @Test
    fun test_existingFile_writeUndoRedo() {
        tempFile.createNewFile()
        Files.writeString(tempFile.toPath(), "Original Content")

        val vfsFile = VfsUtil.findFileByIoFile(tempFile, false)!!
        assertTrue(vfsFile.exists())

        callFileWriteHandler(vfsFile.path, "Some changes")

        assertEquals(1, undoManager.getUndoCount(vfsFile))

        // simulate save action
        writeFile(vfsFile, "Updated Content", "Format On Save")
        assertEquals(2, undoManager.getUndoCount(vfsFile))
        assertEquals(0, undoManager.getRedoCount(vfsFile))

        callUndoHandler(vfsFile.path)
        assertEquals(0, undoManager.getUndoCount(vfsFile))
        assertEquals(2, undoManager.getRedoCount(vfsFile))

        callRedoHandler(vfsFile.path)
        assertEquals(2, undoManager.getUndoCount(vfsFile))
        assertEquals(0, undoManager.getRedoCount(vfsFile))

        runWriteActionAndWait { vfsFile.delete(this) }
    }

    @Test
    fun testExistingFile_multipleUndo() {
        tempFile.createNewFile()
        Files.writeString(tempFile.toPath(), "Original Content")

        val vfsFile = VfsUtil.findFileByIoFile(tempFile, false)!!
        assertTrue(vfsFile.exists())

        // write and format first time
        callFileWriteHandler(vfsFile.path, "Change      one")
        writeFile(vfsFile, "Change one", "Format On Save")

        // idle time
        Thread.sleep(1000)

        // write and format second time
        callFileWriteHandler(vfsFile.path, "Change      two")
        writeFile(vfsFile, "Change two", "Format On Save")

        // idle time
        Thread.sleep(1000)

        // write and format third time
        callFileWriteHandler(vfsFile.path, "Change      three")
        writeFile(vfsFile, "Change three", "Format On Save")

        // 3 Copilot consecutive operations -> all should be possible to undo

        // call undo first time
        callUndoHandler(vfsFile.path)
        assertEquals(2, undoManager.getUndoCount(vfsFile))
        assertEquals(2, undoManager.getRedoCount(vfsFile))

        // call undo second time
        callUndoHandler(vfsFile.path)
        assertEquals(2, undoManager.getUndoCount(vfsFile))
        assertEquals(2, undoManager.getRedoCount(vfsFile))

        // call undo third time
        callUndoHandler(vfsFile.path)
        assertEquals(0, undoManager.getUndoCount(vfsFile))
        assertEquals(2, undoManager.getRedoCount(vfsFile))
    }

    private fun callFileWriteHandler(file: String, text: String) {
        val data = mapOf<String, Any>("content" to text, "undoLabel" to "Vaadin File Write", "file" to file)
        runInEdtAndWait {
            val response = WriteFileHandler(project, data).run()
            assertEquals(200, response.status.code())
        }
    }

    private fun callUndoHandler(file: String) {
        val data = mapOf<String, Any>("files" to listOf(file))
        runInEdtAndWait {
            val response = UndoHandler(project, data).run()
            assertEquals(200, response.status.code())
        }
    }

    private fun callRedoHandler(file: String) {
        val data = mapOf<String, Any>("files" to listOf(file))
        runInEdtAndWait {
            val response = RedoHandler(project, data).run()
            assertEquals(200, response.status.code())
        }
    }

    private fun commitAndFlush(doc: Document) {
        PsiDocumentManager.getInstance(project).commitDocument(doc)
        FileDocumentManager.getInstance().saveDocuments(doc::equals)
    }

    private fun writeFile(vfsFile: VirtualFile, content: String, undoLabel: String = "Test Write") {
        runInEdtAndWait {
            CommandProcessor.getInstance()
                .executeCommand(
                    project,
                    {
                        runWriteActionAndWait {
                            vfsFile.findDocument()!!.let { doc ->
                                doc.setText(content)
                                commitAndFlush(doc)
                            }
                        }
                    },
                    undoLabel,
                    null,
                    UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION,
                )
        }
    }
}
