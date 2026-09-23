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
    implementation "com.bharatmaps:bharatmaps-android:1.0.58"
}
```

### 16 KB native alignment

Version 1.0.44 explicitly aligns both ELF LOAD segments and the end of GNU_RELRO
to 16 KB. Use AGP 8.5.1 or newer for uncompressed native-library packaging and
verify the final APK with `zipalign -c -P 16 4 app.apk`. AAR alignment does not
replace final APK packaging and runtime tests. Existing public APIs are unchanged.
See [Android page-size requirements](https://developer.android.com/guide/practices/page-sizes#check-relro).

## 2) Initialize SDK

On Android 12+ the default system location engine forwards SDK priority as explicit
native request quality (fixed in 1.0.35). HIGH_ACCURACY requests can activate GPS
even when Android selects its fused provider. Callback/Looper and PendingIntent
requests also forward interval, fastest interval, displacement and batching.
Older Android versions retain the legacy registration path. No additional
location provider or app-side GPS subscription is required.

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

Since 1.0.51, all four built-in styles (light, dark, light simplified and dark simplified)
load house numbers from the separate
`martin_house_numbers` vector source (MVT layer `house_number`, z16 tiles with
overzoom). Existing house-number styling and tap callbacks are preserved;
`address` may be absent. Each style retains its own house-number colors, layout
and visibility zoom thresholds. Server data
updates do not push a redraw of tiles already loaded by the map.

### Automatic house-number updates

The SDK polls changes only while a licensed map is active and refreshes affected
Martin tiles without replacing the style or moving the camera. Resume catches up;
network errors retry. HTTP 204 removes old house numbers. UPin and tap callbacks
keep their existing behavior. Automatic polling is enabled by default.

```kotlin
map.setHouseNumbersAutoRefreshEnabled(true) // Default; no application polling needed.
map.refreshHouseNumbers() // Optional manual catch-up while active.
```

Disabling polling cancels feed requests, not normal map loading. Manual catch-up
also works with polling disabled. These methods must run on the UI thread.
Updates are eventual, not instantaneous; network/polling latency applies.

Version 1.0.53 increases only house-number text size from 8.5 to 9.5 in all
four built-in styles. Other label sizes and zoom thresholds are unchanged.

### Automatic detailed-road updates (1.0.54)

All four built-in styles use `martin_roads` for detailed roads at canonical zoom
13 through 16, with overzoom above 16. Low-zoom roads, railway layers and
airport/legacy alias layers keep their existing sources and styling.

```kotlin
map.setRoadsAutoRefreshEnabled(true) // Default.
val enabled = map.isRoadsAutoRefreshEnabled()
map.refreshRoads() // Optional manual catch-up, including with polling disabled.
```

The same methods are available on `BharatMapView`. Call on the UI thread.
Automatic polling runs only while the licensed map is active. Road refresh
updates affected tile revisions without replacing the style, moving the camera
or modifying navigation/follow state. Normal tile loading remains enabled when
polling is disabled. Network errors retry without clearing previously rendered
data; an empty tile removes its old features.

Road cursor, baseline and per-tile revisions are saved together and restored
before rendering after restart. State is scoped to the tile-source URLs and is
independent of house-number refresh. Offline rendering requires cached tiles;
these methods do not download an offline region.

Feature-query consumers should identify roads using `(table_no, gid)`, not gid
alone. The original gid property is retained; the MVT feature identifier is
globally unique across physical road partitions. Source-layer names and style
layer IDs remain unchanged, but detailed-road source IDs are now `martin_roads`
instead of `composite`. Built-in bridge matching accepts both sources and uses
the table/gid pair, with compatibility fallback for old data without table_no.

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

### Same-device Tile Servers Without Internet

Since Android 1.0.43, native HTTP(S) resource requests to `localhost`,
`localhost.`, canonical dotted-decimal `127.0.0.0/8`, or `[::1]` are not suspended
merely because Wi-Fi/cellular connectivity is absent. This supports an
application-owned loopback MBTiles server through ordinary `VectorSource` and
`TileSet` APIs. No network-state override or extra SDK toggle is required.

```kotlin
val tiles = TileSet("2.1.0", "http://localhost:8888/local/{z}/{x}/{y}.pbf")
style.addSource(VectorSource("offline-local", tiles))
// Add the application's layers referencing "offline-local" and their source-layer IDs.
```

The application must start its server, provide downloaded tiles, and restore
its sources/layers after a style reload. Other required style assets (including
sprites and glyphs) must also be locally available/cached; this exception does
not fetch missing remote assets offline. Cleartext policy for HTTP and normal
certificate validation for HTTPS still apply. Non-loopback hosts, LAN addresses
and aliases that happen to resolve to loopback retain normal offline scheduling.
Use the explicit forms above, not shortened/octal/encoded IP spellings.
The effective resource URL is checked after URL transformation. Retry/backoff,
cache policy and request cancellation are unchanged; no camera/follow/layer
ordering changes are made by this connectivity exception.

Licensing remains mandatory: an offline cold launch requires an unexpired
previously saved signed authorization. This does not make REST search/navigation
or remote tile servers available offline. A local server is not a license bypass.

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

Outside active navigation, Locate animates to the user with the requested zoom,
zero bearing/pitch, and location follow enabled. After stopping navigation it can
be called immediately, without a delay or repeated recenter loop:

```kotlin
map.stopNavigation(true)
map.centerOnUserLocation(16.0)
```

Since 1.0.41, cancellation callbacks from an older tracking transition cannot
interrupt a newer Locate transition. A user gesture or an explicit app camera
movement still cancels the current transition normally; the user puck stays visible.

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

### Bounds For A Proposed Camera

Since 1.0.42, `getCoordinateBoundsForCamera(camera)` returns geographic bounds
for a hypothetical `CameraPosition` using the current map viewport size. It does
not move the live camera, stop tracking, interrupt animations, or emit camera
callbacks. No second map is created.

```kotlin
val inputBounds = LatLngBounds.Builder().includes(coordinates).build()
val intermediateCamera = map.getCameraForLatLngBounds(
    inputBounds, intArrayOf(50, 50, 50, 50), bearing, pitch
) ?: return
val expandedBounds = map.getCoordinateBoundsForCamera(intermediateCamera) ?: return
val finalCamera = map.getCameraForLatLngBounds(
    expandedBounds, intArrayOf(20, 120, 30, 220), bearing, pitch
) ?: return
// All calculations above are read-only. Apply finalCamera separately if needed.
```

`coordinates` is a list of `LatLng`. Camera padding is physical pixels in
left/top/right/bottom order; convert dp before passing it. Null padding means
zero. Padding shifts the target within the viewport; the returned bounds cover
the **whole viewport**, not just its unpadded area. All four corners participate
when bearing or pitch is nonzero. Longitudes are continuous and may extend
beyond +/-180 across the antimeridian or span multiple world copies at low zoom.
Do not independently normalize the returned west/east values before a second fit.

Call on the UI thread after layout. An unlaid-out or destroyed map returns null.
Invalid/incomplete cameras throw `IllegalArgumentException`: values must be
finite, latitude within +/-85.0511287798066, zoom 0...25.5 and pitch 0...60 degrees.
Padding must contain four nonnegative values and leave positive viewport space.
The map's configured native camera constraints still apply. Native viewport size
is rounded to logical pixels, so allow density-related pixel rounding when
comparing against physical-screen projections.

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

In normal map mode, `centerOnUserLocation(...)` and `recenterCamera()` preserve
the current viewport padding, including asymmetric insets. Locate restores follow
and resets bearing/pitch without resetting padding. The no-fix fallback also
preserves the viewport. Active navigation retains its separate navigation viewport
contract. Version 1.0.47 also cancels superseded tracking-padding animations when
the app calls `setMapPadding` / `animateMapPadding` or tracking mode changes.
This prevents a pending navigation Stop animation from later overwriting Home
viewport padding. Applications do not need a delayed padding replay.

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
This is an app-owned camera operation: in normal and active navigation modes it stops follow without
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
While navigation is in free-camera mode, this setter stores the viewport for the
next Recenter without changing the current camera or padding.

### Free camera during navigation (Android 1.0.38+)

Navigation starts following the puck. A user camera gesture (pan, zoom, rotation
or tilt), `setCameraPosition`, `moveCameraTo`, `animateCameraTo`, `easeCameraTo`,
`fitCameraToCoordinates`, or `transitionCamera` suspends following. Subsequent
navigation progress/location updates do not take the camera back. Reroute, live
congestion and style reload preserve this intent. Guidance, puck movement, voice,
vanishing route lines and progress callbacks continue normally.

```kotlin
// Existing public location-component API also explicitly selects free camera.
map.locationComponent.cameraMode = CameraMode.NONE
map.cameraPosition = CameraPosition.Builder(map.cameraPosition)
    .target(LatLng(28.65, 77.25))
    .zoom(13.0)
    .bearing(23.0)
    .padding(doubleArrayOf(31.0, 65.0, 43.0, 90.0))
    .build()

