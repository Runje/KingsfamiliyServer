package database.finance

import com.koenig.commonModel.Category
import com.koenig.commonModel.Repository.CategoryDbRepository
import com.koenig.commonModel.database.CategoryTable
import com.koenig.commonModel.database.CategoryTable.Companion.SUBS
import com.koenig.commonModel.database.DatabaseItemTable
import com.koenig.commonModel.database.MonthStatisticTable
import database.ItemTable
import database.NamedParameterStatement
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

class CategoryJavaTable(connection: Connection) : ItemTable<Category>(connection), CategoryTable {



    @Throws(SQLException::class)
    override fun getItem(rs: ResultSet): Category {
        val main = rs.getString(DatabaseItemTable.COLUMN_NAME)
        val subs = DatabaseItemTable.getStringList(rs.getString(SUBS))
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
            if (dbCategorys.isEmpty()) {
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
}

class CategoryJavaRepository(val connection: Connection) : CategoryDbRepository {
    override val categoryTable: CategoryTable = CategoryJavaTable(connection)
    override val allCategoryAbsoluteTable = MonthStatisticJavaTable("all_categories_absolute", connection).apply { create() }
    override val allCategoryDeltaTable = MonthStatisticJavaTable("all_categories_delta", connection).apply { create() }

    override fun getTable(name: String): MonthStatisticTable {
        val table = MonthStatisticJavaTable(name, connection)
        table.create()
        return table
    }
}
