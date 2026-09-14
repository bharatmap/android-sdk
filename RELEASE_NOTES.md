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
