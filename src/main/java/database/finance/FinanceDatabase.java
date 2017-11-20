package database.finance;

import com.koenig.commonModel.Category;
import com.koenig.commonModel.User;
import com.koenig.commonModel.finance.Expenses;
import database.Database;
import database.conversion.Converter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FinanceDatabase extends Database {

    ExpensesTable expensesTable;
    StandingOrderTable standingOrderTable;
    CategoryTable categoryTable;

    public FinanceDatabase(Connection connection) throws SQLException {
        super(connection);
        expensesTable = new ExpensesTable(connection);
        standingOrderTable = new StandingOrderTable(connection);
        categoryTable = new CategoryTable(connection);
        tables.add(expensesTable);
        tables.add(standingOrderTable);
        tables.add(categoryTable);
        createAllTables();
    }

    public void start() throws SQLException {



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
}
