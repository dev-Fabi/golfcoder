package org.golfcoder.endpoints.web

import com.moshbit.katerbase.equal
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import org.golfcoder.database.User
import org.golfcoder.mainDatabase
import org.golfcoder.plugins.UserSession

object LoginView {
    // providerName to providerDisplayName
    val oauth2Providers = mapOf("github" to "GitHub", "google" to "Google", "reddit" to "Reddit")

    suspend fun getHtml(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()
        val currentUser =
            session?.let { mainDatabase.getSuspendingCollection<User>().findOne(User::_id equal session.userId) }

        call.respondHtmlView(if (currentUser == null) "Login" else "Link Account") {
            if (currentUser == null) {
                h1 {
                    +"Login"
                }
                p {
                    +"Please login via OAuth2 with one of theses providers:"
                }
                ul {
                    for ((providerName, providerDisplayName) in oauth2Providers) {
                        li {
                            a(href = "/login/$providerName") { +providerDisplayName }
                        }
                    }
                }
            } else {
                val currentLoginProviders = currentUser.oAuthDetails.map { it.provider }
                val newLoginProviders = oauth2Providers.filter { it.key !in currentLoginProviders }
                h1 {
                    +"Link Account"
                }
                p {
                    +"You are already logged in via ${currentLoginProviders.joinToString { oauth2Providers[it]!! }}."
                }
                if (newLoginProviders.isNotEmpty()) {
                    p {
                        +"If you want to link another account, select one of these providers."
                    }
                    ul {
                        for ((providerName, providerDisplayName) in newLoginProviders) {
                            li {
                                a(href = "/login/$providerName") { +providerDisplayName }
                            }
                        }
                    }
                }
            }

            p {
                +"You can link multiple providers to the same Golfcoder user. "
                +"This allows you to login with any of these providers."
            }
        }
    }
}