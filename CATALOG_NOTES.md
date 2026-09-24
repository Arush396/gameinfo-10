# GameInfo catalog update

This build expands the on-device catalog from 96 entries to more than 1,000 entries (2004 onward), with platform-family tags for PC, Console, and Mobile.

## Platform classification
- Mobile-only titles are tagged `Mobile` and do not appear in PC/Console filters.
- Console-only titles are tagged `Console`.
- PC titles are tagged `PC`; multi-platform titles can have multiple tags.
- The app does not infer a PC version for a console/mobile-only game.

## PC compatibility checker
The checker now accepts:
- RAM
- GPU class
- GPU VRAM
- CPU class
- Available storage

It compares all five against a per-game hardware estimate. Where a game does not have a verified requirement block, GameInfo uses a clearly labeled broad estimate based on its hardware tier. These estimates are not presented as official developer requirements.

## Data note
The catalog is a curated expansion rather than a claim of literally every game ever released. Some games have incomplete platform/requirement information, and users should verify official PC requirements for purchase or installation decisions.

## Game icons
Icons are official artwork fetched on demand (Steam -> App Store -> IGDB), accepted only on an exact title match and cached on the device. Games with no match show "No icon". For best coverage add free IGDB/Twitch keys in `ApiKeys.kt`.
