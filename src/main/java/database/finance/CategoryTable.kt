package database.finance

import com.koenig.commonModel.Category
import com.koenig.commonModel.database.DatabaseTable
import database.NamedParameterStatement
import database.Table
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class CategoryTable(connection: Connection) : Table<Category>(connection) {

    override val tableName: String
        get() = NAME

    override val tableSpecificCreateStatement: String
        get() = ", $SUBS TEXT"

    override val columnNames: List<String>
        get() {
            val columnNames = ArrayList<String>()
            columnNames.add(SUBS)
            return columnNames
        }

    @Throws(SQLException::class)
    override fun getItem(rs: ResultSet): Category {
        val main = rs.getString(DatabaseTable.COLUMN_NAME)
        val subs = DatabaseTable.getStringList(rs.getString(SUBS))
        return Category(main, subs)
    }

    @Throws(SQLException::class)
    override fun setItem(ps: NamedParameterStatement, item: Category) {
        setStringList(ps, SUBS, item.getSubs())
    }

    @Throws(SQLException::class)
    fun addIfNew(category: Category, userId: String) {
        runInLock {
            val dbCategorys = getDatabaseItemsFromName(category.name)
            if (dbCategorys.size == 0) {
                addFrom(category, userId)
            } else {
                val dbCategory = dbCategorys[0]
                var changed = false
                // add subcategories if new

                for (sub in category.getSubs()) {
                    var isNew = true
                    for (dbSub in dbCategory.item.getSubs()) {
                        if (sub == dbSub) {
                            isNew = false
                            break
                        }
                    }

                    if (isNew && !sub.isEmpty()) {
                        // add subcategory
                        dbCategory.item.addSub(sub)
                        changed = true
                    }
                }

                if (changed) {
                    updateFrom(dbCategory.item, userId)
                }
            }
        }
    }

    companion object {
        val NAME = "category_table"
        private val SUBS = "subs"
    }


}
