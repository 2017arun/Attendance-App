package com.sapple.attendanceapp.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.sapple.attendanceapp.datamodel.LoginData

@Database( entities = [LoginData::class], version = 1)
abstract class DbHelper : RoomDatabase() {
    abstract fun attendanceDao() : AttendanceDao

    companion object {
        private var INSTANCE : DbHelper ?= null
        private const val DATABASE_NAME: String = "AttendanceApp.db"

        fun getInstance(context : Context) : DbHelper ? {
            if ( INSTANCE == null ) {
                synchronized(DbHelper::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                            DbHelper::class.java, DATABASE_NAME).build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}