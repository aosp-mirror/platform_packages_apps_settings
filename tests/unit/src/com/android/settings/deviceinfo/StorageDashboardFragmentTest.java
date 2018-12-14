package com.android.settings.deviceinfo;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.SmallTest;

import com.android.settings.R;
import com.android.settings.Settings.StorageDashboardActivity;
import com.android.settings.deletionhelper.AutomaticStorageManagerSettings;

import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class StorageDashboardFragmentTest {

    public static final String EXTRA_KEY = ":settings:show_fragment";

    @Rule
    public IntentsTestRule<StorageDashboardActivity> mActivityRule =
            new IntentsTestRule<>(StorageDashboardActivity.class, true, true);

    @Test
    public void testStorageManagePreference_canClickTextView() throws InterruptedException {
        // Click on the actual textbox instead of just somewhere in the preference
        onView(withText(R.string.automatic_storage_manager_preference_title)).perform(click());

        // Check that it worked by seeing if we switched screens
        intended(hasExtra(equalTo(EXTRA_KEY),
                containsString(AutomaticStorageManagerSettings.class.getName())));

    }
}
