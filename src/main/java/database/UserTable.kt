package database

import com.koenig.commonModel.Component
import com.koenig.commonModel.Permission
import com.koenig.commonModel.User
import com.koenig.commonModel.database.DatabaseItemTable
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class UserTable(connection: Connection) : ItemTable<User>(connection) {

    override val tableName: String
        get() = NAME

    override val itemSpecificCreateStatement: String
        get() = ", " +
                ABBREVIATION + " TEXT," +
                FAMILY_NAME + " TEXT," +
                BIRTHDAY + " LONG," +
                PERMISSIONS + " BLOB"

    override val columnNames: List<String>
        get() = Arrays.asList(ABBREVIATION, FAMILY_NAME, BIRTHDAY, PERMISSIONS)

    @Throws(SQLException::class)
    override fun getItem(rs: ResultSet): User {
        val userName = rs.getString(DatabaseItemTable.COLUMN_NAME)
        val abbreviation = rs.getString(ABBREVIATION)
        val family = rs.getString(FAMILY_NAME)
        val birthday = getDateTime(rs, BIRTHDAY)
        val permissionMap = getPermissions(rs, PERMISSIONS)
        return User(userName, abbreviation, family, birthday, permissionMap)
    }

    @Throws(SQLException::class)
    private fun getPermissions(rs: ResultSet, permissions: String): MutableMap<Component, Permission> {
        val buffer = ByteBuffer.wrap(rs.getBytes(permissions))
        return User.bytesToPermissions(buffer)
    }

    @Throws(SQLException::class)
    override fun setItem(ps: NamedParameterStatement, item: User) {
        ps.setString(ABBREVIATION, item.abbreviation)
        ps.setString(FAMILY_NAME, item.family)
        setDateTime(ps, BIRTHDAY, item.birthday)
        ps.setBytes(PERMISSIONS, User.permissionsToBytes(item.permissions))
    }

    @Throws(SQLException::class)
    fun addFamileToUser(familyName: String, userId: String) {
        lock.lock()
        try {
            val selectQuery = "UPDATE " + tableName + " SET " + getNamedParameter(FAMILY_NAME) + " WHERE " + getNamedParameter(DatabaseItemTable.COLUMN_ID)

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setString(DatabaseItemTable.COLUMN_ID, userId)
            statement.setString(FAMILY_NAME, familyName)
            statement.executeUpdate()
        } finally {
            lock.unlock()
        }
    }

    companion object {
        val NAME = "user_table"
        val ABBREVIATION = "abbr"
        val FAMILY_NAME = "family_name"
        val BIRTHDAY = "birthday"
        val PERMISSIONS = "permissions"
    }
}
