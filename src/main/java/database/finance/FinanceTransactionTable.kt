package database.finance

import database.TransactionTable

import java.sql.Connection

class FinanceTransactionTable(connection: Connection) : TransactionTable(connection) {
    override val tableName = "FinanceTransactionTable"
}
