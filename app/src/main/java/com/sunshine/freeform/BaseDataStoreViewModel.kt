package com.sunshine.freeform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.sunshine.freeform.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * @author sunshine
 * @date 2022/1/3
 */
class BaseDataStoreViewModel(context: Context) {
    private var dataStore: DataStore<Preferences> = context.dataStore

    fun readIntFlow(key: String, default: Int = 0): Flow<Int> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    it.printStackTrace()
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }.map {
                it[intPreferencesKey(key)] ?: default
            }

    fun readBooleanFlow(key: String, default: Boolean = false): Flow<Boolean> =
        dataStore.data
            .catch {
                if (it is IOException) {
                    it.printStackTrace()
                    emit(emptyPreferences())
                } else {
                    throw it
                }
            }.map {
                it[booleanPreferencesKey(key)] ?: default
            }

    suspend fun put(key: String, value: Any) {
        when (value::class) {
            String::class -> {
                dataStore.edit { settings ->
                    settings[stringPreferencesKey(key)] = value as String
                }
            }
            Int::class -> {
                dataStore.edit { settings ->
                    settings[intPreferencesKey(key)] = value as Int
                }
            }
            Double::class -> {
                dataStore.edit { settings ->
                    settings[doublePreferencesKey(key)] = value as Double
                }
            }
            Boolean::class -> {
                dataStore.edit { settings ->
                    settings[booleanPreferencesKey(key)] = value as Boolean
                }
            }
            Float::class -> {
                dataStore.edit { settings ->
                    settings[floatPreferencesKey(key)] = value as Float
                }
            }
            Long::class -> {
                dataStore.edit { settings ->
                    settings[longPreferencesKey(key)] = value as Long
                }
            }
        }
    }
}