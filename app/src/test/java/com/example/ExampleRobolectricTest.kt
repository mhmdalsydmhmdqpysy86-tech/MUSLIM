package com.example

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun directLaunchWithoutLogin_opensShareAppHubAndSharesAppLink() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals("OptiClean AI", context.getString(R.string.app_name))

        // Verify the app opens directly into the operational workspace without a login screen
        composeTestRule.onNodeWithTag("supervisor_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_share_button").assertIsDisplayed()

        // Open Share App Hub dialog from top bar
        composeTestRule.onNodeWithTag("top_share_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("share_app_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_copy_app_link").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_share_app_link").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_share_hotel_summary").assertIsDisplayed()

        // Test copying the app link to clipboard
        composeTestRule.onNodeWithTag("btn_copy_app_link").performClick()
        composeTestRule.waitForIdle()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copiedUrl = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        assertEquals(context.getString(R.string.shared_app_url), copiedUrl)

        // Test sharing the app link via Android Sharesheet
        shadowOf(app).clearNextStartedActivities()
        composeTestRule.onNodeWithTag("btn_share_app_link").performClick()
        composeTestRule.waitForIdle()

        val chooserIntent = shadowOf(app).nextStartedActivity
        assertNotNull("Expected Android Sharesheet chooser intent", chooserIntent)
        assertEquals(Intent.ACTION_CHOOSER, chooserIntent.action)

        @Suppress("DEPRECATION")
        val targetIntent = chooserIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull("Expected wrapped ACTION_SEND intent", targetIntent)
        assertEquals(Intent.ACTION_SEND, targetIntent?.action)
        val sharedText = targetIntent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        assertTrue(sharedText.contains(context.getString(R.string.shared_app_url)))
        assertTrue(sharedText.contains("OptiClean AI"))

        // Navigate across tabs and verify native share actions are available everywhere
        composeTestRule.onNodeWithTag("nav_housekeeper").performClick()
        composeTestRule.onNodeWithTag("btn_share_all_tasks").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_inventory").performClick()
        composeTestRule.onNodeWithTag("btn_share_inventory_summary").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_dashboard").performClick()
        composeTestRule.onNodeWithTag("btn_share_dashboard_kpi").assertIsDisplayed()
    }
}
