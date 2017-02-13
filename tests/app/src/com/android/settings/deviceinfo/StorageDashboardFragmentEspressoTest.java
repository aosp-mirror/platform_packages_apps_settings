package com.android.settings.deviceinfo;

import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnitRunner;
import com.android.settings.R;
import com.android.settings.Settings.StorageDashboardActivity;
import com.android.settings.deletionhelper.AutomaticStorageManagerSettings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@SmallTest
public class StorageDashboardFragmentEspressoTest {

    public static final String EXTRA_KEY = ":settings:show_fragment";

    @Rule
    public IntentsTestRule<StorageDashboardActivity> mActivityRule =
            new IntentsTestRule<>(StorageDashboardActivity.class, true, true);

    @Test
    public void testStorageManagePreference_canClickTextView() throws InterruptedException {
        // Click on the actual textbox instead of just somewhere in the preference
        onView(withText(R.string.storage_menu_manage)).perform(click());

        // Check that it worked by seeing if we switched screens
        intended(hasExtra(equalTo(EXTRA_KEY),
                containsString(AutomaticStorageManagerSettings.class.getName())));

    }
}
