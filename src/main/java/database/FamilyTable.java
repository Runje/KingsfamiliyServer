package database;

import com.koenig.commonModel.Family;
import com.koenig.commonModel.User;
import com.koenig.communication.messages.FamilyMessage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FamilyTable extends Table<Family> {
    public static final String NAME = "family_table";
    public static final String USERS = "users";
    public static final String FAMILY_NAME = "family_name";
    private final UserTable userTable;

    public FamilyTable(Connection connection, UserTable userTable) {
        super(connection);
        this.userTable = userTable;
    }

    @Override
    public String getTableName() {
        return NAME;
    }

    @Override
    protected Family getItem(ResultSet rs) throws SQLException {
        String usersText = rs.getString(USERS);
        List<User> users = new ArrayList<>();
        if (!usersText.isEmpty()) {
            String[] userIds = usersText.split(FamilyMessage.SEPARATOR);


            for (String id :
                    userIds) {
                users.add(userTable.getFromId(id));
            }
        }

        String family = rs.getString(FAMILY_NAME);

        return new Family(family, users);
    }


    @Override
    protected String getTableSpecificCreateStatement() {
        return ", " + USERS + " TEXT, " +
                FAMILY_NAME + " TEXT";
    }

    @Override
    protected void setItem(NamedParameterStatement ps, Family item) throws SQLException {
        setUsers(item.getUsers(), ps);
        ps.setString(FAMILY_NAME, item.getName());
    }

    private void setUsers(List<User> users, NamedParameterStatement ps) throws SQLException {
        StringBuilder builder = new StringBuilder();
        for (User user : users) {
            builder.append(user.getId());
            builder.append(FamilyMessage.SEPARATOR);
        }

        String result = users.size() > 0 ? builder.substring(0, builder.length() - 1) : "";
        ps.setString(USERS, result);
    }

    @Override
    protected String getNamesOfSpecificParameterWithColon() {
        return ",:" + USERS + ", :" + FAMILY_NAME;
    }

    @Override
    protected String getNamesOfSpecificParameter() {
        return ", " + USERS + ", " + FAMILY_NAME;
    }

    public void addUserToFamily(Family family, String userId) throws SQLException {
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
    }

    public Family getFamilyByName(String familyName) throws SQLException {
        DatabaseItem<Family> family = null;
        String selectQuery = "SELECT * FROM " + getTableName() + " WHERE " + COLUMN_DELETED + " = :" + COLUMN_DELETED + " AND " + getNamedParameter(FAMILY_NAME);

        NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
        statement.setInt(COLUMN_DELETED, FALSE);
        statement.setString(FAMILY_NAME, familyName);
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            family = resultToItem(rs);
        }

        return family == null ? null : family.item;
    }
}