// No per-frame camera corrections are necessary.
map.recenterCamera()
// Or center with an explicit zoom and restore navigation following:
map.centerOnUserLocation(15.0)
```

Recenter or an explicit public tracking CameraMode restores following. A new
Start starts a fresh following session. Stop and arrival retain their existing
location/calibration behavior; camera callbacks from an older session cannot
reapply its navigation viewport. `requestRoutePreview(autoFit=false)` remains
draw-only and does not change camera/follow state.

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

Since 1.0.45, traversal updates the existing route source without clearing it or
replacing the guidance route. The passed origin and passed via markers disappear;
unpassed waypoints and the destination remain at their original locations. A new
route/preview resets traversal, while a style reload preserves it. Route casing
and congestion features share the remaining geometry. No new route request or
navigation session is created by display-only trimming.

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

Android 1.0.39+: `currentNavigationLocation()` selects the current source by mode,
not by whether a replay engine was used in an earlier trip. It returns simulated
location while simulating, the held coordinate after simulated Stop(false)/held
arrival, and real/matched location after Stop(true) or a real Start.
`currentSimulatedNavigationLocation()` returns null when there is no active
simulation or intentionally held simulated location. Both getters return defensive
`Location` snapshots; mutating a returned value does not move the SDK puck or
change later results.

```kotlin
map.stopNavigation(true)
// Immediate restart; no delay, map recreation or location-engine replacement.
map.startNavigation(backendRoute, BharatNavigationSimulationOptions(false, false, true, 1.0))
// On subsequent real GPS updates these no longer prefer an old replay position:
val navigationLocation = map.currentNavigationLocation()
val simulatedLocation = map.currentSimulatedNavigationLocation() // null
```

A real Start directly after a held simulated arrival or Stop(false) restores the
original user-location engine. New Start clears the previous hold. Stop(false)
after a real trip does not revive a historical simulation. Route fetching alone
does not switch the active simulation mode.

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

## Native rendered-feature screen-shape queries (Android 1.0.36+)

Use map-local **physical pixels**, measured from the map view's top-left corner.
`ScreenCoordinate` belongs to `com.bharatmaps.android.geometry`.

```kotlin
val contour = listOf(
    ScreenCoordinate(40.0, 100.0),
    ScreenCoordinate(320.0, 100.0),
    ScreenCoordinate(40.0, 400.0),
    ScreenCoordinate(40.0, 100.0) // Explicitly close the contour.
)
val hits = map.queryRenderedFeatures(
    contour,
    Expression.eq(Expression.get("kind"), "building"),
    "buildings", "building-labels"
)
for (hit in hits) {
    val rawFeature = hit.feature // Geometry, identifier and all source properties.
    val nestedProperties = rawFeature.properties() // No POI DTO conversion.
    val sourceId = hit.sourceId
    val sourceLayer = hit.sourceLayer // Empty for GeoJSON.
    val renderedLayerId = hit.layerId
}
// No filter:
val allHits = map.queryRenderedFeatures(contour, "buildings")
```

- One coordinate queries a point. Two or more unclosed coordinates query an open
  path, including each segment. Repeat the first coordinate at the end to query
  a closed contour and its interior. Empty input returns an empty list.
- Native rendered hit-testing and native Expression filters are used; an open
  path or contour is not replaced by its bounding rectangle. Painted width,
  circle radius and symbol collision geometry follow native query semantics.
- Results are `BharatMapsRenderedFeature`: raw `Feature` plus separate metadata.
  Unknown nested property objects, arrays and null values are retained.
- Topmost style layer comes first, independently of the order of `layerIds`.
  Native ordering within a layer is retained. A hit shared by several path
  segments is returned once per native tile feature/rendered layer. Distinct
  features are not merged merely because they have equal properties or IDs.
  Native clipped tile fragments and hits in different layers remain separate.
- Omitted/null/empty layer IDs query all rendered layers, matching the existing
  PointF/RectF convention. A nonexistent layer ID returns no hits.
- Call on the UI thread after the style and desired source tiles have rendered.
  Queries have no camera/follow side effects and do not cache feature results.
  Existing PointF/RectF APIs and their `List<Feature>` results are unchanged.

## Nonblocking rendered queries (Android 1.0.40+)

Use this API for traffic and business-event scans. Existing synchronous shape,
PointF and RectF queries remain source-compatible, but may wait for the renderer;
calling them on an unsupported worker thread is not a substitute for this API.

```kotlin
var pendingQuery: BharatMapsRenderedQuery? = null

