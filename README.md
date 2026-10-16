# rodin-mt6899 — Rodin Essential (Kotlin + Jetpack Compose, Material 3)

A complete source rewrite of the **Rodin Essential** Android interface in
**Kotlin + Jetpack Compose (Material 3)**.

Rodin Essential is hardware-control software for the Xiaomi *rodin* platform
(POCO X7 Pro / Redmi Turbo 4, MediaTek MT6899 / Dimensity 8400-Ultra).

This repository replaces the original Flutter AOT interface (and its
NativeActivity host) with a regular Kotlin Android application while keeping
every feature, control, option, and backend behavior identical. The privileged
Rust backend is **not modified**: the app talks directly to the same
`rodin_essentiald` daemon over the same authenticated local transports and the
same text protocol (`13.6`).

## What replaces what

| Original | This rewrite |
| --- | --- |
| `ui/flutter` (Flutter AOT UI, Dart) | Kotlin + Jetpack Compose UI (Material 3) |
| `runtime/host-rust` (NativeActivity + embedder host) | Kotlin app process (Compose + `RodinBackend` client) |
| `librodin_essential_host.so` Dart FFI bridge | Kotlin `RodinBackend` daemon client (pure Kotlin, no JNI) |
| `android/platform` DEX adapters (tile, back) | Kotlin `BypassTileService` + Activity back callbacks |
| `runtime/daemon-rust` | **unchanged** — same daemon, same protocol |

## Architecture

```
Kotlin + Compose app (io.github.neeschal.rodinessential)
        │  text protocol 13.6 over authenticated local transports
        ▼
rodin_essentiald (Rust, unchanged)
        ├── sysfs / procfs / block controls
        ├── Xiaomi touch and display AIDL services
        ├── MediaTek GED and Mali devfreq
        └── persistence, drift guards, power-supply events
```

Transports, exactly as implemented by the daemon and the original host:

1. **Abstract Unix socket** `rodin_essentiald_v15` — direct connection; the
   app accepts the server only if `SO_PEERCRED` reports UID 0; the daemon
   accepts only the app UID Android assigned to
   `io.github.neeschal.rodinessential`.
2. **Privileged loopback** `127.0.0.1:732` — used only after the app proves it
   cannot bind the neighbouring low port `:731` from its own SELinux/UID
   sandbox (fail-closed privilege probe); the daemon maps the live client
   tuple through `/proc/net/tcp` to the package UID before accepting.
3. **Daemon-initiated reverse socket** `rodin_essential_app_v15` —
   compatibility fallback where the app binds and the daemon connects after a
   UID 0 peer check.

The app never runs as root, never uses the system UID, and requests no
privileged permissions. Kernel and vendor controls remain owned by the daemon.

## Status

Source port in progress. Every file in this repository is committed and pushed
individually as it is written.

- [x] repository scaffold (licensing, Gradle, manifest, resources)
- [ ] `RodinBackend` daemon client (transports, protocol 13.6, snapshot cache)
- [ ] Material 3 theme, shell, and navigation
- [ ] screens: Home, Charging, Touch, Display, Resolution, CPU, GPU, ZRAM,
      Diagnostics, Advanced configuration, Hubs, Support, Settings
- [ ] Per-App Controls and Memory/Storage subsystem clocks
- [ ] System Colors (Material You palettes, wallpaper, contrast)
- [ ] Quick Settings bypass-charging tile
- [ ] appearance extras (background styles, motion, easter eggs)

## Build

Source-only at this stage: the port was written without compiling in its
development environment. The project targets `compileSdk 35`, `minSdk 31`
(matching the original API 31+ requirement), ARM64 devices, and a standard
`./gradlew assembleRelease` once a Gradle wrapper jar is generated.

> The `applicationId` intentionally remains `io.github.neeschal.rodinessential`
> so the unchanged daemon's package-UID validation accepts the app.

## Credits

- Original project: **Rodin Essential** by NEESCHAL-3
  (<https://github.com/NEESCHAL-3/Rodin-Essential>).
- This repository is an independent rewrite of the application interface for
  the same device and the same daemon.

## License

Apache License 2.0 — see `LICENSE` and `NOTICE`.
