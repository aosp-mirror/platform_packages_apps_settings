package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class AppLocationPermissionPreferenceControllerTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private AppLocationPermissionPreferenceController mController;

    @Mock
    private Context mContext;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new AppLocationPermissionPreferenceController(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    public void displayPreference_shouldRemovePreference() {
        Settings.System.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.LOCATION_SETTINGS_LINK_TO_PERMISSIONS_ENABLED,
                0);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_shouldNotRemovePreference() {
        Settings.System.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.LOCATION_SETTINGS_LINK_TO_PERMISSIONS_ENABLED,
                1);
        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }
}
