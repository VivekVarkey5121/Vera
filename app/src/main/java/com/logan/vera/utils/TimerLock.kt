package com.logan.vera.utils

import android.content.Context

object TimerLock {
    private const val PREFS_NAME = "vera_focus_prefs"
    
    // Keys
    private const val KEY_LOCK_UNTIL = "lock_until"
    private const val KEY_ACCUMULATED_TIME = "time_read"
    private const val KEY_TOTAL_TIME_READ = "total_time_read"
    private const val KEY_READ_LIMIT = "read_lock_time"
    private const val KEY_LOCK_DURATION = "lock_duration_time"
    private const val KEY_FORCE_LOCK = "force_lock_duration_time" 




    fun setLockDuration(context: Context, minutes: Int) {
        val lockUntilMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        if (System.currentTimeMillis() >= prefs.getLong(KEY_LOCK_UNTIL, 0)) {
            prefs.edit().putLong(KEY_LOCK_UNTIL, lockUntilMillis).apply()
        }
    }

    fun isLocked(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0)
        return System.currentTimeMillis() < lockUntil
    }
    
    fun getRemainingSeconds(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockUntil = prefs.getLong(KEY_LOCK_UNTIL, 0)
        val diff = lockUntil - System.currentTimeMillis()
        return if (diff > 0) diff / 1000 else 0
    }


    fun addReadingTime(context: Context, millis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentAccumulated = prefs.getLong(KEY_ACCUMULATED_TIME, 0)
        val currentTotal = prefs.getLong(KEY_TOTAL_TIME_READ, 0)
        
        prefs.edit()
            .putLong(KEY_ACCUMULATED_TIME, currentAccumulated + millis)
            .putLong(KEY_TOTAL_TIME_READ, currentTotal + millis)
            .apply()
    }

    fun getAccumulatedTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_ACCUMULATED_TIME, 0)
    }

    fun resetAccumulatedTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_ACCUMULATED_TIME, 0).apply()
    }


    fun getReadLimitMins(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_READ_LIMIT, 15).toInt() 
    }

    fun setReadLimitMins(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_READ_LIMIT, minutes.toLong()).apply()
    }

    fun getLockDurationMins(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LOCK_DURATION, 5).toInt()
    }

    fun setLockDurationMins(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LOCK_DURATION, minutes.toLong()).apply()
    }


    fun getForceLockMins(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_FORCE_LOCK, 15).toInt()
    }

    fun setForceLockMins(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_FORCE_LOCK, minutes.toLong()).apply()
    }
}