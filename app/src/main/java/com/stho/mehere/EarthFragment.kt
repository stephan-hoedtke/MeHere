package com.stho.mehere


// To use osmdroid:
// https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library
//
// To handle permissions:
// https://developer.android.com/training/permissions/requesting

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.fragment_earth.view.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class EarthFragment : Fragment(), LocationListener, SensorEventListener {

    private lateinit var viewModel: EarthViewModel
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var settings: Settings

    private var locationMarker: Marker? = null
    private var homeMarker: Marker? = null
    private val handler = Handler()

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = createEarthViewModel()
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        settings = Settings(requireContext())
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext().applicationContext))
        createMapView()
        requestMissingPermissions()
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> openSettings()
            R.id.action_reset -> reset()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_earth, container, false)

        root.buttonZoomIn.setOnClickListener { viewModel.zoomIn() }
        root.buttonZoomOut.setOnClickListener { viewModel.zoomOut() }
        root.buttonCenter.setOnClickListener { setCenter(viewModel.home) }
        root.buttonTarget.setOnClickListener { setCenter(viewModel.location) }

        viewModel.northPointerLD.observe(viewLifecycleOwner, Observer { angle -> onObserveRotation(angle) })
        viewModel.locationLD.observe(viewLifecycleOwner, Observer { location -> onObserveLocation(location) })
        viewModel.centerLD.observe(viewLifecycleOwner, Observer { center -> onObserveCenter(center) })
        viewModel.zoomLevelLD.observe(viewLifecycleOwner, Observer { zoomLevel -> onObserveZoomLevel(zoomLevel) })
        viewModel.canZoomInLD.observe(viewLifecycleOwner, Observer { canZoomIn -> root.buttonZoomIn.isEnabled = canZoomIn })
        viewModel.canZoomOutLD.observe(viewLifecycleOwner, Observer { canZoomOut -> root.buttonZoomOut.isEnabled = canZoomOut })

        updateActionBar(resources.getString(R.string.app_name))
        return root
    }

    override fun onResume() {
        super.onResume()
        retrieveSettings()
        initializeHandler()

        createMapView()
        setHomeMarker(viewModel.home)
        setCenter(viewModel.center)

        if (settings.useLocation) {
            enableLocationListener()
        }
        if (settings.useOrientation) {
            initializeAccelerationSensor()
            initializeMagneticFieldSensor()
        }

    }

    override fun onPause() {
        super.onPause()
        viewModel.save()
        disableLocationListener()
        clearMapView()
        removeSensorListeners()
        removeHandler()
    }

    private fun enableLocationListener() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, this)
        } catch (ex: SecurityException) {
            //ignore
        }
    }

    private fun disableLocationListener() {
        try {
            locationManager.removeUpdates(this)
        } catch (ex: SecurityException) {
            //ignore
        }
    }

    private fun initializeAccelerationSensor() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun initializeMagneticFieldSensor() {
        val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun initializeHandler() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                viewModel.updateNorthPointer()
                handler.postDelayed(this, HANDLER_DELAY.toLong())
            }
        }, HANDLER_DELAY.toLong())
    }

    private fun removeHandler() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun removeSensorListeners() {
        sensorManager.unregisterListener(this)
    }

    private fun requestMissingPermissions() {
        requestMissingPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }

    private fun requestMissingPermissions(permissions: Array<String>) {
        val permissionsToRequest: ArrayList<String> = ArrayList()
        for (permission in permissions) {
            if (permissionIsNotGranted(permission)) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun permissionIsNotGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED
    }

    private fun createMapView() {
        view?.apply {
            map.minZoomLevel = EarthViewModel.minZoomLevel
            map.maxZoomLevel = EarthViewModel.maxZoomLevel
            map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            map.setMultiTouchControls(true);
            val tileSource: ITileSource = TileSourceFactory.DEFAULT_TILE_SOURCE
            TileSourceFactory.addTileSource(tileSource)
            //map.setTileSource(TileSourceFactory.MAPNIK);
            map.setTileSource(tileSource)
            map.tileProvider.createTileCache()
            map.addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                    viewModel.disableTracking()
                    viewModel.setCenter(map.mapCenter)
                    return true
                }

                override fun onZoom(event: ZoomEvent?): Boolean {
                    viewModel.setZoomLevel(map.zoomLevelDouble)
                    return true
                }
            })
         }
    }

    private fun clearMapView() {
        view?.apply {
            homeMarker?.remove(map)
            locationMarker?.remove(map)
        }
        homeMarker = null
        locationMarker = null
    }

    override fun onLocationChanged(location: Location) {
        viewModel.update(location)
    }

    override fun onStatusChanged(provider: String, status: Int, parameters: Bundle?) {
        // Ignore
    }

    override fun onProviderEnabled(provider: String) {
        // Ignore
    }

    override fun onProviderDisabled(provider: String) {
        // Ignore
    }

    override fun onAccuracyChanged(sensor: Sensor, value: Int) {
        // Ignore
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
                updateOrientationAngles()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
                updateOrientationAngles()
            }
        }
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    private fun updateOrientationAngles() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            viewModel.update(orientationAngles)
        }
    }

    private fun onObserveRotation(angle: Double) {
        view?.compass?.rotation = angle.toFloat()
    }

    private fun onObserveLocation(location: Location) {
        setLocationMarker(location)

        if (settings.useTracking && viewModel.isTrackingEnabled) {
            viewModel.setCenter(location)
        }
    }

    private fun setActionBarSubtitle(center: GeoPoint) {
        supportActionBar?.apply {
            subtitle = String.format(resources.getString(R.string.label_location_with_parameters),
                center.latitude,
                center.longitude,
                center.altitude)
        }
    }

    private fun setLocationMarker(location: Location) {
        view?.apply {
            if (locationMarker == null) {
                locationMarker = Marker(map).also {
                    // Hack:
                    // OSMDroid version 6.1.3, file:
                    // in Marker.java
                    // at protected void drawAt(final Canvas pCanvas, final int pX, final int pY, final float pOrientation)
                    // the offset is calculated with icon.intrinsicWidth (scaled for screen density), while the image is drawn from the original icon.bitmap
                    // this is the hack:
                    //      wanted offset := 0.5 * icon.bitmap.with
                    //      calculated offset := fx * icon.intrinsicWidth
                    //      --> fx = 0.5 * icon.bitmap.with / icon.intrinsicWidth
                    val icon = ContextCompat.getDrawable(requireActivity(), R.drawable.target128) as BitmapDrawable
                    val fx: Float = Marker.ANCHOR_CENTER * icon.bitmap.width / icon.intrinsicWidth
                    val fy: Float = Marker.ANCHOR_CENTER * icon.bitmap.height / icon.intrinsicHeight
                    it.icon = icon
                    it.position = GeoPoint(location.latitude, location.longitude)
                    it.setAnchor(fx, fy)
                    it.title = "Me" + "\n" + String.format(resources.getString(R.string.label_location_with_parameters),
                        location.latitude,
                        location.longitude,
                        location.altitude)
                    it.subDescription = "Your current location"
                    it.infoWindow = BasicInfoWindow(R.layout.marker_window, map)
                }
                map.overlays.add(locationMarker)
            } else {
                locationMarker?.also {
                    it.position.latitude = location.latitude
                    it.position.longitude = location.longitude
                }
            }
        }
    }

    private fun setHomeMarker(home: Location) {
        view?.apply {
            if (homeMarker == null) {
                homeMarker = Marker(map).also {
                    it.position = GeoPoint(home.latitude, home.longitude)
                    it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    it.title = "Home"
                    it.subDescription = String.format(resources.getString(R.string.label_location_with_parameters, home.latitude, home.longitude, home.altitude))
                }
                map.overlays.add(homeMarker)
            } else {
                homeMarker?.also {
                    it.position.latitude = home.latitude
                    it.position.longitude = home.longitude
                    it.subDescription = String.format(resources.getString(R.string.label_location_with_parameters, home.latitude, home.longitude, home.altitude))
                 }
            }
        }
    }

    private fun onObserveZoomLevel(zoomLevel: Double) {
        view?.apply {
            map.controller.setZoom(zoomLevel)
            map.invalidate()
            title.text = resources.getString(R.string.label_zoom_level_with_parameter, zoomLevel)
        }
    }

    private fun onObserveCenter(center: GeoPoint) {
        setActionBarSubtitle(center)
    }

    private fun setCenter(location: Location) {
        setCenter(GeoPoint(location))
    }


    private fun setCenter(center: GeoPoint) {
        view?.apply {
            map.controller.setCenter(center)
        }
    }

    private fun openSettings(): Boolean {
        findNavController().navigate(R.id.action_global_SettingsFragment)
        return true
    }

    private fun reset(): Boolean {
        viewModel.reset()
        return true
    }

    private fun retrieveSettings() {
        settings.update(requireContext())
    }

    private fun updateActionBar(title: String) {
        supportActionBar?.apply {
            this.title = title
            this.subtitle = null
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(true)
        }
    }

    private val supportActionBar: androidx.appcompat.app.ActionBar?
        get() = (activity as AppCompatActivity?)?.supportActionBar

    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
        private const val HANDLER_DELAY = 100
    }
}