fun refreshTraffic() {
    pendingQuery?.cancel()
    val sessionId = currentSessionId
    val revision = currentRouteRevision
    pendingQuery = map.queryRenderedFeaturesAsync(
        contour,
        Expression.eq(Expression.get("highway"), "primary"),
        { hits, error ->
            if (error == null && sessionId == currentSessionId && revision == currentRouteRevision) {
                consumeTraffic(hits.orEmpty())
            }
        },
        "traffic-route-data-layer"
    )
}
// Cancel when the app session/camera scan becomes obsolete or its owner stops.
pendingQuery?.cancel()
```

Create/cancel requests on the UI thread. Geometry work runs on the renderer
thread; completion is posted exactly once to the UI thread, never inline inside
a style mutation, cancellation or native callback. Success preserves the
synchronous shape API's geometry, raw properties, source/source-layer/layer
metadata, native filter, deduplication and topmost-first ordering. No camera,
follow, navigation session or route mutations are performed.

Cancellation, destroyed/unavailable surface, style replacement, changed relevant
native source/layer identity, layer ordering changes and timeout return a
non-null error and null results, not a successful empty list. Up to 8 requests
may be outstanding; requests time out after 5 seconds without waiting for GL.
A native source identity refers to the data already applied to the style, not
an asynchronously queued GeoJSON payload. Cancel/reissue on application-owned
traffic generation, session/revision or camera changes; correlate the callback
with captured app state as in the example. SDK cancellation does not move cameras.

Since Android 1.0.48, unrestricted queries work with the live native location
puck enabled. Its continuously updated GeoJSON data is treated as a rendered
snapshot, not as source replacement. Results still include matching location
features with their metadata; the SDK does not exclude location layers or use a
synchronous fallback. Actual source removal/replacement (including remove/re-add
of the same object), style replacement, and relevant ordinary source/layer
changes still invalidate pending results. Do not disable location or enumerate
private location-layer IDs to query roads, buildings or POIs.

## Authoritative bridge roads (Android 1.0.40+)

Bridge instructions query only the bundled style's road LineLayers in source
`composite` and known road source-layers. Application traffic, route and overlay
features are excluded even when they contain `highway` or `gid`. No raw properties
are removed or renamed. Custom styles can opt into their authoritative layers:

```kotlin
map.setNavigationRoadLayerIds("my-road-lines", "my-road-labels")
// Configure before starting navigation; this selection survives style reload.
// Empty varargs disable rendered bridge matching.
map.setNavigationRoadLayerIds()
// Null restores bundled-style defaults (Java: setNavigationRoadLayerIds((String[]) null)).
```

The matcher refreshes asynchronously while navigating and discards obsolete
step/style requests. It chooses the nearest eligible road geometry, with a
stable tie-break independent of overlay ordering. At coincident intersections
without a road identifier in route steps this remains a geometric heuristic;
the SDK cannot infer a missing authoritative road identity. Genuine missing/F
bridge metadata produces no bridge instruction. The first voice sentence can
wait up to 750 ms for road data; existing voice cooldowns are preserved.

## Source-specific tile readiness (Android 1.0.37+)

Register on the UI thread before adding the pending source/layers:

```kotlin
val sourceListener = BharatMapsSourceDataListener { event ->
    if (event.sourceId == pendingTrafficSourceId &&
        event.dataType == BharatMapsSourceDataEvent.DataType.TILE &&
        event.isLoaded && map.isSourceLoaded(event.sourceId)) {
        // Application-owned policy: reveal this generation, then remove the old one.
        activatePendingTrafficGeneration()
    }
}
map.addOnSourceDataListener(sourceListener)

