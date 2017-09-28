package model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Created by Thomas on 07.01.2017.
 */
public class Main
{
    private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    public static void main(String[] args) throws SQLException
    {
        new FamilyModel().start();
    }
}
