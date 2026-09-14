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
