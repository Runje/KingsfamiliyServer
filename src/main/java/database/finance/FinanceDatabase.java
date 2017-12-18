package database.finance;

import com.koenig.commonModel.Category;
import com.koenig.commonModel.Item;
import com.koenig.commonModel.ItemType;
import com.koenig.commonModel.User;
import com.koenig.commonModel.database.DatabaseItem;
import com.koenig.commonModel.finance.Expenses;
import com.koenig.commonModel.finance.StandingOrder;
import database.Database;
import database.Table;
import database.TransactionID;
import database.conversion.Converter;
import org.joda.time.DateTime;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FinanceDatabase extends Database {

    FinanceTransactionTable transactionTable;
    ExpensesTable expensesTable;
    StandingOrderTable standingOrderTable;
    CategoryTable categoryTable;

    public FinanceDatabase(Connection connection) throws SQLException {
        super(connection);
        expensesTable = new ExpensesTable(connection);
        standingOrderTable = new StandingOrderTable(connection);
        categoryTable = new CategoryTable(connection);
        transactionTable = new FinanceTransactionTable(connection);
        tables.add(expensesTable);
        tables.add(standingOrderTable);
        tables.add(categoryTable);
        tables.add(transactionTable);
        createAllTables();
    }

    public void start() {



    }

    public void convert(User milena, User thomas) throws SQLException {
        logger.info("Starting conversion...");

        Converter converter = new Converter(expensesTable, standingOrderTable, categoryTable, milena, thomas);
        converter.convert("D:\\Bibliotheken\\Dokumente\\finances_db_backup_2017.10.11.sqlite");
    }

    public List<Expenses> getAllExpenses() throws SQLException {
        return expensesTable.toItemList(expensesTable.getAll());
    }

    public void addExpenses(Expenses expenses, String userId) throws SQLException {
        expensesTable.addFrom(expenses, userId);
    }

    public void updateExpenses(Expenses expenses, String userId) throws SQLException {
        expensesTable.updateFrom(expenses, userId);
    }

    public void deleteExpenses(Expenses ex, String userId) throws SQLException {
        expensesTable.deleteFrom(ex.getId(), userId);
    }

    public List<Category> getAllCategorys() throws SQLException {
        return categoryTable.toItemList(categoryTable.getAll());
    }

    public void addTransaction(String id, String userId) throws SQLException {
        transactionTable.addFrom(new TransactionID(id), userId);
    }

    public boolean doesTransactionExist(String id) throws SQLException {
        return transactionTable.doesItemExist(id);
    }

    public List<DatabaseItem<Expenses>> getExpensesChangesSince(DateTime lastSyncDate) throws SQLException {
        return expensesTable.getChangesSinceDatabaseItems(lastSyncDate);
    }

    public List<DatabaseItem<StandingOrder>> getStandingOrderChangesSince(DateTime lastSyncDate) throws SQLException {
        return standingOrderTable.getChangesSinceDatabaseItems(lastSyncDate);
    }

    public List<DatabaseItem<Category>> getCategorysChangesSince(DateTime lastSyncDate) throws SQLException {
        return categoryTable.getChangesSinceDatabaseItems(lastSyncDate);
    }

    public void addCategory(Category transport, String userId) throws SQLException {
        categoryTable.addFrom(transport, userId);
    }

    @Override
    protected Table getItemTable(Item item) throws SQLException {
        switch (ItemType.fromItem(item)) {

            case EXPENSES:
                return expensesTable;
            case STANDING_ORDER:
                return standingOrderTable;
            case CATEGORY:
                return categoryTable;
            default:
                throw new SQLException("Unsupported item");
        }
    }


}
