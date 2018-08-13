package com.sapple.attendanceapp.datamodel

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity(tableName = "employeeInfo")
data class LoginData(
        @PrimaryKey(autoGenerate = true) var primaryId : Long?,
        @ColumnInfo(name = "userId") var userId : String?,
        @ColumnInfo(name = "imeiNumber") var imeiNumber : String? ) {
        constructor():this(null,"","")
}
