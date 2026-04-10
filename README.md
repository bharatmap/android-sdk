# BharatMaps Android SDK Maven Repository

Public Maven repository for binary Android artifacts.

## Repository URL

```text
https://bharatmap.github.io/android-sdk/
```

## Dependency

```gradle
repositories {
    maven { url "https://bharatmap.github.io/android-sdk/" }
}

dependencies {
    implementation "com.bharatmaps:bharatmaps-android:1.0.0"
}
```

Artifacts are served from the `gh-pages` branch.

## Releasing (automated to gh-pages)

1. Add AAR to `releases/bharatmaps-android-X.Y.Z.aar`.
2. Commit and push to `main`.
3. Create and push tag `vX.Y.Z`.
4. GitHub Actions publishes Maven files to `gh-pages` automatically.
