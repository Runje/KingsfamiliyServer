package database

import com.koenig.commonModel.Item
import com.sun.istack.internal.NotNull
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.SQLException
import java.util.*

abstract class Database(protected var connection: Connection) {
    protected var logger = LoggerFactory.getLogger(javaClass.simpleName)
    protected var tables: MutableList<ItemTable<*>> = ArrayList()

    @Throws(SQLException::class)
    protected abstract fun getItemTable(@NotNull item: Item): ItemTable<Item>

    @Throws(SQLException::class)
    fun addItem(@NotNull item: Item, userId: String) {
        getItemTable(item).addFrom(item, userId)
    }

    @Throws(SQLException::class)
    fun deleteItem(@NotNull item: Item, userId: String) {
        getItemTable(item).deleteFrom(item.id, userId)
    }

    @Throws(SQLException::class)
    fun updateItem(@NotNull item: Item, userId: String) {
        getItemTable(item).updateFrom(item, userId)
    }

    @Throws(SQLException::class)
    fun createAllTables() {
        for (table in tables) {
            if (!table.isExisting()) {
                table.create()
                logger.info("ItemTable created: " + table.tableName)
            }
        }
    }

    @Throws(SQLException::class)
    fun deleteAllEntrys() {
        for (table in tables) {
            table.deleteAllEntrys()
        }
    }


    /**
     * Runs a transaction and locks a specific table. Rollback on exception.
     *
     * @param runnable
     * @param table
     * @throws SQLException
     */
    @Throws(SQLException::class)
    fun startTransaction(runnable: () -> Unit, table: ItemTable<*>) {
        table.lock.lock()
        try {
            connection.autoCommit = false
            runnable.invoke()
            connection.commit()
        } catch (e: Exception) {
            logger.error("Error on transaction: " + e.message)
            connection.rollback()
            logger.info("Rolled back")
            throw e
        } finally {
            connection.autoCommit = true
            table.lock.unlock()
        }
    }

    /**
     * Runs a transaction and locks all tables. Rollback on Exception
     *
     * @param runnable
     * @throws SQLException
     */
    @Throws(SQLException::class)
    protected fun startTransaction(runnable: () -> Unit) {
        for (table in tables) {
            table.lock.lock()
        }
        try {
            connection.autoCommit = false
            runnable.invoke()
            connection.commit()
        } catch (e: Exception) {
            logger.error("Error on transaction: " + e.message)
            connection.rollback()
            logger.info("Rolled back")
            throw e
        } finally {
            connection.autoCommit = true
            for (table in tables) {
                table.lock.unlock()
            }
        }
    }

    @Throws(SQLException::class)
    fun itemExists(item: Item): Boolean {

        return getItemTable(item).doesItemExist(item.id)
    }





}
