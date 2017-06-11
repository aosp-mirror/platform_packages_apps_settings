package com.android.settings;

import android.content.Intent;
import android.content.Context;

import android.app.Instrumentation;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroupAdapter;
import com.android.settings.accessibility.AccessibilitySettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SettingsPreferenceFragmentTest {

    private Instrumentation mInstrumentation;
    private Context mTargetContext;

    @Before
    public void setUp() throws Exception {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mTargetContext = mInstrumentation.getTargetContext();
    }

    @Test
    public void testHighlightCaptions() throws InterruptedException {
        final String prefKey = "captioning_preference_screen";
        Bundle args = new Bundle();
        args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, prefKey);

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(mTargetContext, SubSettings.class);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT,
                "com.android.settings.accessibility.AccessibilitySettings");
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);

        SettingsActivity activity  = (SettingsActivity) mInstrumentation.startActivitySync(intent);
        AccessibilitySettings fragment = (AccessibilitySettings)
                activity.getFragmentManager().getFragments().get(0);

        // Allow time for highlight from post-delay.
        Thread.sleep(SettingsPreferenceFragment.DELAY_HIGHLIGHT_DURATION_MILLIS);
        if (!fragment.mPreferenceHighlighted) {
            Thread.sleep(SettingsPreferenceFragment.DELAY_HIGHLIGHT_DURATION_MILLIS);
        }

        int prefPosition = -1;
        PreferenceGroupAdapter adapter = (PreferenceGroupAdapter)
                fragment.getListView().getAdapter();
        for (int n = 0, count = adapter.getItemCount(); n < count; n++) {
            final Preference preference = adapter.getItem(n);
            final String preferenceKey = preference.getKey();
            if (preferenceKey.equals(prefKey)) {
                prefPosition = n;
                break;
            }
        }

        assertThat(fragment.mAdapter.initialHighlightedPosition).isEqualTo(prefPosition);
    }
}
