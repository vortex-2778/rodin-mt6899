package io.github.neeschal.rodinessential.ui

/** Top-level destinations; 1:1 with the Dart `RodinScreen` enum. */
enum class RodinScreen(val title: String) {
    home("Rodin Essential"),
    hubs("Control Hubs"),
    support("Community & Support"),
    settings("Settings"),
    charging("Charging"),
    touchBoost("Touch Response"),
    displayStudio("Display Studio"),
    cpuControl("CPU Core & Frequency"),
    advancedConfiguration("Advanced Configuration"),
    resolution("Resolution"),
    diagnostics("Diagnostics"),
    zramSwap("ZRAM & Swap Manager"),
    maliGpu("MediaTek Mali GPU & GED"),
    systemColors("System Colors"),
    perAppControls("Per-App Controls"),
    subsystemClocks("Memory DVFS & UFS");

    val isRoot: Boolean
        get() = this == home || this == hubs || this == support || this == settings
}
