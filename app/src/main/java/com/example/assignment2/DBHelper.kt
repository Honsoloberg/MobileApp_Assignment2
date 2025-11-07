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

        //when the database is created, calls fill table to initialize database with default locations
        fillTable()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Locations")
    }

    //queries a location from the database based on address
    fun getLocation(address: String): Location? {
        val db = this.readableDatabase
        val query = "SELECT * FROM Locations WHERE address = '$address'"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            val location =
                Location(address, cursor.getDouble(2), cursor.getDouble(3), cursor.getInt(0))
            cursor.close()
            return location
        } else {
            cursor.close()
            return null
        }
    }

    //adds location to the database
    fun addLocation(location: Location) {
        val db = this.writableDatabase
        val query =
            "INSERT INTO Locations (address, lat, lon) VALUES ('${location.address}', ${location.lat}, ${location.lon})"
        db.execSQL(query)
    }

    //deletes a location from the database
    fun deleteLocation(location: Location) {
        val db = this.writableDatabase
        val query = "DELETE FROM Locations WHERE id = '${location.id}'"
        db.execSQL(query)
    }

    //update information for a location in the database
    fun updateLocation(location: Location) {
        val db = this.writableDatabase
        val query =
            "UPDATE Locations SET address = '${location.address}', lat = ${location.lat}, lon = ${location.lon} WHERE id = '${location.id}'"
        db.execSQL(query)
    }

    //function to fill the database with the list of default locations
    fun fillTable() {
        val locationList = Initializer().initialize()
        val db = this.writableDatabase
        for (location in locationList) {
            val query =
                "INSERT INTO Locations (address, lat, lon) VALUES ('${location.address.toLowerCase()}', ${location.lat}, ${location.lon})"
            db.execSQL(query)
        }
    }
}