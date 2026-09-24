package com.arstudio.gameinfo

import android.content.Context

data class PcProfile(
    val ramGb: Int,
    val vramGb: Int,
    val gpuTier: Int,   // 0 integrated, 1 entry, 2 mid, 3 high, 4 enthusiast
    val cpuTier: Int,   // 0 old, 1 basic, 2 mid, 3 high, 4 enthusiast
    val storageGb: Int
)

class SpecStore(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("gameinfo", Context.MODE_PRIVATE)
    var ramGb: Int get() = sp.getInt("ram", 8); set(v) { sp.edit().putInt("ram", v).apply() }
    var vramGb: Int get() = sp.getInt("vram", 4); set(v) { sp.edit().putInt("vram", v).apply() }
    var gpuTier: Int get() = sp.getInt("gpuTier", 2); set(v) { sp.edit().putInt("gpuTier", v).apply() }
    var cpuTier: Int get() = sp.getInt("cpuTier", 2); set(v) { sp.edit().putInt("cpuTier", v).apply() }
    var storageGb: Int get() = sp.getInt("storage", 100); set(v) { sp.edit().putInt("storage", v).apply() }
    var dark: Boolean get() = sp.getBoolean("dark", true); set(v) { sp.edit().putBoolean("dark", v).apply() }

    // Kept for compatibility with the original app's settings/data.
    var hasDedicatedGpu: Boolean
        get() = gpuTier > 0
        set(v) { if (!v) gpuTier = 0 else if (gpuTier == 0) gpuTier = 1 }

    var cpuAge: Int
        get() = when { cpuTier <= 1 -> 0; cpuTier == 2 -> 1; else -> 2 }
        set(v) { cpuTier = when (v) { 0 -> 0; 1 -> 2; else -> 4 } }

    fun profile() = PcProfile(ramGb, vramGb, gpuTier, cpuTier, storageGb)

    fun estimatedTier(): Int {
        val score = ramTier(ramGb) + gpuTier + cpuTier
        return when { score >= 10 -> 4; score >= 7 -> 3; score >= 4 -> 2; else -> 1 }
    }
}

private fun ramTier(ram: Int) = when { ram >= 32 -> 4; ram >= 16 -> 3; ram >= 8 -> 2; else -> 1 }

fun tierLabel(t: Int) = when (t) {
    1 -> "Entry-level"
    2 -> "Mid-range"
    3 -> "Gaming PC"
    else -> "Enthusiast"
}

data class Compatibility(
    val verdict: String,
    val note: String,
    val canRun: Boolean,
    val details: List<String>
)

fun requirementsFor(game: Game): PcProfile {
    // These are broad GameInfo estimates for entries without a verified requirement block.
    // The UI explicitly labels them as estimates rather than official requirements.
    return when (game.tier) {
        1 -> PcProfile(4, 1, 0, 1, 10)
        2 -> PcProfile(8, 2, 1, 2, 30)
        3 -> PcProfile(16, 6, 3, 3, 70)
        else -> PcProfile(16, 8, 4, 4, 100)
    }
}

fun compatibilityFor(profile: PcProfile, game: Game): Compatibility {
    if ("PC" !in game.platforms) {
        return Compatibility("No PC version listed", "This title is not currently categorized as a PC game in GameInfo.", false, emptyList())
    }
    val req = requirementsFor(game)
    val details = mutableListOf<String>()
    val ramOk = profile.ramGb >= req.ramGb
    val vramOk = profile.vramGb >= req.vramGb
    val gpuOk = profile.gpuTier >= req.gpuTier
    val cpuOk = profile.cpuTier >= req.cpuTier
    val storageOk = profile.storageGb >= req.storageGb
    if (!ramOk) details += "RAM: ${profile.ramGb} GB < ~${req.ramGb} GB"
    if (!vramOk) details += "VRAM: ${profile.vramGb} GB < ~${req.vramGb} GB"
    if (!gpuOk) details += "GPU class: below the estimated requirement"
    if (!cpuOk) details += "CPU class: below the estimated requirement"
    if (!storageOk) details += "Free storage: ${profile.storageGb} GB < ~${req.storageGb} GB"

    val score = listOf(ramOk, vramOk, gpuOk, cpuOk, storageOk).count { it }
    return when {
        score == 5 -> Compatibility("Should run well", "Your entered specs meet the GameInfo estimate for this game.", true, listOf("All five checks pass: RAM, VRAM, GPU, CPU and storage."))
        score >= 4 && gpuOk && cpuOk -> Compatibility("Should run on reduced settings", "Most checks pass, but at least one component is below the estimate.", true, details)
        score >= 3 -> Compatibility("May struggle", "Several components are below the GameInfo estimate. Lower settings/resolution may be necessary.", false, details)
        else -> Compatibility("Below the estimate", "Your entered hardware is substantially below the GameInfo estimate for this title.", false, details)
    }
}

fun verdictFor(myTier: Int, gameTier: Int): Triple<String, String, Boolean> = when {
    myTier >= gameTier + 1 -> Triple("Should run great", "Expect smooth performance, likely at high settings.", true)
    myTier == gameTier -> Triple("Should run", "Expect playable performance, possibly on medium-high settings.", true)
    myTier == gameTier - 1 -> Triple("Might struggle", "Playable on low settings, but don't expect it to be smooth.", false)
    else -> Triple("Unlikely to run well", "This game is built for noticeably stronger hardware.", false)
}
