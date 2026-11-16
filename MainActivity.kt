package ai.axel.app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

enum class AxelMood(val emoji: String, val label: String) {
    HAPPY("😊", "Happy"),
    SAD("😔", "Gentle"),
    CONFIDENT("😎", "Confident"),
    CURIOUS("🤔", "Curious"),
    FRUSTRATED("😤", "Frustrated"),
    CALM("😌", "Calm")
}

data class ChatMessage(val text: String, val fromAxel: Boolean, val mood: AxelMood? = null)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            AxelChat(
                speak = { message -> if (ttsReady) tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "axel_reply") }
            )
        }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.getDefault()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

private fun chooseMood(input: String): AxelMood {
    val s = input.lowercase()
    return when {
        listOf("sad", "upset", "lonely", "hard day", "stressed").any { it in s } -> AxelMood.SAD
        listOf("error", "broken", "doesn't work", "failed").any { it in s } -> AxelMood.FRUSTRATED
        listOf("why", "how", "what if", "curious").any { it in s } -> AxelMood.CURIOUS
        listOf("great", "awesome", "won", "finished", "thank").any { it in s } -> AxelMood.HAPPY
        listOf("help", "urgent", "problem").any { it in s } -> AxelMood.CALM
        else -> AxelMood.CONFIDENT
    }
}

private fun starterReply(input: String, mood: AxelMood): String {
    val s = input.lowercase()
    return when (mood) {
        AxelMood.SAD -> "That sounds difficult. I'm here to help you think it through. What's the hardest part right now?"
        AxelMood.FRUSTRATED -> "Okay, that issue is getting in our way. Let's break it down and troubleshoot one step at a time."
        AxelMood.CURIOUS -> "Interesting question. Let's explore it carefully and separate what we know from what we're still figuring out."
        AxelMood.HAPPY -> "That's brilliant! Nice progress. What's the next thing you want to tackle?"
        AxelMood.CALM -> "Let's take this one step at a time. Tell me what's happening, and we'll work through it."
        AxelMood.CONFIDENT -> if (s.isBlank()) "I'm AXEL. Ready when you are." else
            "I'm listening. I can help with that, but this starter build doesn't have its offline language model connected yet."
    }
}

@Composable
fun AxelChat(speak: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf(AxelMood.CALM) }
    val messages = remember {
        mutableStateListOf(ChatMessage("Hey! I'm AXEL AI. My expressive personality is online. Let's build something great.", true, AxelMood.HAPPY))
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF72B7FF),
        background = Color(0xFF0B1020),
        surface = Color(0xFF151D31)
    )) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFF0B1020)).padding(16.dp)
        ) {
            Text("AXEL AI", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("${mood.emoji} ${mood.label} mood", color = Color(0xFF9EB5D8))
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(messages) { msg ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement =
                        if (msg.fromAxel) Arrangement.Start else Arrangement.End) {
                        Surface(
                            color = if (msg.fromAxel) Color(0xFF192943) else Color(0xFF254B75),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(12.dp).widthIn(max = 290.dp)) {
                                if (msg.fromAxel) Text("AXEL ${msg.mood?.emoji.orEmpty()}", color = Color(0xFF9BCBFF))
                                Text(msg.text, color = Color.White)
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message AXEL…") },
                    singleLine = true
                )
                IconButton(onClick = {
                    val sent = input.trim()
                    if (sent.isNotEmpty()) {
                        messages.add(ChatMessage(sent, false))
                        mood = chooseMood(sent)
                        val reply = starterReply(sent, mood)
                        messages.add(ChatMessage(reply, true, mood))
                        speak(reply)
                        input = ""
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF72B7FF))
                }
            }
            Text("Prototype: rule-based replies + Android text-to-speech. Offline LLM and speech input come next.",
                color = Color(0xFF8293AF), style = MaterialTheme.typography.bodySmall)
        }
    }
}
