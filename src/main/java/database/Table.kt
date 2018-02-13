package database

import com.koenig.commonModel.Item
import com.koenig.commonModel.User
import com.koenig.commonModel.database.DatabaseItem
import com.koenig.commonModel.database.DatabaseTable
import org.joda.time.DateTime
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

abstract class Table<T : Item>(protected var connection: Connection) : DatabaseTable<T>() {

    /**
     * Shall return the create statement for the specific tables field, i.e for field name and birthday:
     * name TEXT, birthday LONG
     *
     * @return
     */
    abstract override val tableSpecificCreateStatement: String

    protected val namesOfSpecificParameterWithColon: String
        get() {
            val columnNames = columnNames
            val builder = StringBuilder(", :")
            for (columnName in columnNames) {
                builder.append(columnName + ", :")
            }

            return builder.substring(0, builder.length - 3)
        }


    protected val namesOfSpecificParameter: String
        get() {
            val columnNames = columnNames
            val builder = StringBuilder(", ")
            for (columnName in columnNames) {
                builder.append(columnName + ", ")
            }

            return builder.substring(0, builder.length - 2)
        }

    @Throws(SQLException::class)
    override fun isExisting(): Boolean {
        lock.lock()
        try {
            val dbm = connection.metaData
            val tables = dbm.getTables(null, null, tableName, null)
            return tables.next()
        } finally {
            lock.unlock()
        }
    }

    @Throws(SQLException::class)
    override fun create() {
        lock.lock()
        try {
            val statement = connection.createStatement()
            statement.executeUpdate(buildCreateStatement())
            statement.close()
        } finally {
            lock.unlock()
        }
    }

    @Throws(SQLException::class)
    override fun getAll(): List<DatabaseItem<T>> {
        lock.lock()
        try {

            val items = ArrayList<DatabaseItem<T>>()
            val selectQuery = "SELECT * FROM " + tableName + " WHERE " + DatabaseTable.COLUMN_DELETED + " = :" + DatabaseTable.COLUMN_DELETED

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setInt(DatabaseTable.COLUMN_DELETED, DatabaseTable.FALSE)
            val rs = statement.executeQuery()
            while (rs.next()) {
                items.add(resultToItem(rs))
            }

            return items
        } finally {
            lock.unlock()
        }
    }


    @Throws(SQLException::class)
    protected abstract fun getItem(rs: ResultSet): T

    @Throws(SQLException::class)
    protected fun resultToItem(rs: ResultSet): DatabaseItem<T> {
        val id = rs.getString(DatabaseTable.COLUMN_ID)
        val deleted = getBool(rs, DatabaseTable.COLUMN_DELETED)
        val insertDate = getDateTime(rs, DatabaseTable.COLUMN_INSERT_DATE)
        val lastModifiedDate = getDateTime(rs, DatabaseTable.COLUMN_MODIFIED_DATE)
        val insertId = rs.getString(DatabaseTable.COLUMN_INSERT_ID)
        val modifiedId = rs.getString(DatabaseTable.COLUMN_MODIFIED_ID)
        val name = rs.getString(DatabaseTable.COLUMN_NAME)
        val item = getItem(rs)
        item.id = id
        item.name = name
        return DatabaseItem(item, insertDate, lastModifiedDate, deleted, insertId, modifiedId)
    }


    @Throws(SQLException::class)
    protected fun setDateTime(ps: NamedParameterStatement, columnName: String, date: DateTime) {
        ps.setLong(columnName, date.millis)
    }

    @Throws(SQLException::class)
    protected abstract fun setItem(ps: NamedParameterStatement, item: T)

    @Throws(SQLException::class)
    protected fun setBool(statement: NamedParameterStatement, columnName: String, b: Boolean) {
        // TODO: use Short
        statement.setInt(columnName, if (b) 1 else 0)
    }


