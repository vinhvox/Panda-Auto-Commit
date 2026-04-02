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
            You are an expert Android developer. I will provide you with git diffs from multiple related files.
            Your task is to write ONE cohesive Conventional Commit message for the entire update.

            CRITICAL INSTRUCTIONS:
            1. MUST include a short, contextual scope in parentheses (e.g., ads, ui, auth, core, db).
            2. The subject MUST start with a lowercase letter and use the imperative mood (e.g., "update ad config", not "Update" or "Updated").
            3. Prioritize logic changes to determine the overarching subject.
            4. Group all .xml file changes into a single bullet point: "- Updated UI layouts and resources". Do NOT detail XML changes or list XML file names.
            5. Keep bullet points concise. Avoid repeating file names excessively if the overall context is clear.

            Format exactly like this:
            Title: type(scope): subject
            Description:
            - [Brief logic change 1 without repeating file names]
            - [Brief logic change 2]
            - Updated UI layouts and resources (if any .xml files were changed)

            Return ONLY the raw text. Do NOT wrap in ```.
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

            // 💡 ĐÃ CẬP NHẬT: Chặn không gửi chi tiết XML để tiết kiệm token và tránh nhiễu AI
            if (fileName.endsWith(".xml")) {
                sb.append("[This is a UI/Resource file. Do not detail its changes. Group it as 'Updated UI/resources']\n\n")
                continue
            }

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