package com.nexus.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF080C0B)
private val Panel = Color(0xFF101916)
private val Neon = Color(0xFF39FF88)
private val Purple = Color(0xFFAB70FF)
private val Muted = Color(0xFF82918A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexusDashboard()
        }
    }
}

@Composable
fun NexusDashboard() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = Panel,
            primary = Neon,
            secondary = Purple
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "NEXUS",
                            color = Neon,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp
                        )
                        Text(
                            "TERMINAL ENVIRONMENT",
                            color = Muted,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                    }

                    Text(
                        "● ONLINE",
                        color = Neon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    "SYSTEM OVERVIEW",
                    color = Muted,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusCard(
                        "ENVIRONMENT",
                        "NOT READY",
                        Purple,
                        Modifier.weight(1f)
                    )
                    StatusCard(
                        "VERSION",
                        "1.0.0",
                        Neon,
                        Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusCard(
                        "PYTHON",
                        "NOT INSTALLED",
                        Muted,
                        Modifier.weight(1f)
                    )
                    StatusCard(
                        "NODE.JS",
                        "NOT INSTALLED",
                        Muted,
                        Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(25.dp))

                Text(
                    "COMMAND CENTER",
                    color = Neon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Color(0xFF254235),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = Panel
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "root@nexus:~",
                            color = Neon,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "NEXUS Terminal v1.0",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )

                        Text(
                            "Initializing command interface...",
                            color = Muted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "› Terminal engine: PENDING",
                            color = Purple,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )

                        Text(
                            "› Package manager: PENDING",
                            color = Purple,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )

                        Text(
                            "› GitHub connection: PENDING",
                            color = Purple,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(25.dp))

                Text(
                    "MODULES",
                    color = Muted,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(12.dp))

                ModuleItem("01", "TERMINAL", "Command execution", Neon)
                ModuleItem("02", "PACKAGES", "Library management", Purple)
                ModuleItem("03", "GITHUB", "Repository manager", Neon)
                ModuleItem("04", "FILE SYSTEM", "Files and directories", Purple)

                Spacer(Modifier.weight(1f))

                Text(
                    "NEXUS PROJECT  •  BUILD 001",
                    color = Muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(
            1.dp,
            Color(0xFF25362F),
            RoundedCornerShape(12.dp)
        ),
        colors = CardDefaults.cardColors(
            containerColor = Panel
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(
                title,
                color = Muted,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(9.dp))
            Text(
                value,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ModuleItem(
    number: String,
    title: String,
    description: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Panel, RoundedCornerShape(10.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            number,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(14.dp))

        Column {
            Text(
                title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                description,
                color = Muted,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.weight(1f))

        Text("›", color = accent, fontSize = 22.sp)
    }
}
