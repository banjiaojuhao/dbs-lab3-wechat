package dbs.lab3.wechat.view

import dbs.lab3.wechat.persistence.DbConnection
import dbs.lab3.wechat.persistence.FavoriteTable
import javafx.beans.property.SimpleStringProperty
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import tornadofx.*
import kotlin.coroutines.CoroutineContext

data class Favorite(val id: Int, val content: String, val timestamp: Long)

class FavoriteView : View(), CoroutineScope {
    override lateinit var coroutineContext: CoroutineContext
    val userId: String by param()
    private val favoriteList = arrayListOf<Favorite>().asObservable()

    override fun onDock() {
        super.onDock()
        coroutineContext = MainScope().coroutineContext
        launch {
            while (true) {
                DbConnection.execute {
                    FavoriteTable.select {
                        FavoriteTable.saver eq userId
                    }.forEach {
                        val id = it[FavoriteTable.id].value
                        if (!favoriteList.any { it.id == id }) {
                            favoriteList.add(Favorite(id, it[FavoriteTable.content], it[FavoriteTable.timestamp]))
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    override fun onUndock() {
        this.cancel()
        super.onUndock()
    }

    override val root = vbox {
        listview(favoriteList) {
            cellFormat { favorite ->
                graphic = hbox {
                    label(favorite.content)
                    button("delete") {
                        setOnAction {
                            launch {
                                DbConnection.execute {
                                    FavoriteTable.deleteWhere {
                                        FavoriteTable.id eq favorite.id
                                    }
                                }
                                favoriteList.removeIf { it.id == favorite.id }
                            }
                        }
                    }
                }
            }
        }
        hbox {
            val text = SimpleStringProperty()
            textfield(text)
            button("add") {
                enableWhen {
                    text.isNotEmpty
                }
                setOnAction {
                    launch {
                        DbConnection.execute {
                            FavoriteTable.insert {
                                it[saver] = userId
                                it[content] = text.get()
                                it[timestamp] = System.currentTimeMillis()
                            }
                        }
                        text.set("")
                    }
                }
            }
        }
    }
}