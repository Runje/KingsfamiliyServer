package workflow;

import com.koenig.commonModel.User;
import com.koenig.commonModel.finance.CostDistribution;
import com.koenig.commonModel.finance.Expenses;
import org.joda.time.DateTime;

import java.util.Random;

public class ItemsCreater {

    public static Expenses expensesFrom(DateTime dateTime, User user) {
        Expenses expenses = randomExpenses(user);
        expenses.setDate(dateTime);
        return expenses;
    }

    public static Expenses randomExpenses(User user) {
        int i = Math.abs(new Random().nextInt());
        CostDistribution costDistribution = new CostDistribution();
        costDistribution.putCosts(user, i, i);
        return new Expenses("Test" + i, "Category" + i, "Subcategory" + i, i, costDistribution, DateTime.now(), "" + i);
    }
}
