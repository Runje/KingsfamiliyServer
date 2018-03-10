package model.finance

// TODO:start date is now saved in family
// TODO: save statistics in table(don't use Deprecated!)
/**class FinanceAssetsCalculatorService(val startDateObservable: Observable<LocalDate>) : AssetsCalculatorService {
override val overallString: String
get() = FamilyConstants.OVERALL_STRING

override lateinit var startDate:LocalDate
override val futureString: String
get() = FamilyConstants.FUTURE_STRING
override val endDate: LocalDate
get() = LocalDate()


init {
startDateObservable.subscribe { startDate = it }
}

override fun loadAllBankAccountStatistics(): Map<BankAccount, List<StatisticEntryDeprecated>> {
val buffer = config.loadBuffer(ASSETS) ?: return HashMap()

val size = buffer.int
val listMap = HashMap<BankAccount, List<StatisticEntryDeprecated>>(size)
for (i in 0 until size) {
val bankAccount = BankAccount(buffer)
val entries = buffer.int
val statistics = ArrayList<StatisticEntryDeprecated>(entries)
for (j in 0 until entries) {
statistics.add(StatisticEntryDeprecated(buffer))
}

listMap.put(bankAccount, statistics)
}

return listMap
}


override fun save(statisticEntryLists: Map<BankAccount, List<StatisticEntryDeprecated>>) {
var size = 4
for (bankAccount in statisticEntryLists.keys) {
size += bankAccount.byteLength
size += Byteable.getBigListLength(statisticEntryLists[bankAccount]!!)
}

val buffer = ByteBuffer.allocate(size)
buffer.putInt(statisticEntryLists.size)
for (bankAccount in statisticEntryLists.keys) {
bankAccount.writeBytes(buffer)
Byteable.writeBigList(statisticEntryLists[bankAccount]!!, buffer)
}

config.saveBuffer(buffer, ASSETS)
}


companion object {
private val ASSETS = "ASSETS"
}
}**/