    @Throws(SQLException::class)
    override fun add(databaseItem: DatabaseItem<T>) {
        runInLock({

            val ps = NamedParameterStatement(connection, "insert into " + tableName +
                    "(" + DatabaseTable.COLUMN_ID + ", " + DatabaseTable.COLUMN_DELETED + ", " + DatabaseTable.COLUMN_INSERT_DATE + ", " + DatabaseTable.COLUMN_INSERT_ID + ", " + DatabaseTable.COLUMN_MODIFIED_DATE + ", " + DatabaseTable.COLUMN_MODIFIED_ID + ", " + DatabaseTable.COLUMN_NAME + namesOfSpecificParameter + ") " +
                    " values(:" + DatabaseTable.COLUMN_ID + ", :" + DatabaseTable.COLUMN_DELETED + ", :" + DatabaseTable.COLUMN_INSERT_DATE + ", :" + DatabaseTable.COLUMN_INSERT_ID + ", :" + DatabaseTable.COLUMN_MODIFIED_DATE + ", :" + DatabaseTable.COLUMN_MODIFIED_ID + ", :" + DatabaseTable.COLUMN_NAME +
                    namesOfSpecificParameterWithColon + ")")
            setDateTime(ps, DatabaseTable.COLUMN_MODIFIED_DATE, databaseItem.lastModifiedDate)
            setDateTime(ps, DatabaseTable.COLUMN_INSERT_DATE, databaseItem.insertDate)
            ps.setString(DatabaseTable.COLUMN_INSERT_ID, databaseItem.insertId)
            ps.setString(DatabaseTable.COLUMN_MODIFIED_ID, databaseItem.lastModifiedId)
            ps.setString(DatabaseTable.COLUMN_ID, databaseItem.id)
            setBool(ps, DatabaseTable.COLUMN_DELETED, databaseItem.isDeleted)
            ps.setString(DatabaseTable.COLUMN_NAME, databaseItem.name)
            setItem(ps, databaseItem.item)
            ps.executeUpdate()
        })
    }

    @Throws(SQLException::class)
    protected fun setStringList(ps: NamedParameterStatement, name: String, list: List<String>) {
        ps.setString(name, DatabaseTable.buildStringList(list))
    }


    @Throws(SQLException::class)
    override fun getDatabaseItemFromId(id: String): DatabaseItem<T>? {
        lock.lock()
        try {

            var item: DatabaseItem<T>? = null
            val selectQuery = "SELECT * FROM " + tableName + " WHERE " + DatabaseTable.COLUMN_ID + " = :" + DatabaseTable.COLUMN_ID

            val statement = NamedParameterStatement(connection, selectQuery)

            statement.setString(DatabaseTable.COLUMN_ID, id)
            val rs = statement.executeQuery()
            while (rs.next()) {
                item = resultToItem(rs)
            }

            return item
        } finally {
            lock.unlock()
        }
    }

    @Throws(SQLException::class)
    override fun getFromId(id: String): T? {
        lock.lock()
        try {

            val databaseItemFromId = getDatabaseItemFromId(id) ?: return null

            return databaseItemFromId.item
        } finally {
            lock.unlock()
        }
    }

    @Throws(SQLException::class)
    override fun deleteAllEntrys() {
        lock.lock()
        try {

            val query = "DELETE FROM " + tableName
            val statement = connection.createStatement()
            statement.execute(query)
        } finally {
            lock.unlock()
        }
    }

    @Throws(SQLException::class)
    fun update(itemId: String, updateParameter: String, setter: (NamedParameterStatement) -> Unit) {
        update(itemId, arrayOf(updateParameter), setter)
    }

    @Throws(SQLException::class)
    fun update(itemId: String, updateParameter: String, setter: ParameterSetter) {
        update(itemId, arrayOf(updateParameter), setter)
    }

    @Throws(SQLException::class)
    fun update(itemId: String, updateParameters: Array<String>, setter: (NamedParameterStatement) -> Unit) {
        update(itemId, updateParameters, object : ParameterSetter {
            override fun set(ps: NamedParameterStatement) {
                setter.invoke(ps)
            }
        })
    }

    @Throws(SQLException::class)
    fun update(itemId: String, updateParameters: Array<String>, setter: ParameterSetter) {
        runInLock({
            val selectQuery = "UPDATE " + tableName + " SET " + getNamedParameters(updateParameters) + " WHERE " + getNamedParameter(DatabaseTable.COLUMN_ID)

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setString(DatabaseTable.COLUMN_ID, itemId)
            setter.set(statement)
            statement.executeUpdate()
        })
    }


    @Throws(SQLException::class)
    fun deleteFrom(itemId: String, userId: String) {
        update(itemId, arrayOf(DatabaseTable.COLUMN_DELETED, DatabaseTable.COLUMN_MODIFIED_ID, DatabaseTable.COLUMN_MODIFIED_DATE), { ps ->
            setBool(ps, DatabaseTable.COLUMN_DELETED, true)
            ps.setString(DatabaseTable.COLUMN_MODIFIED_ID, userId)
            setDateTime(ps, DatabaseTable.COLUMN_MODIFIED_DATE, DateTime.now())
        })
    }

