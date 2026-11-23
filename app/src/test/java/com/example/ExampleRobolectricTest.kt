package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("Hirafi", appName)
    val webClientId = context.getString(R.string.default_web_client_id)
    assertEquals("466578892927-61h7tk0gp0r8v53l5mqgn8en436r8g1n.apps.googleusercontent.com", webClientId)
  }
}
