package com.stho.mehere


// To use osmdroid:
// https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library
//
// To handle permissions:
// https://developer.android.com/training/permissions/requesting

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.stho.mehere.databinding.FragmentEarthBinding
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// TODO: fling --> onFling "scrolls" to an unexpected position, even backwards...
// TODO: Cache manager, SQL lite database, see what tiles have been loaded,
// TODO: enable to load tiles for an area


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class EarthFragment : Fragment(), LocationListener, SensorEventListener {

    private lateinit var viewModel: EarthViewModel
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private lateinit var connectivityManager: ConnectivityManager
    private var bindingReference: FragmentEarthBinding? = null
    private val binding: FragmentEarthBinding get() = bindingReference!!

    private val handler = Handler()

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val orientationAngles = FloatArray(3)
    private val rotationMatrix = FloatArray(9)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = createEarthViewModel()
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
            R.id.action_transparency -> setAlpha()
            R.id.action_networkStatus -> displayNetworkStatus()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bindingReference = FragmentEarthBinding.inflate(inflater, container, false)

        binding.buttonZoomIn.setOnClickListener { viewModel.zoomIn() }
        binding.buttonZoomOut.setOnClickListener { viewModel.zoomOut() }
        binding.buttonHome.setOnClickListener { viewModel.updateCenterAndDisableTracking(viewModel.home) }
        binding.buttonTarget.setOnClickListener { viewModel.updateCenterToCurrentLocationAndEnableTracking() }

        viewModel.northPointerLD.observe(viewLifecycleOwner, { angle -> onObserveRotation(angle) })
        viewModel.positionLD.observe(viewLifecycleOwner, { position -> onObservePosition(position) })
        viewModel.homeLD.observe(viewLifecycleOwner, { location -> onObserveHome(location) })
        viewModel.zoomLD.observe(viewLifecycleOwner, { zoomLevel -> onObserveZoom(zoomLevel) })
        viewModel.alphaLD.observe(viewLifecycleOwner, { alpha -> binding.compass.alpha = alpha })
        viewModel.canZoomInLD.observe(viewLifecycleOwner, { canZoomIn -> binding.buttonZoomIn.isEnabled = canZoomIn })
        viewModel.canZoomOutLD.observe(viewLifecycleOwner, { canZoomOut -> binding.buttonZoomOut.isEnabled = canZoomOut })
        viewModel.networkStatusLD.observe(viewLifecycleOwner, { status -> onObserveNetworkStatus(status) })

        createMapView(binding.map, requireContext())
        updateActionBar(resources.getString(R.string.app_name))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingReference = null
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSettings()

        initializeNetworkListener()
        updateNetworkStatus()
        initializeHandler()
        resumeMapView()

        if (viewModel.useLocation) {
            enableLocationListener()
        }
        if (viewModel.useOrientation) {
            initializeAccelerationSensor()
            initializeMagneticFieldSensor()
        }
        if (viewModel.useTracking) {
            viewModel.updateCenterToCurrentLocationAndEnableTracking()
        }

    }

    override fun onPause() {
        super.onPause()
        viewModel.save()
        disableLocationListener()
        disableNetworkListener()
        removeSensorListeners()
        removeHandler()
        pauseMapView()
    }

    private fun alphaToInt(f: Float): Int =
        (255 * f + 0.5).toInt()

    private fun intToAlpha(i: Int): Float =
        (i / 255.0).toFloat()

    private fun setAlpha(): Boolean {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val seek = SeekBar(requireContext())
        seek.max = 255
        seek.keyProgressIncrement = 1
        seek.progress = alphaToInt(viewModel.alpha)

        // TODO: set alert dialog theme: https://stackoverflow.com/questions/18346920/change-the-background-color-of-a-pop-up-dialog#

        dialog.setIcon(R.drawable.alpha_gray);
        dialog.setTitle("Transparency")
        dialog.setView(seek)

        seek.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

                viewModel.alpha = if (progress <= 20) {
                    intToAlpha(20)
                } else  {
                    intToAlpha(progress)
                }
            }

            override fun onStartTrackingTouch(arg0: SeekBar) {
                // TODO Auto-generated method stub
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // option: update the alpha value here when the scrolling stopped...
            }
        })

        // Button OK
        dialog.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
        dialog.create()
        dialog.show()
        return true
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
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    private fun initializeMagneticFieldSensor() {
        val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (magneticField != null) {
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_GAME
            )
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

    private val networkStatusCallback:  ConnectivityManager.NetworkCallback by lazy {
        object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                viewModel.setNetworkAvailable(connectivityManager, network)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                viewModel.setNetworkLost(network)
            }

            override fun onUnavailable() {
                super.onUnavailable()
                viewModel.setNetworkUnavailable()
            }
        }
    }

    private val toolbar: androidx.appcompat.widget.Toolbar
        get() = requireActivity().findViewById(R.id.toolbar)

    private fun onObserveNetworkStatus(status: NetworkStatusInfo) {
        updateNetworkStatusIcon(toolbar.menu, status)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        updateNetworkStatusIcon(menu, viewModel.networkStatus)
    }

    private fun updateNetworkStatusIcon(menu: Menu, status: NetworkStatusInfo) {
        val iconId = status.iconId
        val icon = ContextCompat.getDrawable(requireContext(), iconId)
        menu.findItem(R.id.action_networkStatus)?.icon = icon
    }

    private fun updateNetworkStatus() {
        val network = connectivityManager.activeNetwork
        if (network == null) {
            viewModel.setNetworkUnavailable()
        } else {
            viewModel.setNetworkAvailable(connectivityManager, network)
        }
    }

    private fun displayNetworkStatus(): Boolean {
        viewModel.networkStatusLD.value?.also {
            val message = it.toString()

            displayMessage(it.toString())
        }
        return true
    }

    private fun initializeNetworkListener() {
        connectivityManager.registerDefaultNetworkCallback(networkStatusCallback)
    }

    private fun disableNetworkListener() {
        connectivityManager.unregisterNetworkCallback(networkStatusCallback)
    }

    private fun requestMissingPermissions() {
        requestMissingPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }

    private fun requestMissingPermissions(permissions: Array<String>) {
        val permissionsToRequest: ArrayList<String> = ArrayList()
        for (permission in permissions) {
            if (permissionIsNotGranted(permission)) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun permissionIsNotGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED
    }

    private fun createMapView(map: MapView, context: Context) {
        Configuration.getInstance().load(
            context, PreferenceManager.getDefaultSharedPreferences(
                context.applicationContext
            )
        )
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
        binding.map.also {
            it.onResume()
            it.addMapListener(mapListener)
            it.setOnTouchListener(touchListener)
        }
    }

    private fun pauseMapView() {
        binding.map.also {
            it.removeMapListener(mapListener)
            it.onPause()
        }
    }

    private val mapListener by lazy {
        object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                viewModel.updateCenterOnScroll( binding.map.mapCenter)
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                viewModel.updateZoom(binding.map.zoomLevelDouble)
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
        viewModel.updateCurrentLocationOnLocationChanged(location)
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
                System.arraycopy(
                    event.values,
                    0,
                    accelerometerReading,
                    0,
                    accelerometerReading.size
                )
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
        if (SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading
            )) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            viewModel.update(orientationAngles)
        }
    }

    private fun onObserveRotation(angle: Double) {
        binding.compass.rotation = angle.toFloat()
    }

    private fun onObserveZoom(zoomLevel: Double) {
        if (binding.map.zoomLevelDouble != zoomLevel) {
            binding.map.controller.setZoom(zoomLevel)
        }
        binding.title.text = resources.getString(R.string.label_zoom_level_with_parameter, zoomLevel)
    }

    private fun onObservePosition(position: Repository.MyPosition) {
        setLocationMarker(position.currentLocation)
        setActionBarSubtitle(position.center)
        setMapCenterConditionally(position)
    }

    private fun onObserveHome(home: Position) {
        setHomeMarker(home)
    }

    private fun setMapCenterConditionally(position: Repository.MyPosition) {
        // Remarks:
        // a) Do not store a reference to a GeoPoint, as the referenced object may be changed
        // b) Only set the map center, if the change of the position was NOT caused by scrolling or flinging
        //    Otherwise the map's fling movement will be unpredictable
        if (position.source == Repository.MyPositionSource.MODEL && position.center.isSomewhereElse(binding.map.mapCenter)) {
            binding.map.controller.setCenter(position.center.toGeoPoint())
        }
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
        var marker = findMarkerById(locationMarkerId)
        if (marker == null) {
            marker = Marker(binding.map).also {
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
            binding.map.overlays.add(marker)
            binding.map.invalidate()
        } else {
            marker.also {
                it.position.latitude = position.latitude
                it.position.longitude = position.longitude
                it.subDescription = toDescription(position)
                it.setThisIcon(if (viewModel.useTracking) targetIconRed else targetIconBlue)
            }
            binding.map.invalidate()
        }
    }

    private fun setHomeMarker(home: Position) {
        var marker = findMarkerById(homeMarkerId)
        if (marker == null) {
            marker = Marker(binding.map).also {
                it.id = homeMarkerId
                it.title = "Home"
                it.position = home.toGeoPoint()
                it.subDescription = toDescription(home)
                it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            binding.map.overlays.add(marker)
            binding.map.invalidate()
        } else {
            marker.also {
                it.position.latitude = home.latitude
                it.position.longitude = home.longitude
                it.subDescription = toDescription(home)
            }
            binding.map.invalidate()
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

    private fun getColor(resId: Int): Int =
        ContextCompat.getColor(requireContext(), resId)

    private fun displayMessage(message: String): Boolean {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_INDEFINITE).also {
            it.setAction(R.string.label_close) { _ -> it.dismiss() }
            it.setBackgroundTint(getColor(R.color.colorAccent))
            it.setTextColor(getColor(android.R.color.white))
            it.setActionTextColor(getColor(R.color.colorPrimaryDark))
            it.duration = 23000
            it.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines = 4
            it.show()
        }

        return true
    }

    private fun toDescription(position: Position): String =
        resources.getString(
                R.string.label_location_with_parameters,
                position.latitude,
                position.longitude,
                position.altitude
            )

    private fun findMarkerById(id: String): Marker? {
        binding.map.overlays?.forEach {
            if (it is Marker && it.id == id) {
                return it
            }
        }
        return null
    }

    private fun openSettings(): Boolean {
        findNavController().navigate(R.id.action_global_SettingsFragment)
        return true
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