// Nonblocking latest renderer-confirmed snapshot, also available without a listener:
val ready = map.isSourceLoaded("traffic-pending")

// Remove the same listener instance when no longer needed.
map.removeOnSourceDataListener(sourceListener)
```

`BharatMapsSourceDataEvent` provides `sourceId`, `dataType` (`METADATA` or `TILE`),
`isLoaded`, `generation`, and nullable `tileOperation`. Callbacks run on the UI
thread. Metadata/source changes do not report tile readiness. `tileOperation`
identifies native cache/network requests, load, parse completion, error or
cancellation; metadata has no tile operation. `NullOp` denotes a source readiness
transition after updating its tile set (for example, removing unused fallback
tiles), not a successful download of a particular tile.

Readiness means that the current enabled tiled source has retained tiles and all
of them have successfully loaded/parsed, with no pending request or failed tile.
It is **not** feature-count polling, whole-map readiness, or frame completion.
A successfully parsed empty vector tile counts as ready. Missing sources,
metadata-only sources, sources without active tiles, and failed/pending tiles do
not. Readiness applies to the current native tile set, not every tile in the
world or proof that a frame has reached the display. Camera changes can require
new tiles and make a previously ready source unready.

Since 1.0.40 readiness is computed on the renderer thread and delivered as a
snapshot; source event delivery and `isSourceLoaded` never synchronously wait
for GL. Until the renderer has reported the current native source identity,
readiness is false. During a renderer stall an existing snapshot may be old;
this is not a synchronous freshness barrier. Success/empty/error semantics
remain unchanged. Since 1.0.41, readiness sampling is queued after the current
renderer task, never performed reentrantly while tile collections are being
changed or destroyed during source visibility/style changes.

Events carry the native source implementation identity across the renderer/UI
boundary. Removed/replaced-source and previous-style events are rejected. The
opaque `generation` identifies an observed source implementation within this map;
compare it only within that map, not across launches. Readiness is a delivery-time
snapshot, not a promise that a later camera/source change will remain loaded.

For traffic generation switching, keep the old source/layers while the pending
source loads. The pending source needs an enabled style layer requesting its
tiles; a layer with `visibility = none` cannot establish readiness. The app owns
source IDs, active/pending generations, any 12-second timeout, and the decision to
retain the old generation on error/timeout. The SDK does not switch generations,
remove old traffic, or move the camera. Existing source-change/tile listeners
remain available.

## Martin Session Protection (1.0.55)

For registered applications whose validated license contains
`martinProtection.mode = required`, the SDK automatically prepares a Play Integrity
session and signs Martin roads and house-number TileJSON, change-feed and tile
requests with installation-bound DPoP. Call the normal `validateLicense` API
independently of map/style readiness. Do not wait for tiles before validating the
license and do not implement app-owned token refresh or signing interceptors.

Registration requires the package name, Play App Signing SHA256 certificate,
allowed version policy and linked Google Cloud project. The project number is
provided by the server's license configuration, not inferred from Firebase.
The default policy requires a recognized, licensed Play-distributed installation
with device integrity. An adb-installed debug build does not prove these verdicts.
Missing configuration or failed evidence denies protected requests; no anonymous
fallback is used when protection is required.

The SDK uses Play Integrity 1.6.0 as a dependency. Installation P-256 keys stay in
Android Keystore. Installation metadata is stored in
`Context.noBackupFilesDir/bharat-martin-<scope>` and must not be copied between
installations. If the Keystore key is absent, stale metadata is discarded. Session
refresh/provider recovery is SDK-owned and does not depend on activity resume or
application license retry timers. Work queues are bounded and the main thread is
not blocked by attestation/network waits.

Legacy `/data/production` authentication is unchanged. Subtiles are excluded.
Revision query parameters/cache identities remain unchanged; authorization stays
in headers. Auth errors do not become empty tiles or feed deletion events.

This opt-in transport is available from 1.0.55; it is absent from 1.0.54.
Existing license configurations without Martin protection retain their behavior.
Production activation requires joint issuer/gateway/SDK checks and real
Play-distribution verification. Adding the dependency alone does not activate it.

### Explicit Martin Attestation Diagnostic (1.0.56)

For operator-controlled acceptance testing, run a single issuer diagnostic after
`validateLicense` succeeds. It does not depend on map loading and reuses the SDK's
validated license context. Never call it automatically on each launch or location
update. It works with an enabled registered server policy even when `required=false`.
The server supplies the Cloud project in its authenticated challenge; applications
must not fabricate a required license response or call transport internals.

```kotlin
import com.bharatmaps.android.BharatMaps
import com.bharatmaps.android.MartinAttestationDiagnostic

