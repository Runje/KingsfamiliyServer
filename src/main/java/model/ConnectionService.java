package model;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionService {
    Connection getConnectionFromUser(String userId) throws SQLException;
}
