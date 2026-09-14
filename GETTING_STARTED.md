# BharatMaps Android SDK: Quick Start

## SDK naming and resources

Use `BharatMapView` and `BharatMapsMap` from `com.bharatmaps.android.maps`.
Navigation implementation types use the `com.bharatmaps.navigation` namespace;
applications normally need only the high-level map/navigation API. Optional
annotation helpers use `com.bharatmaps.android.plugins.annotation`.

The high-level map/navigation API is unchanged by the branding cleanup. Clients
using low-level types should rebuild against the renamed `BharatMapsNavigation`
and `BharatMapsNavigationOptions` types. Required third-party license notices and
external data formats remain intact.

## 1) Install via Maven

Repository URL:

```text
https://bharatmap.github.io/android-sdk/
```

Add repository and dependency:

```gradle
repositories {
    maven { url "https://bharatmap.github.io/android-sdk/" }
}

dependencies {
    implementation "com.bharatmaps:bharatmaps-android:1.0.34"
}
```

## 2) Initialize SDK

Initialize once in Activity/Fragment before map usage:

```kotlin
BharatMaps.getInstance(this)
```

## 3) Add map view in XML

Use `BharatMapView` (not raw `MapView`):

```xml
<com.bharatmaps.android.maps.BharatMapView
    android:id="@+id/mapView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

`BharatMapView` handles map lifecycle automatically.
You do not need to call `mapView.onStart()/onStop()/onDestroy()` manually.

## 4) Get map instance

```kotlin
mapView.getMapAsync { map ->
    // map is BharatMapsMap
}
```


## 4A) Built-in map styles

Supported enum values:
- `BharatMapStyle.LIGHT`
- `BharatMapStyle.DARK`
- `BharatMapStyle.LIGHT_SIMPLIFIED`
- `BharatMapStyle.DARK_SIMPLIFIED`

Initialize map view with style:

```kotlin
val mapView = BharatMapView(this, BharatMapStyle.DARK)
```

Switch style at runtime:

```kotlin
mapView.setMapStyle(BharatMapStyle.LIGHT_SIMPLIFIED)

mapView.getMapAsync { map ->
    map.setMapStyle(BharatMapStyle.DARK)
}
```

## 4B) Accent color

`accentColor` is optional. When set, SDK uses it for route lines, user puck and accuracy ring.

XML:

```xml
<com.bharatmaps.android.maps.BharatMapView
    android:id="@+id/mapView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:bharatmaps_accentColor="#FF6B00" />
```

Kotlin:

```kotlin
mapView.setAccentColor(Color.parseColor("#FF6B00"))

mapView.getMapAsync { map ->
    map.setAccentColor(Color.parseColor("#FF6B00"))
}
```

## 5) Validate SDK API key (required)

Call once on app start before map-heavy flows:

```kotlin
map.validateLicense("BMK_TEST_xxx") { result, error ->
    if (error != null) {
        Log.e("BHARAT_LOG", "License validation failed", error)
        return@validateLicense
    }
    Log.d("BHARAT_LOG", "License validated")
}
```

Notes:
- SDK sends `platform=android`
- `appId` is auto-filled from app package name
- validation endpoint: `https://portal.bharat-maps.com/sdk/v1/license/validate`
- until validation succeeds, SDK keeps map interactions and navigation/location APIs locked
- Validation performs network I/O off the main thread. Authorization state and map
  visibility/interactions are applied on the main thread before the callback,
  including when no callback is supplied. `isLicenseValidated()` reflects the
  applied result when the callback runs.
- A newer validation request supersedes older requests. Superseded requests and
  responses arriving after map destruction report `CancellationException` on the
  main thread without changing the map. A background/foreground transition alone
  does not cancel validation; destroying the map does.
- The SDK reads the installed APK signing certificate automatically. The optional
  signing-hash overload must match that certificate (SHA-256, case-insensitive,
  colon separators accepted). Prefer the overload without a manually supplied hash:

```kotlin
map.validateLicense("BMK_TEST_xxx", "ABCD...SHA256") { result, error -> /* ... */ }
```

### Signed offline authorization (A04, 1.0.30)

Use the same `validateLicense(apiKey)` on every app launch. No new initialization,
permissions or application cache is required. A first successful online validation
with the updated portal returns a signed permission valid for at most **24 hours**,
shortened by API-key or subscription expiry. It survives process death and restart.

If the network is unavailable, or the portal returns HTTP 429/5xx, the SDK can restore
that permission for the **same key, installed package and APK signing certificate**.
It never extends the signed expiry offline. A fresh install without a saved permission
must connect once. TLS failures, explicit rejection, malformed responses, invalid
signatures, expired permission, mismatched identity or corrupted storage fail closed.
An explicit rejection removes the saved permission; going offline cannot undo it.

`LicenseValidationResult.validationSource` is `"online"` or `"offline"`.
`offlineAvailable` indicates that a signed permission was successfully persisted.
`expiresAt` is ISO-8601 UTC and `ttlSeconds` is remaining signed lifetime when a
signed permission is used. An old server with no offline envelope remains online-only.