private var attestationCheck: MartinAttestationDiagnostic.Operation? = null

// Explicit operator action, only after license validation succeeds.
fun checkAttestation() {
    attestationCheck = BharatMaps.runMartinAttestationDiagnostic { result ->
        // Always main thread. Inspect lifecycle before touching an Activity view.
        val succeeded = result.status == MartinAttestationDiagnostic.Status.SUCCESS
        val verifiedExchanges = result.completedExchanges // 0, 1, or 2
        // Only sanitized outcomes are returned: no keys, tokens or provider bodies.
    }
}

// For example, when the owning Activity is destroyed:
fun cancelAttestationCheck() {
    attestationCheck?.cancel()
    attestationCheck = null
}
```

The operation has a 90-second work deadline and exactly one main-thread callback,
including cancellation. Only one diagnostic runs at a time; concurrent calls return
`BUSY`. Before license validation it returns `LICENSE_UNAVAILABLE`. Other statuses
are `SUCCESS`, `CANCELLED`, `TIMEOUT`, `DENIED`, `UNAVAILABLE`, and
`CONFIGURATION_CHANGED`. `completedExchanges` preserves partial success.

Success means two verified issuer exchanges, the second using fresh Play evidence
and refresh. The first may also be refresh if this diagnostic installation was
previously enrolled. A dedicated persistent Android Keystore/no-backup namespace
keeps this identity separate from regular SDK transport. Transient Play token
errors have a bounded same-key/request-hash retry; an interrupted issuer exchange
can recover using the same PoP identity, not a new registration. No access tokens
are returned, logged or attached to map resources by this diagnostic.

Cancellation stops further client work but cannot undo a server request that was
already accepted. The callback can arrive after Activity destruction; retain no
view references unnecessarily. The SDK does not change camera, navigation, user
location, normal Martin protection mode or the resource interceptor configuration.

Prerequisites: supported Android device with Play Store/Play services, a licensed
Play-distributed app matching the registered package, Play app-signing certificate
and minimum version, and enabled issuer policy/Google credentials. Emulator or
sideloaded success is not assumed. Success is **issuer/attestation verification,
not protected tile transport or gateway/cache-HIT end-to-end acceptance**.

### Normal Martin Authorization State (1.0.57)

Observe the shared SDK session before validating the license or loading the map.
This observer is independent of the explicit attestation diagnostic. It never
exposes tokens, attestation evidence, API keys or installation identifiers.

```kotlin
import com.bharatmaps.android.BharatMaps
import com.bharatmaps.android.MartinAuthorization

