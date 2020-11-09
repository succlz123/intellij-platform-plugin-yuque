package org.succlz123.plugins.yuque.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.fxml.FXMLLoader
import javafx.fxml.JavaFXBuilderFactory
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.paint.Color
import org.succlz123.plugins.yuque.support.YuqueArticleList
import org.succlz123.plugins.yuque.support.YuqueHelper
import org.succlz123.plugins.yuque.support.YuqueRepoList
import org.succlz123.plugins.yuque.ui.article.ArticleDetailSceneController
import org.succlz123.plugins.yuque.ui.main.MainSceneController
import org.succlz123.plugins.yuque.ui.repo.RepoDetailSceneController

class YuqueWindowFactory : ToolWindowFactory {

    companion object {
        const val TAG = "Yuque"
        const val YUQUE_INFO = "yuque_info"

        fun saveInfo(info: String) {
//            val credentialAttributes = CredentialAttributes(YUQUE_INFO, "123")
//            val credentials = Credentials("yuque", info)
//            PasswordSafe.instance.set(credentialAttributes, credentials)
            PropertiesComponent.getInstance().setValue(YUQUE_INFO, info)
        }

        fun getInfo(): String? {
//            val credentialAttributes = CredentialAttributes(YUQUE_INFO, "123")
//            val credentials = PasswordSafe.instance.get(credentialAttributes)
//            if (credentials != null) {
//                return credentials.getPasswordAsString()
//            }
//            return null
            return PropertiesComponent.getInstance().getValue(YUQUE_INFO)
        }
    }

    private lateinit var jfxPanel: JFXPanel
    private lateinit var yuqueHelper: YuqueHelper

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        jfxPanel = JFXPanel()
        yuqueHelper = YuqueHelper()
        val component = toolWindow.component
        Platform.setImplicitExit(false)
        Platform.runLater {
            gotoMain(project, true)
        }
        component.parent.add(jfxPanel)
    }

    fun gotoMain(project: Project, fromFirstInit: Boolean) {
        val c = MainSceneController(yuqueHelper, fromFirstInit) { repo ->
            gotoRepoDetail(project, repo, true)
        }
        c.project = project
        replaceSceneContent(c, "/fxml/yuque_main.fxml")
    }

    fun gotoRepoDetail(project: Project, repo: YuqueRepoList, fromFirstInit: Boolean) {
        val c = RepoDetailSceneController(
            yuqueHelper,
            repo,
            fromFirstInit,
            { gotoMain(project, false) },
            { gotoArticleDetail(project, repo, it) }
        )
        c.project = project
        replaceSceneContent(c, "/fxml/yuque_repo_detail.fxml")
    }

    fun gotoArticleDetail(project: Project, repo: YuqueRepoList, article: YuqueArticleList) {
        val c = ArticleDetailSceneController(yuqueHelper, repo, article) {
            gotoRepoDetail(project, repo, false)
        }
        c.project = project
        replaceSceneContent(c, "/fxml/yuque_article_detail.fxml")
    }

    private fun replaceSceneContent(controller: Any, fxml: String) {
        val fxmlLoader = FXMLLoader()
        fxmlLoader.builderFactory = JavaFXBuilderFactory()
        val location = javaClass.getResource(fxml)
        fxmlLoader.location = location
        fxmlLoader.setController(controller)
        val root = fxmlLoader.load<Parent>()
        val scene = Scene(root, Color.ALICEBLUE)
        jfxPanel.scene = scene
    }
}
