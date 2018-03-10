package workflow

import com.koenig.commonModel.User
import com.koenig.commonModel.finance.CostDistribution
import com.koenig.commonModel.finance.Expenses
import org.joda.time.LocalDate
import java.util.*

object ItemsCreater {

    fun expensesFrom(dateTime: LocalDate, user: User): Expenses {
        val expenses = randomExpenses(user)
        expenses.day = dateTime
        return expenses
    }

    fun randomExpenses(user: User): Expenses {
        val i = Math.abs(Random().nextInt())
        val costDistribution = CostDistribution()
        costDistribution.putCosts(user, i, i)
        return Expenses("Test$i", "Category$i", "Subcategory$i", i, costDistribution, LocalDate(), "" + i)
    }
}