```kotlin
map.validateLicense(apiKey) { result, error ->
    if (error != null || result == null) return@validateLicense
    val restoredOffline = result.validationSource == "offline"
    val canRestartOffline = result.isOfflineAvailable
    // Continue with map setup. Never persist a local "validated" boolean.
}
```

The stored envelope is authenticated/encrypted using an app-scoped Android Keystore
key, excluded from Android backup, and verified against the SDK's pinned P-256 public
key on restore. No raw API key is stored. APK certificate rotation or changing the
package/key requires online validation again. Expiry is checked with both wall and
elapsed time while running; stored time high-water checks reject observed rollback.
These checks do not claim to resist a rooted/modified app or all offline clock attacks.
Certificate binding is local permit validation, not remote device attestation.

Callbacks and visibility changes remain on main, including null-callback validation.
The map is gated when signed authorization expires, including after resume.
Late/cancelled validation cannot overwrite a newer decision. Authorization is shared
within the process: a denial/identity change invalidates existing grants; successful
validation with the same identity does not move the camera.

Offline authorization does not download tiles or make REST search/routing available
without a network. Revocation is learned at the next successful server contact, or
access expires at the signed deadline, whichever happens first.

---

## Map tap entities (POI / Building Number / U-Pin)

SDK can resolve feature under tap and return typed models:
- `BharatMapsPoiResult` for `poi` and `building_name`
- `BharatMapsBuildingNumberResult` for `house_number`
- `BharatMapsUPinResult` for `u_pin`

```kotlin
map.setOnMapEntityClickListener(object : BharatMapsMap.MapEntityClickListener {
    override fun onPoiClick(poi: BharatMapsPoiResult) {
        // poi.location is Point
        // poi.category / poi.subcategory are resolved human-readable labels
        // poi.iconResId can be used directly in ImageView
    }

    override fun onBuildingNumberClick(building: BharatMapsBuildingNumberResult) {
        // building.buildingNumber, building.address, building.location
    }

    override fun onUPinClick(upin: BharatMapsUPinResult) {
        // upin.uPin, upin.address, upin.location
    }
})
```

Remove listener:

```kotlin
map.removeOnMapEntityClickListener()
```

Notes:
- tap priority: `markers_layer` -> `house_number` -> `house_name/building_names` -> `u_pin` -> `poi`
- cluster taps are ignored
- POI result does not expose internal ids/layer fields (`id`, `gid`, `category_id`, `layer`)

---

## Advanced Annotations (Bubble + Tail)

You can add fully custom map-bound views (including inflated XML layouts).

```kotlin
val content = layoutInflater.inflate(R.layout.my_custom_bubble, null)

val options = BharatMapsAdvancedAnnotationOptions.builder()
    .backgroundColor(Color.WHITE)
    .borderColor(Color.parseColor("#D9D9D9"))
    .borderWidthDp(1f)
    .cornerRadiusDp(12f)
    .tailWidthDp(21f)
    .tailHeightDp(12f)
    .tailOffsetXDp(0f)
    .contentPaddingDp(12f)
    .maxWidth(280f)
    .closeClickOutside(true)
    .persistent(false)
    .build()

map.addAdvancedAnnotation(
    id = "poi_bubble_1",
    point = Point.fromLngLat(77.2295, 28.6129),
    content = content,
    options = options
)
```

Update/remove:

```kotlin
map.updateAdvancedAnnotation("poi_bubble_1", Point.fromLngLat(77.2300, 28.6132))
map.updateAdvancedAnnotationOptions("poi_bubble_1", BharatMapsAdvancedAnnotationOptions.builder().cornerRadiusDp(20f).build())
map.removeAdvancedAnnotation("poi_bubble_1")
map.clearAdvancedAnnotations()
```

Notes:
- Annotation body auto-sizes to content (`wrap_content`)
- If `maxWidth(...)` is not set, SDK applies a safe default max width based on screen size
- Tail always points to the bound geo point
- Position updates automatically on camera movement/idle
- `persistent(true)` keeps annotation bound to geo-point even when it goes beyond screen edges (no edge clamping / tail shift)
- Any Android `View` is supported as content (including custom `LinearLayout` from XML)

---

## Dynamic Custom Markers

Use custom markers for moving objects such as taxis, couriers, drivers, or tracked assets.

```kotlin
val carId = "car_42"
val start = Point.fromLngLat(77.2295, 28.6129)

map.addCustomMarker(
    carId,
    start,
    R.drawable.ic_car_marker,
    90.0 // bearing in degrees
)
```

Update position instantly:

```kotlin
map.updateCustomMarker(
    carId,
    Point.fromLngLat(77.2310, 28.6138),
    120.0 // bearing in degrees
)
```

Update position with animation:

```kotlin
map.updateCustomMarker(
    carId,
    Point.fromLngLat(77.2320, 28.6144),
    135.0,
    true,
    1000L
)
```

The same API is also available directly on `BharatMapView`:

```kotlin
mapView.addCustomMarker(carId, start, R.drawable.ic_car_marker, 90.0)
mapView.updateCustomMarker(carId, Point.fromLngLat(77.2320, 28.6144), 135.0, true, 1000L)
```

When the vehicle must follow backend-owned route geometry instead of moving in a straight line between polling updates:

```kotlin
map.updateCustomMarkerAlongRoute(
    "assigned_driver",
    "assigned_driver_route",
    driverPoint,
    true,
    1000L
)
```

To move the marker along the route and update the vanishing route line in one call:

```kotlin
map.updateCustomMarkerAlongRouteAndProgress(
    "assigned_driver",
    "assigned_driver_route",
    driverPoint,
    true,
    1000L
)
```

`updateCustomMarkerAlongRouteAndProgress` synchronizes marker animation and vanishing route line frame-by-frame: the remaining route starts exactly at the currently rendered marker position during the animation. `updateRoutePolylineProgress(...)` remains an instant/manual trim API.

Remove markers:

```kotlin
map.removeCustomMarker(carId)
map.clearCustomMarkers()
```

Notes:
- marker ids are stable; calling `addCustomMarker` again with the same id replaces the old marker
- bearing is clockwise degrees where `0` keeps the original image orientation
- animated updates use linear interpolation between old and new coordinates
- `updateCustomMarkerAlongRoute` uses geometry previously set by `showRoutePolyline(... routeId)`, projects current driver location to the route, and animates by route segments instead of a direct line
- for live vehicle tracking, push every new GPS fix through `updateCustomMarker(..., animated = true, durationMs = updateIntervalMs)`

---

## Map long press reverse geocoding

SDK can resolve reverse geocoding data on map long press and also return nearby typed entities.

```kotlin
map.setOnMapReverseGeocodingListener(object : BharatMapsMap.MapReverseGeocodingListener {
    override fun onReverseGeocoding(result: BharatMapsReverseGeocodingResult) {
        // result.location
        // result.name / result.address / result.wardNumber / result.policeStationPhone
        // result.nearestAddressDistanceMeters
        // result.nearbyPois / result.nearbyBuildingNumbers
    }

    override fun onReverseGeocodingError(message: String, location: Point) {
        // request failed
    }
})
```

Remove listener:

```kotlin
map.removeOnMapReverseGeocodingListener()
```

Notes:
- callback is delivered on main thread
- reverse request uses long press coordinates
- nearby entities contain only POI + building number models

---

## User Location API

### Enable user location

```kotlin
map.enableUserLocation { enabled ->
    if (enabled) {
        // location component enabled
    } else {
        // permission denied or location could not be enabled
    }
}
```

Permissions are requested by SDK internally (for Activity context).

Behavior:
- camera starts in follow mode (`TRACKING`) after enable
- follow stops when user manually pans the map
- `centerOnUserLocation(...)` re-enables follow mode

### Instant follow transition

For an instant follow transition when a user location is already available,
the public location component also supports duration zero (fixed in Android 1.0.34):

```kotlin
val component = map.locationComponent
component.setCameraMode(CameraMode.NONE)
component.setCameraMode(CameraMode.TRACKING, 0L, 16.0, 0.0, 0.0,
    object : OnLocationCameraTransitionListener {
        override fun onLocationCameraTransitionFinished(cameraMode: Int) {
            // Camera reached the location; subsequent GPS updates continue following.
        }
        override fun onLocationCameraTransitionCanceled(cameraMode: Int) {
            // An unfinished transition was cancelled.
        }
    })
```

Use `com.bharatmaps.android.location.modes.CameraMode` and
`com.bharatmaps.android.location.OnLocationCameraTransitionListener`.
The instant transition preserves viewport padding and completes once on main,
without waiting for an animated native event. The tracking transition state is
cleared before its finish callback. Existing nonzero transitions and
`centerOnUserLocation` retain their animated behavior. No app-owned camera
controller or extra camera update is required.

### Disable user location

```kotlin
map.disableUserLocation()
```

### Get current user location

```kotlin
val location = map.currentUserLocation()
```

### Center camera on user

```kotlin
val centered = map.centerOnUserLocation()      // default zoom = 14
val centeredAt16 = map.centerOnUserLocation(16.0)
```

### Custom pulse ring

```kotlin
// visual-only ring, not tied to GPS accuracy. Radius is dp.
map.startUserLocationPulseRing(Color.parseColor("#4C7DF6"), 44f)

// stop pulse ring
map.stopUserLocationPulseRing()
```

### Custom user puck image

Use this when the user puck must represent a driver vehicle state, for example bike, auto, or cab.
The SDK still uses the native location component, not a custom marker workaround.

