package database

import java.sql.Connection
import java.sql.ResultSet
import java.util.*

// TODO: convert to JavaTable
abstract class TransactionTable(connection: Connection) : ItemTable<TransactionID>(connection) {


    override val columnNames: List<String>
        get() = ArrayList()

    override val itemSpecificCreateStatement: String
        get() = ""

    override fun getItem(rs: ResultSet): TransactionID {
        return TransactionID("")
    }

    override fun setItem(ps: NamedParameterStatement, item: TransactionID) {

    }
}
