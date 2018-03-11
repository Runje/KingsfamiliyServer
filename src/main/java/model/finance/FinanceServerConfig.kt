package model.finance

import com.koenig.commonModel.Family
import com.koenig.commonModel.FamilyConfigAbstract
import com.koenig.commonModel.User
import com.koenig.commonModel.finance.FinanceConfig
import org.joda.time.DateTime
import org.joda.time.YearMonth
import java.nio.ByteBuffer


// TODO: loadBuffer etc. macht hier keinen Sinn
class FinanceServerConfig(val family: Family, override val overallString: String, override val futureString: String, override val compensationName: String, override val compensationCategory: String) : FinanceConfig, FamilyConfigAbstract() {
    override fun loadBuffer(key: String): ByteBuffer? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveBuffer(buffer: ByteBuffer, key: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLastSyncDate(key: String): DateTime {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveLastSyncDate(date: DateTime, key: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadUserId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadFamilyMembers(): List<User> {
        return family.users
    }

    override fun loadStartDate(): YearMonth {
        return family.startMonth
    }

    override fun saveUserId(userId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveStartDate(date: YearMonth) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveFamilyMembers(members: List<User>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}