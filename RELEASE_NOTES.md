# 1.0.56

- Add public `BharatMaps.runMartinAttestationDiagnostic(callback)` with sanitized status/exchange count, main-thread completion, cancellation and a 90-second deadline.
- Explicit operator-run issuer diagnostic works with an optional registered server policy, using server-provided Cloud project configuration and isolated persistent installation keys.
- Two fresh issuer exchanges do not enable the map resource interceptor or change camera, location, navigation or normal protection mode. No keys/tokens/provider response bodies are exposed.
- Bounded transient recovery retains the same PoP identity; cancellation and changed license context stop further work.
- 29 server tests and full-AAR Android runtime fixtures passed, including cancellation, timeout, lost-response recovery and unchanged normal transport. Provider fixtures do not certify real Play attestation or protected tile E2E.
- Both shipped 64-bit ABIs pass 16 KB ELF LOAD/RELRO checks. Play Integrity 1.6.0 remains transitive.

Source revision: 485ba4de10868c412182a58395c0e407bd2272f1.
AAR SHA256: 9013f0ddc9b0b65570e50cb4d9e219d0ade1ec8999cb461770cd7773027c3035.

# 1.0.55

- Adds opt-in installation-bound Martin session protection: Play Integrity 1.6.0 and Android Keystore, ES256 DPoP, SDK-owned refresh and bounded recovery.
- Existing licenses without Martin protection retain current behavior. Installing this release does not enable production enforcement. Server registration and real platform attestation validation are required before activation.
- Credentials stay in headers; roads/house-number URLs and revision/cache identity are preserved. Legacy production tiles and subtiles are unchanged.
- Binary artifact smoke and resource checks passed using provider fixtures; this does not claim real production attestation.
- Maven includes Play Integrity transitively; both 64-bit native ABIs pass 16 KB alignment checks.

Source revision: 5fb9acf36f1bc57fb299e51a0e181e699c249151.

# 1.0.53

- Increase only house-number text-size by 1 (8.5 to 9.5) in all four built-in styles.
- Other label sizes, zoom thresholds and styling are unchanged.
- This Android release does not change house-number snapshot persistence or refresh behavior; the restart correction is iOS-only.
- Release build, packaged style comparisons and 16 KB native alignment checks passed.

Source revision: 7a09b98a2cd94c97dcf89c402c9a966388180cf5.

# 1.0.52

- Automatically refresh Martin house numbers in all four styles while a licensed map is active.
- Refresh only changed canonical tiles; retain old content during loading/network errors, support deletion and fence stale responses.
- Add automatic-refresh toggle and manual refresh API; preserve camera, UPin and tap behavior.
- Native renderer, lifecycle and production Martin smoke checks passed on both platforms. Physical-device smoothness was not tested.

Source revision: 91ed74da0c084d1fb14db157607a13548e24a8f3.

# 1.0.51

- Load house numbers from Martin in all four built-in styles, including dark and simplified variants.
- Preserve each style's layer order, colors, layout and zoom thresholds. No public API changes.
- AAR resource parity and 16 KB ELF alignment verified. No device runtime test for this release.

Source revision: 8575dc80aeeeff0bdb8ea7f58ae3c1c040a12ffc.

# 1.0.50

- Restore the original house_number symbol layer in default/light style, with its original placement, minzoom, layout and paint.
- Load it from martin_house_numbers: https://map.bharat-maps.com/martin/v1/house_number/{z}/{x}/{y}.pbf (MVT Point, source-layer house_number, z16 tiles with overzoom).
- Keep dark/simplified styles and other labels unchanged. Existing tap mapping accepts house_number and optional address; raw MVT feature ids retain gid.
- Server updates do not push automatic redraws of already-loaded tiles. No new API or application changes.
- Candidate iOS simulator verifies rendered features, id/properties, tap dispatch, UPin and reload. Android verification covers build/resources/network and 16 KB alignment, not runtime rendering. No physical-device test.

Source revision: 92d865e2d259f7f5e159aea8ab060342658b1ec2.

# 1.0.49

