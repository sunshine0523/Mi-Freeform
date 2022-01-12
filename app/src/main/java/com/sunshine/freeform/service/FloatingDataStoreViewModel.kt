package com.sunshine.freeform.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.sunshine.freeform.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * @author sunshine
 * @date 2022/1/3
 */
class FloatingDataStoreViewModel(context: Context) {
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
}