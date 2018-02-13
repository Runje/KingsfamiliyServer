package workflow

import com.koenig.commonModel.Component
import com.koenig.commonModel.ItemType
import com.koenig.commonModel.finance.Expenses
import com.koenig.communication.messages.AUDMessage
import com.koenig.communication.messages.AskForUpdatesMessage
import com.koenig.communication.messages.UpdatesMessage
import com.koenig.communication.messages.finance.FinanceTextMessages
import database.DatabaseHelper
import org.joda.time.DateTime
import org.joda.time.Duration
import org.junit.Assert
import org.junit.Test

class ExpensesConflict : WorkflowBase() {
    @Test
    @Throws(InterruptedException::class)
    fun addWhileGone() {
        val lastSyncDate = DateTime.now()
        askForExpensesUpdatesSince(simulatorThomas, lastSyncDate)
        assertLastExpensesUpdates(0, simulatorThomas)

        simulatorThomas.clearMessages()
        addExpenses(ItemsCreater.expensesFrom(lastSyncDate.minus(Duration.millis(1)), DatabaseHelper.milena), simulatorMilena)
        askForExpensesUpdatesSince(simulatorThomas, lastSyncDate)
        assertLastExpensesUpdates(1, simulatorThomas)
    }

    @Throws(InterruptedException::class)
    private fun addExpenses(expenses: Expenses, simulator: Simulator) {
        simulator.sendFamilyMessage(AUDMessage.createAdd(expenses))
        Assert.assertTrue(simulator.waitForTextMessage(FinanceTextMessages.AUD_SUCCESS, 2))
    }

    @Throws(InterruptedException::class)
    private fun assertLastExpensesUpdates(expectedNumber: Int, simulator: Simulator) {
        val updatesMessage = waitForMessage(UpdatesMessage.NAME, simulator) as UpdatesMessage<*>
        Assert.assertEquals(expectedNumber.toLong(), updatesMessage.items.size.toLong())
    }

    private fun askForExpensesUpdatesSince(simulator: Simulator, lastSyncDate: DateTime) {
        val askForUpdatesMessage = AskForUpdatesMessage(Component.FINANCE, lastSyncDate, ItemType.EXPENSES)
        simulator.sendFamilyMessage(askForUpdatesMessage)
    }


}
