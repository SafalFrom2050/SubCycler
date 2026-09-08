package com.subtrack.core.domain.model

import java.util.Locale

/** A curated external destination, never a cancellation command or proof of cancellation. */
data class CancellationDestination(
    val label: String,
    val url: String,
    val guidance: String
)

/**
 * Exact, case-insensitive aliases only: a custom name must never become a URL or match by substring.
 * Links and official source references are documented in plans/features/CANCELLATION_DEEP_LINKS_FEATURE.md.
 * Billing channel is chosen on each visit; it cannot be inferred from the installed app or service name.
 */
object CancellationLinks {
    val googlePlay = CancellationDestination(
        label = "Google Play subscriptions",
        url = "https://play.google.com/store/account/subscriptions",
        guidance = "Use the Google account that paid for this subscription. Select the service, then follow the cancellation steps."
    )
    val apple = CancellationDestination(
        label = "Apple billing instructions",
        url = "https://support.apple.com/118428",
        guidance = "For purchases billed by Apple, follow Apple's instructions. You may need an Apple device or a computer."
    )

    private fun account(label: String, url: String) = CancellationDestination(
        label, url, "For subscriptions billed directly by this service. Sign in with the account that pays and follow the provider's cancellation steps."
    )

    private fun help(url: String) = CancellationDestination(
        "Service cancellation instructions", url,
        "Follow the official instructions for your billing provider. Some changes require a desktop browser or another device."
    )

    private val netflix = account("Netflix membership", "https://www.netflix.com/cancelplan")
    private val disney = account("Disney+ account", "https://www.disneyplus.com/account")
    private val max = account("HBO Max subscription", "https://www.hbomax.com/subscription")
    private val youtube = account("YouTube memberships", "https://www.youtube.com/paid_memberships")
    private val spotify = account("Spotify subscription", "https://www.spotify.com/account/subscription/manage/")
    private val prime = help("https://www.primevideo.com/help?nodeId=GWGDSNXVPJ93UW5V")
    private val chatgpt = help("https://help.openai.com/en/articles/7232927")
    private val claude = help("https://support.claude.com/en/articles/8325617")
    private val googleOne = account("Google One settings", "https://one.google.com/settings")
    private val notion = help("https://www.notion.com/help/upgrade-or-downgrade-your-plan")
    private val xbox = account("Microsoft subscriptions", "https://account.microsoft.com/services")
    private val playstation = help("https://www.playstation.com/en-us/support/store/cancel-ps-store-subscription/")
    private val icloud = help("https://support.apple.com/108318")

    private val services = mapOf(
        "netflix" to netflix,
        "disney+" to disney, "disney plus" to disney,
        "max (hbo)" to max, "max" to max, "hbo max" to max,
        "youtube premium" to youtube, "youtube music premium" to youtube,
        "spotify" to spotify, "spotify premium" to spotify,
        "apple music" to apple, "apple tv+" to apple, "apple tv plus" to apple,
        "amazon prime video" to prime, "prime video" to prime,
        "chatgpt plus" to chatgpt, "chatgpt pro" to chatgpt, "chatgpt" to chatgpt,
        "claude pro" to claude, "claude max" to claude, "claude" to claude,
        "google ai plus" to googleOne, "google ai pro" to googleOne,
        "google ai ultra" to googleOne, "google one" to googleOne,
        "notion plus" to notion, "notion" to notion,
        "xbox game pass ultimate" to xbox, "xbox game pass" to xbox,
        "playstation plus" to playstation, "ps plus" to playstation,
        "icloud+" to icloud, "icloud plus" to icloud
    )

    fun forService(serviceName: String): CancellationDestination? =
        services[serviceName.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")]

    fun destinationsFor(serviceName: String): List<CancellationDestination> =
        listOfNotNull(forService(serviceName), googlePlay, apple).distinctBy { it.url }
}
