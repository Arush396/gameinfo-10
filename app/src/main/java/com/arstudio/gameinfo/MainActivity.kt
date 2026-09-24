package com.arstudio.gameinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val accent = Color(0xFF8B7CFF)
private val accent2 = Color(0xFF2DD4BF)

private val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light), Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium), Font(R.font.poppins_bold, FontWeight.Bold))

private fun poppinsTypography(): Typography {
    val b = Typography(); val f = Poppins
    return Typography(
        displayLarge = b.displayLarge.copy(fontFamily = f), displayMedium = b.displayMedium.copy(fontFamily = f),
        displaySmall = b.displaySmall.copy(fontFamily = f), headlineLarge = b.headlineLarge.copy(fontFamily = f),
        headlineMedium = b.headlineMedium.copy(fontFamily = f), headlineSmall = b.headlineSmall.copy(fontFamily = f),
        titleLarge = b.titleLarge.copy(fontFamily = f), titleMedium = b.titleMedium.copy(fontFamily = f),
        titleSmall = b.titleSmall.copy(fontFamily = f), bodyLarge = b.bodyLarge.copy(fontFamily = f),
        bodyMedium = b.bodyMedium.copy(fontFamily = f), bodySmall = b.bodySmall.copy(fontFamily = f),
        labelLarge = b.labelLarge.copy(fontFamily = f), labelMedium = b.labelMedium.copy(fontFamily = f),
        labelSmall = b.labelSmall.copy(fontFamily = f))
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SpecStore(this)
        IconRepo.init(this)
        setContent { GameInfoApp(store) }
    }
}

private fun platformIcon(p: String) = when (p) { "PC" -> "🖥️"; "Console" -> "🎮"; else -> "📱" }

