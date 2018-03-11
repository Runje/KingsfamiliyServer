package database

import com.koenig.commonModel.Item
import com.koenig.commonModel.Repository.BankAccountRepository
import com.koenig.commonModel.Repository.ExpensesRepository
import com.koenig.commonModel.Repository.Repository
import com.koenig.commonModel.Repository.StandingOrderRepository
import com.koenig.commonModel.database.DatabaseItem
import com.koenig.commonModel.finance.BankAccount
import com.koenig.commonModel.finance.Expenses
import com.koenig.commonModel.finance.StandingOrder
import database.finance.BankAccountTable
import database.finance.ExpensesTable
import database.finance.StandingOrderTable
import io.reactivex.Observable
import org.joda.time.LocalDate

abstract class DbRepository<T : Item>(val table: ItemTable<T>) : Repository<T> {
    val thomasId = "c572d4e7-da4b-41d8-9c1f-7e9a97657155"

    override val hasChanged: Observable<Boolean> // is not needed on server
        get() = Observable.never()

    override val allItems: List<T>
        get() = table.allItems

    override fun updateFromServer(items: List<DatabaseItem<T>>) {
        // not needed
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

class ExpensesDbRepository(private val expensesTable: ExpensesTable) : ExpensesRepository, DbRepository<Expenses>(expensesTable) {
    override val compensations: Map<LocalDate, Expenses>
        get() = expensesTable.compensations
}

class StandingOrderDbRepository(val standingOrderTable: StandingOrderTable) : StandingOrderRepository, DbRepository<StandingOrder>(standingOrderTable) {
    override fun addExpensesToStandingOrders(standingOrderId: String, expensesId: String, day: LocalDate) {
        standingOrderTable.addExpensesToStandingOrders(standingOrderId, expensesId, day)
    }

}

class BankAccountDbRepository(val bankAccountTable: BankAccountTable) : BankAccountRepository, DbRepository<BankAccount>(bankAccountTable)

