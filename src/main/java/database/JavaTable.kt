package database

import com.koenig.commonModel.database.DatabaseItemTable
import com.koenig.commonModel.database.DatabaseTable
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

abstract class JavaTable<T>(val connection: Connection) : DatabaseTable<T> {
    protected val logger = LoggerFactory.getLogger(javaClass.simpleName)
    override var lock: Lock = ReentrantLock()

    protected val namesOfSpecificParameterWithColon: String
        get() {
            val columnNames = columnNames
            val builder = StringBuilder(", :")
            for (columnName in columnNames) {
                builder.append("$columnName, :")
            }

            return builder.substring(0, builder.length - 3)
        }


    protected val namesOfSpecificParameter: String
        get() {
            val columnNames = columnNames
            val builder = StringBuilder(", ")
            for (columnName in columnNames) {
                builder.append("$columnName, ")
            }

            return builder.substring(0, builder.length - 2)
        }

    override fun addAll(items: Collection<T>) {
        runInLock({
            val ps = NamedParameterStatement(connection, "insert into " + tableName +
                    "(" + namesOfSpecificParameter + ") " +
                    " values(:" + namesOfSpecificParameterWithColon + ")")
            items.forEach {
                setDatabaseItem(ps, it)
                ps.addBatch()
            }

            ps.executeBatch()
        })
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
    protected abstract fun setDatabaseItem(ps: NamedParameterStatement, item: T)

    @Throws(SQLException::class)
    override fun add(item: T) {
        runInLock({
            val ps = NamedParameterStatement(connection, "insert into " + tableName +
                    "(" + namesOfSpecificParameter + ") " +
                    " values(:" + namesOfSpecificParameterWithColon + ")")

            setDatabaseItem(ps, item)
            ps.executeUpdate()
        })
    }

    override val all: List<T>
        get () {
            lock.lock()
            try {

                val items = ArrayList<T>()
                val selectQuery = "SELECT * FROM $tableName"
                val statement = NamedParameterStatement(connection, selectQuery)
                val rs = statement.executeQuery()
                while (rs.next()) {
                    items.add(getDatabaseItem(rs))
                }

                return items
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

    abstract fun getDatabaseItem(rs: ResultSet): T

    @Throws(SQLException::class)
    protected fun setDateTime(ps: NamedParameterStatement, columnName: String, date: DateTime) {
        ps.setLong(columnName, date.millis)
    }


    @Throws(SQLException::class)
    protected fun setBool(statement: NamedParameterStatement, columnName: String, b: Boolean) {
        // TODO: use Short
        statement.setInt(columnName, if (b) 1 else 0)
    }

    @Throws(SQLException::class)
    protected fun setStringList(ps: NamedParameterStatement, name: String, list: List<String>) {
        ps.setString(name, DatabaseItemTable.buildStringList(list))
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