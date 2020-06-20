package dbs.lab3.wechat.view

import dbs.lab3.wechat.persistence.*
import javafx.beans.binding.Bindings
import javafx.collections.ObservableList
import javafx.scene.control.TextInputDialog
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import tornadofx.*
import kotlin.coroutines.CoroutineContext

data class Comment(val id: Int, val sender: String, val senderName: String, val content: String, val timestamp: Long)

data class Moment(val id: Int, val sender: String, val senderName: String, val content: String, val timestamp: Long, val commentList: ObservableList<Comment> = arrayListOf<Comment>().asObservable())


class MomentView : View(), CoroutineScope {
    override lateinit var coroutineContext: CoroutineContext
    val userId: String by param()
    private val momentList = arrayListOf<Moment>().asObservable()

    override fun onDock() {
        super.onDock()
        coroutineContext = MainScope().coroutineContext
        launch {
            while (true) {
                DbConnection.execute {
                    MomentTable
                            .leftJoin(UserTable, { sender }, { userId })
                            .select {
                                ((MomentTable.sender eq userId)
                                        or (MomentTable.sender inSubQuery (
                                        FriendshipTable.slice(FriendshipTable.right).select {
                                            FriendshipTable.left eq userId
                                        })))
                            }.forEach {
                                val id = it[MomentTable.id].value
                                if (!momentList.any { it.id == id }) {
                                    momentList.add(Moment(id, it[UserTable.userId],it[UserTable.nickname],
                                            it[MomentTable.content], it[MomentTable.timestamp]))
                                }
                            }
                    momentList.forEach { moment ->
                        val commentList = moment.commentList
                        MomentCommentTable
                                .leftJoin(UserTable, { sender }, { userId })
                                .select {
                                    MomentCommentTable.momentId eq moment.id
                                }.forEach {
                                    val commentId = it[MomentCommentTable.id].value
                                    if (!commentList.any { it.id == commentId }) {
                                        commentList.add(Comment(commentId, it[UserTable.userId],it[UserTable.nickname],
                                                it[MomentCommentTable.content], it[MomentCommentTable.timestamp]))
                                    }
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
        button("send moment") {
            setOnAction {
                val inputDialog = TextInputDialog("").apply {
                    title = "Send Moment"
                    contentText = "Input moment content"
                }
                inputDialog.resizableProperty().set(true)
                inputDialog.perfSize()
                inputDialog.showAndWait()
                val momentText = inputDialog.result ?: ""
                if (momentText == "") {
                    return@setOnAction
                }
                launch {
                    try {
                        DbConnection.execute {
                            MomentTable.insert {
                                it[sender] = userId
                                it[content] = momentText
                                it[timestamp] = System.currentTimeMillis()
                            }
                        }
//                        information("successful", "send moment").perfSize()
                    } catch (e: Exception) {
//                        error("failed", "failed to send moment").perfSize()
                    }
                }
            }
        }
        listview(momentList) {
            cellFormat { moment ->
                graphic = vbox {
                    hbox {
                        label("${moment.senderName}: ${moment.content}")
                        button("add comment") {
                            setOnAction {
                                val inputDialog = TextInputDialog("").apply {
                                    title = "Send Comment"
                                    contentText = "Input comment content"
                                }
                                inputDialog.resizableProperty().set(true)
                                inputDialog.perfSize()
                                inputDialog.showAndWait()
                                val commentText = inputDialog.result ?: ""
                                if (commentText == "") {
                                    return@setOnAction
                                }
                                launch {
                                    try {
                                        DbConnection.execute {
                                            MomentCommentTable.insert {
                                                it[sender] = userId
                                                it[content] = commentText
                                                it[timestamp] = System.currentTimeMillis()
                                                it[momentId] = moment.id
                                            }
                                        }
//                                        information("successful", "send moment").perfSize()
                                    } catch (e: Exception) {
//                                        error("failed", "failed to send moment").perfSize()
                                    }
                                }
                            }
                        }
                        if (moment.sender == userId) {
                            button("delete") {
                                setOnAction {
                                    launch {
                                        DbConnection.execute {
                                            MomentCommentTable.deleteWhere {
                                                MomentCommentTable.momentId eq moment.id
                                            }
                                            MomentTable.deleteWhere {
                                                MomentTable.id eq moment.id
                                            }
                                        }
                                    }
                                    momentList.removeIf {
                                        it.id == moment.id
                                    }
                                }
                            }
                        }
                    }
                    listview(moment.commentList) {
                        paddingLeftProperty.set(20.0)
                        prefHeightProperty().bind(Bindings.size(this.itemsProperty().value).multiply(31))
                        cellFormat { comment ->
                            prefHeight(30.0)
                            graphic = cache {
                                hbox {
                                    label("${comment.senderName}: ${comment.content}")
                                    if (comment.sender == userId) {
                                        button("delete") {
                                            setOnAction {
                                                launch {
                                                    DbConnection.execute {
                                                        MomentCommentTable.deleteWhere {
                                                            MomentCommentTable.id eq comment.id
                                                        }
                                                    }
                                                }
                                                moment.commentList.removeIf {
                                                    it.id == comment.id
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}