```kotlin
// Drawable resource, rendered at explicit pixel size.
mapView.setUserLocationPuckImage(R.drawable.map_icon_bike, 70, 70)

// Rotate puck by movement bearing.
mapView.setUserLocationPuckBearingEnabled(true)

// Explicit bearing behavior.
mapView.setUserLocationPuckBearingMode(BharatMapsUserLocationPuckBearingMode.COURSE_THEN_HEADING)

// Same API is available on BharatMapsMap.
map.setUserLocationPuckImage(R.drawable.map_icon_cab, 70, 70)
map.setUserLocationPuckBearingMode(BharatMapsUserLocationPuckBearingMode.NAVIGATION_ROUTE)

// Restore default SDK puck.
mapView.setUserLocationPuckImage(null)
```

Bitmap overload:

```kotlin
mapView.setUserLocationPuckImage(vehicleBitmap, 70, 70)
```

Bearing modes:

- `NONE`: custom puck is not rotated.
- `COURSE_ONLY`: custom puck rotates only by movement bearing/course; compass heading is ignored.
- `COURSE_THEN_HEADING`: backward-compatible behavior used by `setUserLocationPuckBearingEnabled(true)`.
- `NAVIGATION_ROUTE`: recommended for driver navigation. During active navigation SDK uses navigation/GPS route direction and does not use compass fallback.

### Default zoom/location

Set defaults on `BharatMapView`:

```kotlin
mapView.setDefaultZoom(16.0)
mapView.setDefaultLocation(Point.fromLngLat(77.2385, 28.6259))
```

Or pass in constructor:

```kotlin
val mapView = BharatMapView(
    this,
    BharatMapStyle.LIGHT,
    Point.fromLngLat(77.2385, 28.6259),
    16.0
)
```

If location is unavailable, method returns `false`.

Behavior outside navigation mode:
- camera bearing and pitch are reset to `0`
- camera follow/tracking mode is re-enabled (useful after user manually panned map)

### Fit camera to coordinates

Use this when you need to show multiple points on screen at once. Padding is in pixels:

```kotlin
val points = listOf(
    Point.fromLngLat(77.22958, 28.61290),
    Point.fromLngLat(77.23850, 28.62590)
)

map.fitCameraToCoordinates(
    points,
    32,   // left
    120,  // top
    32,   // right
    220,  // bottom
    true, // animated
    350   // durationMs
)

// same via BharatMapView facade
mapView.fitCameraToCoordinates(points, 32, 120, 32, 220, true, 350)
```

For assigned-driver screens, clamp the maximum zoom so driver and user do not get framed too close:

```kotlin
map.fitCameraToCoordinates(
    listOf(userPoint, driverPoint),
    40,   // left
    138,  // top
    40,   // right
    40,   // bottom
    true,
    350,
    18.0  // maxZoom
)
```

Camera behavior:
- `enableUserLocation()` starts user follow when location is available.
- `centerOnUserLocation(...)` and `recenterCamera()` restore user follow.
- app-owned camera methods (`moveCameraTo`, `animateCameraTo`, `easeCameraTo`, `fitCameraToCoordinates`) stop user follow but keep the user puck visible.
- `requestRoutePreview(..., autoFit=false)` stays draw-only and does not change camera/follow state.

User camera gestures:

```kotlin
mapView.setCameraInteractionListener(object : BharatMapCameraInteractionListener {
    override fun onUserCameraGestureStarted(reason: BharatMapCameraInteractionReason) {
        pauseAutoFitForUserControl()
    }

    override fun onUserCameraGestureEnded(reason: BharatMapCameraInteractionReason) {
        // Optional: keep auto-fit paused until your own timeout expires.
    }
})
```

This listener is user-only. Programmatic camera calls such as `fitCameraToCoordinates`, `centerOnUserLocation`, `recenterCamera`, and route preview camera fitting do not trigger it.

### Map padding (instant + animated)

Set map viewport padding instantly (pixels). This affects the camera viewport, not the BharatMaps logo:

```kotlin
// via BharatMapsMap
map.setMapPadding(leftPx, topPx, rightPx, bottomPx)

// or via BharatMapView facade
mapView.setMapPadding(leftPx, topPx, rightPx, bottomPx)
```

Animate map viewport padding (pixels):

```kotlin
// via BharatMapsMap
map.animateMapPadding(leftPx, topPx, rightPx, bottomPx, durationMs = 350)

// or via BharatMapView facade
mapView.animateMapPadding(leftPx, topPx, rightPx, bottomPx, durationMs = 350)
```

### Atomic camera and padding transition (Android 1.0.33+)

When changing center/zoom/bearing/pitch and viewport padding together, use one
transition instead of calling `animateMapPadding` and `easeCameraTo` separately.
Separate camera animations replace each other; they do not compose.

Call on main after `getMapAsync` supplies `BharatMapsMap`:

```kotlin
val options = BharatCameraTransitionOptions.Builder()
    .center(Point.fromLngLat(77.24, 28.63))
    .zoom(14.0)
    .bearing(25.0)
    .pitch(30.0)
    .padding(80, 80, 80, 300) // left, top, right, bottom in px
    .build()

map.transitionCamera(options, 1000, object : BharatMapsMap.CancelableCallback {
    override fun onFinish() {
        // All requested camera and viewport targets have been reached.
    }
    override fun onCancel() {
        // Replaced, explicitly cancelled, interrupted by a gesture, or map stopped/destroyed.
    }
})

// Optional explicit cancellation:
// map.cancelTransitions()
```

