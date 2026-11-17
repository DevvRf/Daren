package com.gamespace.app

import android.content.Context

/** Simple SharedPreferences-backed list of [GameProfile]s, keyed by id. */
class GameProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences("game_profiles", Context.MODE_PRIVATE)
    private val KEY = "profiles_json"

    fun loadAll(): List<GameProfile> {
        val json = prefs.getString(KEY, null)
        return if (json == null) {
            val defaults = GameProfile.defaultProfiles()
            saveAll(defaults)
            defaults
        } else {
            parseProfileList(json)
        }
    }

    fun saveAll(profiles: List<GameProfile>) {
        prefs.edit().putString(KEY, profiles.toJsonArray()).apply()
    }

    fun upsert(profile: GameProfile) {
        val current = loadAll().toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) current[index] = profile else current.add(profile)
        saveAll(current)
    }

    fun delete(id: String) {
        saveAll(loadAll().filterNot { it.id == id })
    }
}
