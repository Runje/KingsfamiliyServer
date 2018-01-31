package database

import com.koenig.commonModel.Family
import com.koenig.commonModel.database.DatabaseItem
import com.koenig.commonModel.database.DatabaseTable
import com.koenig.commonModel.database.UserService
import com.koenig.communication.messages.FamilyMessage
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class FamilyTable(connection: Connection, private val userService: UserService) : Table<Family>(connection) {

    override val tableName: String
        get() = NAME

    override val tableSpecificCreateStatement: String
        get() = ", $USERS TEXT "

    override val columnNames: List<String>
        get() = Arrays.asList(USERS)

    @Throws(SQLException::class)
    override fun getItem(rs: ResultSet): Family {
        val usersText = rs.getString(USERS)
        val users = getUsers(userService, usersText)
        val family = rs.getString(DatabaseTable.COLUMN_NAME)
        return Family(family, users)
    }

    @Throws(SQLException::class)
    override fun setItem(ps: NamedParameterStatement, item: Family) {
        setUsers(item.users, ps, USERS)
    }

    @Throws(SQLException::class)
    fun addUserToFamily(family: Family, userId: String) {
        lock.lock()
        try {
            val selectQuery = "UPDATE " + tableName + " SET " + Table.Companion.getNamedParameter(USERS) + " WHERE " + Table.Companion.getNamedParameter(DatabaseTable.COLUMN_ID)

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setString(DatabaseTable.COLUMN_ID, family.id)
            val builder = StringBuilder()
            for (user in family.users) {
                builder.append(user.id)
                builder.append(FamilyMessage.SEPARATOR)
            }

            builder.append(userId)
            val users = builder.substring(0, builder.length)
            statement.setString(USERS, users)
            statement.executeUpdate()
        } finally {
            lock.unlock()
        }
    }

    @Throws(SQLException::class)
    fun getFamilyByName(familyName: String): Family? {
        lock.lock()
        try {
            var family: DatabaseItem<Family>? = null
            val selectQuery = "SELECT * FROM " + tableName + " WHERE " + DatabaseTable.COLUMN_DELETED + " = :" + DatabaseTable.COLUMN_DELETED + " AND " + Table.Companion.getNamedParameter(DatabaseTable.COLUMN_NAME)

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setInt(DatabaseTable.COLUMN_DELETED, DatabaseTable.FALSE)
            statement.setString(DatabaseTable.COLUMN_NAME, familyName)
            val rs = statement.executeQuery()
            while (rs.next()) {
                family = resultToItem(rs)
            }

            return if (family == null) null else family.item
        } finally {
            lock.unlock()
        }
    }

    companion object {
        val NAME = "family_table"
        val USERS = "users"
    }
}
