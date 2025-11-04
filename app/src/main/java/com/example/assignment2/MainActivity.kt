package com.example.assignment2

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private val geocoder = Geocoder(this)
    private var mapMarker: Marker? = null
    private var mapPermission = false
    private val zoomLevel = 12f

    private val REQUEST_CODE_PERMISSION = 299
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
        == PackageManager.PERMISSION_GRANTED)

        if (!mapPermission) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                REQUEST_CODE_PERMISSION
            )
        }

        val db = DBHelper(this, null)
        if(db.isEmpty()){
            db.fillTable()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        addressBar = findViewById(R.id.addressBar)
        addButton = findViewById(R.id.addLocation)

        addressBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val location = db.getLocation(query!!.lowercase())
                if(location != null){
                    mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.lat, location.lon), zoomLevel))
                    setMarker(LatLng(location.lat, location.lon), location.address)
                    return true
                }
                addButton.visibility = View.VISIBLE
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //do Nothing
                return true
            }

        })

        addButton.setOnClickListener {
            val addressName = addressBar.query.toString()
            val address = geocoder.getFromLocationName(addressName, 1)
            if(address!!.isEmpty()){
                Toast.makeText(this, "Couldn't Find Address", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val location = address[0]
            db.addLocation(Location(addressName.lowercase(), location.latitude, location.longitude))
            mainMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), zoomLevel))
            setMarker(LatLng(location.latitude, location.longitude), addressName)
            addButton.visibility = View.GONE
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mainMap = googleMap
        mainMap.uiSettings.isZoomControlsEnabled = true

        //Default location set to Toronto
        val defaultLocation = LatLng(43.651070, -79.347015)
        mainMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, zoomLevel))
    }

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

    fun setMarker(position: LatLng, name: String){
        mapMarker?.remove()
        mapMarker = mainMap.addMarker(MarkerOptions().position(position).title(name))
        if(addButton.isVisible) {
            addButton.visibility = View.GONE
        }
    }
}