package database;

import com.koenig.commonModel.finance.Expenses;
import database.conversion.Converter;
import database.conversion.LGAExpenses;
import database.conversion.LGAExpensesTable;
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
    private String DB_TEST_NAME = "testDatabase.sqlite";

    @Before
    public void setup() throws SQLException {
        // Can be created with BackupBrowser!
        path = "D:\\Bibliotheken\\Dokumente\\finances_db_backup_2017.10.11.sqlite";
        lgaConnection = DriverManager.getConnection("jdbc:sqlite:" + path);
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_TEST_NAME);
    }

    @After
    public void teardown() throws SQLException {
        lgaConnection.close();
        connection.close();
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
        if (!expensesTable.isExisting()) {
            expensesTable.create();
        } else expensesTable.deleteAllEntrys();

        if (!standingOrderTable.isExisting()) standingOrderTable.create();
        else standingOrderTable.deleteAllEntrys();

        Converter converter = new Converter(expensesTable, standingOrderTable, "MILENA", "THOMAS");
        converter.convert(path);

        List<DatabaseItem<Expenses>> allExpenses = expensesTable.getAll();
        Assert.assertTrue(allExpenses.size() > 1);

    }

}
