package com.android.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.test.uiautomator.UiDevice;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
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
        onView(withText("Calls")).check(matches(isDisplayed()));
        onView(withText("SMS, MMS, and messaging apps")).check(matches(isDisplayed()));
        onView(withText("Restrict notifications")).check(matches(isDisplayed()));
        onView(withText("Duration")).check(matches(isDisplayed()));
        onView(withText("Schedules")).check(matches(isDisplayed()));
    }

    @Test
    public void testZenModeBehaviorPreferences() {
        launchZenBehaviorSettings();
        onView(withText("Calls")).check(matches(isDisplayed()));
        onView(withText("SMS, MMS, and messaging apps")).check(matches(isDisplayed()));
        onView(withText("Restrict notifications")).check(matches(isDisplayed()));
        onView(withText("Duration")).check(matches(isDisplayed()));
        onView(withText("Schedules")).check(matches(isDisplayed()));
    }

    @Test
    public void testZenModeAutomationPreferences() {
        launchZenAutomationSettings();
        onView(withText("Sleeping")).check(matches(isDisplayed()));
        onView(withText("Event")).check(matches(isDisplayed()));
        onView(withText("Add more")).check(matches(isDisplayed()));
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