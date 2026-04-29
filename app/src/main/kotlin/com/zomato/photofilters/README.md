# AndroidPhotoFilters compatibility shim

This package keeps the small AndroidPhotoFilters API surface used by Gallery while avoiding the upstream prebuilt native library.

The filter API and preset definitions are based on `naveensingh/AndroidPhotoFilters` / `com.github.naveensingh:androidphotofilters` (Apache-2.0). The pixel operations formerly delegated to `libNativeImageProcessor.so` are implemented in Kotlin so the APK no longer ships the 4 KB-aligned native library that fails 16 KB page-size compatibility checks.
