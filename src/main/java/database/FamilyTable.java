package database;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import com.koenig.commonModel.database.DatabaseItem;
import com.koenig.commonModel.database.UserService;
import com.koenig.communication.messages.FamilyMessage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class FamilyTable extends Table<Family> {
    public static final String NAME = "family_table";
    public static final String USERS = "users";
    private final UserService userService;

    public FamilyTable(Connection connection, UserService userService) {
        super(connection);
        this.userService = userService;
    }

    @Override
    public String getTableName() {
        return NAME;
    }

    @Override
    protected Family getItem(ResultSet rs) throws SQLException {
        String usersText = rs.getString(USERS);
        List<User> users = getUsers(userService, usersText);
        String family = rs.getString(COLUMN_NAME);
        return new Family(family, users);
    }

    @Override
    protected String getTableSpecificCreateStatement() {
        return ", " + USERS + " TEXT ";
    }

    @Override
    protected void setItem(NamedParameterStatement ps, Family item) throws SQLException {
        setUsers(item.getUsers(), ps, USERS);
    }

    @Override
    protected List<String> getColumnNames() {
        return Arrays.asList(USERS);
    }

    public void addUserToFamily(Family family, String userId) throws SQLException {
        lock.lock();
        try {
            String selectQuery = "UPDATE " + getTableName() + " SET " + getNamedParameter(USERS) + " WHERE " + getNamedParameter(COLUMN_ID);

            NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
            statement.setString(COLUMN_ID, family.getId());
            StringBuilder builder = new StringBuilder();
            for (User user : family.getUsers()) {
                builder.append(user.getId());
                builder.append(FamilyMessage.SEPARATOR);
            }

            builder.append(userId);
            String users = builder.substring(0, builder.length());
            statement.setString(USERS, users);
            statement.executeUpdate();
        } finally {
            lock.unlock();
        }
    }

    public Family getFamilyByName(String familyName) throws SQLException {
        lock.lock();
        try {
            DatabaseItem<Family> family = null;
            String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_DELETED + " = :" + COLUMN_DELETED + " AND " + getNamedParameter(COLUMN_NAME);

            NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
            statement.setInt(COLUMN_DELETED, FALSE);
            statement.setString(COLUMN_NAME, familyName);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                family = resultToItem(rs);
            }

            return family == null ? null : family.getItem();
        } finally {
            lock.unlock();
        }
    }
}
