package database

import com.koenig.commonModel.Item
import com.koenig.commonModel.User
import com.koenig.commonModel.database.DatabaseItem
import com.koenig.commonModel.database.DatabaseItemTable
import com.koenig.commonModel.toLocalDate
import io.reactivex.subjects.BehaviorSubject
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

abstract class ItemTable<T : Item>(connection: Connection) : JavaTable<DatabaseItem<T>>(connection), DatabaseItemTable<T> {
    override var onDeleteListeners: MutableList<DatabaseItemTable.OnDeleteListener<T>> = mutableListOf()
    override var onAddListeners: MutableList<DatabaseItemTable.OnAddListener<T>> = mutableListOf()
    override var onUpdateListeners: MutableList<DatabaseItemTable.OnUpdateListener<T>> = mutableListOf()
    override val hasChanged: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
    override val all: List<DatabaseItem<T>>
        get () {
            lock.lock()
            try {
                val items = ArrayList<DatabaseItem<T>>()
                val selectQuery = "SELECT * FROM " + tableName + " WHERE " + DatabaseItemTable.COLUMN_DELETED + " = :" + DatabaseItemTable.COLUMN_DELETED
                val statement = NamedParameterStatement(connection, selectQuery)
                statement.setInt(DatabaseItemTable.COLUMN_DELETED, DatabaseItemTable.FALSE)
                val rs = statement.executeQuery()
                while (rs.next()) {
                    items.add(getDatabaseItem(rs))
                }

                return items
            } finally {
                lock.unlock()
            }
        }

    abstract fun getItem(rs: ResultSet): T

    fun getLocalDate(rs: ResultSet, key: String): LocalDate {
        return rs.getInt(key).toLocalDate()
    }

    @Throws(SQLException::class)
    override fun getDatabaseItem(rs: ResultSet): DatabaseItem<T> {
        val id = rs.getString(DatabaseItemTable.COLUMN_ID)
        val deleted = getBool(rs, DatabaseItemTable.COLUMN_DELETED)
        val insertDate = getDateTime(rs, DatabaseItemTable.COLUMN_INSERT_DATE)
        val lastModifiedDate = getDateTime(rs, DatabaseItemTable.COLUMN_MODIFIED_DATE)
        val insertId = rs.getString(DatabaseItemTable.COLUMN_INSERT_ID)
        val modifiedId = rs.getString(DatabaseItemTable.COLUMN_MODIFIED_ID)
        val name = rs.getString(DatabaseItemTable.COLUMN_NAME)
        val item: T = getItem(rs)
        item.id = id
        item.name = name
        return DatabaseItem<T>(item, insertDate, lastModifiedDate, deleted, insertId, modifiedId)
    }



    @Throws(SQLException::class)
    protected abstract fun setItem(ps: NamedParameterStatement, item: T)

    @Throws(SQLException::class)
    override fun add(item: DatabaseItem<T>) {
        runInLock({

            val ps = NamedParameterStatement(connection, "insert into " + tableName +
                    "(" + DatabaseItemTable.COLUMN_ID + ", " + DatabaseItemTable.COLUMN_DELETED + ", " + DatabaseItemTable.COLUMN_INSERT_DATE + ", " + DatabaseItemTable.COLUMN_INSERT_ID + ", " + DatabaseItemTable.COLUMN_MODIFIED_DATE + ", " + DatabaseItemTable.COLUMN_MODIFIED_ID + ", " + DatabaseItemTable.COLUMN_NAME + namesOfSpecificParameter + ") " +
                    " values(:" + DatabaseItemTable.COLUMN_ID + ", :" + DatabaseItemTable.COLUMN_DELETED + ", :" + DatabaseItemTable.COLUMN_INSERT_DATE + ", :" + DatabaseItemTable.COLUMN_INSERT_ID + ", :" + DatabaseItemTable.COLUMN_MODIFIED_DATE + ", :" + DatabaseItemTable.COLUMN_MODIFIED_ID + ", :" + DatabaseItemTable.COLUMN_NAME +
                    namesOfSpecificParameterWithColon + ")")
            setDateTime(ps, DatabaseItemTable.COLUMN_MODIFIED_DATE, item.lastModifiedDate)
            setDateTime(ps, DatabaseItemTable.COLUMN_INSERT_DATE, item.insertDate)
            ps.setString(DatabaseItemTable.COLUMN_INSERT_ID, item.insertId)
            ps.setString(DatabaseItemTable.COLUMN_MODIFIED_ID, item.lastModifiedId)
            ps.setString(DatabaseItemTable.COLUMN_ID, item.id)
            setBool(ps, DatabaseItemTable.COLUMN_DELETED, item.isDeleted)
            ps.setString(DatabaseItemTable.COLUMN_NAME, item.name)
            setItem(ps, item.item)
            ps.executeUpdate()
        })
    }

