package golf.adventofcode.database

import com.moshbit.katerbase.MongoDatabase
import com.moshbit.katerbase.child
import com.moshbit.katerbase.equal
import golf.adventofcode.Sysinfo

class MainDatabase(uri: String) : MongoDatabase(
    uri,
    allowReadFromSecondaries = Sysinfo.isOneOff,
    useMajorityWrite = Sysinfo.isWorker,
    supportChangeStreams = false,
    autoCreateCollections = Sysinfo.isPrimaryWeb || Sysinfo.isLocal,
    autoCreateIndexes = Sysinfo.isPrimaryWeb || Sysinfo.isLocal,
    autoDeleteIndexes = Sysinfo.isPrimaryWeb || Sysinfo.isLocal,
    collections = {
        collection<User>("users") {
            index(
                User::oAuthDetails.child(User.OAuthDetails::provider).ascending(),
                User::oAuthDetails.child(User.OAuthDetails::providerUserId).ascending()
            )
        }
        collection<Solution>("solutions") {
            index(
                Solution::userId.ascending(),
                Solution::year.ascending(),
                Solution::day.ascending(),
                Solution::part.ascending(),
                Solution::tokenCount.descending()
            )
            index(
                Solution::year.ascending(),
                Solution::day.ascending(),
                Solution::part.ascending(),
                Solution::tokenCount.descending()
            )
            index(
                Solution::tokenCountAnalyzeDate.descending(),
                Solution::uploadDate.descending(),
                partialIndex = arrayOf(Solution::tokenCountAnalyzeDate equal null)
            )
        }
    }
) {

}