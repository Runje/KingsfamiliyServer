package database;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({UserTableTest.class, ConversionTest.class, DatabaseTest.class})
final public class DatabaseSuite {
}
