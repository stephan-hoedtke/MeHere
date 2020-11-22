package com.stho.mehere


// To use osmdroid:
// https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library
//
// To handle permissions:
// https://developer.android.com/training/permissions/requesting

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
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
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_earth.view.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.lang.StringBuilder


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
            R.id.action_home -> centerAsHome()
            R.id.action_orientation -> displayOrientation()
            R.id.action_about -> displayAbout()
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
            viewModel.updateCenterAndDisableTracking(viewModel.home)
        }
        root.buttonTarget.setOnClickListener {
            viewModel.updateCenterToCurrentLocationAndEnableTracking()
        }

        viewModel.northPointerLD.observe(viewLifecycleOwner, Observer { angle -> onObserveRotation(angle) })
        viewModel.currentLocationLD.observe(viewLifecycleOwner, Observer { location -> onObserveCurrentLocation(location) })
        viewModel.homeLD.observe(viewLifecycleOwner, Observer { location -> onObserveHome(location) })
        viewModel.centerLD.observe(viewLifecycleOwner, Observer { center -> onObserveCenter(center) })
        viewModel.zoomLD.observe(viewLifecycleOwner, Observer { zoomLevel -> onObserveZoom(zoomLevel) })
        viewModel.canZoomInLD.observe(viewLifecycleOwner, Observer { canZoomIn -> root.buttonZoomIn.isEnabled = canZoomIn })
        viewModel.canZoomOutLD.observe(viewLifecycleOwner, Observer { canZoomOut -> root.buttonZoomOut.isEnabled = canZoomOut })

        createMapView(root.map, requireContext())
        updateActionBar(resources.getString(R.string.app_name))

        if (viewModel.useTracking)
            viewModel.updateCenterToCurrentLocationAndEnableTracking()

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
                handler.postDelayed(this, 100)
            }
        }, 100)
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
        map.minZoomLevel = EarthViewModel.minZoom
        map.maxZoomLevel = EarthViewModel.maxZoom
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.setMultiTouchControls(true)
        map.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        map.isFlingEnabled = true
    }

    private fun resumeMapView() {
        view?.apply {
            map.onResume()
            map.addMapListener(mapListener)
            map.setOnTouchListener(touchListener)
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
                view?.also {
                    viewModel.updateCenter(it.map.mapCenter)
                }
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                view?.also {
                    viewModel.updateZoom(it.map.zoomLevelDouble)
                }
                return true
            }
        }
    }

    private val touchListener by lazy {
        object : View.OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                viewModel.disableTracking()
                return false
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

    private fun onObserveCurrentLocation(position: Position) {
        setLocationMarker(position)
    }

    private fun onObserveHome(home: Position) {
        setHomeMarker(home)
    }

    private fun setActionBarSubtitle(center: Position) {
        supportActionBar?.apply {
            subtitle = toDescription(center)
        }
    }

    private val targetIconBlue by lazy {
        ContextCompat.getDrawable(requireActivity(), R.drawable.target_blue) as BitmapDrawable
    }

    private val targetIconRed by lazy {
        ContextCompat.getDrawable(requireActivity(), R.drawable.target_red) as BitmapDrawable
    }

    private fun setLocationMarker(position: Position) {
        view?.apply {
            var marker = findMarkerById(locationMarkerId)
            if (marker == null) {
                marker = Marker(map).also {
                    // Hack:
                    // OSMDroid version 6.1.3
                    // in Marker.java
                    // at protected void drawAt(final Canvas pCanvas, final int pX, final int pY, final float pOrientation)
                    // the offset is calculated with icon.intrinsicWidth (scaled for screen density), while the image is drawn from the original icon.bitmap
                    //      wanted offset := 0.5 * icon.bitmap.with
                    //      calculated offset := fx * icon.intrinsicWidth
                    //      --> fx = 0.5 * icon.bitmap.with / icon.intrinsicWidth
                    val icon = targetIconBlue
                    val fx: Float = Marker.ANCHOR_CENTER * icon.bitmap.width / icon.intrinsicWidth
                    val fy: Float = Marker.ANCHOR_CENTER * icon.bitmap.height / icon.intrinsicHeight
                    it.id = locationMarkerId
                    it.icon = icon
                    it.title = "Me"
                    it.position = position.toGeoPoint()
                    it.subDescription = toDescription(position)
                    it.setAnchor(fx, fy)
                }
                map.overlays.add(marker)
                map.invalidate()
            } else {
                marker.also {
                    it.position.latitude = position.latitude
                    it.position.longitude = position.longitude
                    it.subDescription = toDescription(position)
                    it.setThisIcon(if (viewModel.useTracking) targetIconRed else targetIconBlue)
                }
                map.invalidate()
            }
        }
    }

    private fun setHomeMarker(home: Position) {
        view?.apply {
            var marker = findMarkerById(homeMarkerId)
            if (marker == null) {
                marker = Marker(map).also {
                    it.id = homeMarkerId
                    it.title = "Home"
                    it.position = home.toGeoPoint()
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

    private fun displayOrientation(): Boolean {
        val message = StringBuilder()

        message.append("Orientation: ")
        message.append("\nO: " + orientationAngles[0])
        message.append("\nM: " + magnetometerReading[0] + " | " + magnetometerReading[1] + " | " + magnetometerReading[2])

        return displayMessage(message.toString())
    }

    private fun displayAbout(): Boolean {
        val message: String = String.format(
            resources.getString(R.string.label_about_with_parameters),
            BuildConfig.VERSION_NAME
        )
        return displayMessage(message)
    }

    private fun displayMessage(message: String): Boolean {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_INDEFINITE).also {
            it.setAction(R.string.label_close) { _ -> it.dismiss() }
            it.setActionTextColor(Color.GRAY)
            it.setTextColor(Color.YELLOW)
            it.setBackgroundTint(Color.BLACK)
            it.duration = 23000
            it.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 4
            it.show()
        }
        return true
    }

    private fun toDescription(position: Position): String {
        return String.format(resources.getString(R.string.label_location_with_parameters, position.latitude, position.longitude, position.altitude))
    }

    private fun findMarkerById(id: String): Marker? {
        view?.map?.overlays?.forEach {
            if (it is Marker && it.id == id) {
                return it
            }
        }
        return null
    }

    private fun onObserveZoom(zoomLevel: Double) {
        view?.apply {
            if (map.zoomLevelDouble != zoomLevel) {
                map.controller.setZoom(zoomLevel)
            }
            title.text = resources.getString(R.string.label_zoom_level_with_parameter, zoomLevel)
        }
    }

    private fun onObserveCenter(center: Position) {
        setCenter(center)
        setActionBarSubtitle(center)
    }

    private fun openSettings(): Boolean {
        findNavController().navigate(R.id.action_global_SettingsFragment)
        return true
    }

    private fun setCenter(center: Position) {
        // Do not store a reference to a GeoPoint, as the referenced object may be changed
        view?.apply {
            if (center.isSomewhereElse(map.mapCenter)) {
                map.controller.setCenter(center.toGeoPoint())
            }
        }
    }


    private fun reset(): Boolean {
        viewModel.reset()
        return true
    }

    private fun centerAsHome(): Boolean {
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
        private const val locationMarkerId = "location"
        private const val homeMarkerId = "home"
    }
}

private fun Marker.setThisIcon(icon: BitmapDrawable) {
    if (this.icon != icon)
        this.icon = icon
}

