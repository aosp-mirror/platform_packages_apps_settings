package com.android.settings.deletionhelper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class AutomaticStorageManagerDescriptionPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;
    private AutomaticStorageManagerDescriptionPreferenceController mController;
    private Context mContext = RuntimeEnvironment.application;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new AutomaticStorageManagerDescriptionPreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
        when(mPreference.getContext()).thenReturn(mContext);
    }

    @Test
    public void displayPreference_asmDisabled_shouldHaveDescription() {
        mController.displayPreference(mScreen);

        verify(mPreference).setSummary(eq(R.string.automatic_storage_manager_text));
    }

    @Test
    public void displayPreference_asmEnabledButUnused_shouldHaveDescription() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED,
                1);

        mController.displayPreference(mScreen);

        verify(mPreference).setSummary(eq(R.string.automatic_storage_manager_text));
    }

    @Ignore("Robolectric doesn't do locale switching for date localization -- yet.")
    @Test
    @Config(qualifiers = "en")
    public void displayPreference_asmEnabledAndUsed_shouldHaveDescriptionFilledOut() {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_ENABLED,
                1);
        Settings.Secure.putLong(
                mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_BYTES_CLEARED,
                10);
        Settings.Secure.putLong(
                mContext.getContentResolver(),
                Settings.Secure.AUTOMATIC_STORAGE_MANAGER_LAST_RUN,
                43200000); // January 1, 1970 12:00:00 PM to avoid timezone issues.

        mController.displayPreference(mScreen);

        verify(mPreference)
                .setSummary(eq("10.00B total made available\n\nLast ran on January 1, 1970"));
    }
}
