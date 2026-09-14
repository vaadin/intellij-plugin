package com.vaadin.plugin.copilot.service

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDocument
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.vaadin.plugin.copilot.handler.HandlerResponse
import com.vaadin.plugin.copilot.handler.RedoHandler
import com.vaadin.plugin.copilot.handler.UndoHandler
import com.vaadin.plugin.copilot.handler.WriteFileHandler
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/**
 * A [HeavyPlatformTestCase] rather than a JUnit 5 `@TestApplication` class: the JUnit 5 test application tears down
 * inside a fixed twenty second budget, which a slow CI agent misses. This base class reaches the same checks through
 * TestApplicationManager, which has no such deadline.
 *
 * Heavy rather than light, because the handlers resolve real java.io.File paths under the project's base directory, and
 * a light fixture's project lives in an in-memory filesystem.
 */
class CopilotUndoManagerTest : HeavyPlatformTestCase() {

    private lateinit var tempFile: File

    private val undoManager: CopilotUndoManager
        get() = project.getService(CopilotUndoManager::class.java)

    override fun setUp() {
        super.setUp()
        // the handlers resolve paths against it, and a file that does not exist yet is checked
        // through its parent directory, so it has to be there before the first write
        Files.createDirectories(Path.of(project.basePath!!))
        tempFile = File("${project.basePath}/${UUID.randomUUID()}.tmp")
        tempFile.deleteOnExit()
    }

    /** The handlers open what they write in an editor, and the project outlives the test. */
    override fun tearDown() {
        try {
            runInEdtAndWait {
                val manager = FileEditorManager.getInstance(project)
                manager.openFiles.forEach { manager.closeFile(it) }
                EditorHistoryManager.getInstance(project).removeAllFiles()
            }
        } finally {
            super.tearDown()
        }
    }

    fun testNewFileIsCreatedUndoneAndRedone() {
        callFileWriteHandler(tempFile.path, "Some changes")

        assertNotNull(findFile())
        assertNotNull(undoManager.peekUndoBatch(findFile()!!))

        // undo should remove the file
        callUndoHandler(tempFile.path)
        assertNull(findFile())

        // redo should recreate it
        callRedoHandler(tempFile.path)
        val vfsFile = findFile()
        assertNotNull(vfsFile)
        assertEquals("Some changes", contentOf(vfsFile!!))
    }

    fun testExistingFileIsWrittenUndoneAndRedone() {
        val vfsFile = createFile("Original Content")

        callFileWriteHandler(vfsFile.path, "Some changes")
        assertEquals("Some changes", contentOf(vfsFile))

        callUndoHandler(vfsFile.path)
        assertEquals("Original Content", contentOf(vfsFile))

        callRedoHandler(vfsFile.path)
        assertEquals("Some changes", contentOf(vfsFile))
    }

    /** An action on save landing right after the write is reverted together with it. */
    fun testImmediateActionOnSaveIsRevertedWithTheWrite() {
        val vfsFile = createFile("Original Content")

        callFileWriteHandler(vfsFile.path, "Change      one")
        runCommand(vfsFile, "Change one", "Reformat Code")

        callUndoHandler(vfsFile.path)
        assertEquals("Original Content", contentOf(vfsFile))
    }

    /**
     * The same, for an action on save that takes longer to arrive. It used to fall outside the one second window the
     * write was counted in, which either dropped the whole undo history of the file or left the write with a command
     * count one too low to revert it.
     */
    fun testDelayedActionOnSaveIsRevertedWithTheWrite() {
        val vfsFile = createFile("Original Content")

        callFileWriteHandler(vfsFile.path, "Change      one")
        Thread.sleep(1500)
        runCommand(vfsFile, "Change one", "Reformat Code")

        callUndoHandler(vfsFile.path)
        assertEquals("Original Content", contentOf(vfsFile))
    }

    /**
     * A command of the user's own, made after the write, sits on top of it in the IDE undo stack and can only be
     * reverted together with it. What must not happen is reverting the user's command while leaving the Copilot write
     * in the file.
     */
    fun testUserEditAfterTheWriteIsRevertedWithIt() {
        val vfsFile = createFile("Original Content")

        callFileWriteHandler(vfsFile.path, "Copilot content")
        runCommand(vfsFile, "Copilot content, edited by hand", "Typing")

        callUndoHandler(vfsFile.path)
        assertEquals("Original Content", contentOf(vfsFile))
    }

    fun testConsecutiveWritesAreUndoneOneByOne() {
        val vfsFile = createFile("Original Content")

        callFileWriteHandler(vfsFile.path, "Change      one")
        runCommand(vfsFile, "Change one", "Reformat Code")
        Thread.sleep(1500)

        callFileWriteHandler(vfsFile.path, "Change      two")
        runCommand(vfsFile, "Change two", "Reformat Code")
        Thread.sleep(1500)

        callFileWriteHandler(vfsFile.path, "Change      three")
        runCommand(vfsFile, "Change three", "Reformat Code")

        callUndoHandler(vfsFile.path)
        assertEquals("Change two", contentOf(vfsFile))

        callUndoHandler(vfsFile.path)
        assertEquals("Change one", contentOf(vfsFile))

        callUndoHandler(vfsFile.path)
        assertEquals("Original Content", contentOf(vfsFile))
    }

    fun testUndoOfAFileCopilotNeverWroteDoesNothing() {
        val vfsFile = createFile("Original Content")

        assertNull(undoManager.peekUndoBatch(vfsFile))
        assertFalse(callUndoHandler(vfsFile.path))
        assertEquals("Original Content", contentOf(vfsFile))
    }

    private fun findFile(): VirtualFile? {
        return computeInEdt { VfsUtil.findFileByIoFile(tempFile, true) }
    }

    private fun createFile(content: String): VirtualFile {
        return runWriteActionAndWait {
            val parent = VfsUtil.createDirectories(tempFile.parent)
            val vfsFile = parent.createChildData(this, tempFile.name)
            VfsUtil.saveText(vfsFile, content)
            vfsFile
        }
    }

    private fun contentOf(vfsFile: VirtualFile): String {
        return computeInEdt { vfsFile.findDocument()?.text ?: VfsUtil.loadText(vfsFile) }
    }

    private fun callFileWriteHandler(file: String, text: String) {
        val data = mapOf<String, Any>("content" to text, "undoLabel" to "Vaadin File Write", "file" to file)
        callHandler { WriteFileHandler(project, data).run() }
    }

    private fun callUndoHandler(file: String): Boolean {
        val data = mapOf<String, Any>("files" to listOf(file))
        return callHandler { UndoHandler(project, data).run() }
    }

    private fun callRedoHandler(file: String): Boolean {
        val data = mapOf<String, Any>("files" to listOf(file))
        return callHandler { RedoHandler(project, data).run() }
    }

    private fun callHandler(call: () -> HandlerResponse): Boolean {
        val response = computeInEdt(call)
        assertEquals(200, response.status.code())
        // handlers schedule their work with runInEdt, which only queues it
        runInEdtAndWait { PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue() }
        return response.data?.get("performed") == true
    }

    private fun <T> computeInEdt(block: () -> T): T {
        var result: T? = null
        runInEdtAndWait { result = block() }
        @Suppress("UNCHECKED_CAST") return result as T
    }

    private fun commitAndFlush(doc: Document) {
        PsiDocumentManager.getInstance(project).commitDocument(doc)
        FileDocumentManager.getInstance().saveDocuments(doc::equals)
    }

    /** Simulates a command made by somebody other than Copilot: an action on save, or the user. */
    private fun runCommand(vfsFile: VirtualFile, content: String, undoLabel: String) {
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