All target fields are optional; omitted fields keep their current values.
Options are an immutable snapshot. Padding must be nonnegative; numeric targets
must be finite. Duration <= 0 applies immediately. Native smooth easing is the
default; `.easing(false)` selects linear interpolation. Custom app interpolators
are not supported by this API.

The callback is optional. It receives exactly one terminal event on main:
`onFinish` or `onCancel`, never both. A replacement starts from the currently
rendered camera. Map stop/destruction cancels an unfinished transition.
This is an app-owned camera operation: in normal map mode it stops follow without
hiding the user puck. `centerOnUserLocation` restores follow. Attribution/logo
margins remain independent. Existing camera methods and route preview behavior
are unchanged.

### Logo margins

Set BharatMaps logo margins separately from map padding (pixels):

```kotlin
map.setLogoMargins(leftPx, topPx, rightPx, bottomPx)
mapView.setLogoMargins(leftPx, topPx, rightPx, bottomPx)
```

For bottom-sheet layouts, use attribution insets to position BharatMaps logo/attribution independently from camera padding:

```kotlin
mapView.setMapPadding(0, 64, 0, sheetHeightPx)
mapView.setAttributionInsets(0, 0, 0, safeAreaBottomPx + 8)

// Same API is available on BharatMapsMap.
map.setAttributionInsets(0, 0, 0, safeAreaBottomPx + 8)
```

### Navigation camera viewport

`setMapPadding(...)` is general camera/user-location viewport padding. `setAttributionInsets(...)` only positions Bharat Maps logo/attribution. Navigation has its own viewport so active navigation follow camera is not overwritten by bottom-sheet hacks:

```kotlin
val density = resources.displayMetrics.density
val topPx = (80 * density).toInt()
val extraBottomPx = (24 * density).toInt()

mapView.setNavigationCameraViewport(
    0,
    topPx,
    0,
    sheetHeightPx + extraBottomPx
)
```

Recommended taxi-driver setup:

```kotlin
val density = resources.displayMetrics.density
val topPx = (80 * density).toInt()
val bottomPx = sheetHeightPx + (24 * density).toInt()
val attributionBottomPx = safeAreaBottomPx + (8 * density).toInt()

mapView.setUserLocationPuckImage(R.drawable.map_icon_cab, 70, 70)
mapView.setUserLocationPuckBearingMode(BharatMapsUserLocationPuckBearingMode.NAVIGATION_ROUTE)
mapView.setNavigationCameraViewport(0, topPx, 0, bottomPx)
mapView.setAttributionInsets(0, 0, 0, attributionBottomPx)
mapView.enableUserLocation()
```

When bottom-sheet height changes, call `setNavigationCameraViewport(...)` again. Active navigation follow/recenter uses the latest viewport.

### Configure default zoom for center/recenter

```kotlin
map.setDefaultUserLocationZoom(15.5)
val zoom = map.getDefaultUserLocationZoom()
```

### Recenter (mode-aware)

```kotlin
map.recenterCamera()
```

Behavior:
- normal map mode: same as `centerOnUserLocation(defaultZoom)`
- active navigation mode: restores navigation camera tracking

### Ready UI button: BharatMapsLocationButton

```xml
<com.bharatmaps.android.ui.BharatMapsLocationButton
    android:layout_width="52dp"
    android:layout_height="52dp"
    app:bharatMapViewId="@id/mapView" />
```

Optional customization attributes:
- `app:cornerRadius`
- `app:iconColor`
- `app:icon`
- `app:iconNavigation`

### Ready UI control: BharatMapsZoomControl

```xml
<com.bharatmaps.android.ui.BharatMapsZoomControl
    android:layout_width="52dp"
    android:layout_height="120dp"
    app:bharatMapViewId="@id/mapView"
    app:orientation="vertical" />
```

Behavior:
- `+` is disabled at max zoom
- `-` is disabled at min zoom
- disabled icon alpha = `0.5`
- state updates during gestures (pinch/pan) and on camera idle

Optional customization attributes:
- `app:cornerRadius`
- `app:iconColor`
- `app:iconPlus`
- `app:iconMinus`
- `app:backgroundColor` (button background)
- `app:orientation` (`horizontal` or `vertical`)

## Navigation-bound visibility binding

You can bind any `View` visibility to navigation state.

```kotlin
// Visible only while navigation is active (old/default behavior)
map.bindNavigationVisibility(myNavOnlyView)

// Visible only while navigation is NOT active
map.bindNavigationVisibility(myMapOnlyView, visibleWhenNavigationActive = false)

// Unbind when no longer needed
map.unbindNavigationVisibility(myNavOnlyView)
map.unbindNavigationVisibility(myMapOnlyView)
```

---

## Camera API (high-level)

All camera helpers below use `Point` from GeoJSON:

```kotlin
val point = Point.fromLngLat(77.22958, 28.61290)
```

