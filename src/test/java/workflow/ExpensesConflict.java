package workflow;

import com.koenig.commonModel.Component;
import com.koenig.commonModel.ItemType;
import com.koenig.commonModel.finance.Expenses;
import com.koenig.communication.messages.AUDMessage;
import com.koenig.communication.messages.AskForUpdatesMessage;
import com.koenig.communication.messages.UpdatesMessage;
import com.koenig.communication.messages.finance.FinanceTextMessages;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Test;

public class ExpensesConflict extends WorkflowBase {
    @Test
    public void addWhileGone() throws InterruptedException {
        DateTime lastSyncDate = DateTime.now();
        askForExpensesUpdatesSince(simulatorThomas, lastSyncDate);
        assertLastExpensesUpdates(0, simulatorThomas);

        simulatorThomas.clearMessages();
        addExpenses(ItemsCreater.expensesFrom(lastSyncDate.minus(Duration.millis(1)), milena), simulatorMilena);
        askForExpensesUpdatesSince(simulatorThomas, lastSyncDate);
        assertLastExpensesUpdates(1, simulatorThomas);
    }

    private void addExpenses(Expenses expenses, Simulator simulator) throws InterruptedException {
        simulator.sendFamilyMessage(AUDMessage.createAdd(expenses));
        Assert.assertTrue(simulator.waitForTextMessage(FinanceTextMessages.AUD_SUCCESS, 2));
    }

    private void assertLastExpensesUpdates(int expectedNumber, Simulator simulator) throws InterruptedException {
        UpdatesMessage updatesMessage = (UpdatesMessage) waitForMessage(UpdatesMessage.NAME, simulator);
        Assert.assertEquals(expectedNumber, updatesMessage.getItems().size());
    }

    private void askForExpensesUpdatesSince(Simulator simulator, DateTime lastSyncDate) {
        AskForUpdatesMessage askForUpdatesMessage = new AskForUpdatesMessage(Component.FINANCE, lastSyncDate, ItemType.EXPENSES);
        simulator.sendFamilyMessage(askForUpdatesMessage);
    }


}
