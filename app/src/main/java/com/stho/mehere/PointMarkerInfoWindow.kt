package com.stho.mehere

import android.widget.TextView
import androidx.fragment.app.Fragment
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

class PointMarkerInfoWindow(
    private val fragment: Fragment,
    private val point: PointOfInterest,
    mapView: MapView
) : MarkerInfoWindow(R.layout.bonuspack_bubble, mapView) {
    override fun onOpen(item: Any?) {
        super.onOpen(item)
        mView.findViewById<TextView>(R.id.bubble_title).setOnClickListener {
            EditPointDialogFragment.display(fragment.requireActivity(), point)
            close()
        }
    }
}
