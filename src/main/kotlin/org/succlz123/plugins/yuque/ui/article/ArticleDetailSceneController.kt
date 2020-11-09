package org.succlz123.plugins.yuque.ui.article

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import org.succlz123.plugins.yuque.support.YuqueArticleList
import org.succlz123.plugins.yuque.support.YuqueHelper
import org.succlz123.plugins.yuque.support.YuqueRepoList
import org.succlz123.plugins.yuque.ui.YuqueWindowFactory

open class ArticleDetailSceneController(
    val yuqueHelper: YuqueHelper,
    val repo: YuqueRepoList,
    val article: YuqueArticleList,
    val backCallback: () -> Unit
) {
    var project: Project? = null

    @FXML
    private lateinit var android_icon: ImageView

    @FXML
    private lateinit var content: VBox

    @FXML
    private lateinit var back_button: Button

    @FXML
    private lateinit var refresh_button: Button

    @FXML
    private lateinit var title_label: Label

    @FXML
    private lateinit var webview: WebView

    @FXML
    fun onBackClick(event: ActionEvent?) {
        Platform.runLater {
            backCallback.invoke()
        }
    }

    @FXML
    fun onRefreshClick(event: ActionEvent?) {
        getArticleDetail()
    }

    private fun getArticleDetail() {
        ProgressManager.getInstance().runProcess({
            val articles = yuqueHelper.fetchArticleDetail(repo.namespace, article)
            if (articles == null) {
                Platform.runLater {
                    val error = "Get Article Detail Failed, Please Check Your Token or Network!"
                    ApplicationManager.getApplication().invokeLater(
                        {
                            Messages.showMessageDialog(project, error, YuqueWindowFactory.TAG, Messages.getInformationIcon())
                        },
                        ModalityState.NON_MODAL
                    )
                }
                return@runProcess
            }
            Platform.runLater {
                webview.engine.userStyleSheetLocation = "data:,body { font: 14px Arial; }"
                webview.engine.loadContent(articles.body_html)
            }
        }, ProgressIndicatorProvider.getGlobalProgressIndicator())
    }

    @FXML
    fun initialize() {
        val resource = javaClass.getResource("/META-INF/yuque.png")
        android_icon.image = Image(resource.toString())
        title_label.text = if (article.title?.length ?: 0 > 32) {
            article.title?.substring(0, 32) + "..."
        } else {
            article.title
        }
        webview.prefHeightProperty().bind(content.prefHeightProperty())

        getArticleDetail()
    }
}
