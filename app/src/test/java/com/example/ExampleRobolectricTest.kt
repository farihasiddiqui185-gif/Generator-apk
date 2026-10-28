package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.robloxshorts.data.model.RobloxScript
import com.example.robloxshorts.data.model.ShortsCategory
import com.example.robloxshorts.engine.StoryCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Roblox Shorts", appName)
  }

  @Test
  fun `story catalogue generates rich scenes`() {
    val horrorStory = StoryCatalogue.pickRandomStory(ShortsCategory.HORROR)
    assertNotNull(horrorStory.title)
    assertTrue(horrorStory.scenes.isNotEmpty())
    val firstScene = horrorStory.scenes.first()
    assertNotNull(firstScene.characterType)
    assertNotNull(firstScene.environment)
  }

  @Test
  fun `script JSON serialization roundtrip`() {
    val story = StoryCatalogue.pickRandomStory(ShortsCategory.RICH_VS_POOR)
    val script = com.example.robloxshorts.data.model.RobloxScript(
        title = story.title,
        category = "Rich vs Poor",
        language = "en",
        hook = story.hook,
        scenes = emptyList(),
        musicStyle = story.musicStyle,
        estimatedDurationSec = 30
    )
    val json = script.toJson()
    val parsed = RobloxScript.fromJson(json)
    assertEquals(script.title, parsed.title)
    assertEquals(script.estimatedDurationSec, parsed.estimatedDurationSec)
  }
}
