# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Improved seek control in videos ([#325])

### Fixed
- Fixed broken looping in videos shorter than one second ([#565])
- Slideshows now automatically start in full-screen mode ([#529])
- Fixed pixelation and artifacts in some JPEG XL images ([#567])

## [1.4.0] - 2025-07-14
### Added
- Support for Ultra HDR images (Android 14+) ([#166])
- Support for wide-color-gamut images ([#375])

### Changed
- Updated translations

### Fixed
- Fixed crash in some external image editors ([#525])

## [1.3.1] - 2025-06-17
### Changed
- Updated translations

## [1.3.0] - 2025-05-31
### Added
- Copy to clipboard button for images ([#199])
- Option to keep screen on while viewing media ([#365])
- Ability to sort folders by item count ([#379])
- Confirmation dialog when restoring media ([#447])

### Changed
- Updated translations

### Fixed
- Fixed unresponsive image/video controls after rotating device ([#275])
- Swipe-to-close gesture now works with WebP images ([#362])
- Fixed inaccurate or broken seeking in some videos ([#475])
- Image rotation edits no longer auto-save without confirmation ([#241])
- External keyboards now work properly in copy/move dialogs ([#128])

## [1.2.1] - 2024-09-28
### Added
- Added option to control video playback speed
- Added option to mute videos
- Added error indicator for media load failures
- Added initial support for JPEG XL format (increased app size)

### Changed
- Updated target Android version to 14
- Replaced checkboxes with switches
- Improve scrolling performance and interface
- Improved app lock logic and interface
- Other minor bug fixes and improvements
- Added more translations

## [1.2.0] - 2024-09-21
### Added
- Added option to control video playback speed
- Added option to mute videos
- Added error indicator for media load failures
- Added initial support for JPEG XL format

### Changed
- Updated target Android version to 14
- Replaced checkboxes with switches
- Improved app lock logic and user interface
- Other minor bug fixes and improvements
- Added more translations

## [1.1.3] - 2024-04-16
### Changed
- Added some translations

### Fixed
- Fixed black thumbnails for some images.

## [1.1.2] - 2024-03-10
### Added
- Added support for AVIF.

### Changed
- Added some translations

### Fixed
- Fixed crash when playing videos.
- Fixed slideshow on Android 14.
- Fixed position reset after device rotation.
- Fixed zooming screenshots when one to one double tap zoom enabled.

## [1.1.1] - 2024-01-10
### Changed
- Added some translations

### Removed
- Removed fake app message when using the editor.

## [1.1.0] - 2024-01-02
### Changed
- Added some translations

### Removed
- Removed proprietary panorama library

## [1.0.2] - 2023-12-30
### Changed
- Added some translations

### Fixed
- Fixed zooming in high-res images

## [1.0.1] - 2023-12-28
### Changed
- Added some translation, UI/UX improvements

### Fixed
- Fixed privacy policy link

[#128]: https://github.com/FossifyOrg/Gallery/issues/128
[#166]: https://github.com/FossifyOrg/Gallery/issues/166
[#199]: https://github.com/FossifyOrg/Gallery/issues/199
[#241]: https://github.com/FossifyOrg/Gallery/issues/241
[#275]: https://github.com/FossifyOrg/Gallery/issues/275
[#362]: https://github.com/FossifyOrg/Gallery/issues/362
[#365]: https://github.com/FossifyOrg/Gallery/issues/365
[#375]: https://github.com/FossifyOrg/Gallery/issues/375
[#379]: https://github.com/FossifyOrg/Gallery/issues/379
[#447]: https://github.com/FossifyOrg/Gallery/issues/447
[#475]: https://github.com/FossifyOrg/Gallery/issues/475
[#525]: https://github.com/FossifyOrg/Gallery/issues/525
[#529]: https://github.com/FossifyOrg/Gallery/issues/529
[#567]: https://github.com/FossifyOrg/Gallery/issues/567

[Unreleased]: https://github.com/FossifyOrg/Gallery/compare/1.4.0...HEAD
[1.4.0]: https://github.com/FossifyOrg/Gallery/compare/1.3.1...1.4.0
[1.3.1]: https://github.com/FossifyOrg/Gallery/compare/1.3.0...1.3.1
[1.3.0]: https://github.com/FossifyOrg/Gallery/compare/1.2.1...1.3.0
[1.2.1]: https://github.com/FossifyOrg/Gallery/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/FossifyOrg/Gallery/compare/1.1.3...1.2.0
[1.1.3]: https://github.com/FossifyOrg/Gallery/compare/1.1.2...1.1.3
[1.1.2]: https://github.com/FossifyOrg/Gallery/compare/1.1.1...1.1.2
[1.1.1]: https://github.com/FossifyOrg/Gallery/compare/1.1.0...1.1.1
[1.1.0]: https://github.com/FossifyOrg/Gallery/compare/1.0.2...1.1.0
[1.0.2]: https://github.com/FossifyOrg/Gallery/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/FossifyOrg/Gallery/releases/tag/1.0.1
