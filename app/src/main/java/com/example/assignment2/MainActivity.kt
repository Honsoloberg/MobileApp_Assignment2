package com.example.assignment2

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mainMap: GoogleMap
    private lateinit var addressBar: SearchView
    private lateinit var addButton: Button
    private lateinit var delButton: Button
    private lateinit var updateButton: Button
    private lateinit var latText: EditText
    private lateinit var lonText: EditText
    private val geocoder = Geocoder(this)
    private var mapMarker: Marker? = null
    private var currentLocation: Location? = null
    private var mapPermission = false
    private val zoomLevel = 12f

    private val REQUEST_CODE_PERMISSION = 299
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //check that map permission is granted
        mapPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
        == PackageManager.PERMISSION_GRANTED)

        //if the map permission is not granted, request it
        if (!mapPermission) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                REQUEST_CODE_PERMISSION
            )
        }

        //initialize database helper.
        val db = DBHelper(this, null)
        if(db.isEmpty()){
            //when the database is created, calls fill table to initialize database with default locations
            db.fillTable()
        }

        //initialize map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //initialize all text views
        addressBar = findViewById(R.id.addressBar)
        latText = findViewById(R.id.latBar)
        lonText = findViewById(R.id.lonBar)

        //initialize all buttons
        addButton = findViewById(R.id.addLocation)
        delButton = findViewById(R.id.delete)
        updateButton = findViewById(R.id.update)

        //the search view that allows the user to search for an address
        //implements onSubmit and does nothing on textChange
        addressBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                //get location from database based on address
                currentLocation = db.getLocation(query!!.lowercase())

                //check if the location returned from the database exists
                if(currentLocation != null){
                    //set the map location and set a marker for the location
                    mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(currentLocation!!.lat, currentLocation!!.lon), zoomLevel))
                    setMarker(LatLng(currentLocation!!.lat, currentLocation!!.lon), currentLocation!!.address)

                    //fill the textViews with corresponding location data to be presented to the user
                    latText.setText(currentLocation!!.lat.toString())
                    lonText.setText(currentLocation!!.lon.toString())

                    //change the visibility of function buttons based on search state
                    //show delete and update buttons if the location entry exists and ensure the add location button is hidden
                    delButton.visibility = View.VISIBLE
                    updateButton.visibility = View.VISIBLE
                    addButton.visibility = View.GONE

                    return true
                }

                //if the location entry doesn't exist ensure the text view's show nothing
                latText.setText("")
                lonText.setText("")

                //change the visibility of function buttons based on search state
                //show add button if the location entry doesn't exist
                delButton.visibility = View.GONE
                updateButton.visibility = View.GONE
                addButton.visibility = View.VISIBLE

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //do Nothing
                return true
            }

        })

        delButton.setOnClickListener {
            //delete the current searched location from the database
            db.deleteLocation(currentLocation!!)
            currentLocation = null

            //remove the location marker from the map
            mapMarker!!.remove()

            //clear all text views of data since the location was deleted.
            latText.setText("")
            lonText.setText("")
            addressBar.setQuery("", false)
            addressBar.clearFocus()

            //remove visibility of the delete and update buttons since search state isn't a found location
            delButton.visibility = View.GONE
            updateButton.visibility = View.GONE
        }

        updateButton.setOnClickListener {
            //take current information from all text views
            val addressName = addressBar.query.toString()
            val lat = latText.text.toString().toDouble()
            val lon = lonText.text.toString().toDouble()

            //set camera location to the current location gathered from text views
            mainMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), zoomLevel))
            setMarker(LatLng(lat, lon), addressName)

            //update the new data for the current location
            currentLocation!!.address = addressName
            currentLocation!!.lat = lat
            currentLocation!!.lon = lon

            //store new location data into the database
            db.updateLocation(currentLocation!!)
        }

        addButton.setOnClickListener {
            //get the address from the search view
            val addressName = addressBar.query.toString()

            //ensure that the search view is not empty. Prompt user if it's empty
            if(addressName == ""){
                Toast.makeText(this, "Please Enter an Address", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            //use geocoder to retrieve location data and ensure that the location was found by the geocoder
            //prompt user of error if the location was not found
            val address = geocoder.getFromLocationName(addressName, 1)
            if(address!!.isEmpty()){
                Toast.makeText(this, "Couldn't Find Address", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            //parse data from geocoder and store the new location into the database
            val location = address[0]
            db.addLocation(Location(addressName.lowercase(), location.latitude, location.longitude))
            currentLocation = db.getLocation(addressName.lowercase())

            //set map location to the newly added location and create a marker
            mainMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), zoomLevel))
            setMarker(LatLng(location.latitude, location.longitude), addressName)

            //present location data to the user in the text views
            latText.setText(currentLocation!!.lat.toString())
            lonText.setText(currentLocation!!.lon.toString())

            //change the visibility of function buttons based on search state
            //show delete and update buttons if the location entry exists and ensure the add location button is hidden
            delButton.visibility = View.VISIBLE
            updateButton.visibility = View.VISIBLE
            addButton.visibility = View.GONE
        }
    }

    //onMapReady callback function
    override fun onMapReady(googleMap: GoogleMap) {
        //initialize the mainMap as a googlemap and enable the zoom controls for the user
        mainMap = googleMap
        mainMap.uiSettings.isZoomControlsEnabled = true

        //Default location set to Toronto and set as map location
        val defaultLocation = LatLng(43.651070, -79.347015)
        mainMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, zoomLevel))
    }

    //request permissions result callback function
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == REQUEST_CODE_PERMISSION
            && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            mapPermission = true
        }else{
            val message = "Location permission is required for this application to run.\n Please enable this permission"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    //Set marker function
    //Ensures only one marker is displayed at a time
    //when a new marker is set, the previous marker is removed to ensure only one is tracked
    fun setMarker(position: LatLng, name: String){
        mapMarker?.remove()
        mapMarker = mainMap.addMarker(MarkerOptions().position(position).title(name))
        if(addButton.isVisible) {
            addButton.visibility = View.GONE
        }
    }
}