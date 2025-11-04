package com.example.assignment2

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Locale

class DBHelper(context: Context, factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context,
    "LocationStore", factory, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        val query = """
            CREATE TABLE Locations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            address TEXT,
            lat REAL,
            lon REAL
            )
        """.trimIndent()
        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Locations")
    }

    fun getLocation(address: String): Location?{
        val db = this.readableDatabase
        val query = "SELECT lat, lon FROM Locations WHERE address = '$address'"
        val cursor = db.rawQuery(query, null)

        if(cursor.moveToFirst()){
            val location = Location(address, cursor.getDouble(0), cursor.getDouble(1))
            cursor.close()
            return location
        }else{
            cursor.close()
            return null
        }
    }

    fun addLocation(location: Location) {
        val db = this.writableDatabase
        val query = "INSERT INTO Locations (address, lat, lon) VALUES ('${location.address}', ${location.lat}, ${location.lon})"
        db.execSQL(query)
    }

    fun deleteLocation(location: Location) {
        val db = this.writableDatabase
        val query = "DELETE FROM Locations WHERE address = '${location.address}'"
        db.execSQL(query)
    }

    fun updateLocation(location: Location) {
        val db = this.writableDatabase
        val query = "UPDATE Locations SET lat = ${location.lat}, lon = ${location.lon} WHERE address = '${location.address}'"
        db.execSQL(query)
    }

    fun fillTable(){
        val locationList = Initializer().initialize()
        val db = this.writableDatabase
        for(location in locationList){
            val query = "INSERT INTO Locations (address, lat, lon) VALUES ('${location.address.toLowerCase()}', ${location.lat}, ${location.lon})"
            db.execSQL(query)
        }
    }

    fun isEmpty(): Boolean{
        val db = this.readableDatabase
        val query = "SELECT * FROM Locations"
        val cursor = db.rawQuery(query, null)
        if(cursor.moveToFirst()){
            cursor.close()
            return false
        }else{
            cursor.close()
            return true
        }
    }
}