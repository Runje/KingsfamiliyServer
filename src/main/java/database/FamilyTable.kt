package database

import com.koenig.commonModel.Family
import com.koenig.commonModel.Repository.FamilyRepository
import com.koenig.commonModel.User
import com.koenig.commonModel.database.DatabaseItem
import com.koenig.commonModel.database.DatabaseItemTable
import com.koenig.commonModel.toLocalDate
import com.koenig.commonModel.toYearMonth
import com.koenig.communication.messages.FamilyMessage
import org.joda.time.LocalDate
import org.joda.time.YearMonth
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class FamilyTable(connection: Connection, private val userService: (String) -> User?) : ItemTable<Family>(connection) {

    override val tableName: String
        get() = NAME

    override val itemSpecificCreateStatement: String
        get() = ", $USERS TEXT , $START_MONTH INT"

    override val columnNames: List<String>
        get() = Arrays.asList(USERS, START_MONTH)

    @Throws(SQLException::class)
    override fun getItem(rs: ResultSet): Family {
        val usersText = rs.getString(USERS)
        val users = getUsers(userService, usersText)
        val startDate = rs.getYearMonth(START_MONTH)
        val family = rs.getString(DatabaseItemTable.COLUMN_NAME)
        return Family(family, users, startDate)
    }

    @Throws(SQLException::class)
    override fun setItem(ps: NamedParameterStatement, item: Family) {
        setUsers(item.users, ps, USERS)
        ps.setYearMonth(START_MONTH, item.startMonth)
    }

    @Throws(SQLException::class)
    fun addUserToFamily(family: Family, userId: String) {
        lock.lock()
        try {
            val selectQuery = "UPDATE " + tableName + " SET " + getNamedParameter(USERS) + " WHERE " + getNamedParameter(DatabaseItemTable.COLUMN_ID)

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setString(DatabaseItemTable.COLUMN_ID, family.id)
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
            val selectQuery = "SELECT * FROM " + tableName + " WHERE " + DatabaseItemTable.COLUMN_DELETED + " = :" + DatabaseItemTable.COLUMN_DELETED + " AND " + getNamedParameter(DatabaseItemTable.COLUMN_NAME)

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setInt(DatabaseItemTable.COLUMN_DELETED, DatabaseItemTable.FALSE)
            statement.setString(DatabaseItemTable.COLUMN_NAME, familyName)
            val rs = statement.executeQuery()
            while (rs.next()) {
                family = getDatabaseItem(rs)
            }

            return family?.item
        } finally {
            lock.unlock()
        }
    }

    companion object {
        val NAME = "family_table"
        val USERS = "users"
        val START_MONTH = "start_month"
    }
}

fun ResultSet.getYearMonth(key: String): YearMonth {
    return getInt(key).toYearMonth()
}

fun ResultSet.getLocalDate(key: String): LocalDate {
    return getInt(key).toLocalDate()
}

class FamilyDbRepository(val table: FamilyTable) : FamilyRepository {
    override val allFamilies: List<Family>
        get() = table.allItems

}
