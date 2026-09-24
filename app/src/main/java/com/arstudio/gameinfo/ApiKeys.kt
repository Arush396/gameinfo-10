package com.arstudio.gameinfo

/**
 * Free IGDB / Twitch developer credentials (https://dev.twitch.tv/console -> Register Your Application,
 * then use the Client ID and generate a Client Secret). IGDB is a games database whose cover art comes from
 * the publishers, and it covers console and mobile games that Steam does not.
 * Leave blank and the app still works, but only Steam + App Store icons will load.
 */
object ApiKeys {
    val IGDB_CLIENT_ID: String = ""
    val IGDB_CLIENT_SECRET: String = ""
    val igdbConfigured: Boolean get() = IGDB_CLIENT_ID.isNotBlank() && IGDB_CLIENT_SECRET.isNotBlank()
}
