package org.succlz123.plugins.yuque.ui.repo

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.VBox
import org.succlz123.plugins.yuque.support.YuqueArticleList
import org.succlz123.plugins.yuque.support.YuqueHelper
import org.succlz123.plugins.yuque.support.YuqueRepoList
import org.succlz123.plugins.yuque.ui.YuqueWindowFactory

open class RepoDetailSceneController(
        val yuqueHelper: YuqueHelper,
        val repo: YuqueRepoList,
        val force: Boolean,
        val backCallback: () -> Unit,
        val detailCallback: (article: YuqueArticleList) -> Unit
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
    private lateinit var repo_name_field: Label

    @FXML
    private lateinit var list_view: ListView<YuqueArticleList>

    @FXML
    fun onBackClick(event: ActionEvent?) {
        Platform.runLater {
            backCallback.invoke()
        }
    }

    @FXML
    fun onRefreshClick(event: ActionEvent?) {
        getArticleList(true)
    }

    private fun getArticleList(force: Boolean) {
        ProgressManager.getInstance().runProcess({
            Platform.runLater {
                list_view.placeholder = Label("Loading Article List......")
            }
            val articles = yuqueHelper.fetchArticles(repo.id, force)
            if (articles.isNullOrEmpty()) {
                Platform.runLater {
                    val error = "Get Article List Failed, Please Check Your Token or Network!"
                    list_view.placeholder = Label(error)
                    ApplicationManager.getApplication().invokeLater(
                            {
                                Messages.showMessageDialog(project, error, YuqueWindowFactory.TAG, Messages.getInformationIcon())
                            },
                            ModalityState.NON_MODAL
                    )
                }
                return@runProcess
            }
            val list = FXCollections.observableArrayList<YuqueArticleList>()
            for (article in articles) {
                list.add(article)
            }
            Platform.runLater {
                list_view.placeholder = null
                list_view.items = list
                list_view.selectionModel.select(0)
                list_view.setOnMouseClicked {
                    if (it.button == MouseButton.PRIMARY) {
                        if (it.clickCount == 2) {
                            val article = list_view.selectionModel.selectedItem ?: return@setOnMouseClicked
                            detailCallback.invoke(article)
                        }
                    }
                }
                list_view.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue -> }
            }
        }, ProgressIndicatorProvider.getGlobalProgressIndicator())
    }

    @FXML
    fun initialize() {
        val resource = javaClass.getResource("/META-INF/yuque.png")
        android_icon.image = Image(resource.toString())
        repo_name_field.text = repo.name
        list_view.isEditable = false
        list_view.prefHeightProperty().bind(content.prefHeightProperty())

        getArticleList(force)
    }
}
