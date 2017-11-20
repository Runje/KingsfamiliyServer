package model;

import com.koenig.commonModel.User;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionService {
    Connection getConnectionFromUser(String userId) throws SQLException;

    User getUser(String userId) throws SQLException;
}