private var martinAuthorization: MartinAuthorization.Subscription? = null

private fun observeMartinAuthorization() {
    martinAuthorization?.cancel()
    martinAuthorization = BharatMaps.addMartinAuthorizationListener { snapshot ->
        when (snapshot.status) {
            MartinAuthorization.Status.DENIED,
            MartinAuthorization.Status.RETRYABLE_FAILURE -> {
                // End indefinite loading UI and offer an application-owned recovery action.
                showMapAuthorizationError(snapshot.errorCode.name)
            }
            else -> Unit
        }
    }
}

override fun onDestroy() {
    martinAuthorization?.cancel()
    martinAuthorization = null
    super.onDestroy()
}
```

`getMartinAuthorizationState()` returns the current immutable snapshot.
Statuses are `LICENSE_UNAVAILABLE`, `DISABLED`, `PREPARING`, `READY`, `DENIED`,
`RETRYABLE_FAILURE`. Error codes are `NONE`, `LICENSE_NOT_VALIDATED`,
`CONFIGURATION_INVALID`, `SERVER_DENIED`, `UNAVAILABLE`, `TIMEOUT`, `SESSION_EXPIRED`.
`DISABLED` means the validated license does not require Martin protection.
`READY` means issuer/session authorization, **not mapLoaded or successful rendering**.
Resource loading and map lifecycle callbacks remain separate.

Callbacks run on the main thread, including asynchronous initial replay. This is
an observer of the latest state, not a lossless event history: superseded queued
snapshots are suppressed. Identical states are deduplicated and stale license
configurations cannot publish a result into a new configuration. Initial required
session preparation has a 45-second outcome deadline; a late valid result may
recover `TIMEOUT` to `READY`. Session expiry/token rejection stops reporting READY.
Transient recovery can occur on subsequent requests; successful online license
validation explicitly retries preparation without changing security policy.

Cancel on owner destruction (or onStop if only observing while visible), then
subscribe again when needed. Cancellation releases the listener and suppresses
queued callbacks; a callback already in flight may finish. It does not cancel
shared transport. Background expiry is reflected in the next main-thread delivery
or subscription replay. No application UI, camera or follow behavior is changed.

### Explicit Developer Installation Access (1.0.58)

An ADB/Android Studio installation can use Martin only after an administrator
explicitly pairs that installation. This is a separate administrative access
method, **not Play Integrity or verified hardware attestation**. Default SDK
behavior remains Play Integrity; neither a debug flag nor a failed Play check
creates a grant. No shared development secret belongs in the application.

```kotlin
// Explicit development configuration only, before normal license validation.
BharatMaps.setMartinDeveloperAccessEnabled(BuildConfig.DEBUG && developerAccessOptIn)

