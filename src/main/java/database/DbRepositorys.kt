package database

import com.koenig.commonModel.Item
import com.koenig.commonModel.Repository.ExpensesRepository
import com.koenig.commonModel.Repository.Repository
import com.koenig.commonModel.Repository.StandingOrderRepository
import com.koenig.commonModel.database.DatabaseItem
import com.koenig.commonModel.finance.Expenses
import com.koenig.commonModel.finance.StandingOrder
import database.finance.ExpensesTable
import database.finance.StandingOrderTable
import io.reactivex.Observable
import org.joda.time.DateTime

abstract class DbRepository<T : Item>(val table: Table<T>) : Repository<T> {
    val thomasId = "c572d4e7-da4b-41d8-9c1f-7e9a97657155"

    override val hasChanged: Observable<Boolean>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val allItemsObservable: Observable<List<T>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val allItems: List<T>
        get() = table.allItems

    override fun updateFromServer(items: List<DatabaseItem<T>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(item: T) {
        table.deleteFrom(item.id, thomasId)
    }

    override fun add(item: T) {
        table.addFrom(item, thomasId)
    }

    override fun update(item: T) {
        table.updateFrom(item, thomasId)
    }

    override fun getFromId(id: String): T? {
        return table.getFromId(id)
    }
}

class ExpensesDbRepository(expensesTable: ExpensesTable) : ExpensesRepository, DbRepository<Expenses>(expensesTable)
class StandingOrderDbRepository(val standingOrderTable: StandingOrderTable) : StandingOrderRepository, DbRepository<StandingOrder>(standingOrderTable) {
    override fun addExpensesToStandingOrders(standingOrderId: String, expensesId: String, dateTime: DateTime) {
        standingOrderTable.addExpensesToStandingOrders(standingOrderId, expensesId, dateTime)
    }

}