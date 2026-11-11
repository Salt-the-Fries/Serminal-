package com.saltyfries.serminal

import android.os.Bundle
import android.view.TextureView
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    var blur by remember { mutableFloatStateOf(30f) }
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
            Header(blur, { blur = it }, shade, { shade = it })
            TerminalPanel(Modifier.weight(1f))
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
            TextureView(ctx).apply {
                // A TextureView stays in the normal view render tree, so Compose can blur it.
                player.setVideoTextureView(this)
            }
        },
        onRelease = { view -> player.clearVideoTextureView(view) },
        modifier = Modifier.fillMaxSize().blur(blur.dp).background(Night)
    )
}

@Composable
private fun Header(
    blur: Float,
    onBlur: (Float) -> Unit,
    shade: Float,
    onShade: (Float) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Serminal-", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("A little shell. A lot of possibility.", color = Muted, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color(0x4427D6A0))
                    .border(1.dp, Color(0x5537D9A5), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(Modifier.size(5.dp).background(Color(0xFF69E6B0), CircleShape))
                Text("LOCAL SESSION", color = Color(0xFFBDEFD8), fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
            }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "Open settings", tint = Color.White)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.width(280.dp).background(Color(0xF018263A))
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Text("BACKGROUND", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Blur", color = Muted, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                        Slider(value = blur, onValueChange = onBlur, valueRange = 0f..48f, modifier = Modifier.weight(1f))
                        Text("${blur.toInt()}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shade", color = Muted, fontSize = 12.sp, modifier = Modifier.width(48.dp))
                        Slider(value = shade, onValueChange = onShade, valueRange = 0.15f..0.82f, modifier = Modifier.weight(1f))
                        Text("${(shade * 100).toInt()}%", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(34.dp))
                    }
                }
            }
        }
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