- Remove only the legacy house_number style layer from bundled default/light style.json.
- Dark and simplified styles, sources, other layers and public APIs are unchanged.
- No replacement source is added in this release; server/Martin migration is separate.

Source revision: 6011f17e6660d2afd30faad53b75620075e0d11e.

# 1.0.48

Android live-location async rendered queries (A22). Source: a4ffe4f1.

- Treat continuous native location-source data updates as rendered snapshots instead of invalidating all unrestricted queries.
- Preserve matching puck features, raw geometry/properties, metadata and topmost ordering; no layer exclusions or synchronous fallback.
- Retain strict ordinary source/layer validation and detect actual style/source remove/re-add with a structural revision.
- Regression reproduces 30/30 generation errors on 1.0.47 and verifies 30/30 successful full-viewport queries with animated location on the fix.
- Pan, source replacement/mutation, cancellation, style reload, timeout and main-thread responsiveness checked in SDK fixture.
- Android only. Physical Studio Edit existing acceptance remains a separate consuming-app check.

# 1.0.47

Android navigation Stop/Home padding race (A21 follow-up). Source: a21b06be.

- Cancel superseded tracking-padding animations on app viewport changes and tracking-mode changes.
- Prevent navigation teardown from zeroing padding after Home/Locate.
- Reproduced on published 1.0.46; fixed regression covers actual simulated navigation Start/Stop, repeated Locate and instant/animated padding.
- Preserve puck, normal follow, requested zoom/orientation and navigation viewport defaults.
- Vanishing-route renderer regression and both ABI 16 KB ELF checks pass.
- Android only. Physical consuming-app acceptance is separate from SDK emulator verification.

# 1.0.46

Android Locate viewport padding (A21). Source: 085e411a.

- Remove the zero-padding tracking request from normal-mode centerOnUserLocation.
- Preserve app viewport padding without changing navigation viewport, puck,
  tracking transition, requested zoom or bearing/pitch reset.
- Public-API padding regression fixture passes; physical-app Stop/Home
  acceptance must be repeated by the consuming app.
- Android only; iOS unchanged.

# 1.0.45

Android navigation vanishing route source updates (A20). Source: 0d0787a9.

- Update remaining route and casing in place without clearing the source on ticks.
- Retain original guidance route identity; hide passed origin/via, retain destination.
- Preserve traversal on style reload and reset it for new routes/previews.
- Cancel superseded async work and rebind style-restored progress/arrow listeners.
- Renderer source-state regression covers ticks, traffic, waypoints and lifecycle.
- Android only; no public high-level API or iOS changes.

# 1.0.44

Android 16 KB GNU_RELRO alignment (A19). Source revision: 8019fce.

- Explicitly align LOAD segments and GNU_RELRO end to 16 KB.
- Preserve RELRO protection, public APIs and existing behavior. Android only.
- Add release CI validation of all arm64/x86_64 ELF libraries.
- Previous successful 1.0.43 runtime checks remain valid; this addresses the
  documented static ELF mismatch, not a reproduced runtime crash.

# 1.0.43

Android offline loopback tile requests (A18). Source revision: dd1e8b1.

- Native HTTP(S) requests to explicit localhost, canonical 127/8 and [::1]
  authorities are no longer suspended solely because external connectivity is
  offline. Remote resources retain their normal offline scheduling.
- No global network-state override, licensing changes or camera/layer changes.
  Cache/retry/cancellation and HTTP cleartext/TLS policies remain unchanged.
- Document application-owned local tile servers and required offline style assets.
  Binary fixtures reproduce the 1.0.42 failure and verify uncached native tile
  requests, rendered features, LIGHT/DARK reloads and process restart with
  Wi-Fi/data disabled. Real application MBTiles/signed-license acceptance is separate.

Android only; no iOS behavior change or artifact release.

# 1.0.42

Android future-camera bounds (A17). Source revision: fb8c809.

- Add `BharatMapsMap.getCoordinateBoundsForCamera(CameraPosition)` for calculating
  full-viewport geographic bounds without moving the live camera, changing follow,
  cancelling animations or emitting camera callbacks.
