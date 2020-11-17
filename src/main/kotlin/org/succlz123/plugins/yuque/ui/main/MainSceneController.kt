package org.succlz123.plugins.yuque.ui.main

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
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import org.succlz123.plugins.yuque.support.YuqueHelper
import org.succlz123.plugins.yuque.support.YuqueRepoList
import org.succlz123.plugins.yuque.ui.YuqueWindowFactory
import org.succlz123.plugins.yuque.ui.YuqueWindowFactory.Companion.getInfo
import org.succlz123.plugins.yuque.ui.YuqueWindowFactory.Companion.saveInfo

open class MainSceneController(val yuqueHelper: YuqueHelper, var force: Boolean, val callback: (repo: YuqueRepoList) -> Unit) {
    var project: Project? = null

    @FXML
    private lateinit var android_icon: ImageView

    @FXML
    private lateinit var content: VBox

    @FXML
    private lateinit var input_token: TextField

    @FXML
    private lateinit var token_layout: VBox

    @FXML
    private lateinit var ok_layout: HBox

    @FXML
    private lateinit var logout_layout: HBox

    @FXML
    private lateinit var logout_button: Button

    @FXML
    private lateinit var refresh_button: Button

    @FXML
    private lateinit var list_view: ListView<YuqueRepoList>

    @FXML
    fun onOkClick(event: ActionEvent?) {
        val token = input_token.text
        if (token.length < 16) {
            ApplicationManager.getApplication().invokeLater(
                    {
                        Messages.showMessageDialog(project, "Please input the valid Yuque Token!", YuqueWindowFactory.TAG, Messages.getInformationIcon())
                    },
                    ModalityState.NON_MODAL
            )
            return
        }
        ProgressManager.getInstance().runProcess({
            yuqueHelper.initToken(token)
            val userInfo = yuqueHelper.fetchUserInfo(force)
            if (userInfo == null || userInfo.login.isNullOrEmpty()) {
                ApplicationManager.getApplication().invokeLater(
                        {
                            Messages.showMessageDialog(project, "Login Failed!", YuqueWindowFactory.TAG, Messages.getInformationIcon())
                        },
                        ModalityState.NON_MODAL
                )
                return@runProcess
            }
            Platform.runLater {
                saveInfo("$token-/-/-${userInfo.name}-/-/-${userInfo.login}")
                userInfo.token = token
                hideTokenInput(userInfo.name)
            }
            getRepoList(userInfo.login, true)
        }, ProgressIndicatorProvider.getGlobalProgressIndicator())
    }

    @FXML
    fun onLogoutClick(event: ActionEvent?) {
        showTokenInput()
    }

    @FXML
    fun onRefreshClick(event: ActionEvent?) {
        initContentView(true)
    }

    private fun showTokenInput() {
        token_layout.isVisible = true
        token_layout.isManaged = true
        ok_layout.isVisible = true
        ok_layout.isManaged = true
        logout_layout.isVisible = false
        logout_layout.isManaged = false
        refresh_button.isVisible = false
        refresh_button.isManaged = false
        list_view.items = null
        list_view.placeholder = Label("No Yuque Token")
        saveInfo("")
        force = true
    }

    private fun hideTokenInput(name: String?) {
        token_layout.isVisible = false
        token_layout.isManaged = false
        ok_layout.isVisible = false
        ok_layout.isManaged = false
        logout_layout.isVisible = true
        logout_layout.isManaged = true
        refresh_button.isVisible = true
        refresh_button.isManaged = true
        logout_button.text = "${name} - Switch Account"
    }

    private fun getRepoList(login: String?, force: Boolean) {
        ProgressManager.getInstance().runProcess({
            Platform.runLater {
                list_view.placeholder = Label("Loading Repository List......")
            }
            val repos = yuqueHelper.fetchUserRepo(login, force)
            if (repos.isNullOrEmpty()) {
                Platform.runLater {
                    val error = "Get Repository List Failed, Please Check Your Token or Network!"
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
            val list = FXCollections.observableArrayList<YuqueRepoList>()
            for (repo in repos) {
                list.add(repo)
            }
            Platform.runLater {
                list_view.placeholder = null
                list_view.setCellFactory {
                    object : ListCell<YuqueRepoList>() {
                        override fun updateItem(item: YuqueRepoList?, empty: Boolean) {
                            super.updateItem(item, empty)
                            if (empty) {
                                text = null
                                this.onMouseClicked = null
                            } else {
                                text = item?.name
                                this.setOnMouseClicked { mouseClickedEvent ->
                                    if (mouseClickedEvent.button == MouseButton.PRIMARY && mouseClickedEvent.clickCount == 2) {
                                        val si = list_view.selectionModel.selectedItem ?: return@setOnMouseClicked
                                        callback.invoke(si)
                                    }
                                }
                            }
                        }
                    }
                }
                list_view.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue -> }
                list_view.items = list
                list_view.onMouseClicked = null
            }
        }, ProgressIndicatorProvider.getGlobalProgressIndicator())
    }

    @FXML
    fun initialize() {
        val resource = javaClass.getResource("/META-INF/yuque.png")
        android_icon.image = Image(resource.toString())
        list_view.isEditable = false
        list_view.prefHeightProperty().bind(content.prefHeightProperty())
        initContentView(force)
    }

    private fun initContentView(force: Boolean) {
        val str = getInfo()
        if (str.isNullOrEmpty()) {
            showTokenInput()
            return
        }
        val token = str.split("-/-/-")[0]
        val name = str.split("-/-/-")[1]
        val login = str.split("-/-/-")[2]
        if (token.isEmpty() || name.isEmpty() || login.isEmpty()) {
            showTokenInput()
        } else {
            hideTokenInput(name)
            yuqueHelper.initToken(token)
            getRepoList(login, force)
        }
    }
}
