package database;

import com.koenig.commonModel.Component;
import com.koenig.commonModel.Permission;
import com.koenig.commonModel.User;
import org.joda.time.DateTime;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UserTable extends Table<User> {
    public static final String NAME = "user_table";
    public static final String ABBREVIATION = "abbr";
    public static final String FAMILY_NAME = "family_name";
    public static final String BIRTHDAY = "birthday";
    public static final String PERMISSIONS = "permissions";

    public UserTable(Connection connection) {
        super(connection);
    }

    @Override
    public String getTableName() {
        return NAME;
    }

    @Override
    protected User getItem(ResultSet rs) throws SQLException {
        String userName = rs.getString(COLUMN_NAME);
        String abbreviation = rs.getString(ABBREVIATION);
        String family = rs.getString(FAMILY_NAME);
        DateTime birthday = getDateTime(rs, BIRTHDAY);
        Map<Component, Permission> permissionMap = getPermissions(rs, PERMISSIONS);
        return new User(userName, abbreviation, family, birthday, permissionMap);
    }

    private Map<Component, Permission> getPermissions(ResultSet rs, String permissions) throws SQLException {
        ByteBuffer buffer = ByteBuffer.wrap(rs.getBytes(permissions));
        return User.bytesToPermissions(buffer);
    }

    @Override
    protected String getTableSpecificCreateStatement() {
        return ", " +
                ABBREVIATION + " TEXT," +
                FAMILY_NAME + " TEXT," +
                BIRTHDAY + " LONG," +
                PERMISSIONS + " BLOB";
    }

    @Override
    protected void setItem(NamedParameterStatement ps, User item) throws SQLException {
        ps.setString(ABBREVIATION, item.getAbbreviation());
        ps.setString(FAMILY_NAME, item.getFamily());
        setDateTime(ps, BIRTHDAY, item.getBirthday());
        ps.setBytes(PERMISSIONS, User.permissionsToBytes(item.getPermissions()));
    }

    @Override
    protected List<String> getColumnNames() {
        return Arrays.asList(ABBREVIATION, FAMILY_NAME, BIRTHDAY, PERMISSIONS);
    }

    public void addFamileToUser(String familyName, String userId) throws SQLException {
        lock.lock();
        try {
            String selectQuery = "UPDATE " + getTableName() + " SET " + getNamedParameter(FAMILY_NAME) + " WHERE " + getNamedParameter(COLUMN_ID);

            NamedParameterStatement statement = new NamedParameterStatement(connection, selectQuery);
            statement.setString(COLUMN_ID, userId);
            statement.setString(FAMILY_NAME, familyName);
            statement.executeUpdate();
        } finally {
            lock.unlock();
        }
    }
}
