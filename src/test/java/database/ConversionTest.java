package database;

import com.koenig.commonModel.database.DatabaseItem;
import com.koenig.commonModel.finance.Expenses;
import database.conversion.Converter;
import database.conversion.LGAExpenses;
import database.conversion.LGAExpensesTable;
import database.finance.BankAccountTable;
import database.finance.CategoryJavaTable;
import database.finance.ExpensesTable;
import database.finance.StandingOrderTable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class ConversionTest {

    private Connection lgaConnection;
    private String path;
    private Connection connection;
    private UserDatabase userDatabase;


    @Before
    public void setup() throws SQLException {
        // Can be created with BackupBrowser!
        path = "D:\\Bibliotheken\\Dokumente\\finances_db_backup_2017.10.11.sqlite";
        lgaConnection = DriverManager.getConnection("jdbc:sqlite:" + path);
        connection = DriverManager.getConnection("jdbc:sqlite:" + DatabaseHelper.FINANCEDB_TEST);
        userDatabase = DatabaseHelper.createUserDatabaseWithThomasAndMilena();
    }

    @After
    public void teardown() throws SQLException {
        lgaConnection.close();
        connection.close();
        userDatabase.stop();
    }

    @Test
    public void readOldDatabase() throws SQLException {
        LGAExpensesTable lgaExpensesTable = new LGAExpensesTable(lgaConnection);
        List<LGAExpenses> all = lgaExpensesTable.getAll();
        Assert.assertTrue(all.size() > 1);
    }

    @Test
    public void convert() throws SQLException {
        ExpensesTable expensesTable = new ExpensesTable(connection);
        StandingOrderTable standingOrderTable = new StandingOrderTable(connection);
        CategoryJavaTable categoryTable = new CategoryJavaTable(connection);
        BankAccountTable bankAccountTable = new BankAccountTable(connection, userDatabase.getUserService());
        if (!expensesTable.isExisting()) {
            expensesTable.create();
        } else expensesTable.deleteAllEntrys();

        if (!standingOrderTable.isExisting()) standingOrderTable.create();
        else standingOrderTable.deleteAllEntrys();

        if (!categoryTable.isExisting()) categoryTable.create();
        else categoryTable.deleteAllEntrys();

        if (!bankAccountTable.isExisting()) bankAccountTable.create();
        else bankAccountTable.deleteAllEntrys();

        Converter converter = new Converter(expensesTable, standingOrderTable, categoryTable, bankAccountTable, DatabaseHelper.milena, DatabaseHelper.thomas);
        converter.convert(path);

        List<DatabaseItem<Expenses>> allExpenses = expensesTable.getAll();
        Assert.assertTrue(allExpenses.size() > 1);

    }

}
