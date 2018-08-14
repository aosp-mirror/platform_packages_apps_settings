package com.android.settings;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ZenModeSettingsIntegrationTest {
    private static final String WM_DISMISS_KEYGUARD_COMMAND = "wm dismiss-keyguard";

    private Context mContext;
    private UiDevice mUiDevice;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
        mUiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mUiDevice.wakeUp();
        mUiDevice.executeShellCommand(WM_DISMISS_KEYGUARD_COMMAND);
    }

    @Test
    public void testZenModeSettingsPreferences() {
        launchZenSettings();
        onView(withText("Behavior")).check(matches(isDisplayed()));
        onView(withText("Turn on automatically")).check(matches(isDisplayed()));
    }

    @Test
    public void testZenModeBehaviorPreferences() {
        launchZenBehaviorSettings();
        onView(withText("Alarms")).check(matches(isDisplayed()));
        onView(withText("Media and system feedback")).check(matches(isDisplayed()));
        onView(withText("Reminders")).check(matches(isDisplayed()));
        onView(withText("Events")).check(matches(isDisplayed()));
        onView(withText("Messages")).check(matches(isDisplayed()));
        onView(withText("Calls")).check(matches(isDisplayed()));
        onView(withText("Repeat callers")).check(matches(isDisplayed()));
    }

    @Test
    public void testZenModeAutomationPreferences() {
        launchZenAutomationSettings();
        onView(withText("Weekend")).check(matches(isDisplayed()));
        onView(withText("Add rule")).check(matches(isDisplayed()));
    }

    private void launchZenSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_ZEN_MODE_SETTINGS)
                .setPackage(mContext.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(settingsIntent);
    }

    private void launchZenAutomationSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_ZEN_MODE_AUTOMATION_SETTINGS)
                .setPackage(mContext.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(settingsIntent);
    }

    private void launchZenBehaviorSettings() {
        Intent settingsIntent = new Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
                .setPackage(mContext.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(settingsIntent);
    }
}