@Composable
fun GameInfoApp(store: SpecStore) {
    var dark by remember { mutableStateOf(store.dark) }
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1100); showSplash = false }

    val bg = if (dark) Color(0xFF0A0B10) else Color(0xFFF5F5FA)
    val card = if (dark) Color(0xFF15161F) else Color.White
    val ink = if (dark) Color(0xFFEDEEF5) else Color(0xFF12131C)
    val faint = if (dark) Color(0xFF9497A8) else Color(0xFF63667A)
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = accent, onPrimary = Color.White, secondary = accent2,
        background = bg, surface = bg, onBackground = ink, onSurface = ink,
        surfaceVariant = card, onSurfaceVariant = faint)

    MaterialTheme(colorScheme = scheme, typography = poppinsTypography()) {
        if (showSplash) { SplashScreen(); return@MaterialTheme }

        var tab by remember { mutableIntStateOf(0) }
        var platformFilter by remember { mutableStateOf("All") }
        var query by remember { mutableStateOf("") }
        var selected by remember { mutableStateOf<Game?>(null) }
        var checkGame by remember { mutableStateOf<Game?>(null) }

        Scaffold(
            containerColor = bg,
            bottomBar = {
                NavigationBar(containerColor = card) {
                    val c = NavigationBarItemDefaults.colors(indicatorColor = accent.copy(alpha = 0.18f), selectedIconColor = accent, selectedTextColor = accent)
                    NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Default.Home, null) }, label = { Text("Library") }, colors = c)
                    NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Default.Build, null) }, label = { Text("Can it run?") }, colors = c)
                    NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, colors = c)
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad)) {
                when (tab) {
                    0 -> LibraryScreen(platformFilter, { platformFilter = it }, query, { query = it }) { selected = it }
                    1 -> SpecCheckerScreen(store, checkGame) { checkGame = null }
                    else -> SettingsScreen(dark) { dark = it; store.dark = it }
                }
            }
        }
        selected?.let { g ->
            GameDetailDialog(g, onClose = { selected = null }, onCheckSpec = { checkGame = it; selected = null; tab = 1 })
        }
    }
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0A0B10), Color(0xFF1A1730)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Text("🎮", fontSize = 44.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("GameInfo", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text("Every platform. One library.", fontSize = 14.sp, color = Color(0xFFB6B8C9))
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(color = accent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun PlatformChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(modifier.height(40.dp).clip(RoundedCornerShape(12.dp))
        .background(if (selected) accent else cs.surfaceVariant).clickable(onClick = onClick)
        .padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (selected) Color.White else cs.onSurface)
    }
}

@Composable
fun LibraryScreen(platform: String, onPlatform: (String) -> Unit, query: String, onQuery: (String) -> Unit, onOpen: (Game) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val filtered = ALL_GAMES.filter {
        (platform == "All" || platform in it.platforms) &&
        (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.genre.contains(query, ignoreCase = true))
    }.sortedBy { it.name }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("GameInfo", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("${ALL_GAMES.size} games across PC, console and mobile", fontSize = 13.sp, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(query, onQuery, placeholder = { Text("Search games or genres") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "PC", "Console", "Mobile")) { p -> PlatformChip(p, platform == p) { onPlatform(p) } }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filtered, key = { it.name }) { g ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cs.surfaceVariant)
                    .clickable { onOpen(g) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    GameIcon(g, 50.dp, 14.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${g.genre} · ${g.year}" + if (g.indie) " · Indie" else "", fontSize = 12.sp, color = cs.onSurfaceVariant)
                    }
                    Row { g.platforms.sorted().forEach { Text(platformIcon(it), fontSize = 16.sp, modifier = Modifier.padding(start = 2.dp)) } }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun GameIcon(g: Game, size: Dp, corner: Dp) {
    val urls by produceState<List<String>?>(initialValue = null, key1 = g.name) { value = IconRepo.icons(g) }
    var idx by remember(g.name) { mutableIntStateOf(0) }
    val list = urls
    val hasImage = list != null && idx < list.size
    val noIcon = list != null && !hasImage
    Box(Modifier.size(size).clip(RoundedCornerShape(corner)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        // Neutral tile: shown while loading, and stays only if no official icon exists.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(g.name.take(1), fontWeight = FontWeight.Bold, color = accent, fontSize = (size.value * 0.36f).sp)
            if (noIcon) Text("No icon", fontSize = (size.value * 0.16f).sp, color = accent)
        }
        if (hasImage) {
            AsyncImage(
                model = list!![idx],
                contentDescription = g.name,
                contentScale = ContentScale.Crop,
                onError = { idx++ },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun GameDetailDialog(g: Game, onClose: () -> Unit, onCheckSpec: (Game) -> Unit) {
    val cs = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onClose, containerColor = cs.surfaceVariant, shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GameIcon(g, 56.dp, 14.dp)
                Spacer(Modifier.width(12.dp))
                Text(g.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row { g.platforms.sorted().forEach { Text("${platformIcon(it)} $it   ", fontSize = 13.sp, color = cs.onSurfaceVariant) } }
                Text("${g.genre} · ${g.year}" + if (g.indie) " · Indie" else "", fontSize = 13.sp, color = cs.onSurfaceVariant)
                Text(g.desc, fontSize = 14.sp)
                if (g.minSpec != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("Minimum (community-sourced estimate)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant)
                    Text(g.minSpec, fontSize = 13.sp)
                }
                if (g.recSpec != null) {
                    Text("Recommended (community-sourced estimate)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant)
                    Text(g.recSpec, fontSize = 13.sp)
                }
                if ("PC" in g.platforms) {
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { onCheckSpec(g) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Check if my PC can run this")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}

@Composable
fun SpecCheckerScreen(store: SpecStore, checkGame: Game?, onConsumed: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var ram by remember { mutableIntStateOf(store.ramGb) }
    var vram by remember { mutableIntStateOf(store.vramGb) }
    var gpuTier by remember { mutableIntStateOf(store.gpuTier) }
    var cpuTier by remember { mutableIntStateOf(store.cpuTier) }
    var storage by remember { mutableIntStateOf(store.storageGb) }

    LaunchedEffect(ram, vram, gpuTier, cpuTier, storage) {
        store.ramGb = ram
        store.vramGb = vram
        store.gpuTier = gpuTier
        store.cpuTier = cpuTier
        store.storageGb = storage
    }

    val profile = PcProfile(ram, vram, gpuTier, cpuTier, storage)
    val myTier = when {
        ram >= 32 && gpuTier >= 4 && cpuTier >= 4 -> 4
        ram >= 16 && gpuTier >= 3 && cpuTier >= 3 -> 3
        ram >= 8 && gpuTier >= 1 && cpuTier >= 2 -> 2
        else -> 1
    }

    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Can it run?", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Enter your PC hardware and GameInfo will compare RAM, VRAM, GPU class, CPU class and available storage against each game's estimated requirement.",
            fontSize = 13.sp, color = cs.onSurfaceVariant)

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceVariant).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("RAM", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 8, 16, 32, 64).forEach { v -> PlatformChip("${v} GB", ram == v, Modifier.weight(1f)) { ram = v } }
            }
            Text("Dedicated GPU / graphics class", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Integrated" to 0, "Entry" to 1, "Mid" to 2, "High" to 3, "Enthusiast" to 4).forEach { (label, value) ->
                    PlatformChip(label, gpuTier == value, Modifier.weight(1f)) { gpuTier = value }
                }
            }
            Text("GPU VRAM", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 4, 6, 8, 12).forEach { v -> PlatformChip("${v} GB", vram == v, Modifier.weight(1f)) { vram = v } }
            }
            Text("CPU class", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Old" to 0, "Basic" to 1, "Mid" to 2, "High" to 3, "Enthusiast" to 4).forEach { (label, value) ->
                    PlatformChip(label, cpuTier == value, Modifier.weight(1f)) { cpuTier = value }
                }
            }
            Text("Free storage", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(20, 50, 100, 200, 500).forEach { v -> PlatformChip("${v} GB", storage == v, Modifier.weight(1f)) { storage = v } }
            }
        }

        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(accent.copy(alpha = 0.16f)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Your estimated PC tier", fontSize = 12.sp, color = cs.onSurfaceVariant)
                Text(tierLabel(myTier), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = accent)
                Text("This tier is only a quick summary; the game check below uses all five hardware categories.", fontSize = 11.sp, color = cs.onSurfaceVariant)
            }
        }

        if (checkGame != null) {
            val result = compatibilityFor(profile, checkGame)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceVariant).padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(checkGame.name, fontWeight = FontWeight.Medium)
                Text(result.verdict, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (result.canRun) accent2 else Color(0xFFFF8A80))
                Text(result.note, fontSize = 13.sp, color = cs.onSurfaceVariant)
                result.details.forEach { Text("• $it", fontSize = 12.sp, color = cs.onSurfaceVariant) }
                Text("Requirement data is a GameInfo estimate unless an official requirement is shown in the game's detail card.", fontSize = 10.sp, color = cs.onSurfaceVariant)
            }
        } else {
            Text("Open a PC game's details and tap \"Check if my PC can run this\" to test it.", fontSize = 13.sp, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsScreen(dark: Boolean, onDark: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceVariant).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Dark theme", Modifier.weight(1f))
            Switch(dark, onDark)
        }
        Text("GameInfo 1.1 · © 2026 AR Studio", fontSize = 12.sp, color = cs.onSurfaceVariant)
        Text("Game library is curated and stored on the device. PC compatibility uses verified requirement text where available and clearly labeled GameInfo estimates otherwise.",
            fontSize = 12.sp, color = cs.onSurfaceVariant)
    }
}
