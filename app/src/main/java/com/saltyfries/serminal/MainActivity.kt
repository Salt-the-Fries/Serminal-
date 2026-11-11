package com.saltyfries.serminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val Night = Color(0xFF07111F)
private val Cyan = Color(0xFF8BE9FD)
private val Muted = Color(0xFF9BAFC4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        setContent { SerminalApp() }
    }
}

@Composable
private fun SerminalApp() {
    var blur by remember { mutableFloatStateOf(16f) }
    var shade by remember { mutableFloatStateOf(0.48f) }
    Box(Modifier.fillMaxSize().background(Night)) {
        JellyfishBackdrop(blur)
        Box(Modifier.fillMaxSize().background(Night.copy(alpha = shade)))
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0x332C3D70), Color.Transparent, Color(0xAA050B15)))
        ))
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Header()
            TerminalPanel(Modifier.weight(1f))
            AmbienceControls(blur, { blur = it }, shade, { shade = it })
        }
    }
}

@Composable
private fun JellyfishBackdrop(blur: Float) {
    val context = LocalContext.current
    val player = remember { JellyfishPlayerFactory.create(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = Modifier.fillMaxSize().blur(blur.dp)
    )
}

@Composable
private fun Header() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(Color(0x553D8FA8))
                .border(1.dp, Color(0x668BE9FD), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Terminal, null, tint = Cyan) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Serminal-", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("A little shell. A lot of possibility.", color = Muted, fontSize = 11.sp)
        }
        Box(
            Modifier.clip(CircleShape).background(Color(0x5531C48D))
                .border(1.dp, Color(0x665DE2B1), CircleShape)
                .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Text("● LOCAL SESSION", color = Color(0xFFB8F6D9), fontSize = 9.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun TerminalPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val home = remember { context.filesDir }
    var cwd by remember { mutableStateOf(home) }
    var command by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val output = remember {
        mutableStateListOf(
            "Serminal- core preview  •  Android",
            "Type 'help' to see available commands.",
            ""
        )
    }

    Column(
        modifier.shadow(24.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xC0121D30))
            .border(1.dp, Color(0x55C9E9FF), RoundedCornerShape(26.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Color(0xFFFF7B83), Color(0xFFFFD27D), Color(0xFF75E6B1)).forEach {
                    Box(Modifier.size(8.dp).background(it, CircleShape))
                }
            }
            Spacer(Modifier.width(12.dp))
            Text("session-01", color = Color(0xFFB7C7D9), fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(if (busy) "RUNNING" else "READY", color = if (busy) Cyan else Muted, fontSize = 9.sp)
        }
        Spacer(Modifier.height(14.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(output) { line ->
                Text(
                    if (line.isEmpty()) " " else line,
                    color = if (line.startsWith("error:")) Color(0xFFFF929B) else Color(0xFFE1ECF7),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Color(0x66202F46)).padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("›", color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                cursorBrush = Brush.verticalGradient(listOf(Cyan, Color(0xFFB7A1FF))),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val entered = command.trim()
                    if (entered.isNotEmpty() && !busy) {
                        command = ""
                        busy = true
                        output.add("› " + entered)
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { executeCommand(entered, cwd, home) }
                            cwd = result.second
                            if (result.first == listOf("__CLEAR__")) output.clear()
                            else output.addAll(result.first.ifEmpty { listOf("") })
                            busy = false
                        }
                    }
                }),
                decorationBox = { inner ->
                    Box {
                        if (command.isEmpty()) Text("Run a command…", color = Color(0xFF71859D), fontSize = 13.sp)
                        inner()
                    }
                }
            )
        }
    }
}

private fun executeCommand(command: String, cwd: File, home: File): Pair<List<String>, File> {
    val pieces = command.trim().split(Regex("\\s+"), limit = 2)
    val name = pieces.firstOrNull().orEmpty()
    val arg = pieces.getOrNull(1).orEmpty().trim()
    return try {
        when (name) {
            "help" -> listOf(
                "Serminal commands:",
                "  help     show available commands",
                "  clear    clear terminal output",
                "  pwd      print working directory",
                "  ls       list files",
                "  cd PATH  change directory",
                "  mkdir N  create a directory",
                "  cat FILE read a text file",
                "  echo TXT print text",
                "Other commands are sent to Android /system/bin/sh."
            ) to cwd
            "clear" -> listOf("__CLEAR__") to cwd
            "pwd" -> listOf(cwd.absolutePath) to cwd
            "echo" -> listOf(arg) to cwd
            "ls" -> {
                val target = if (arg.isBlank()) cwd else File(cwd, arg)
                (target.listFiles()?.sortedBy { it.name }?.map {
                    (if (it.isDirectory) "▸ " else "  ") + it.name
                } ?: listOf("error: cannot list " + target.path)) to cwd
            }
            "cd" -> {
                val target = when (arg) {
                    "", "~" -> home
                    else -> if (arg.startsWith("/")) File(arg) else File(cwd, arg)
                }.canonicalFile
                if (target.isDirectory) listOf(target.absolutePath) to target
                else listOf("error: directory not found: " + arg) to cwd
            }
            "mkdir" -> {
                if (arg.isBlank()) listOf("error: usage: mkdir NAME") to cwd
                else {
                    val dir = File(cwd, arg)
                    listOf(if (dir.mkdirs() || dir.isDirectory) "created: " + dir.name else "error: could not create directory") to cwd
                }
            }
            "cat" -> {
                val file = File(cwd, arg)
                (if (file.isFile) file.readLines().ifEmpty { listOf("") } else listOf("error: file not found: " + arg)) to cwd
            }
            else -> {
                val process = ProcessBuilder("/system/bin/sh", "-c", command)
                    .directory(cwd).redirectErrorStream(true).start()
                val lines = process.inputStream.bufferedReader().use { it.readLines() }
                val code = process.waitFor()
                (lines + if (code != 0) listOf("exit code: " + code) else emptyList()) to cwd
            }
        }
    } catch (error: Exception) {
        listOf("error: " + (error.message ?: "command failed")) to cwd
    }
}

@Composable
private fun AmbienceControls(blur: Float, onBlur: (Float) -> Unit, shade: Float, onShade: (Float) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color(0x99101C2C))
            .border(1.dp, Color(0x44D7EDFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, null, tint = Cyan, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("AMBIENCE", color = Color.White, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("JELLYFISH LOOP", color = Color(0xFF9FC8D8), fontSize = 9.sp, letterSpacing = 1.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Blur", color = Muted, fontSize = 10.sp, modifier = Modifier.width(42.dp))
            Slider(value = blur, onValueChange = onBlur, valueRange = 0f..32f, modifier = Modifier.weight(1f))
            Text(blur.toInt().toString() + "dp", color = Color.White, fontSize = 10.sp, modifier = Modifier.width(38.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Shade", color = Muted, fontSize = 10.sp, modifier = Modifier.width(42.dp))
            Slider(value = shade, onValueChange = onShade, valueRange = 0.15f..0.82f, modifier = Modifier.weight(1f))
            Text((shade * 100).toInt().toString() + "%", color = Color.White, fontSize = 10.sp, modifier = Modifier.width(38.dp))
        }
    }
}