### Instant move

```kotlin
map.moveCameraTo(point, 14.0)
map.moveCameraTo(point, 14.0, bearing = 120.0, pitch = 40.0)
```

### Smooth ease

```kotlin
map.easeCameraTo(point, 14.0, durationMs = 1200)
map.easeCameraTo(point, 14.0, bearing = 120.0, pitch = 40.0, durationMs = 1200)
```

### Animated transition

```kotlin
map.animateCameraTo(point, 14.0)
map.animateCameraTo(point, 14.0, bearing = 120.0, pitch = 40.0)
```

Low-level `CameraUpdateFactory`/`CameraUpdate` API is intentionally hidden from Kotlin client code.

---

## Embedded Navigation (high-level)

Navigation runs on the same `BharatMapView` (no separate navigation Activity).

### Ready route selector UI

SDK includes `BharatMapsRouteSelector` (`LinearLayout`) that renders route-choice buttons automatically.

```xml
<com.bharatmaps.android.ui.BharatMapsRouteSelector
    android:id="@+id/routeSelector"
    android:layout_width="match_parent"
    android:layout_height="72dp"
    app:bharatMapViewId="@id/mapView" />
```

If `bharatMapViewId` is set, selector auto-attaches to map and updates itself on:
- `requestRouteOptions(...)` result
- route tap on map
- programmatic `selectRouteOption(...)`
- start navigation (selector is cleared)

When no routes are available, selector visibility is `INVISIBLE`.

Programmatic attach:

```kotlin
routeSelector.attachTo(mapView) // or routeSelector.attachToMap(map)
```

Route selector customization attributes:
- `app:buttonColor` default `#F1F1F1`
- `app:buttonColor_active` default `#4C7DF6`
- `app:textColor` default black
- `app:textColor_active` default white
- `app:fontFamily` default system
- `app:textSize` default `15sp`
- `app:buttonPadding` default `12dp`
- `app:borderRadius` default `12dp`
- `app:gap` default `12dp`

Button content:
- line 1: duration
- line 2: distance (`70%` of duration font size, alpha `0.8`)
- duration format:
  - `< 1h` -> `X min`
  - `>= 1h` -> `Xh YYm`

Single-route behavior:
- style is rendered as text-like tile, not active-button style
- uses selector background and `textColor`
- does not apply `buttonColor_active` / `textColor_active`

### Maneuver sign UI

SDK includes `BharatMapsManeuverSign` (`LinearLayout`) that renders upcoming maneuver icon automatically.

```xml
<com.bharatmaps.android.ui.BharatMapsManeuverSign
    android:id="@+id/maneuverSign"
    android:layout_width="72dp"
    android:layout_height="72dp"
    android:orientation="vertical"
    app:bharatMapViewId="@id/mapView" />
```

When no maneuver is available, visibility is `INVISIBLE`.

Customization:
- `android:orientation` (`horizontal` / `vertical`)
- `app:iconColor` default `#4C7DF6`
- `app:backgroundColor` default white
- standard paddings and layout params

### Navigation labels UI

Ready `TextView` subclasses:
- `BharatMapsManeuverDistanceLabel`
- `BharatMapsTripDistanceLabel`
- `BharatMapsTripTimeRemainingLabel`
- `BharatMapsTripTimeArrivalLabel`
- `BharatMapsNextRoadNameLabel`
- `BharatMapsSpeedLabel`
- `BharatMapsTripEndView`

`BharatMapsNextRoadNameLabel` behavior:
- hidden when no data
- one name -> show one
- equal names -> show one
- different names -> `Current  ➜  Next`

`BharatMapsTripTimeRemainingLabel` behavior:
- `< 1h` -> `X min`
- `>= 1h` -> `Xh YYm`

### Minimal flow (recommended)

```kotlin
map.requestRouteOptions(origin, destination) { options, error ->
    if (error != null || options.isNullOrEmpty()) return@requestRouteOptions
}
```

```kotlin
map.selectRouteOption(routeId)
map.startSelectedNavigation(true) { error -> /* simulation=true */ }
```

### Controlled route preview

Use this for non-navigation previews, for example assigned driver route between driver and pickup.

```kotlin
map.requestRoutePreview(origin, destination, false, false) { options, error ->
    if (error != null || options.isNullOrEmpty()) return@requestRoutePreview
    map.selectRouteOption(options.first().id)
}
```

`alternatives=false` requests and draws only the primary route. Preview route layers are placed below SDK annotation/user-location layers so custom driver markers and the user puck stay visible above the line.

`autoFit=true` means SDK draws the route, may disable user follow for preview mode, and may fit the camera to the selected preview route.

`autoFit=false` means SDK draws the route only. Camera position, user tracking mode, follow state, map padding and content inset remain app-owned and unchanged.

### Backend-owned route geometry

Use this when your backend already returns OSRM-like route geometry and the app must draw exactly that route without SDK route requests, alternatives, camera fit, or follow/tracking changes.

