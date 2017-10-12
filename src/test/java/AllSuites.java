import database.DatabaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import workflow.WorkflowSuite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
        DatabaseSuite.class,
        WorkflowSuite.class
})
public class AllSuites {
}