- Honor target, zoom, bearing, pitch, physical-pixel padding and native constraints
  using an isolated Transform. Include all four corners and continuous longitudes
  across the antimeridian, including multiple world copies at low zoom.
- Document two-stage editor/Live framing, padding semantics, validation and
  unlaid-out viewport behavior. Java/Kotlin binary fixtures cover portrait,
  landscape, follow and active animation. Application integration remains separate.

No existing camera API behavior or iOS call sites changed.

# 1.0.41

Android A16/A17 follow-up. Source revision: 8ea2efa. No iOS changes or public API changes.

- Fix native source-readiness crash during multi-tile visibility/style changes.
  Readiness is sampled in a queued render task after tile mutation unwinds, with
  current-renderer/source-identity validation. UI queries remain nonblocking.
- Fix interrupted Stop -> Locate and repeated Locate animations. Stale tracking
  transition callbacks cannot clear/reset a newer transition. Original terminal
  callbacks, user gesture cancellation and app camera takeover are preserved.
- Regression fixtures cover the published 1.0.40 failures, multi-tile DARK/LIGHT
  reloads, source readiness, async queries, exact stop/recenter camera targets,
  navigation simulation/arrival, free camera, PiP and Cancel.

No app-side recenter loops, delay or style-reload suppression is required.
Full consuming-app/physical-device acceptance is separate from SDK fixtures.

# 1.0.40

Android A15/A16. Source revision: dd88f8d.

- Bridge instructions use authoritative bundled road layers rather than the first
  rendered highway property. Custom styles can use setNavigationRoadLayerIds.
  Traffic properties, navigation session/revision and route data are not modified.
- queryRenderedFeaturesAsync provides renderer-thread geometry queries with A11
  raw metadata/filter/order semantics, UI-thread completion, cancellation,
  style/source-generation validation, a 5-second deadline and bounded concurrency.
  Completion is posted outside native/style/destruction stacks and revalidated
  immediately before delivery. Existing synchronous queries remain compatible.
- Source readiness is computed on the renderer thread; events and isSourceLoaded
  no longer synchronously wait for GL. The getter reads the latest matching
  renderer-confirmed snapshot. A12 parsed/empty/error/generation rules remain.

Android release build includes four ABIs. SDK fixture regressions cover native
queries/readiness, authoritative bridge/traffic coexistence, actual PiP enter /
return and Cancel Yes on host GPU and SwiftShader, plus A13/A14 navigation modes.
A deterministic 6.5-second GL delay leaves the UI heartbeat and readiness getter
responsive. Documentation includes the async migration and snapshot contracts.

Important: applications must migrate synchronous business/traffic scans to the
new async API and correlate requests with their own session/camera/generation.
Full consuming-app and physical-device A16 acceptance is still required; this
release does not claim that emulator GPU stalls or all lifecycle waits are fixed.
No iOS or consuming-app changes.

# 1.0.39

Android A14: mode-aware navigation location after simulation/real transitions.

currentNavigationLocation no longer prefers a historical replay engine after
Stop(true) or real Start. currentSimulatedNavigationLocation returns null outside
active simulation or an intentional hold. Both navigation accessors return
defensive Location snapshots. Held simulation survives Stop(false)/arrival;
new Start clears that hold and restores the original user-location engine
instance for real navigation. Real Stop(false) cannot revive an old simulation.
Explicit Start after a held arrival resets the previous guidance session instead
of retaining its arrived leg/step. Route lookup alone does not change simulation
mode. No public signatures, iOS, backend or consuming-app changes.

Source revision: 430d2ec. Published 1.0.38 baseline reproduced the stale replay
accessor despite fresh system GPS. Final four-ABI AAR passes simulation -> real,
real -> simulation -> real, held Stop(false), exact held destination -> real Start
without a timer delay, moved system GPS, original engine identity, current reroute
input and defensive snapshots. A13 actual-input/free-camera, two-trip restart and
reroute cancellation regressions pass. Branding and production trust anchor checks
pass. Runtime verification is API36/arm64 emulator testing, not physical-device testing.

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