// After successful license validation, including when Martin reports DENIED:
BharatMaps.getMartinDeveloperIdentity { identity ->
    if (identity != null) {
        // Transfer through a trusted local developer channel to the administrator.
        // These are PUBLIC values, never an API key or private signing key.
        val appId = identity.appId
        val publicJwk = identity.publicKeyJwk
        val thumbprint = identity.thumbprint
    }
}
```

The callback runs on the main thread. A null identity means that opt-in/validated
license is unavailable, the license changed, the queue is full or the key could
not be loaded. There is no cancellation handle; a destroyed UI owner should
ignore its late callback. Identity retrieval uses the shared worker and can wait
for an ongoing authorization request. It does not require mapLoaded or READY.

The administrator approves that exact public-key thumbprint for the existing
Android app, tenant and API-key ID, for at most seven days. After approval, retry
normal online license validation or restart the app. Normal SDK transport signs
fresh one-use challenges and uses short-lived DPoP-bound sessions. Expired,
revoked, unapproved or differently scoped installations fail closed. No fallback
from production Play authentication occurs automatically.

Developer keys/installation metadata are separate from production and diagnostic
identities. Same-signer ADB updates retain them; uninstalling/clearing app data or
losing the Keystore key requires explicit re-pairing. Do not uninstall to recover
an authorization error. Opt-in is process-local and defaults to false. Setting
it to false restores the separate normal Play identity. User location, camera,
styles and navigation behavior are not changed.
