package dbs.lab3.wechat.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection.TRANSACTION_SERIALIZABLE

object DbConnection {
    private val db by lazy {
        val db = Database.connect("jdbc:mysql://localhost:3306/wechat",
                driver = "com.mysql.jdbc.Driver",
                user = "root", password = " ")
        transaction(db = db, transactionIsolation = TRANSACTION_SERIALIZABLE,
                repetitionAttempts = 3) {
            SchemaUtils.createMissingTablesAndColumns(
                    UserTable, FriendshipTable, GroupTable, JoinedGroup,
                    MessageTable, FavoriteTable, MomentTable, MomentCommentTable,
                    SubscriptionTable, SubscribedTable, SubscriptionNewsTable
            )

            // create 3 users
            this.connection.prepareStatement(
                    "insert ignore into ${UserTable.tableName}(" +
                            "${UserTable.userId.name}, " +
                            "${UserTable.password.name}, " +
                            "${UserTable.nickname.name}) " +
                            "values(?,?,?);"
            ).apply {

                setString(1, "zhang3")
                setString(2, " ")
                setString(3, "法外狂徒")
                addBatch()

                setString(1, "luolaoshi")
                setString(2, " ")
                setString(3, "老罗")
                addBatch()

                setString(1, "li4")
                setString(2, " ")
                setString(3, "小李子")
                addBatch()

                executeBatch()
            }

            // create 1 group
            this.connection.prepareStatement(
                    "insert ignore into ${GroupTable.tableName}(" +
                            "${GroupTable.groupId.name}, " +
                            "${GroupTable.name.name}) " +
                            "values(?,?);"
            ).apply {

                setString(1, "big_group")
                setString(2, "大群")
                addBatch()

                setString(1, "group2")
                setString(2, "2nd group")

                executeBatch()
            }

            // add 3 users into group
            this.connection.prepareStatement(
                    "insert ignore into ${JoinedGroup.tableName}(" +
                            "${JoinedGroup.groupId.name}, " +
                            "${JoinedGroup.userId.name}) " +
                            "values(?,?);"
            ).apply {
                setString(1, "big_group")
                setString(2, "zhang3")
                addBatch()

                setString(1, "big_group")
                setString(2, "luolaoshi")
                addBatch()

                setString(1, "big_group")
                setString(2, "li4")
                addBatch()

                executeBatch()
            }

            this.connection.prepareStatement(
                    "insert ignore into ${SubscriptionTable.tableName}(" +
                            "${SubscriptionTable.subscriptionId.name}, " +
                            "${SubscriptionTable.name.name}) " +
                            "values(?,?);"
            ).apply {
                setString(1, "sub1")
                setString(2, "这是公众号")
                addBatch()

                executeBatch()
            }

            this.connection.prepareStatement(
                    "insert ignore into ${SubscribedTable.tableName}(" +
                            "${SubscribedTable.subscriptionId.name}, " +
                            "${SubscribedTable.userId.name}) " +
                            "values(?,?);"
            ).apply {
                setString(1, "sub1")
                setString(2, "zhang3")
                addBatch()

                setString(1, "sub1")
                setString(2, "luolaoshi")
                addBatch()

                setString(1, "sub1")
                setString(2, "li4")
                addBatch()

                executeBatch()
            }

            this.connection.prepareStatement(
                    "insert ignore into ${SubscriptionNewsTable.tableName}(" +
                            "${SubscriptionNewsTable.newsId.name}, " +
                            "${SubscriptionNewsTable.subscriptionId.name}, " +
                            "${SubscriptionNewsTable.content.name}, " +
                            "${SubscriptionNewsTable.timestamp.name}) " +
                            "values(?,?,?,?);"
            ).apply {
                setString(1, "xxxxxx")
                setString(2, "sub1")
                setString(3, "这是一个公众号的消息推送")
                setLong(4, System.currentTimeMillis())
                addBatch()

                setString(1, "xxxxxy")
                setString(2, "sub1")
                setString(3, "这也是一个公众号的消息推送")
                setLong(4, System.currentTimeMillis())
                addBatch()

                executeBatch()
            }
        }
        db
    }

    //    private val context = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val context = Dispatchers.JavaFx
    suspend fun <T> execute(task: Transaction.() -> T): T =
            withContext(context) {
                transaction(db = db, transactionIsolation = TRANSACTION_SERIALIZABLE,
                        repetitionAttempts = 3) {
                    task()
                }
            }
}
