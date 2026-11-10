package com.nexus.terminal

import java.io.File

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.nexus.terminal.engine.NexusCore
import kotlinx.coroutines.delay

private val Black = Color(0xFF000000)
private val Green = Color(0xFF39FF88)
private val Purple = Color(0xFFAB70FF)
private val White = Color(0xFFE5F5EC)
private val Grey = Color(0xFF82918A)
private val Red = Color(0xFFFF5364)

private fun formatPromptPath(path: String, homePath: String): String {
    val home = File(homePath).canonicalFile
    val current = File(path).canonicalFile
    return when {
        current.path == home.path -> "~"
        current.path.startsWith(home.path + File.separator) ->
            "~/" + current.path.removePrefix(home.path + File.separator)
        else -> current.path
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var nexusCore: NexusCore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )

        nexusCore = lastCustomNonConfigurationInstance as? NexusCore
            ?: NexusCore(filesDir)

        setContent {
            NexusApp(nexusCore)
        }
    }

    override fun onRetainCustomNonConfigurationInstance(): Any {
        return nexusCore
    }

    override fun onDestroy() {
        if (isFinishing && ::nexusCore.isInitialized) {
            nexusCore.shutdown()
        }
        super.onDestroy()
    }
}

@Composable
private fun NexusApp(core: NexusCore) {
    MaterialTheme(colorScheme = darkColorScheme()) {

        var phase by remember { mutableIntStateOf(0) }
        var visibleLines by remember { mutableIntStateOf(0) }
        var glitchTick by remember { mutableIntStateOf(0) }

        val bootLines = listOf(
            "[ OK ] Starting NEXUS core",
            "[ OK ] Checking Android environment",
            "[ OK ] Loading terminal interface",
            "[ OK ] Preparing private workspace",
            "[ OK ] Initializing command engine",
            "[ OK ] Loading system components",
            "[ OK ] Establishing local session",
            "[ OK ] NEXUS kernel interface ready"
        )

        val tone = remember {
            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 35)
            } catch (_: Exception) {
                null
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    tone?.release()
                } catch (_: Exception) {
                }
            }
        }

        LaunchedEffect(Unit) {
            delay(5000)

            phase = 1

            bootLines.forEachIndexed { index, _ ->
                visibleLines = index + 1

                try {
                    tone?.startTone(
                        ToneGenerator.TONE_PROP_BEEP,
                        45
                    )
                } catch (_: Exception) {
                }

                delay(230)
            }

            delay(450)
            phase = 2

            repeat(12) {
                glitchTick++
                delay(75)
            }

            delay(850)
            phase = 3
        }

        when (phase) {
            0 -> Box(
                Modifier
                    .fillMaxSize()
                    .background(Black)
            )

            1 -> BootScreen(bootLines.take(visibleLines))

            2 -> {
                val alpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(250),
                    label = "glitch"
                )

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "NEXUS",
                        color = Green,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .offset(
                                x = if (glitchTick % 2 == 0) {
                                    (-3).dp
                                } else {
                                    3.dp
                                }
                            )
                            .alpha(alpha)
                    )

                    Text(
                        "SYSTEM INITIALIZING",
                        color = Purple,
                        fontSize = 9.sp,
                        letterSpacing = 3.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 48.dp)
                            .alpha(alpha)
                    )
                }
            }

            else -> TerminalScreen(core)
        }
    }
}

@Composable
private fun BootScreen(lines: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(18.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            "NEXUS BOOT SEQUENCE",
            color = Purple,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(Modifier.height(14.dp))

        lines.forEachIndexed { index, line ->
            Text(
                line,
                color = if (index == lines.lastIndex) Green else Grey,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "________________________",
            color = Green.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TerminalScreen(core: NexusCore) {

    var input by remember { mutableStateOf("") }
    var currentDir by remember {
        mutableStateOf(
            core.getActiveSession()?.engine?.currentDirectory
                ?: core.homeDirectory.absolutePath
        )
    }

    val history = remember {
        mutableStateListOf(
            "NEXUS TERMINAL",
            "NEXUS local command engine",
            "Type 'help' to see available commands.",
            ""
        )
    }

    val scroll = rememberScrollState()

    fun submit() {
        val command = input.trim()
        if (command.isEmpty()) return

        history.add("NEXUS@android:${formatPromptPath(currentDir, core.homeDirectory.absolutePath)}$ $command")
        input = ""

        try {
            val result = core.execute(command)

            currentDir = result.currentDirectory

            if (result.clearScreen) {
                history.clear()
                return
            }

            history.addAll(result.output)
        } catch (error: Exception) {
            history.add(
                "ERROR: ${error.message ?: "Command failed."}"
            )
        }
    }

    LaunchedEffect(history.size) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "NEXUS",
                color = Green,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.width(10.dp))

            Text(
                "LOCAL SHELL",
                color = Grey,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.weight(1f))

            Text(
                "● READY",
                color = Green,
                fontSize = 9.sp
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF183326))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(
                    horizontal = 13.dp,
                    vertical = 12.dp
                )
        ) {
            history.forEach { line ->
                Text(
                    text = line,
                    color = when {
                        line.startsWith("NEXUS@") -> Green
                        line.startsWith("ERROR:") -> Red
                        line.startsWith("NEXUS TERMINAL") -> Purple
                        else -> White
                    },
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF183326))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${formatPromptPath(currentDir, core.homeDirectory.absolutePath)} $ ",
                color = Green,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(Green),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submit() }
                )
            )
        }
    }
}
