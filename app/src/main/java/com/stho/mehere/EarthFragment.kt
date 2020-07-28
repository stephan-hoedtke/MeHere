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
import android.util.Log
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
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class EarthFragment : Fragment(), LocationListener, SensorEventListener {

    private lateinit var viewModel: EarthViewModel
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager

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
        requestMissingPermissions()
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> openSettings()
            R.id.action_reset -> reset()
            R.id.action_home -> home()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_earth, container, false)

        root.buttonZoomIn.setOnClickListener {
            viewModel.zoomIn()
        }
        root.buttonZoomOut.setOnClickListener {
            viewModel.zoomOut()
        }
        root.buttonHome.setOnClickListener {
            viewModel.updateCenter(viewModel.home)
        }
        root.buttonTarget.setOnClickListener {
            viewModel.updateCenter(viewModel.location)
            viewModel.enableTracking()
        }

        viewModel.northPointerLD.observe(viewLifecycleOwner, Observer { angle -> onObserveRotation(angle) })
        viewModel.locationLD.observe(viewLifecycleOwner, Observer { location -> onObserveLocation(location) })
        viewModel.homeLD.observe(viewLifecycleOwner, Observer { location -> onObserveHome(location) })
        viewModel.centerLD.observe(viewLifecycleOwner, Observer { center -> onObserveCenter(center) })
        viewModel.zoomLevelLD.observe(viewLifecycleOwner, Observer { zoomLevel -> onObserveZoomLevel(zoomLevel) })
        viewModel.canZoomInLD.observe(viewLifecycleOwner, Observer { canZoomIn -> root.buttonZoomIn.isEnabled = canZoomIn })
        viewModel.canZoomOutLD.observe(viewLifecycleOwner, Observer { canZoomOut -> root.buttonZoomOut.isEnabled = canZoomOut })

        createMapView(root.map, requireContext())
        updateActionBar(resources.getString(R.string.app_name))
        return root
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSettings()

        initializeHandler()
        resumeMapView()

        if (viewModel.useLocation) {
            enableLocationListener()
        }
        if (viewModel.useOrientation) {
            initializeAccelerationSensor()
            initializeMagneticFieldSensor()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.save()
        disableLocationListener()
        removeSensorListeners()
        removeHandler()
        pauseMapView()
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

    private fun createMapView(map: MapView, context: Context) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context.applicationContext))
        createMapView(map)
    }

    private fun createMapView(map: MapView) {
        map.minZoomLevel = EarthViewModel.minZoomLevel
        map.maxZoomLevel = EarthViewModel.maxZoomLevel
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.setMultiTouchControls(true)
        val tileSource: ITileSource = TileSourceFactory.DEFAULT_TILE_SOURCE
        TileSourceFactory.addTileSource(tileSource)
        //map.setTileSource(TileSourceFactory.MAPNIK);
        map.setTileSource(tileSource)
        map.tileProvider.createTileCache()
        map.isFlingEnabled = true
    }

    private fun resumeMapView() {
        view?.apply {
            map.onResume()
            map.addMapListener(mapListener)
        }
    }

    private fun pauseMapView() {
        view?.apply {
            map.removeMapListener(mapListener)
            map.onPause()
        }
    }

    private val mapListener by lazy {
        object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                viewModel.disableTracking()
                view?.also {
                    viewModel.updateCenter(it.map.mapCenter)
                }
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                view?.also {
                    viewModel.updateZoomLevel(it.map.zoomLevelDouble)
                }
                return true
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        viewModel.updateCurrentLocation(location)
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
        view?.apply {
            val rotation = angle.toFloat()
            if (compass.rotation != rotation) {
                compass.rotation = rotation
            }
        }
    }

    private fun onObserveLocation(location: Location) {
        setLocationMarker(location)

        if (viewModel.useTracking) {
            viewModel.updateCenter(location)
        }
    }

    private fun onObserveHome(location: Location) {
        setHomeMarker(location)
    }

    private fun setActionBarSubtitle(center: GeoPoint) {
        supportActionBar?.apply {
            subtitle = toDescription(center)
        }
    }

    private fun setLocationMarker(location: Location) {
        view?.apply {
            var marker = findMarkerById(locationMarkerId)
            if (marker == null) {
                marker = Marker(map).also {
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
                    it.id = locationMarkerId
                    it.icon = icon
                    it.title = "Me"
                    it.position = GeoPoint(location.latitude, location.longitude)
                    it.subDescription = toDescription(location)
                    it.setAnchor(fx, fy)
                }
                map.overlays.add(marker)
                map.invalidate()
            } else {
                marker.also {
                    it.position.latitude = location.latitude
                    it.position.longitude = location.longitude
                    it.subDescription = toDescription(location)
                }
                map.invalidate()
            }
        }
    }

    private fun setHomeMarker(home: Location) {
        view?.apply {
            var marker = findMarkerById(homeMarkerId)
            if (marker == null) {
                marker = Marker(map).also {
                    it.id = homeMarkerId
                    it.title = "Home"
                    it.position = GeoPoint(home.latitude, home.longitude)
                    it.subDescription = toDescription(home)
                    it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
                map.invalidate()
            } else {
                marker.also {
                    it.position.latitude = home.latitude
                    it.position.longitude = home.longitude
                    it.subDescription = toDescription(home)
                 }
                map.invalidate()
            }
        }
    }

    private fun toDescription(location: Location): String {
        return String.format(resources.getString(R.string.label_location_with_parameters, location.latitude, location.longitude, location.altitude))
    }

    private fun toDescription(point: GeoPoint): String {
        return String.format(resources.getString(R.string.label_location_with_parameters, point.latitude, point.longitude, point.altitude))
    }

    private fun toDescription(point: IGeoPoint): String {
        return String.format(resources.getString(R.string.label_location_with_parameters, point.latitude, point.longitude, 0.0))
    }

    private fun findMarkerById(id: String): Marker? {
        view?.map?.overlays?.forEach {
            if (it is Marker && it.id == id) {
                return it
            }
        }
        return null
    }

    private fun onObserveZoomLevel(zoomLevel: Double) {
        view?.apply {
            if (map.zoomLevelDouble != zoomLevel) {
                map.controller.setZoom(zoomLevel)
            }
            title.text = resources.getString(R.string.label_zoom_level_with_parameter, zoomLevel)
        }
    }

    private fun onObserveCenter(center: GeoPoint) {
        view?.apply {
            if (map.mapCenter.latitude != center.latitude || map.mapCenter.longitude != center.longitude) {
                map.controller.setCenter(center)
            }
        }
        setActionBarSubtitle(center)
    }

    private fun openSettings(): Boolean {
        findNavController().navigate(R.id.action_global_SettingsFragment)
        return true
    }

    private fun reset(): Boolean {
        viewModel.reset()
        return true
    }

    private fun home(): Boolean {
        viewModel.setHome(viewModel.center)
        return true
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
        private const val locationMarkerId = "location"
        private const val homeMarkerId = "home"
    }
}