    @Throws(SQLException::class)
    fun updateFrom(item: T, userId: String) {
        val columns = ArrayList<String>()
        columns.add(DatabaseTable.COLUMN_MODIFIED_ID)
        columns.add(DatabaseTable.COLUMN_MODIFIED_DATE)
        columns.add(DatabaseTable.COLUMN_NAME)
        columns.addAll(columnNames)
        update(item.id, columns.toTypedArray(), { ps ->
            ps.setString(DatabaseTable.COLUMN_MODIFIED_ID, userId)
            setDateTime(ps, DatabaseTable.COLUMN_MODIFIED_DATE, DateTime.now())
            ps.setString(DatabaseTable.COLUMN_NAME, item.name)
            setItem(ps, item)
        })


    }

    @Throws(SQLException::class)
    fun getChangesSinceDatabaseItems(lastSyncDate: DateTime): List<DatabaseItem<T>> {
        val query = "SELECT * FROM " + tableName + " WHERE " + DatabaseTable.COLUMN_MODIFIED_DATE + " > " + ":" + DatabaseTable.COLUMN_MODIFIED_DATE
        val statement = NamedParameterStatement(connection, query)
        setDateTime(statement, DatabaseTable.COLUMN_MODIFIED_DATE, lastSyncDate)

        return createListFromStatement(statement)
    }

    @Throws(SQLException::class)
    fun getChangesSince(lastSyncDate: DateTime): List<T> {
        return toItemList(getChangesSinceDatabaseItems(lastSyncDate))
    }

    @Throws(SQLException::class)
    private fun createListFromStatement(statement: NamedParameterStatement): List<DatabaseItem<T>> {
        val rs = statement.executeQuery()
        val result = ArrayList<DatabaseItem<T>>()
        while (rs.next()) {
            result.add(resultToItem(rs))
        }

        return result
    }

    @Throws(SQLException::class)
    fun doesItemExist(id: String): Boolean {
        val query = "SELECT COUNT(*) FROM " + tableName + " WHERE " + getNamedParameter(DatabaseTable.COLUMN_ID) + " LIMIT 1"
        val statement = NamedParameterStatement(connection, query)
        statement.setString(DatabaseTable.COLUMN_ID, id)
        val rs = statement.executeQuery()
        if (rs.next()) {

            return rs.getInt(1) != 0
        }

        throw SQLException("Couldn't check existence")
    }

    @Throws(SQLException::class)
    fun getDatabaseItemsFromName(name: String): List<DatabaseItem<T>> {
        lock.lock()
        try {

            val selectQuery = "SELECT * FROM " + tableName + " WHERE " + DatabaseTable.COLUMN_DELETED + " = :" + DatabaseTable.COLUMN_DELETED + " AND " + DatabaseTable.COLUMN_NAME + " = :" + DatabaseTable.COLUMN_NAME

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setInt(DatabaseTable.COLUMN_DELETED, DatabaseTable.FALSE)
            statement.setString(DatabaseTable.COLUMN_NAME, name)
            return createListFromStatement(statement)
        } finally {
            lock.unlock()
        }
    }

    @Throws(SQLException::class)
    fun getFromName(name: String): List<T> {
        return toItemList(getDatabaseItemsFromName(name))
    }


    @Throws(SQLException::class)
    protected fun setUsers(users: List<User>, ps: NamedParameterStatement, columnName: String) {
        val result = usersToId(users)
        ps.setString(columnName, result)
    }


    interface ParameterSetter {
        @Throws(SQLException::class)
        fun set(ps: NamedParameterStatement)
    }

    companion object {

        @Throws(SQLException::class)
        fun getBool(rs: ResultSet, name: String): Boolean {
            return rs.getInt(name) != 0
        }

        @Throws(SQLException::class)
        fun getDateTime(rs: ResultSet, name: String): DateTime {
            return DateTime(rs.getLong(name))
        }

        fun getParameter(number: Int): String {
            val builder = StringBuilder()
            for (i in 0 until number) {
                builder.append("?,")
            }

            // deleteFrom last separator
            builder.deleteCharAt(builder.length - 1)
            return builder.toString()
        }


        fun getNamedParameters(parameters: Array<String>): String {
            val builder = StringBuilder()
            for (parameter in parameters) {
                builder.append(getNamedParameter(parameter) + ", ")
            }

            return builder.substring(0, builder.length - 2)
        }

        fun getNamedParameter(parameter: String): String {
            return parameter + " = :" + parameter
        }
    }
}