```kotlin
map.showRoutePolyline(
    trip.assignedDriverRouteGeometry,
    Color.rgb(76, 125, 246),
    5f,
    "assigned_driver"
)
```

Use the overload with `precision` when the backend returns a precision other than polyline6.

```kotlin
map.clearRoutePolyline("assigned_driver")
map.clearRoutePolyline(null) // clears all app-owned route polylines
```

To make the already travelled part of the backend-owned route disappear as the driver moves:

```kotlin
map.updateRoutePolylineProgress("assigned_driver", driverPoint)
```

You can also trim by explicit progress:

```kotlin
map.updateRoutePolylineProgress("assigned_driver", 0.45)
```

`progress` is clamped to `0.0..1.0`. `0.0` shows the full line, `1.0` hides it. Reusing `showRoutePolyline` with the same `routeId` replaces the original geometry and resets progress to `0.0`.

This API only draws/removes app-owned route layers. It does not call `requestRoutePreview`, does not move camera, does not change follow/tracking, and does not hide the user puck. Reusing the same `routeId` updates the existing line.

`RouteOption` also exposes `encodedPolyline`, `polylinePrecision`, `distanceMeters`, and `durationSeconds` for apps that still use SDK route preview responses.

### Direct start APIs

```kotlin
map.startNavigation(origin, destination) { route, error -> }
map.startNavigation(origin, destination, true) { route, error -> } // simulation=true
```

To make the SDK-owned active navigation route vanish behind the navigation puck, enable it before starting navigation:

```kotlin
map.setNavigationRouteVanishingEnabled(true)

map.startNavigation(origin, destination) { route, error ->
    if (error != null) return@startNavigation
}
```

`setNavigationRouteVanishingEnabled(false)` is the default for backward compatibility. This affects only the SDK-owned active navigation route. It does not disable app-owned route polylines, route progress callbacks, maneuver callbacks, custom user puck, or navigation camera.

For taxi/driver apps with multiple simulated legs, use simulation options:

```kotlin
val options = BharatNavigationSimulationOptions(
    true,  // enabled
    true,  // holdAtDestination
    false, // autoStopOnArrival
    1.0    // speedMultiplier
)

map.startNavigation(driverPoint, pickupPoint, options) { route, error ->
    if (error != null) return@startNavigation
}
```

Trip-end calibration line is enabled by default. Disable it when the app wants to keep the simulated puck at the destination without showing the red-dot calibration overlay:

```kotlin
map.setNavigationCalibrationLineEnabled(false)
val enabled = map.isNavigationCalibrationLineEnabled()
```

This only disables trip-end calibration dots/end marker/follow camera overlay. It does not disable route line, user puck, navigation progress callbacks, `currentNavigationLocation()`, or `currentSimulatedNavigationLocation()`.

When the driver reaches pickup, use the held simulated puck location as the next origin:

```kotlin
val held = map.currentNavigationLocation()
val nextOrigin = if (held != null) {
    Point.fromLngLat(held.longitude, held.latitude)
} else {
    pickupPoint
}

map.startNavigation(nextOrigin, destinationPoint, options) { route, error ->
    if (error != null) return@startNavigation
}
```

`holdAtDestination=true` keeps the native user-location puck at the last simulated coordinate. `autoStopOnArrival=false` prevents SDK from auto-stopping navigation at arrival. `stopNavigation()` keeps backward-compatible behavior and resets to real/system location. Use `stopNavigation(false)` only when you need to close guidance while keeping the puck on the held simulated location.

### Stop navigation

After arrival, `stopNavigation()` followed immediately by `startNavigation(route, options)`
starts a fresh session, even when reusing the same supplied `DirectionsRoute` object.
Simulation replays from the first leg. Queued progress, milestone and running-state
events from the stopped session cannot complete the new session. No delay or map
recreation is required (fixed in 1.0.32).

```kotlin
map.stopNavigation()
```

### Auto reroute and simulation

Auto reroute works during active navigation.

For simulation tests:

```kotlin
map.simulateOffRouteDeviation()
map.simulateOffRouteDeviation(180.0)
```

### Trip progress listener

```kotlin
map.setTripProgressChangedListener { progress ->
    val distanceToManeuver = progress.maneuverDistanceMeters
    val timeToManeuver = progress.maneuverDurationRemainingSeconds
    val tripDistanceLeft = progress.tripDistanceMeters
    val tripTimeLeft = progress.tripDurationRemainingSeconds
    val eta = progress.arrivalTimeMillis
    val speed = progress.speedKmh
    val currentRoad = progress.currentRoadName
    val roadLabel = progress.nextRoadName
    val voiceText = progress.voiceInstructionText
}
```

## Navigation voice and application announcements

Call on the main thread. The mute preference is persisted across maps and process
restarts, and applied before the first navigation instruction. Muting immediately
stops current and queued speech. Instruction/progress callbacks continue.

```kotlin
map.setNavigationVoiceMuted(savedMute)
map.startNavigation(selectedRoute)
map.speakNavigationAnnouncement("Approaching your pickup", false)
// true interrupts and clears the existing queue before adding this announcement.
map.speakNavigationAnnouncement("Pickup changed", true)
map.stopNavigationSpeech() // cancel speech without changing the saved mute setting
map.stopNavigation()       // also clears all current/queued speech
```

