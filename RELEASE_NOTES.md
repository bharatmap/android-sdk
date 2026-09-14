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
