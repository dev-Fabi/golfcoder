package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.flow.toList
import kotlinx.html.*
import org.golfcoder.database.LeaderboardPosition
import org.golfcoder.database.Solution
import org.golfcoder.database.User
import org.golfcoder.endpoints.api.EditUserApi
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession
import org.golfcoder.utils.relativeToNow


object EditUserView {
    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()!!
        val currentUser = mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId)!!

        val mySolutions = mainDatabase.getSuspendingCollection<Solution>()
            .find(Solution::userId equal session.userId)
            .sortByDescending(Solution::uploadDate)
            .toList()

        val myLeaderboardPositions = mainDatabase.getSuspendingCollection<LeaderboardPosition>()
            .find(LeaderboardPosition::userId equal session.userId)
            .toList()
            .sortedByDescending { it.year * 10000 + it.day }
            .groupBy { it.year * 10000 + it.day }

        call.respondHtmlView("Golfcoder ${currentUser.name}") {
            h1 {
                renderUserProfileImage(currentUser, big = true)
                +currentUser.name
            }
            p {
                +"You joined Golfcoder ${currentUser.createdOn.relativeToNow}. "
            }

            val currentLoginProviders = currentUser.oAuthDetails.map { it.provider }
            p {
                +"Logged in via ${currentLoginProviders.joinToString { LoginView.oauth2Providers[it]!! }}. "
                a(href = "/login") { +"Add another OAuth2 provider." }
            }

            form(action = "/api/user/edit") {
                label {
                    attributes["for"] = "name"
                    +"Name: "
                    input(type = InputType.text) {
                        name = "name"
                        value = currentUser.name
                        maxLength = EditUserApi.MAX_USER_NAME_LENGTH.toString()
                    }
                }

                label("checkbox-container") {
                    +"Show my name on the leaderboard (${currentUser.name} vs \"anonymous\")"
                    input(type = InputType.checkBox) {
                        name = "nameIsPublic"
                        checked = currentUser.nameIsPublic
                        span("checkbox")
                    }
                }

                label("checkbox-container") {
                    +"Show my profile picture on the leaderboard (or use just my initials)"
                    input(type = InputType.checkBox) {
                        name = "profilePictureIsPublic"
                        checked = currentUser.profilePictureIsPublic
                        span("checkbox")
                    }
                }

                val singleAocRepositoryUrl = currentUser.adventOfCodeRepositoryInfo?.singleAocRepositoryUrl
                val yearAocRepositoryUrl = currentUser.adventOfCodeRepositoryInfo?.yearAocRepositoryUrl

                label {
                    attributes["for"] = "githubProfileUrl"
                    +"GitHub profile name to be linked next to your submissions: "
                    input(type = InputType.text) {
                        name = "githubProfileUrl"
                        // Transform e.g. "https://github.com/user/repo" to "user"
                        value = (singleAocRepositoryUrl ?: yearAocRepositoryUrl?.values?.firstOrNull())
                            ?.substringBeforeLast("/")?.substringAfterLast("/") ?: ""
                    }
                }
                p("text-secondary-info") {
                    +"If your GitHub profile is set, your advent-of-code repository will get automatically linked. "
                    +"You might name your repository e.g. advent-of-code, my-aoc-solutions, AdventOfCodeInPython or AoC-XXX."
                    br()
                    +"If you add years (e.g. 2023) to your repo names, the corresponding repository will be linked to your submission. "
                    +"So you might name your repositories also advent-of-code-2023, my-aoc2023-solutions, AdventOfCode_2023 or XXX-2023-AoC."
                }
                p {
                    when {
                        singleAocRepositoryUrl == null && yearAocRepositoryUrl.isNullOrEmpty() -> {
                            +"No advent-of-code repository linked."
                        }

                        singleAocRepositoryUrl != null -> {
                            +"Linked advent-of-code repository (1 for all years): "
                            a(href = singleAocRepositoryUrl) { +singleAocRepositoryUrl }
                        }

                        else -> {
                            +"Linked advent-of-code repositories per year:"
                            ul {
                                yearAocRepositoryUrl?.forEach { (year, repositoryUrl) ->
                                    li {
                                        +"$year: "
                                        a(href = repositoryUrl, target = "_blank") { +repositoryUrl }
                                    }
                                }
                            }
                        }
                    }
                }

                input(type = InputType.submit) {
                    onClick = "submitForm(event)"
                    value = "Save"
                }
            }

            h2 { +"My leaderboard positions" }
            if (mySolutions.isEmpty()) {
                p { +"No solutions uploaded yet." }
            } else {
                myLeaderboardPositions.forEach { (_, leaderboardPositionPerDay) ->
                    h3 { +"${leaderboardPositionPerDay.first().year} day ${leaderboardPositionPerDay.first().day}" }
                    with(LeaderboardDayView) {
                        renderLeaderboard(
                            leaderboardPositionPerDay,
                            userIdsToUsers = mapOf(session.userId to currentUser),
                            currentUser = currentUser
                        )
                    }
                }
            }

            h2 { +"My solutions" }
            if (mySolutions.isEmpty()) {
                p { +"No solutions uploaded yet." }
            } else {
                renderMySolutions(mySolutions)
            }
        }
    }

    private fun HtmlBlockTag.renderMySolutions(solutions: List<Solution>) {
        table("leaderboard") {
            thead {
                tr {
                    th(classes = "left-align") { +"Language" }
                    th(classes = "right-align") { +"Year" }
                    th(classes = "right-align") { +"Day" }
                    th(classes = "right-align") { +"Part" }
                    th(classes = "right-align") { +"Tokens" }
                    th(classes = "right-align") { +"Date" }
                }
            }
            tbody {
                // Render leaderboard
                solutions.forEach { solution ->
                    tr {
                        td("left-align") { +solution.language.displayName }
                        td("right-align") { +solution.year.toString() }
                        td("right-align") { +solution.day.toString() }
                        td("right-align") { +solution.part.toString() }
                        td("right-align") {
                            a(href = "/${solution.year}/day/${solution.day}?solution=${solution._id}#solution") {
                                +solution.tokenCount.toString()
                            }
                        }
                        td("right-align") { +solution.uploadDate.relativeToNow }
                    }
                }
            }
        }
    }

}