Announcements and instructions share the same SDK speaker and serial TTS queue;
no second navigation player is required. `speakNavigationAnnouncement` returns
false if navigation is inactive/arrived, muted, unauthorized or text is empty.
A true result means submitted, not proof that a device has an installed TTS voice.
`isNavigationVoiceMuted()` returns the saved preference. `stopNavigationSpeech()`
does not suppress later instructions; use mute when persistent silence is intended.

## Detailed navigation progress

Existing `TripProgress` fields/constructors remain compatible. SDK-generated values
also contain `details: BharatNavigationProgress`; `map.getNavigationProgress()`
returns the latest snapshot for late subscribers (null before start/after stop).

```kotlin
map.setOnTripProgressChangedListener { progress ->
    val details = progress?.details ?: return@setOnTripProgressChangedListener
    val stepId = details.stepId
    val remaining = details.stepDistanceRemainingMeters
    val traveled = details.tripDistanceTraveledMeters
    val maneuver = details.upcomingStep?.maneuver()
    val geometry = details.upcomingStepCoordinates
    val bridgeText = details.bridgeInstructionText
}
```

`sessionId` changes on a new Start; `routeRevision` increases on route replacement.
`routeId`, `legId`, `stepId` and `upcomingStepId` remain stable within that revision.
Leg/step indices are zero-based. Current/upcoming `LegStep` expose maneuver type,
modifier, location, bearing, names and instruction data; decoded geometries are
available without app-side map matching. Upcoming step crosses a leg boundary.
Totals/remaining/traveled distances are meters, durations seconds. Remaining
waypoints are remaining leg endpoints in order, including the final destination.
`INITIAL` precedes live `ACTIVE` updates; `ARRIVED` has zero remaining distance and
is emitted before legacy progress is cleared. The latest detailed ARRIVED snapshot
remains readable until stop/new start. Bridge text is nullable when no bridge data
is available from the loaded map; the SDK does not invent missing instructions.

## Application reroute provider

Install before Start, on main. `startNavigation(DirectionsRoute)` still starts the
supplied route without a request. When a provider is installed, manual/off-route
rerouting uses only that provider; failure does not silently fall back to the SDK
backend. Without a provider, existing SDK rerouting remains available.

Register the provider and listener before the first `startNavigation` if desired.
The SDK transfers both to the navigation controller when it is first created.
Changing `setNavigationCalibrationLineEnabled` does not replace the provider,
replay listener events, or cancel a pending reroute (fixed in 1.0.31).

```kotlin
map.setNavigationRerouteProvider { request, completion ->
    val fix = request.location // defensive Location copy, including bearing when known
    val progress = request.progress
    val remainingViaAndDestination = progress.remainingWaypoints
    // Your async backend policy owns bearing retries, ranking and candidate selection.
    val call = backend.requestRankedRoute(fix, remainingViaAndDestination) { selected, error ->
        completion.onResult(selected, error)
    }
    BharatNavigationReroute.Cancellation { call.cancel() }
}
map.setOnNavigationRerouteChangedListener { event ->
    // STARTED, APPLIED, FAILED or CANCELLED; requestId and captured progress identify it.
}
map.requestNavigationReroute() // optional manual trigger; false without an active session/fix
map.cancelNavigationReroute()
```

`backend` above is application code, not another SDK navigation player/engine.
Provider invocation and events run on main; perform network I/O asynchronously.
Completion may run on any thread and only its first current result is applied.
Return a cancellation handle (or null for noncancellable work). Cancel/Finish,
arrival, provider replacement, new Start or a newer reroute invalidate pending
responses. Late results cannot change the session. Failed reroutes preserve the
current route. Applied reroutes preserve the session ID, increment route revision
and update the SDK engine, without an application call to Start.

## Live congestion on the active navigation route

```kotlin
val route = map.navigationProgress ?: return
// Exactly one value per adjacent pair in route.routeCoordinates.
val accepted = map.updateNavigationRouteCongestion(levels, route.sessionId, route.routeRevision)
// Optional reset, with the same identity guard:
map.clearNavigationRouteCongestion(route.sessionId, route.routeRevision)
```

Call on main. Levels are `unknown`, `low`, `moderate`, `heavy`, `severe` (case-sensitive).
Colors: low/unknown use route accent; moderate #F9A825, heavy #EF6C00, severe #C62828.
The API returns false without mutation for stale session/revision, wrong count,
invalid levels, inactive/arrived navigation or calls off main. It updates a dedicated
source in place above the route and below its markers; it does not Start, request
routes, reset progress, move camera or interrupt speech. State is reapplied after
style reload and cleared on reroute/new Start/arrival/stop. Vanishing mode clips
congestion to the same projected location as the remaining route. Revision identifies
the route geometry, not the congestion poll; the application should discard older
poll responses for the same route before calling this API.
