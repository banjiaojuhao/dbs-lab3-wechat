package dbs.lab3.wechat.view

import dbs.lab3.wechat.persistence.*
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.control.TextInputDialog
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import tornadofx.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.coroutines.CoroutineContext

enum class MsgChannelType {
    Friend, Group, Subscription, None
}

data class MsgChannel(val type: MsgChannelType, val userId: String, val nickname: String)
data class Message(val from: String, val content: String, val timestamp: Long)

class MainView : View(), CoroutineScope {
    override lateinit var coroutineContext: CoroutineContext
    val userId: String by param()
    val nickname: String by param()
    private val msgChannelList = arrayListOf<MsgChannel>().asObservable()
    private val displayChannelId = SimpleStringProperty("")
    private val displayChannelType = SimpleObjectProperty<MsgChannelType>(MsgChannelType.None)
    private val messageList = arrayListOf<Message>().asObservable()
    private val messageSet = hashSetOf<String>()

    override fun onDock() {
        super.onDock()
        coroutineContext = MainScope().coroutineContext
        title = nickname
        msgChannelList.clear()
        messageList.clear()
        messageSet.clear()
        launch {
            val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            while (true) {
                DbConnection.execute {
                    // update added friend list
                    FriendshipTable
                            .leftJoin(UserTable, { right }, { userId })
                            .select {
                                FriendshipTable.left eq userId
                            }.forEach {
                                val uid = it[UserTable.userId]
                                if (!msgChannelList.any { it.userId == uid && it.type == MsgChannelType.Friend }) {
                                    msgChannelList.add(MsgChannel(MsgChannelType.Friend, uid, "[好友]" + it[UserTable.nickname]))
                                }
                            }

                    // update joined group list
                    JoinedGroup
                            .leftJoin(GroupTable, { groupId }, { groupId })
                            .select {
                                JoinedGroup.userId eq userId
                            }.forEach {
                                val gid = it[GroupTable.groupId]
                                if (!msgChannelList.any { it.userId == gid && it.type == MsgChannelType.Group }) {
                                    msgChannelList.add(MsgChannel(MsgChannelType.Group, gid, "[群聊]" + it[GroupTable.name]))
                                }
                            }

                    SubscribedTable
                            .leftJoin(SubscriptionTable, { subscriptionId }, { subscriptionId })
                            .select {
                                SubscribedTable.userId eq userId
                            }.forEach {
                                val subid = it[SubscribedTable.subscriptionId]
                                if (!msgChannelList.any { it.userId == subid && it.type == MsgChannelType.Subscription }) {
                                    msgChannelList.add(MsgChannel(MsgChannelType.Subscription, subid, "[公众号]" + it[SubscriptionTable.name]))
                                }
                            }

                    // update news
                    if (displayChannelId.get() != "") {
                        when (displayChannelType.get()) {
                            MsgChannelType.Friend -> {
                                MessageTable
                                        .leftJoin(UserTable, { sender }, { userId })
                                        .select {
                                            (((MessageTable.receiver eq userId) and (MessageTable.sender eq displayChannelId.get()))
                                                    or ((MessageTable.receiver eq displayChannelId.get()) and (MessageTable.sender eq userId))) and (MessageTable.isToGroup eq Op.FALSE)
                                        }.orderBy(MessageTable.timestamp)
                                        .forEach {
                                            val msg = Message(it[UserTable.nickname], it[MessageTable.content], it[MessageTable.timestamp])
                                            val msgstr = msg.toString()
                                            if (!messageSet.contains(msgstr)) {
                                                messageSet.add(msgstr)
                                                messageList.add(msg)
                                            }
                                        }
                            }
                            MsgChannelType.Group -> {
                                MessageTable
                                        .leftJoin(UserTable, { sender }, { userId })
                                        .select {
                                            (MessageTable.receiver eq displayChannelId.get()) and (MessageTable.isToGroup eq Op.TRUE)
                                        }.orderBy(MessageTable.timestamp)
                                        .forEach {
                                            val msg = Message(it[UserTable.nickname], it[MessageTable.content], it[MessageTable.timestamp])
                                            val msgstr = msg.toString()
                                            if (!messageSet.contains(msgstr)) {
                                                messageSet.add(msgstr)
                                                messageList.add(msg)
                                            }
                                        }
                            }
                            MsgChannelType.Subscription -> {
                                SubscriptionNewsTable
                                        .leftJoin(SubscribedTable, { subscriptionId }, { subscriptionId })
                                        .select {
                                            SubscribedTable.userId eq userId
                                        }.orderBy(SubscriptionNewsTable.timestamp)
                                        .forEach {
                                            val timestamp = it[SubscriptionNewsTable.timestamp]
                                            val postTime = LocalDateTime
                                                    .ofEpochSecond(timestamp / 1000, 0, ZoneOffset.ofHours(8))
                                                    .format(timeFormatter)
                                            val msg = Message(postTime, it[SubscriptionNewsTable.content], timestamp)
                                            val msgstr = msg.toString()
                                            if (!messageSet.contains(msgstr)) {
                                                messageSet.add(msgstr)
                                                messageList.add(msg)
                                            }
                                        }
                            }
                            else -> {
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

    override val root = borderpane {

        top = buttonbar {
            button("add user") {
                setOnAction {
                    val inputDialog = TextInputDialog("").apply {
                        title = "Add User"
                        contentText = "Input user id"
                    }
                    inputDialog.resizableProperty().set(true)
                    inputDialog.perfSize()
                    inputDialog.showAndWait()
                    val friendId = inputDialog.result ?: ""
                    if (friendId == "") {
                        return@setOnAction
                    }
                    launch {
                        try {
                            DbConnection.execute {
                                FriendshipTable.insert {
                                    it[left] = userId
                                    it[right] = friendId
                                }
                                FriendshipTable.insert {
                                    it[left] = friendId
                                    it[right] = userId
                                }
                            }
//                            information("successful", "added user $friendId").perfSize()
                        } catch (e: Exception) {
//                            error("failed", "failed to add user").perfSize()
                        }
                    }
                }
            }
            button("moments") {
                setOnAction {
                    find<MomentView>(mapOf(
                            MomentView::userId to userId
                    )).openWindow(block = true)
                }
            }
            button("favorites") {
                setOnAction {
                    find<FavoriteView>(mapOf(
                            FavoriteView::userId to userId
                    )).openWindow(block = true)
                }
            }
        }
        left = listview(msgChannelList) {
            cellFormat { user ->
                setOnMouseClicked {
                    messageList.clear()
                    messageSet.clear()
                    displayChannelId.set(user.userId)
                    displayChannelType.set(user.type)
                }
                graphic = hbox {
                    label(user.nickname)
                    if (user.type == MsgChannelType.Friend) {
                        button("delete") {
                            setOnAction {
                                msgChannelList.removeIf {
                                    it.userId == user.userId
                                }
                                launch {
                                    DbConnection.execute {
                                        FriendshipTable.deleteWhere {
                                            ((FriendshipTable.left eq user.userId) and (FriendshipTable.right eq userId)) or
                                                    ((FriendshipTable.left eq userId) and (FriendshipTable.right eq user.userId))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }

        center = vbox {
            this.visibleProperty().set(false)
            displayChannelId.onChange {
                this.visibleProperty().set(it != "")
            }
            listview(messageList) {
                cellFormat {
                    graphic = label("${it.from}: ${it.content}")
                }
            }
            hbox {
                enableWhen {
                    displayChannelType.isEqualTo(MsgChannelType.Friend)
                            .or(displayChannelType.isEqualTo(MsgChannelType.Group))
                }
                val text = SimpleStringProperty("")
                textfield(text)
                button("send") {
                    enableWhen {
                        text.isNotEmpty
                    }
                    setOnAction {
                        launch {
                            DbConnection.execute {
                                MessageTable.insert {
                                    it[sender] = userId
                                    it[content] = text.get()
                                    it[timestamp] = System.currentTimeMillis()
                                    it[receiver] = displayChannelId.get()
                                    it[isToGroup] = displayChannelType.get() == MsgChannelType.Group
                                }
                            }
                            text.set("")
                        }
                    }
                }
            }
        }
    }
}