# My LogBook Mom (Travel Log)

Offline-first travel log built with Kotlin, Jetpack Compose, Material 3, and Room.

## Build & Run

- Open the project in Android Studio.
- Sync Gradle.
- Run the `app` configuration on a device or emulator (minSdk 24).

## Features

- Add, edit, delete travel entries
- Autocomplete previous Patient/Address values while still allowing manual entry
- Reason defaults to **Nursing** (can switch to **Personal**)
- Summary screen with destination totals and breakdowns
- Export CSV and Excel (.xls) with formulas and totals
- Share or email CSV/Excel exports
- Settings for theme + export email
- Simple PIN login (default PIN: 1234)

## Export Format

### CSV
Headers (exact order):
`Date;Day;From;Address;To:;Address;Pvt;Buss;Odometer;Reason for Travel`

### Excel (.xls)
- Excel-compatible HTML `.xls` (opens in Excel).
- Includes headers, totals, and formulas.
- Row 6 includes baseline **I6 = 0** and **J6 = "<- Prev Month Odomoter"**.

## Sharing / Email
- CSV and Excel exports can be shared or emailed.
- Export uses Android Storage Access Framework (no extra permissions).

## Login / PIN
- Default PIN is `1234`.
- Change PIN in Settings.
- PIN reset generates a random 8-digit PIN and emails it to the configured address.
