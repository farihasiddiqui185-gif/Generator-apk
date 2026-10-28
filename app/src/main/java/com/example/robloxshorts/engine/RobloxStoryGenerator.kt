package com.example.robloxshorts.engine

import android.content.Context
import android.util.Log
import com.example.robloxshorts.data.model.RobloxScene
import com.example.robloxshorts.data.model.RobloxScript
import com.example.robloxshorts.data.model.ShortsCategory
import com.example.robloxshorts.data.model.SubtitleWord
import com.example.robloxshorts.data.model.VideoDuration
import com.example.robloxshorts.data.model.VideoLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class RobloxStoryGenerator(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun generateScript(
        category: ShortsCategory,
        duration: VideoDuration,
        language: VideoLanguage,
        customPrompt: String,
        apiKey: String?
    ): RobloxScript = withContext(Dispatchers.IO) {
        val totalSec = duration.seconds
        val sceneCount = when (duration) {
            VideoDuration.SEC_15 -> 3
            VideoDuration.SEC_30 -> 4
            VideoDuration.SEC_45 -> 5
            VideoDuration.SEC_60 -> 6
        }

        if (!apiKey.isNullOrBlank()) {
            try {
                val geminiScript = callGeminiForScript(
                    category = category,
                    sceneCount = sceneCount,
                    totalSec = totalSec,
                    language = language,
                    customPrompt = customPrompt,
                    apiKey = apiKey
                )
                if (geminiScript != null && geminiScript.scenes.isNotEmpty()) {
                    return@withContext geminiScript
                }
            } catch (e: Exception) {
                Log.w("RobloxStoryGen", "Gemini API call failed, falling back to procedural engine: ${e.message}")
            }
        }

        // Procedural generation fallback: guaranteed high viral potential & rich scene data
        return@withContext generateProceduralScript(category, duration, language, customPrompt)
    }

    private fun callGeminiForScript(
        category: ShortsCategory,
        sceneCount: Int,
        totalSec: Int,
        language: VideoLanguage,
        customPrompt: String,
        apiKey: String
    ): RobloxScript? {
        val prompt = """
            You are a viral Roblox YouTube Shorts creator. Create an intense, high-retention vertical Roblox Short script.
            Category: ${category.title} (${category.subtitle})
            Target Duration: $totalSec seconds.
            Number of Scenes: $sceneCount.
            Language: ${language.displayName}.
            Additional Theme Idea: $customPrompt.
            
            Return ONLY a valid JSON object matching this schema:
            {
              "title": "Short catchy title with emojis",
              "hook": "First 2-second hook line",
              "musicStyle": "SPOOKY_SYNTH or UPBEAT_GAMING or DRAMATIC_PIANO",
              "scenes": [
                {
                  "sceneNumber": 1,
                  "title": "Scene short title",
                  "characterType": "BACON_HAIR or PRO_GAMER or ENTITY_666 or DOMINUS_RICH or STUDENT_GIRL",
                  "characterExpression": "SHOCKED or LAUGHING or CRYING or ANGRY or COOL or TERRIFIED",
                  "environment": "SPOOKY_CORRIDOR or NEON_CITY or LAVA_OBBY or ROBLOX_SCHOOL or SURVIVAL_ISLAND or VAULT_ROOM",
                  "cameraMotion": "ZOOM_IN or PAN_RIGHT or SCREEN_SHAKE or ORBIT",
                  "narration": "Energetic narrator voiceover line for this scene (15-20 words max)",
                  "characterDialogue": "What the character screams or says",
                  "soundFx": "OOF or JUMPSCARE_BOOM or DRAMATIC_RISER or WHOOSH or CHA_CHING"
                }
              ]
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.9)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val respString = response.body?.string() ?: return null
            val root = JSONObject(respString)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            val rawText = parts.getJSONObject(0).optString("text")

            val scriptObj = JSONObject(rawText)
            val title = scriptObj.optString("title", "${category.title} Short")
            val hook = scriptObj.optString("hook", "Wait till the end!")
            val musicStyle = scriptObj.optString("musicStyle", "UPBEAT_GAMING")
            val scenesArray = scriptObj.optJSONArray("scenes") ?: return null

            val scenes = mutableListOf<RobloxScene>()
            val secPerScene = totalSec.toFloat() / scenesArray.length()

            for (i in 0 until scenesArray.length()) {
                val sObj = scenesArray.getJSONObject(i)
                val narration = sObj.optString("narration", "")
                val words = narration.split(" ").filter { it.isNotBlank() }
                val subWords = words.mapIndexed { idx, w ->
                    val wordStart = (idx.toFloat() / words.size) * secPerScene
                    val wordEnd = ((idx + 1).toFloat() / words.size) * secPerScene
                    SubtitleWord(
                        word = w,
                        startSec = wordStart,
                        endSec = wordEnd,
                        isHighlighted = idx % 2 == 0
                    )
                }

                scenes.add(
                    RobloxScene(
                        sceneNumber = i + 1,
                        durationSec = secPerScene,
                        title = sObj.optString("title", "Scene ${i + 1}"),
                        characterType = sObj.optString("characterType", "BACON_HAIR"),
                        characterExpression = sObj.optString("characterExpression", "SHOCKED"),
                        environment = sObj.optString("environment", "NEON_CITY"),
                        cameraMotion = sObj.optString("cameraMotion", "ZOOM_IN"),
                        narration = narration,
                        characterDialogue = sObj.optString("characterDialogue").takeIf { it.isNotBlank() },
                        soundFx = sObj.optString("soundFx").takeIf { it.isNotBlank() },
                        subtitles = subWords
                    )
                )
            }

            return RobloxScript(
                title = title,
                category = category.title,
                language = language.code,
                hook = hook,
                scenes = scenes,
                musicStyle = musicStyle,
                estimatedDurationSec = totalSec
            )
        }
    }

    private fun generateProceduralScript(
        category: ShortsCategory,
        duration: VideoDuration,
        language: VideoLanguage,
        customPrompt: String
    ): RobloxScript {
        val totalSec = duration.seconds
        val sceneCount = when (duration) {
            VideoDuration.SEC_15 -> 3
            VideoDuration.SEC_30 -> 4
            VideoDuration.SEC_45 -> 5
            VideoDuration.SEC_60 -> 6
        }
        val secPerScene = totalSec.toFloat() / sceneCount

        val storyTemplate = StoryCatalogue.pickRandomStory(category)
        val title = if (customPrompt.isNotBlank()) {
            "Roblox: $customPrompt"
        } else {
            storyTemplate.title
        }

        val scenes = mutableListOf<RobloxScene>()
        for (i in 0 until sceneCount) {
            val templateScene = storyTemplate.scenes.getOrNull(i) ?: storyTemplate.scenes.last()
            val narrationText = localizeNarration(templateScene.narration, language)
            val dialogueText = templateScene.dialogue?.let { localizeNarration(it, language) }

            val words = narrationText.split(" ").filter { it.isNotBlank() }
            val subWords = words.mapIndexed { idx, w ->
                val start = (idx.toFloat() / words.size.coerceAtLeast(1)) * secPerScene
                val end = ((idx + 1).toFloat() / words.size.coerceAtLeast(1)) * secPerScene
                SubtitleWord(
                    word = w.uppercase(),
                    startSec = start,
                    endSec = end,
                    isHighlighted = (idx % 3 == 0)
                )
            }

            scenes.add(
                RobloxScene(
                    sceneNumber = i + 1,
                    durationSec = secPerScene,
                    title = templateScene.title,
                    characterType = templateScene.characterType,
                    characterExpression = templateScene.expression,
                    environment = templateScene.environment,
                    cameraMotion = templateScene.cameraMotion,
                    narration = narrationText,
                    characterDialogue = dialogueText,
                    soundFx = templateScene.soundFx,
                    subtitles = subWords
                )
            )
        }

        return RobloxScript(
            title = title,
            category = category.title,
            language = language.code,
            hook = storyTemplate.hook,
            scenes = scenes,
            musicStyle = storyTemplate.musicStyle,
            estimatedDurationSec = totalSec
        )
    }

    private fun localizeNarration(text: String, language: VideoLanguage): String {
        // Provide thematic flavor when Hindi or Spanish are picked
        return when (language) {
            VideoLanguage.HI -> {
                when {
                    text.contains("Bacon", ignoreCase = true) -> "Bacon Hair ne sabhi ko hairaan kar diya! Dekho aage kya hua..."
                    text.contains("Doors", ignoreCase = true) || text.contains("monster", ignoreCase = true) -> "Raat ke 3 baje darwaza khula aur saamne dangerous monster khada tha!"
                    text.contains("Robux", ignoreCase = true) -> "0 Robux se seedha 100 Million Robux ka glitch mil gaya!"
                    else -> "Roblox ki sabse shocking kahani! Aage kya hua dekhkar aap dhang reh jaoge!"
                }
            }
            VideoLanguage.ES -> {
                when {
                    text.contains("Bacon", ignoreCase = true) -> "¡El Bacon Hair hizo lo imposible y dejó a todos en shock!"
                    text.contains("monster", ignoreCase = true) -> "¡A las 3:00 AM apareció la entidad prohibida en Roblox!"
                    text.contains("Robux", ignoreCase = true) -> "¡De 0 Robux a multimillonario gracias a este secreto!"
                    else -> "¡La historia más épica de Roblox que jamás viste!"
                }
            }
            else -> text
        }
    }
}

// Rich pre-authored story seeds with viral YouTube Shorts formulas
object StoryCatalogue {
    data class SceneTemplate(
        val title: String,
        val characterType: String,
        val expression: String,
        val environment: String,
        val cameraMotion: String,
        val narration: String,
        val dialogue: String?,
        val soundFx: String?
    )

    data class StoryData(
        val title: String,
        val hook: String,
        val musicStyle: String,
        val scenes: List<SceneTemplate>
    )

    fun pickRandomStory(category: ShortsCategory): StoryData {
        val stories = when (category) {
            ShortsCategory.HORROR -> listOf(
                StoryData(
                    title = "NEVER Play Roblox Doors at 3:00 AM! 😱",
                    hook = "I found Door 101 and what was inside still haunts me...",
                    musicStyle = "SPOOKY_SYNTH",
                    scenes = listOf(
                        SceneTemplate("The Forbidden Elevator", "PRO_GAMER", "TERRIFIED", "SPOOKY_CORRIDOR", "ZOOM_IN", "I entered the secret service elevator in Roblox Doors past midnight.", "Wait... the lights are flickering?!", "WHOOSH"),
                        SceneTemplate("The Shadow Figure", "ENTITY_666", "ANGRY", "SPOOKY_CORRIDOR", "SCREEN_SHAKE", "Suddenly a 10-foot tall red glitched beast stepped into the hallway!", "DON'T LOOK BEHIND YOU!", "JUMPSCARE_BOOM"),
                        SceneTemplate("The Ultimate Chase", "BACON_HAIR", "TERRIFIED", "SPOOKY_CORRIDOR", "PAN_RIGHT", "My stamina bar reached ZERO as the monster sprinted directly towards us!", "I can't run any faster!!", "DRAMATIC_RISER"),
                        SceneTemplate("The Closet Hideout", "PRO_GAMER", "SHOCKED", "SPOOKY_CORRIDOR", "ZOOM_IN", "I dove into the closet, but the heartbeat minigame was moving at 10x speed!", "PLEASE DON'T HEAR ME!!", "OOF"),
                        SceneTemplate("The Shocking Truth", "BACON_HAIR", "COOL", "VAULT_ROOM", "ORBIT", "The beast walked past and handed me the legendary golden skeleton key!", "He was just trying to help?!", "CHA_CHING"),
                        SceneTemplate("Subscribers Saved Me", "PRO_GAMER", "LAUGHING", "NEON_CITY", "ZOOM_IN", "Subscribe right now or this entity will join your next server!", "LIKE AND SUBSCRIBE!", "CHIME")
                    )
                ),
                StoryData(
                    title = "The Glitched Guest 666 Mystery 👁️",
                    hook = "Everyone told me Guest 666 was deleted... they were WRONG.",
                    musicStyle = "SPOOKY_SYNTH",
                    scenes = listOf(
                        SceneTemplate("Empty Server", "BACON_HAIR", "TERRIFIED", "SPOOKY_CORRIDOR", "ZOOM_IN", "I joined an abandoned 2008 Roblox baseplate at 3:33 AM.", "Why is my avatar turning grey?", "WHOOSH"),
                        SceneTemplate("Message in Chat", "ENTITY_666", "ANGRY", "SPOOKY_CORRIDOR", "SCREEN_SHAKE", "A player with no username joined and typed coordinates to my room!", "RUN BEFORE MIDNIGHT", "JUMPSCARE_BOOM"),
                        SceneTemplate("Red Sky Event", "PRO_GAMER", "SHOCKED", "LAVA_OBBY", "PAN_RIGHT", "The entire sky turned crimson red and the gravity inverted!", "MY CHARACTER IS FLOATING!!", "DRAMATIC_RISER"),
                        SceneTemplate("The Safe Zone", "BACON_HAIR", "COOL", "VAULT_ROOM", "ORBIT", "I solved the block puzzle just in time before the server crashed!", "We actually survived!", "CHIME")
                    )
                )
            )
            ShortsCategory.RICH_VS_POOR -> listOf(
                StoryData(
                    title = "0 ROBUX Noob vs 100M Robux Dominus Lord! 💎",
                    hook = "He laughed at my Bacon Hair until I showed him this trade...",
                    musicStyle = "UPBEAT_GAMING",
                    scenes = listOf(
                        SceneTemplate("The Flex Plaza", "DOMINUS_RICH", "COOL", "NEON_CITY", "ZOOM_IN", "This billionaire was showing off every limited item in Trade Tower.", "Get out of here broke noob!", "WHOOSH"),
                        SceneTemplate("The Secret Glitch", "BACON_HAIR", "SHOCKED", "VAULT_ROOM", "PAN_RIGHT", "I noticed an unindexed ancient crate behind the golden fountain.", "Wait, is this from 2007?!", "CHIME"),
                        SceneTemplate("The Mega Unboxing", "BACON_HAIR", "LAUGHING", "VAULT_ROOM", "SCREEN_SHAKE", "I unlocked the ultra-rare Sparkle Time Dominus with 1 in 10 million odds!", "LET'S GOOOOO!!", "CHA_CHING"),
                        SceneTemplate("The Begging Billionaire", "DOMINUS_RICH", "CRYING", "NEON_CITY", "ZOOM_IN", "Now he's begging on his knees offering all his Robux for my hat!", "Never judge a player by their skin!", "CHIME")
                    )
                )
            )
            ShortsCategory.FUNNY -> listOf(
                StoryData(
                    title = "Troll Admin with Infinite Jump Fails! 😂",
                    hook = "I gave a Bacon Hair admin commands and instantly regretted it...",
                    musicStyle = "UPBEAT_GAMING",
                    scenes = listOf(
                        SceneTemplate("Admin Granted", "BACON_HAIR", "LAUGHING", "LAVA_OBBY", "ZOOM_IN", "I accidentally gave owner permissions to the newest player in the game.", "HEHEHE watch this!", "WHOOSH"),
                        SceneTemplate("Fling Command Chaos", "PRO_GAMER", "SHOCKED", "LAVA_OBBY", "SCREEN_SHAKE", "He pressed the mega fling button and launched 50 players into outer space!", "WHY AM I IN ORBIT?!", "OOF"),
                        SceneTemplate("The Giant Bacon", "BACON_HAIR", "ANGRY", "NEON_CITY", "ORBIT", "Then he resized himself to 500 feet tall and started dancing on the towers!", "I AM THE ROBLOX GOD!", "DRAMATIC_RISER"),
                        SceneTemplate("Self Destruction", "BACON_HAIR", "TERRIFIED", "LAVA_OBBY", "ZOOM_IN", "He stepped on his own troll landmine and reset character with the classic sound!", "OOF!!", "OOF")
                    )
                )
            )
            ShortsCategory.SCHOOL -> listOf(
                StoryData(
                    title = "Escaping Mr. Stinky's Crazy School! 🏫",
                    hook = "Our math teacher locked the gates with 99 lava traps!",
                    musicStyle = "UPBEAT_GAMING",
                    scenes = listOf(
                        SceneTemplate("Detention Lockdown", "STUDENT_GIRL", "SHOCKED", "ROBLOX_SCHOOL", "ZOOM_IN", "The bell rang but the classroom door morphed into a steel laser gate!", "We have to escape right now!", "WHOOSH"),
                        SceneTemplate("The Cafeteria Lava", "BACON_HAIR", "TERRIFIED", "ROBLOX_SCHOOL", "SCREEN_SHAKE", "The lunch room floor turned into boiling nacho cheese lava!", "Jump on the pizza tables!", "DRAMATIC_RISER"),
                        SceneTemplate("Vent Parkour", "PRO_GAMER", "COOL", "ROBLOX_SCHOOL", "PAN_RIGHT", "We crawled through the HVAC system dodging angry janitor drones!", "Almost at the exit vent!", "CHIME"),
                        SceneTemplate("The Freedom Glider", "STUDENT_GIRL", "LAUGHING", "NEON_CITY", "ORBIT", "We hijacked the yellow school bus rocket and flew out of the stadium!", "SCHOOL IS OVER FOREVER!", "CHA_CHING")
                    )
                )
            )
            ShortsCategory.SURVIVAL -> listOf(
                StoryData(
                    title = "Surviving 99 Days on Roblox Disaster Island! ⚡",
                    hook = "Meteor shower + Tsunami + Tornado all spawned at once!",
                    musicStyle = "DRAMATIC_PIANO",
                    scenes = listOf(
                        SceneTemplate("Disaster Warning", "PRO_GAMER", "SHOCKED", "SURVIVAL_ISLAND", "ZOOM_IN", "The siren flashed triple red: Meteor Storm and Mega Flood simultaneously!", "EVERYONE GET TO HIGH GROUND!", "WHOOSH"),
                        SceneTemplate("The Falling Lighthouse", "BACON_HAIR", "TERRIFIED", "SURVIVAL_ISLAND", "SCREEN_SHAKE", "A giant meteor cleaved our tower in half, sending 20 players flying!", "HOLD ONTO THE LADDER!", "JUMPSCARE_BOOM"),
                        SceneTemplate("The Last Platform", "PRO_GAMER", "COOL", "SURVIVAL_ISLAND", "PAN_RIGHT", "Water level rose to the top brick with only 5 seconds left on the clock!", "DON'T LET GO!", "DRAMATIC_RISER"),
                        SceneTemplate("Victory Badge", "BACON_HAIR", "LAUGHING", "SURVIVAL_ISLAND", "ORBIT", "The timer hit zero and we unlocked the Impossible Survivor halo!", "We actually survived 99 days!", "CHIME")
                    )
                )
            )
            ShortsCategory.PLOT_TWIST -> listOf(
                StoryData(
                    title = "My Best Friend was the Secret Impostor! 😱",
                    hook = "I gave him all my legendary pets... then I checked his profile.",
                    musicStyle = "DRAMATIC_PIANO",
                    scenes = listOf(
                        SceneTemplate("The Duo Trust", "PRO_GAMER", "COOL", "NEON_CITY", "ZOOM_IN", "We played every day for 2 years and built the ultimate Roblox clan.", "You're like a brother to me, bro!", "CHIME"),
                        SceneTemplate("The Big Borrow", "BACON_HAIR", "SHOCKED", "VAULT_ROOM", "PAN_RIGHT", "He asked to borrow my Rainbow Huge Cat pet just for a screenshot.", "I promise I'll trade it right back!", "WHOOSH"),
                        SceneTemplate("The Disconnect", "PRO_GAMER", "CRYING", "NEON_CITY", "SCREEN_SHAKE", "He immediately left the server and blocked me everywhere!", "WHY WOULD HE DO THIS?!", "DRAMATIC_RISER"),
                        SceneTemplate("The Ultimate Twist", "ENTITY_666", "COOL", "VAULT_ROOM", "ORBIT", "I opened my mailbox: he sent 10 Titanic Pets and was testing my loyalty!", "HE GAVE ME 10 TITANICS?!", "CHA_CHING")
                    )
                )
            )
            ShortsCategory.EMOTIONAL -> listOf(
                StoryData(
                    title = "The Abandoned Noob Who Saved Everyone 🥺",
                    hook = "Nobody wanted to team up with the 0 Robux player until...",
                    musicStyle = "DRAMATIC_PIANO",
                    scenes = listOf(
                        SceneTemplate("Left Behind", "BACON_HAIR", "CRYING", "SPOOKY_CORRIDOR", "ZOOM_IN", "All the rich players laughed and locked him out of the safe room.", "Please... the monster is coming...", "WHOOSH"),
                        SceneTemplate("The Generator Puzzle", "BACON_HAIR", "COOL", "VAULT_ROOM", "PAN_RIGHT", "Instead of giving up, he found the emergency power breaker in the basement.", "I can fix this for everyone!", "CHIME"),
                        SceneTemplate("The Hero's Sacrifice", "BACON_HAIR", "SHOCKED", "SPOOKY_CORRIDOR", "SCREEN_SHAKE", "He pulled the lever opening the bunker, trapping himself outside with the beast!", "GET INSIDE, I GOT THIS!", "DRAMATIC_RISER"),
                        SceneTemplate("The Crown of Respect", "DOMINUS_RICH", "COOL", "NEON_CITY", "ORBIT", "He outmaneuvered the monster and the server voted him Server Legend!", "True heroes wear Bacon Hair!", "CHIME")
                    )
                )
            )
            ShortsCategory.MYSTERY -> listOf(
                StoryData(
                    title = "The Secret Room in Brookhaven NO ONE Knows! 🗝️",
                    hook = "Type this hidden number code behind the hospital...",
                    musicStyle = "SPOOKY_SYNTH",
                    scenes = listOf(
                        SceneTemplate("The Hidden Wall", "PRO_GAMER", "SHOCKED", "ROBLOX_SCHOOL", "ZOOM_IN", "Behind the Brookhaven medical archives, there is a hollow wall panel.", "The texture is slightly different here...", "WHOOSH"),
                        SceneTemplate("The Ancient Terminal", "BACON_HAIR", "COOL", "VAULT_ROOM", "PAN_RIGHT", "Inside is a 1990s retro computer flashing developer commands.", "Inputting developer override...", "CHIME"),
                        SceneTemplate("The Portal Opens", "ENTITY_666", "TERRIFIED", "VAULT_ROOM", "SCREEN_SHAKE", "A purple neon portal opened pulling my avatar into a forgotten lobby!", "WHERE DOES THIS GO?!", "DRAMATIC_RISER"),
                        SceneTemplate("Unreleased Items", "PRO_GAMER", "LAUGHING", "VAULT_ROOM", "ORBIT", "The room was filled with unreleased test vehicles and flying carpets!", "LIKE FOR PART 2 TO GET THE CODE!", "CHA_CHING")
                    )
                )
            )
        }

        return stories[Random.nextInt(stories.size)]
    }
}
