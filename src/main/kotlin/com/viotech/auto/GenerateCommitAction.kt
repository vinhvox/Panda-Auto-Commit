package com.viotech.auto

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.CommitMessageI

class GenerateCommitAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val checkinPanel = e.getData(CheckinProjectPanel.PANEL_KEY) as? CheckinProjectPanel
        val commitMessageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as? CommitMessageI

        val changes = checkinPanel?.selectedChanges?.toList()
            ?: e.getData(VcsDataKeys.CHANGES)?.toList()
            ?: emptyList()

        if (changes.isEmpty()) {
            Messages.showWarningDialog(project, "No files were found selected. Please select the file you wish to commit", "File Selection Error")
            return
        }

        if (commitMessageControl == null && checkinPanel == null) {
            Messages.showWarningDialog(project, "The Commit Message input field is inaccessible. Please click on the Commit Message field before clicking the button.", "Interface Error")
            return
        }

         val defaultRoleText = """
        You are an expert developer. I will provide you with git diffs of multiple files. 
        Instead of writing one single summary, you MUST analyze and write a commit message for EACH file separately.

        Format your response EXACTLY like this structure for each file:

        File: [File Name]
        Title: [type]([scope]): [subject]
        Description:
        - [Brief detail of change 1]
        - [Brief detail of change 2]

        Do NOT include any general intro or outro. Do NOT wrap the response in markdown code blocks. Return ONLY the raw text.
    """.trimIndent()
        val settings = GeminiSettingsState.instance
        val apiKey = settings.apiKey
        val role = if (settings.useCustomRole) settings.customRoleText else defaultRoleText

        if (apiKey.isBlank()) {
            Messages.showErrorDialog(project, "Gemini API Key has not been configured. Please go to File > Settings > Tools > Panda Commit Auto.", "Configuration Error")
            return
        }

        val gitDiff = buildDiffString(changes)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating commit with AI...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val service = GeminiService()
                    val result = service.generateCommitMessage(apiKey, role, gitDiff)

                    ApplicationManager.getApplication().invokeLater {
                        if (commitMessageControl != null) {
                            commitMessageControl.setCommitMessage(result)
                        } else {
                            checkinPanel?.setCommitMessage(result)
                        }
                    }
                } catch (ex: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(project, "Error calling AI: ${ex.message}", "Error")
                    }
                }
            }
        })
    }

    private fun isIgnoredFile(path: String): Boolean {
        val ignoredKeywords = listOf(
            "/.idea/",
            "/.gradle/",
            "/build/",
            "/captures/",
            "local.properties",
            ".iml"
        )
        return ignoredKeywords.any { path.contains(it) }
    }

    private fun buildDiffString(changes: List<Change>): String {
        val sb = java.lang.StringBuilder()

        val validChanges = changes.filter { change ->
            val fullPath = change.virtualFile?.path ?: change.beforeRevision?.file?.path ?: ""
            !isIgnoredFile(fullPath)
        }

        for (change in validChanges.take(15)) {
            val fileName = change.virtualFile?.name ?: change.beforeRevision?.file?.name ?: continue

            sb.append("--- $fileName ---\n")

            val before = try { change.beforeRevision?.content ?: "" } catch (e: Exception) { "" }
            val after = try { change.afterRevision?.content ?: "" } catch (e: Exception) { "" }

            sb.append("[Before]\n").append(before.take(1500)).append("\n")
            sb.append("[After]\n").append(after.take(1500)).append("\n\n")
        }

        if (sb.isEmpty()) {
            return "No valid code changes found. Only configuration files were modified."
        }

        return sb.toString()
    }
}