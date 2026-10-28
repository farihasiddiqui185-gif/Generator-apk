package com.example.robloxshorts.data.model

import org.json.JSONArray
import org.json.JSONObject

data class SubtitleWord(
    val word: String,
    val startSec: Float,
    val endSec: Float,
    val isHighlighted: Boolean = false
)

data class RobloxScene(
    val sceneNumber: Int,
    val durationSec: Float,
    val title: String,
    val characterType: String, // "BACON_HAIR", "PRO_GAMER", "ENTITY_666", "DOMINUS_RICH", "STUDENT_GIRL"
    val characterExpression: String, // "SHOCKED", "LAUGHING", "CRYING", "ANGRY", "COOL", "TERRIFIED"
    val environment: String, // "SPOOKY_CORRIDOR", "NEON_CITY", "LAVA_OBBY", "ROBLOX_SCHOOL", "SURVIVAL_ISLAND", "VAULT_ROOM"
    val cameraMotion: String, // "ZOOM_IN", "PAN_RIGHT", "SCREEN_SHAKE", "ORBIT"
    val narration: String,
    val characterDialogue: String?,
    val soundFx: String?, // "OOF", "JUMPSCARE_BOOM", "DRAMATIC_RISER", "WHOOSH", "CHA_CHING", "CHIME"
    val subtitles: List<SubtitleWord>
)

data class RobloxScript(
    val title: String,
    val category: String,
    val language: String,
    val hook: String,
    val scenes: List<RobloxScene>,
    val musicStyle: String, // "SPOOKY_SYNTH", "UPBEAT_GAMING", "DRAMATIC_PIANO", "CYBERPUNK"
    val estimatedDurationSec: Int
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("title", title)
        root.put("category", category)
        root.put("language", language)
        root.put("hook", hook)
        root.put("musicStyle", musicStyle)
        root.put("estimatedDurationSec", estimatedDurationSec)

        val scenesArray = JSONArray()
        scenes.forEach { scene ->
            val sceneObj = JSONObject()
            sceneObj.put("sceneNumber", scene.sceneNumber)
            sceneObj.put("durationSec", scene.durationSec.toDouble())
            sceneObj.put("title", scene.title)
            sceneObj.put("characterType", scene.characterType)
            sceneObj.put("characterExpression", scene.characterExpression)
            sceneObj.put("environment", scene.environment)
            sceneObj.put("cameraMotion", scene.cameraMotion)
            sceneObj.put("narration", scene.narration)
            sceneObj.put("characterDialogue", scene.characterDialogue ?: "")
            sceneObj.put("soundFx", scene.soundFx ?: "")

            val subsArray = JSONArray()
            scene.subtitles.forEach { sub ->
                val subObj = JSONObject()
                subObj.put("word", sub.word)
                subObj.put("startSec", sub.startSec.toDouble())
                subObj.put("endSec", sub.endSec.toDouble())
                subObj.put("isHighlighted", sub.isHighlighted)
                subsArray.put(subObj)
            }
            sceneObj.put("subtitles", subsArray)
            scenesArray.put(sceneObj)
        }
        root.put("scenes", scenesArray)
        return root.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): RobloxScript {
            val root = JSONObject(jsonStr)
            val title = root.optString("title", "Roblox Short")
            val category = root.optString("category", "Roblox Story")
            val language = root.optString("language", "en")
            val hook = root.optString("hook", "Wait for the end!")
            val musicStyle = root.optString("musicStyle", "UPBEAT_GAMING")
            val estimatedDurationSec = root.optInt("estimatedDurationSec", 30)

            val scenes = mutableListOf<RobloxScene>()
            val scenesArray = root.optJSONArray("scenes") ?: JSONArray()
            for (i in 0 until scenesArray.length()) {
                val obj = scenesArray.getJSONObject(i)
                val subsList = mutableListOf<SubtitleWord>()
                val subsArray = obj.optJSONArray("subtitles") ?: JSONArray()
                for (j in 0 until subsArray.length()) {
                    val subObj = subsArray.getJSONObject(j)
                    subsList.add(
                        SubtitleWord(
                            word = subObj.getString("word"),
                            startSec = subObj.getDouble("startSec").toFloat(),
                            endSec = subObj.getDouble("endSec").toFloat(),
                            isHighlighted = subObj.optBoolean("isHighlighted", false)
                        )
                    )
                }

                scenes.add(
                    RobloxScene(
                        sceneNumber = obj.optInt("sceneNumber", i + 1),
                        durationSec = obj.optDouble("durationSec", 5.0).toFloat(),
                        title = obj.optString("title", "Scene ${i + 1}"),
                        characterType = obj.optString("characterType", "BACON_HAIR"),
                        characterExpression = obj.optString("characterExpression", "SHOCKED"),
                        environment = obj.optString("environment", "NEON_CITY"),
                        cameraMotion = obj.optString("cameraMotion", "ZOOM_IN"),
                        narration = obj.optString("narration", ""),
                        characterDialogue = obj.optString("characterDialogue").takeIf { it.isNotBlank() },
                        soundFx = obj.optString("soundFx").takeIf { it.isNotBlank() },
                        subtitles = subsList
                    )
                )
            }

            return RobloxScript(
                title = title,
                category = category,
                language = language,
                hook = hook,
                scenes = scenes,
                musicStyle = musicStyle,
                estimatedDurationSec = estimatedDurationSec
            )
        }
    }
}
