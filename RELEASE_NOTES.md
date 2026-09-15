# 1.0.38

Android A13: retain free-camera intent during active navigation.

Actual user camera gestures, public cameraPosition/overview and app-owned camera
methods stop following until explicit Recenter or a public tracking-mode request.
Moving navigation updates, reroute, live congestion and style reload no longer
force TRACKING_GPS back on. Navigation viewport changes while free wait for the
next Recenter. Cancelled/obsolete tracking transitions cannot overwrite a newer
camera or session. centerOnUserLocation restores follow with its requested zoom.
Guidance, native puck, progress and route rendering remain active while free.

Source revision: 7fdc7a2. Final four-ABI AAR passes actual emulator input pan and
separate programmatic overview checks over five moving updates, reroute/style/
congestion, Recenter/interruption, Stop/hold/restart/arrival and real-provider
restoration. Independent reroute-provider, two-trip restart and zero-duration
tracking regressions pass, as do branding and production offline trust anchor
checks. Physical-device smoothness and multi-touch gestures were not tested.
No iOS or consuming-app changes.

# 1.0.37

Android: source-specific native tile readiness.

BharatMapsMap.addOnSourceDataListener/removeOnSourceDataListener and isSourceLoaded expose METADATA/TILE events, readiness, source identity generation and native tile operation. Successfully parsed empty vector tiles count as ready; errors/cancellation do not. Removed/replaced sources and old-style callbacks are rejected, including source removal from a preceding listener. Readiness transitions after fallback-tile cleanup are delivered without requiring another successful download. Active/pending traffic generation switching and timeouts remain application policy. No camera or tracking changes.

Source implementation: 21ffc18; verification: 219393c. Final four-ABI AAR passes Kotlin/Java usage, 21 source-readiness checks, 19 screen-shape query regressions, zero-duration tracking, branding and production offline trust anchor verification. No iOS release.

# 1.0.36

Android A11: native rendered-feature screen-shape queries.

New queryRenderedFeatures(List<ScreenCoordinate>, filter, layerIds) returns BharatMapsRenderedFeature with raw Feature and sourceId/sourceLayer/layerId metadata. Supports points, open paths and explicitly closed contours without bounding-box substitution. Native filters, top-to-bottom layer order and per-tile-feature/per-layer deduplication. Existing PointF/RectF APIs remain unchanged.

Source revision: aa9c062. Java/Kotlin compile, 19 shape-query checks and zero-duration tracking regression pass on the final four-ABI AAR. No iOS release. A12 source readiness is not included.

# 1.0.35

Android: fix default system GPS request quality on Android 12+.

HIGH_ACCURACY is now forwarded explicitly when the system selects the fused provider. Callback/Looper and PendingIntent requests preserve priority, interval, fastest interval, displacement and batching. Older Android retains its existing registration path. No public API or iOS changes.

Source revision: ce433ab. Two fresh emulator system GPS fixes pass through both request paths; map follow and zero-duration tracking regression pass. No auxiliary GPS consumer is required.

# 1.0.34

Android A09: complete zero-duration user tracking transitions.

NONE -> TRACKING with duration 0 now uses the instant camera update path, delivers finish once on main and clears transitioning so subsequent GPS fixes continue following. Zoom and viewport padding are preserved. Positive-duration behavior and public APIs are unchanged.

Source revision: 3700c3a. Published1.0.33 baseline reproduced missing finish and blocked follow; final A09, A08 and13 license/UI regression cases pass. iOS and portal unchanged.

# 1.0.33

Android A08: atomic camera and viewport transition.

Public transitionCamera(BharatCameraTransitionOptions, durationMs, callback) combines optional center, zoom, bearing, pitch and padding in one native update. Native easing or linear. Main-thread exactly-once terminal callback on finish, replacement, gesture, cancel, stop or destroy. No custom app interpolator. Existing APIs remain compatible.

Source revision: 0e18bdd. Public Kotlin compile, A08 runtime, A02, A07 and13 license/UI cases pass. iOS and portal unchanged.

# 1.0.32

Android A07: isolate stopped navigation sessions from immediate restart.

- Queued worker results and posted progress/milestone/running events from a
  stopped trip no longer enter the next session, even with the same route object.
