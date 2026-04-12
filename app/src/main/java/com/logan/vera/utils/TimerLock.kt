package com.logan.vera.utils

import android.content.Context
import java.time.LocalDate


object TimerLock {
    private const val PREFS_NAME = "vera_focus_prefs"
    
    // Keys
    private const val KEY_LOCK_UNTIL = "lock_until"
    private const val KEY_ACCUMULATED_TIME = "time_read"
    private const val KEY_TOTAL_TIME_READ = "total_time_read"
    private const val KEY_READ_LIMIT = "read_lock_time"
    private const val KEY_LOCK_DURATION = "lock_duration_time"
    private const val KEY_FORCE_LOCK = "force_lock_duration_time" 
    
    private const val KEY_DAILY_LOCK_TIME = "daily_timer_duration_time"
    private const val KEY_LAST_DATE = "key_last_date_accessed"






    fun getDailyLockMins(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_DAILY_LOCK_TIME, 120).toInt()
    }

    fun setDailyLockMins(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_DAILY_LOCK_TIME, minutes.toLong()).apply()
    }


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
        return if (diff > 0) diff else 0
    }


    fun addReadingTime(context: Context, millis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentAccumulated = prefs.getLong(KEY_ACCUMULATED_TIME, 0)
        val currentTotal = prefs.getLong(KEY_TOTAL_TIME_READ, 0)
        // val last_date_accessed = prefs.get
        
        prefs.edit()
            .putLong(KEY_ACCUMULATED_TIME, currentAccumulated + millis)
            .putLong(KEY_TOTAL_TIME_READ, currentTotal + millis)
            .apply()
    
        //if ((currentTotal + millis) <= getDailyLockMins(context) * 60 * 1000) {
        //    setLockDuration(context, 12 * 60 * 60)  //12 hours (is that too long?)
        //}
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

    fun formatMillis(millis: Long): String {
        
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        
        return "%d:%02d".format(minutes, seconds)
    }
}