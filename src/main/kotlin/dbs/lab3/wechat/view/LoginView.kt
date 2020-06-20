package dbs.lab3.wechat.view

import dbs.lab3.wechat.persistence.DbConnection
import dbs.lab3.wechat.persistence.UserTable
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.select
import tornadofx.*
import kotlin.coroutines.CoroutineContext

class LoginView : View(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.JavaFx

    private val username = SimpleStringProperty("zhang3")
    private val password = SimpleStringProperty(" ")
    private val stateString = SimpleStringProperty("")

    override val root = form {
        title = "Wechat Login"
        fieldset {
            field("username") {
                textfield {
                    bind(username)
                }
            }
            field("password") {
                passwordfield {
                    bind(password)
                }
            }
        }
        hbox {
            button("login") {
                setOnAction {
                    launch {
                        val resultSet = DbConnection.execute {
                            UserTable.select {
                                UserTable.userId eq username.get()
                            }.toList()
                        }
                        if (resultSet.isEmpty()) {
                            stateString.set("user does not exist")
                        } else {
                            val user = resultSet.first()
                            val userpwd = user[UserTable.password]
                            val nickname = user[UserTable.nickname]
                            if (password.get() != userpwd) {
                                stateString.set("wrong password")
                            } else {
                                stateString.set("login successfully")
//                                delay(500L)
                                find<MainView>(mapOf(
                                        MainView::userId to username.get(),
                                        MainView::nickname to nickname
                                )).openWindow(owner = null)
                                currentStage?.close()
                            }
                        }
                    }
                }
            }
            label(stateString)
        }
    }
}