- Simulated ARRIVED -> Stop -> Start replays both legs from the origin without
  an app delay or map recreation. App-provider reroute remains available.
- No public API changes. A02 and all 13 license/UI regression cases pass.
- iOS and portal are unchanged.

Source revision: `56c71ba`. Baseline on published 1.0.31 reproduces A07;
fixed runtime traverses both legs in both consecutive trips.

# 1.0.31

Android A02 fix: pre-start reroute provider registration.

- Stored app provider and listener are now applied when EmbeddedNavigation is
  first created, including Start with an already supplied DirectionsRoute.
- Changing the calibration-line flag no longer resets the provider, cancels a
  pending reroute, or replays listener state.
- No public API changes. A04 offline authorization and iOS are unchanged.

Source revision: `0f8308d` in the SDK source repository.
Runtime regression reproduces the missing callbacks in published 1.0.30 and
passes on the fixed AAR: pre-start registration, calibration isolation, cancel,
late completion, error, successful replacement and stop/late completion.
The app provider is invoked; no SDK navigation-backend request is observed.

# 1.0.30

Android signed offline authorization (A04).

- Existing validateLicense API now restores a previously signed grant after
  process death and network loss, or HTTP 429/5xx, without extending its expiry.
- Maximum lifetime is 24 hours, capped by API key/subscription expiry. The permit
  binds the actual package, APK signing certificate and API-key fingerprint.
- Authenticated encrypted no-backup storage uses Android Keystore. No raw API key
  or unsigned authorization flag is persisted.
- Explicit denial, TLS error, malformed/tampered/foreign/expired permissions and
  observed clock rollback fail closed. Late or cancelled requests cannot revive
  a rejected grant. Signed expiry gates the map on main, including after resume.
- LicenseValidationResult adds validationSource and offlineAvailable metadata.
- Old online-only server responses remain supported. Existing public
  BharatMapsMap signatures and A06 main-thread callback behavior are preserved.

Source revision: `099df90` in the SDK source repository. The Android signing
extension is deployed on the portal; existing iOS issuance is unchanged.

Verification uses an isolated loopback issuer with the actual deployed signer
and fixed nonproduction claims, plus attached-map runtime tests. It does not
create server organizations/API-key records, export the private key, or test
customer-key revocation. Offline authorization does not provide uncached tiles
or offline REST APIs. iOS artifacts are unchanged.

# 1.0.29

Android navigation API additions (A01, A02, A03, A05 implementation):

- Persisted navigation mute/unmute and announcements through the existing speech player, with speech cancellation.
- App-owned asynchronous reroute provider, cancellation, session/revision guards and reroute state events.
- Detailed navigation progress snapshots, route/session/leg/step identity and maneuver geometry.
- Revision-guarded live congestion overlay updates without restarting navigation.

Existing public BharatMapsMap signatures are retained; new APIs are additive.
Source revision: `5c387ad` in the SDK source repository.

Android offline license restoration (A04) is NOT included. Unfinished A04 files
were excluded from the AAR. iOS artifacts are unchanged.

Validation scope: release build, binary API comparison and branding audit.
License/UI regression checks are recorded in the source release report.
End-to-end navigation acceptance for the new APIs (voice timing, provider races,
via/arrival snapshots and congestion rendering) remains open. Publication does
not mark migration A01-A05 accepted.

# 1.0.28

Android migration fix A06: license validation on attached maps.

- Apply authorization state and map visibility/interactions on the main thread before the callback.
- Reject stale responses from superseded requests or destroyed maps without changing UI or authorization.
- Keep validation working across background/foreground transitions and with a null callback.
- Reject non-2xx HTTP responses even when the response body claims success.
- No public API signature changes. Superseded/destroyed requests report CancellationException on main.

Source revision: `222400d` in the SDK source repository.

Validation: Android 16 API 36 arm64, real attached BharatMapView, 13 fixture cases.
The published 1.0.27 baseline reproduces CalledFromWrongThreadException.
No production test organizations or API keys were created. Actual production-key
activation remains a consuming-app verification step. This release does not add
Android offline licensing, voice controls, app reroute provider, expanded navigation
progress, or live congestion refresh (migration A01-A05). iOS artifacts are unchanged.
