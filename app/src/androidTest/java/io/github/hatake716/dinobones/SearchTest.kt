package io.github.hatake716.dinobones

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchTest {
    @get:Rule val compose=createEmptyComposeRule()

    @Test fun typingRetainsFocusWhileFilteringTheSelectedPeriod() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("collection-grid").performScrollToNode(hasSetTextAction())
            compose.onNode(hasSetTextAction()).performClick().performTextInput("t")
            compose.onNode(hasSetTextAction()).assertIsFocused().assertTextContains("t")
            compose.onNode(hasSetTextAction()).performTextInput("itanosaurus")
            compose.onNode(hasSetTextAction()).assertIsFocused().assertTextContains("titanosaurus")
            compose.onNodeWithTag("collection-grid").performScrollToNode(hasText("ティタノサウルス"))
            compose.onNodeWithText("ティタノサウルス").assertIsDisplayed()
        }
    }
}