    override fun setDatabaseItem(ps: NamedParameterStatement, item: DatabaseItem<T>) {

    }

    @Throws(SQLException::class)
    override fun getDatabaseItemFromId(id: String): DatabaseItem<T>? {
        lock.lock()
        try {

            var item: DatabaseItem<T>? = null
            val selectQuery = "SELECT * FROM " + tableName + " WHERE " + DatabaseItemTable.COLUMN_ID + " = :" + DatabaseItemTable.COLUMN_ID

            val statement = NamedParameterStatement(connection, selectQuery)

            statement.setString(DatabaseItemTable.COLUMN_ID, id)
            val rs = statement.executeQuery()
            while (rs.next()) {
                item = getDatabaseItem(rs)
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
    fun update(itemId: String, updateParameters: Array<String>, setter: ParameterSetter) {
        runInLock({
            val selectQuery = "UPDATE " + tableName + " SET " + getNamedParameters(updateParameters) + " WHERE " + getNamedParameter(DatabaseItemTable.COLUMN_ID)

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setString(DatabaseItemTable.COLUMN_ID, itemId)
            setter.set(statement)
            statement.executeUpdate()
        })
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
    fun deleteFrom(itemId: String, userId: String) {
        update(itemId, arrayOf(DatabaseItemTable.COLUMN_DELETED, DatabaseItemTable.COLUMN_MODIFIED_ID, DatabaseItemTable.COLUMN_MODIFIED_DATE), { ps ->
            setBool(ps, DatabaseItemTable.COLUMN_DELETED, true)
            ps.setString(DatabaseItemTable.COLUMN_MODIFIED_ID, userId)
            setDateTime(ps, DatabaseItemTable.COLUMN_MODIFIED_DATE, DateTime.now())
        })
    }

    @Throws(SQLException::class)
    fun updateFrom(item: T, userId: String) {
        val columns = ArrayList<String>()
        columns.add(DatabaseItemTable.COLUMN_MODIFIED_ID)
        columns.add(DatabaseItemTable.COLUMN_MODIFIED_DATE)
        columns.add(DatabaseItemTable.COLUMN_NAME)
        columns.addAll(columnNames)
        update(item.id, columns.toTypedArray(), { ps ->
            ps.setString(DatabaseItemTable.COLUMN_MODIFIED_ID, userId)
            setDateTime(ps, DatabaseItemTable.COLUMN_MODIFIED_DATE, DateTime.now())
            ps.setString(DatabaseItemTable.COLUMN_NAME, item.name)
            setItem(ps, item)
        })


    }

    @Throws(SQLException::class)
    fun getChangesSinceDatabaseItems(lastSyncDate: DateTime): List<DatabaseItem<T>> {
        val query = "SELECT * FROM " + tableName + " WHERE " + DatabaseItemTable.COLUMN_MODIFIED_DATE + " > " + ":" + DatabaseItemTable.COLUMN_MODIFIED_DATE
        val statement = NamedParameterStatement(connection, query)
        setDateTime(statement, DatabaseItemTable.COLUMN_MODIFIED_DATE, lastSyncDate)

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
            result.add(getDatabaseItem(rs))
        }

        return result
    }

    @Throws(SQLException::class)
    fun doesItemExist(id: String): Boolean {
        val query = "SELECT COUNT(*) FROM " + tableName + " WHERE " + getNamedParameter(DatabaseItemTable.COLUMN_ID) + " LIMIT 1"
        val statement = NamedParameterStatement(connection, query)
        statement.setString(DatabaseItemTable.COLUMN_ID, id)
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

            val selectQuery = "SELECT * FROM " + tableName + " WHERE " + DatabaseItemTable.COLUMN_DELETED + " = :" + DatabaseItemTable.COLUMN_DELETED + " AND " + DatabaseItemTable.COLUMN_NAME + " = :" + DatabaseItemTable.COLUMN_NAME

            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setInt(DatabaseItemTable.COLUMN_DELETED, DatabaseItemTable.FALSE)
            statement.setString(DatabaseItemTable.COLUMN_NAME, name)
            return createListFromStatement(statement)
        } finally {
            lock.unlock()
        }
    }

    @Throws(SQLException::class)
    fun getFromName(name: String): List<T> {
        return toItemList(getDatabaseItemsFromName(name))
    }

    fun getWith(condition: String, setter: (NamedParameterStatement) -> Unit): List<DatabaseItem<T>> {
        return runInLockWithResult {
            val selectQuery = "SELECT * FROM " + tableName + " WHERE " + DatabaseItemTable.COLUMN_DELETED + " = ? AND " + condition
            val statement = NamedParameterStatement(connection, selectQuery)
            statement.setInt(DatabaseItemTable.COLUMN_DELETED, DatabaseItemTable.FALSE)
            setter.invoke(statement)
            createListFromStatement(statement)
        }
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


}
