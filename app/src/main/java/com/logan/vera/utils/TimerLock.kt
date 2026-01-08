package com.logan.vera.utils

import android.content.Context

object TimerLock {
    private const val PREFS_NAME = "vera_focus_prefs"
    private const val KEY_LOCK_UNTIL = "lock_until"
    private const val KEY_ACCUMULATED_TIME = "time_read"
    private const val KEY_TOTAL_TIME_READ = "total_time_read"

    // Call this to lock the app for a certain number of minutes
    fun setLockDuration(context: Context, minutes: Int) {
        val lockUntilMillis = System.currentTimeMillis() + (minutes * 60 * 1000)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LOCK_UNTIL, lockUntilMillis).apply()
    }

    // Call this to check if we are currently in "Lock Mode"
    fun isLocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0)
        return System.currentTimeMillis() < lockUntil
    }
    
    // Helper to see how many seconds are left (for your UI)
    fun getRemainingSeconds(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0)
        val diff = lockUntil - System.currentTimeMillis()
        return if (diff > 0) diff / 1000 else 0
    }

    fun addReadingTime(context: Context, millis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 1. Get the current values
        val currentAccumulated = prefs.getLong(KEY_ACCUMULATED_TIME, 0)
        val currentTotal = prefs.getLong(KEY_TOTAL_TIME_READ, 0)
        
        // 2. Save both new values in one single edit block
        prefs.edit()
            .putLong(KEY_ACCUMULATED_TIME, currentAccumulated + millis)
            .putLong(KEY_TOTAL_TIME_READ, currentTotal + millis)
            .apply()
    }

    // Gets the total time in the "Reading Bank"
    fun getAccumulatedTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_ACCUMULATED_TIME, 0)
    }

    // Resets the bank back to 0
    fun resetAccumulatedTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_ACCUMULATED_TIME, 0).apply()
    }


}