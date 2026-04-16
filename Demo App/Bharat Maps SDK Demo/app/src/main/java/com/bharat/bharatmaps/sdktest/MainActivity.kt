package com.bharat.bharatmaps.sdktest

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.bharat.bharatmaps.sdktest.databinding.ActivityMainBinding
import com.bharatmaps.android.BharatMaps
import com.bharatmaps.android.maps.BharatMapView
import com.bharatmaps.android.maps.BharatMapsBuildingNumberResult
import com.bharatmaps.android.maps.BharatMapsMap
import com.bharatmaps.android.maps.BharatMapsPoiResult
import com.bharatmaps.android.maps.BharatMapsReverseGeocodingResult
import com.bharatmaps.android.maps.BharatMapsAdvancedAnnotationOptions
import com.bharatmaps.android.maps.BharatMapsUPinResult
import com.bharatmaps.geojson.Point
import com.google.gson.Gson
import android.graphics.Color
import android.util.TypedValue
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : FullScreenActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapView: BharatMapView

    private var bharatMap: BharatMapsMap? = null
    private var isUpinEnabled: Boolean = false
    private val reverseGeocodingAnnotationId = "reverse_geocoding_preview"

    private val defaultOrigin = Point.fromLngLat(77.22958479945846, 28.612902557101886)
    private val defaultDestination =  Point.fromLngLat(75.79366500197791, 26.86366227665046) // Point.fromLngLat(75.79923241670603, 26.859956142389272) // Point.fromLngLat(77.2385, 28.6259)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        BharatMaps.getInstance(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView

        mapView.getMapAsync { map ->
            bharatMap = map
            map.validateLicense("YOUR_BHARATMAPS_API_KEY") { result, error ->
                runOnUiThread {
                    if (error != null) {
                        Log.e("BHARAT_LOG", "License validation failed", error)
                        binding.tvStatus.text = "License failed"
                    } else {
                        Log.d("BHARAT_LOG", "License OK token=${result?.token}")
                    }
                }
            }
            binding.tvStatus.text = getString(R.string.status_map_ready)
            map.enableUserLocation { enabled ->
                runOnUiThread {
                    binding.tvStatus.text = if (enabled) {
                        getString(R.string.status_location_enabled)
                    } else {
                        getString(R.string.status_permission_denied)
                    }
                }
            }

            map.setOnMapEntityClickListener(object : BharatMapsMap.MapEntityClickListener {
                override fun onPoiClick(poi: BharatMapsPoiResult) {
                    Log.d("BHARAT_LOG", "POI ${poi}")
                    Log.d("BHARAT_LOG", "Location ${poi.location}")
                    Log.d("BHARAT_LOG", Gson().toJson(poi))
                    // poi.location: Point
                    // poi.category / poi.subcategory
                    // poi.iconResId
                }

                override fun onBuildingNumberClick(building: BharatMapsBuildingNumberResult) {
                    // building.buildingNumber, building.address, building.location
                    Log.d("BHARAT_LOG", "BUILDING ${building}")
                    Log.d("BHARAT_LOG", Gson().toJson(building))
                }

                override fun onUPinClick(upin: BharatMapsUPinResult) {
                    Log.d("BHARAT_LOG", "UPIN ${upin}")
                    Log.d("BHARAT_LOG", Gson().toJson(upin))
                }
            })

            map.setOnMapReverseGeocodingListener(object : BharatMapsMap.MapReverseGeocodingListener {
                override fun onReverseGeocoding(result: BharatMapsReverseGeocodingResult) {
                    Log.d("BHARAT_LOG", Gson().toJson(result))
                    showReverseGeocodingAnnotation(result)


                    result.nearbyPois.forEachIndexed { i, poi ->
                        Log.d("BHARAT_LOG", "POI Location ${poi.location}")
                        Log.d("BHARAT_LOG", "POI ${Gson().toJson(poi)}")
                    }

                    result.nearbyBuildingNumbers.forEachIndexed { i, b ->
                        Log.d("BHARAT_LOG", Gson().toJson(b))
                    }
                }

                override fun onReverseGeocodingError(message: String, location: Point) {
                    Log.e("BHARAT_LOG", "REVERSE ERROR: $message at ${location.latitude()},${location.longitude()}")
                }
            })


            bharatMap?.bindNavigationVisibility(binding.navigationIndicator)
        }

        binding.btnMyLocation.setOnClickListener {
            if (bharatMap?.centerOnUserLocation(16.0) != true) {
                binding.tvStatus.text = getString(R.string.status_location_missing)
                Toast.makeText(this, R.string.status_location_missing, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStartNavigation.setOnClickListener {
            startSelectedNavigation()
        }

        binding.btnStopNavigation.setOnClickListener {
            stopEmbeddedNavigation()
        }

        binding.btnRequestRoutes.setOnClickListener {
            requestRouteOptions()
        }


        /*
        binding.btnToggleUpin.setOnClickListener {
            isUpinEnabled = !isUpinEnabled
            bharatMap?.setUPinLayerEnabled(isUpinEnabled)
            binding.tvStatus.text = if (isUpinEnabled) "U-Pin layer enabled" else "U-Pin layer disabled"
        }

        binding.btnSimulateOffRoute.setOnClickListener {
            val applied = bharatMap?.simulateOffRouteDeviation(180.0) ?: false
            if (applied) {
                binding.tvStatus.text = "Simulated off-route deviation"
            } else {
                Toast.makeText(this, "Start simulated navigation first", Toast.LENGTH_SHORT).show()
            }
        }
        */
    }

    private fun requestRouteOptions() {
        val location = bharatMap?.currentUserLocation()
        val origin = if (location != null) {
            Point.fromLngLat(location.longitude, location.latitude)
        } else {
            defaultOrigin
        }

        binding.tvStatus.text = getString(R.string.status_route_request)

        bharatMap?.requestRouteOptions(origin, defaultDestination) { options, error ->
            runOnUiThread {
                when {
                    error != null -> {
                        binding.tvStatus.text = getString(R.string.status_route_fail)
                        Toast.makeText(this@MainActivity, "${getString(R.string.status_route_fail)}: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                    options.isNullOrEmpty() -> {
                        binding.tvStatus.text = getString(R.string.status_route_fail)
                        Toast.makeText(this@MainActivity, getString(R.string.status_route_fail), Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        binding.tvStatus.text = getString(R.string.status_routes_preview_ready)
                    }
                }
            }
        }
    }

    private fun startSelectedNavigation() {
        bharatMap?.startSelectedNavigation(true) { error ->
            runOnUiThread {
                if (error != null) {
                    binding.tvStatus.text = getString(R.string.status_route_fail)
                    Toast.makeText(this@MainActivity, error.message ?: getString(R.string.status_route_fail), Toast.LENGTH_LONG).show()
                } else {
                    binding.tvStatus.text = getString(R.string.status_route_ok)
                }
            }
        } ?: run {
            binding.tvStatus.text = "BharatMap is not initialized"
        }
    }

    private fun stopEmbeddedNavigation() {
        bharatMap?.clearRoutePreview()
        bharatMap?.stopNavigation()
        bharatMap?.centerOnUserLocation(16.0)
        binding.tvStatus.text = getString(R.string.status_navigation_stopped)
    }

    private fun showReverseGeocodingAnnotation(result: BharatMapsReverseGeocodingResult) {
        val map = bharatMap ?: return
        val point = result.location
        val coordsText = String.format(
            Locale.US,
            "%.6f, %.6f",
            point.latitude(),
            point.longitude()
        )
        val nameText = result.name?.takeIf { it.isNotBlank() } ?: "—"
        val addressText = result.address?.takeIf { it.isNotBlank() } ?: "—"

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addView(createAnnotationLine(coordsText, true))
            addView(createAnnotationLine(nameText, false))
            addView(createAnnotationLine(addressText, false))
            addView(createAnnotationCloseButton())
        }

        val options = BharatMapsAdvancedAnnotationOptions.builder()
            .backgroundColor(Color.WHITE)
            .borderColor(Color.parseColor("#D9D9D9"))
            .borderWidthDp(1f)
            .cornerRadiusDp(12f)
            .tailWidthDp(22f)
            .tailHeightDp(16f)
            .contentPaddingDp(12f)
            .maxWidth(300f)
            .closeClickOutside(true)
            .build()

        map.addAdvancedAnnotation(
            reverseGeocodingAnnotationId,
            point,
            content,
            options
        )
    }

    private fun createAnnotationLine(text: String, isPrimary: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setTextColor(Color.parseColor("#1E1E1E"))
            isSingleLine = false
            setHorizontallyScrolling(false)
            maxLines = Int.MAX_VALUE
            ellipsize = null
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                if (isPrimary) 13f else 12f
            )
        }
    }

    private fun createAnnotationCloseButton(): Button {
        return Button(this).apply {
            text = "CLOSE"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#4C7DF6"))
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dpToPx(8f)
            }
            setOnClickListener {
                bharatMap?.removeAdvancedAnnotation(reverseGeocodingAnnotationId)
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }
}
