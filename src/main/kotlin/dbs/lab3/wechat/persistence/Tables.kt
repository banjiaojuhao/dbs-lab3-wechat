package dbs.lab3.wechat.persistence

import org.jetbrains.exposed.dao.IntIdTable

object UserTable : IntIdTable() {
    override val tableName: String
        get() = "user"
    val userId = varchar("user_id", 32).uniqueIndex()
    val password = text("password")
    val nickname = text("nickname")
}

object FriendshipTable : IntIdTable() {
    override val tableName: String
        get() = "friendship"
    val left = varchar("left_user", 32).references(UserTable.userId)
    val right = varchar("right_user", 32).references(UserTable.userId)

    init {
        index(true, left, right)
    }
}
object GroupTable : IntIdTable() {
    override val tableName: String
        get() = "chat_group"
    val groupId = varchar("group_id", 32).uniqueIndex()
    val name = text("name")

}

object JoinedGroup : IntIdTable() {
    override val tableName: String
        get() = "joined_group"
    val groupId = varchar("group_id", 32).references(GroupTable.groupId)
    val userId = varchar("user_id", 32).references(UserTable.userId)

    init {
        index(true, groupId, userId)
    }
}

object MessageTable : IntIdTable() {
    override val tableName: String
        get() = "message"
    val sender = varchar("sender", 32).references(UserTable.userId)
    val receiver = text("receiver")
    val content = text("content")
    val timestamp = long("timestamp")
    val isToGroup = bool("is_to_group")
}

object FavoriteTable : IntIdTable() {
    override val tableName: String
        get() = "favorite"
    val saver = varchar("saver", 32).references(UserTable.userId)
    val content = text("content")
    val timestamp = long("timestamp")
}

object MomentTable : IntIdTable() {
    override val tableName: String
        get() = "moment"
    val sender = varchar("sender", 32).references(UserTable.userId)
    val content = text("content")
    val timestamp = long("timestamp")
}

object MomentCommentTable : IntIdTable() {
    override val tableName: String
        get() = "moment_comment"
    val sender = varchar("sender", 32).references(UserTable.userId)
    val momentId = integer("moment_id").references(MomentTable.id)
    val content = text("content")
    val timestamp = long("timestamp")
}

object SubscriptionTable : IntIdTable() {
    override val tableName: String
        get() = "subscription"
    val subscriptionId = varchar("subscription_id", 32).uniqueIndex()
    val name = text("name")
}

object SubscribedTable : IntIdTable() {
    override val tableName: String
        get() = "subscribed"
    val userId = varchar("user_id", 32).references(UserTable.userId)
    val subscriptionId = varchar("subscription_id", 32).references(SubscriptionTable.subscriptionId)
    init {
        index(true, userId, subscriptionId)
    }
}

object SubscriptionNewsTable : IntIdTable() {
    override val tableName: String
        get() = "subscription_news"
    val newsId = varchar("news_id", 32).uniqueIndex()
    val subscriptionId = varchar("subscription_id", 32).references(SubscriptionTable.subscriptionId)
    val content = text("content")
    val timestamp = long("timestamp")
}