package database;

import com.koenig.BinaryConverter;
import com.koenig.commonModel.Component;
import com.koenig.commonModel.Permission;
import com.koenig.commonModel.User;
import org.joda.time.DateTime;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class UserTable extends Table<User> {
    public static final String NAME = "user_table";
    public static final String USER_NAME = "user_name";
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
        String userName = rs.getString(USER_NAME);
        String family = rs.getString(FAMILY_NAME);
        DateTime birthday = getDateTime(rs, BIRTHDAY);
        Map<Component, Permission> permissionMap = getPermissions(rs, PERMISSIONS);
        return new User(userName, family, birthday, permissionMap);
    }

    private Map<Component, Permission> getPermissions(ResultSet rs, String permissions) throws SQLException {
        ByteBuffer buffer = ByteBuffer.wrap(rs.getBytes(permissions));
        return BinaryConverter.bytesToPermissions(buffer);
    }

    @Override
    protected String getTableSpecificCreateStatement() {
        return ", " + USER_NAME + " TEXT, " +
                FAMILY_NAME + " TEXT," +
                BIRTHDAY + " LONG," +
                PERMISSIONS + " BLOB";
    }

    @Override
    protected void setItem(NamedParameterStatement ps, User item) throws SQLException {
        ps.setString(USER_NAME, item.getName());
        ps.setString(FAMILY_NAME, item.getFamily());
        setDateTime(ps, BIRTHDAY, item.getBirthday());
        ps.setBytes(PERMISSIONS, BinaryConverter.permissionsToBytes(item.getPermissions()));
    }

    @Override
    protected String getNamesOfSpecificParameterWithColon() {
        return ",:" + USER_NAME + ", :" + FAMILY_NAME + ", :" + BIRTHDAY + ", :" + PERMISSIONS;
    }

    @Override
    protected String getNamesOfSpecificParameter() {
        return ", " + USER_NAME + ", " + FAMILY_NAME + ", " + BIRTHDAY + ", " + PERMISSIONS;
    }
}
