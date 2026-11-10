package com.nexus.terminal

import android.graphics.Paint
import android.graphics.Typeface
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.nexus.terminal.engine.NexusCore
import kotlinx.coroutines.delay
import java.io.File
import kotlin.random.Random

private val Black = Color(0xFF000000)
private val Green = Color(0xFF39FF88)
private val White = Color(0xFFE5F5EC)
private val Grey = Color(0xFF82918A)
private val Red = Color(0xFFFF5364)
private val DarkRed = Color(0xFF8B0018)

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

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        // Keep the status bar hidden, but allow the keyboard and navigation
        // area to appear normally when the user starts typing.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

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
        var animationFrame by remember { mutableIntStateOf(0) }

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
            // Fast, continuous falling-code intro.
            repeat(300) {
                animationFrame++
                delay(16)
            }

            phase = 1

            // Glitch transition.
            repeat(26) {
                animationFrame++

                if (it % 6 == 0) {
                    try {
                        tone?.startTone(
                            ToneGenerator.TONE_PROP_BEEP,
                            35
                        )
                    } catch (_: Exception) {
                    }
                }

                delay(55)
            }

            delay(350)
            phase = 2
        }

        when (phase) {
            0 -> MatrixRainScreen(animationFrame)

            1 -> NexusGlitchScreen(animationFrame)

            else -> TerminalScreen(core)
        }
    }
}

@Composable
private fun MatrixRainScreen(frame: Int) {
    val symbols = "01ABCDEFGHIJKLMNOPQRSTUVWXYZ#$%&<>[]{}+-=*/"

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textSize = 15f
        }

        val columnWidth = 19f
        val lineHeight = 19f
        val columns = (size.width / columnWidth).toInt() + 1

        for (column in 0 until columns) {
            val x = column * columnWidth

            val speed = 3 + (column % 6)
            val offset = (column * 113) % (size.height.toInt() + 220)
            val headY = (
                (frame * speed + offset) %
                    (size.height.toInt() + 220)
                ).toFloat()

            for (row in 0..11) {
                val y = headY - row * lineHeight

                if (y < 0f || y > size.height + 20f) continue

                val seed = frame / 2 + column * 17 + row * 31
                val character = symbols[
                    (seed + column + row) % symbols.length
                ]

                val alpha = (255 - row * 19).coerceIn(25, 255)

                paint.color = if (row == 0) {
                    android.graphics.Color.argb(255, 220, 255, 230)
                } else {
                    android.graphics.Color.argb(alpha, 0, 210, 95)
                }

                canvas.drawText(
                    character.toString(),
                    x,
                    y,
                    paint
                )
            }
        }

        // Subtle dark overlay gives the intro a deeper terminal look.
        paint.color = android.graphics.Color.argb(35, 0, 0, 0)
        for (y in 0..size.height.toInt() step 4) {
            canvas.drawRect(
                0f,
                y.toFloat(),
                size.width,
                y + 1f,
                paint
            )
        }
    }
}

@Composable
private fun NexusGlitchScreen(frame: Int) {
    val fade by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "nexusFade"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .alpha(fade)
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(
                Typeface.MONOSPACE,
                Typeface.BOLD
            )
            textSize = 57f
            textAlign = Paint.Align.CENTER
        }

        val glitch = frame % 4
        val shift = if (glitch % 2 == 0) 8f else -8f

        // Red and cyan displaced layers.
        paint.color = android.graphics.Color.rgb(0, 110, 125)
        canvas.drawText(
            "NEXUS",
            centerX - shift,
            centerY,
            paint
        )

        paint.color = android.graphics.Color.rgb(150, 0, 25)
        canvas.drawText(
            "NEXUS",
            centerX + shift,
            centerY,
            paint
        )

        // Main dark-red title.
        paint.color = android.graphics.Color.rgb(139, 0, 24)
        canvas.drawText(
            "NEXUS",
            centerX,
            centerY,
            paint
        )

        // Moving horizontal glitch slices.
        for (slice in 0..5) {
            val sliceY = centerY - 42f + slice * 15f
            val offset = if ((frame + slice) % 2 == 0) {
                13f
            } else {
                -11f
            }

            canvas.save()
            canvas.clipRect(
                0f,
                sliceY,
                size.width,
                sliceY + 7f
            )

            paint.color = android.graphics.Color.rgb(190, 0, 35)
            canvas.drawText(
                "NEXUS",
                centerX + offset,
                centerY,
                paint
            )
            canvas.restore()
        }

        // Random horizontal interference.
        val random = Random(frame)
        paint.color = android.graphics.Color.rgb(115, 0, 25)

        repeat(15) {
            val y = random.nextFloat() * size.height
            val x = random.nextFloat() * size.width
            val width = random.nextFloat() * 100f + 15f

            canvas.drawRect(
                x,
                y,
                (x + width).coerceAtMost(size.width),
                y + 2f,
                paint
            )
        }

        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        paint.color = android.graphics.Color.rgb(110, 0, 25)

        canvas.drawText(
            "SYSTEM INITIALIZING",
            centerX,
            centerY + 62f,
            paint
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

        history.add(
            "NEXUS@android:${formatPromptPath(currentDir, core.homeDirectory.absolutePath)}$ $command"
        )

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
            // Shrink the terminal area when the keyboard appears.
            .imePadding()
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
                        line.startsWith("NEXUS TERMINAL") -> DarkRed
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
                    vertical = 10.dp
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
                    fontSize = 14.sp,
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
