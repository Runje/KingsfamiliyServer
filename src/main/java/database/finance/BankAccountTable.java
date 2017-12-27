package database.finance;

import com.koenig.commonModel.User;
import com.koenig.commonModel.database.UserService;
import com.koenig.commonModel.finance.Balance;
import com.koenig.commonModel.finance.BankAccount;
import database.NamedParameterStatement;
import database.Table;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BankAccountTable extends Table<BankAccount> {
    public static final String NAME = "bankaccount_table";
    private static final String BANK = "bank";
    private static final String BALANCES = "balances";
    private static final String OWNERS = "owners";
    private UserService userService;

    public BankAccountTable(Connection connection, UserService userService) {
        super(connection);
        this.userService = userService;
    }

    @Override
    public String getTableName() {
        return NAME;
    }

    @Override
    protected BankAccount getItem(ResultSet rs) throws SQLException {
        String name = rs.getString(COLUMN_NAME);
        String bank = rs.getString(BANK);
        List<Balance> balances = Balance.getBalances(rs.getBytes(BALANCES));
        List<User> owners = getUsers(userService, rs.getString(OWNERS));
        return new BankAccount(name, bank, owners, balances);
    }


    @Override
    protected String getTableSpecificCreateStatement() {
        return ", " + BANK + " TEXT, " + BALANCES + " BLOB, " + OWNERS + " TEXT";
    }

    @Override
    protected void setItem(NamedParameterStatement ps, BankAccount item) throws SQLException {
        ps.setString(BANK, item.getBank());
        ps.setBytes(BALANCES, Balance.listToBytes(item.getBalances()));
        setUsers(item.getOwners(), ps, OWNERS);
    }


    @Override
    protected List<String> getColumnNames() {
        ArrayList<String> columnNames = new ArrayList<>();
        columnNames.add(BANK);
        columnNames.add(BALANCES);
        columnNames.add(OWNERS);
        return columnNames;
    }

}
