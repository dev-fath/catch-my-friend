package com.example.catchapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ScoreManager(private val context: Context) {

    companion object {
        private val SCORE_KEY = intPreferencesKey("score")
    }

    // 현재 점수를 가져오는 Flow
    val scoreFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SCORE_KEY] ?: 0
        }

    // 점수를 저장하는 함수
    suspend fun saveScore(score: Int) {
        context.dataStore.edit { preferences ->
            preferences[SCORE_KEY] = score
        }
    }

    // 점수를 증가시키는 함수
    suspend fun increaseScore(amount: Int) {
        context.dataStore.edit { preferences ->
            val currentScore = preferences[SCORE_KEY] ?: 0
            preferences[SCORE_KEY] = currentScore + amount
        }
    }

    // 점수를 초기화하는 함수
    suspend fun resetScore() {
        context.dataStore.edit { preferences ->
            preferences[SCORE_KEY] = 0
        }
    }
} 