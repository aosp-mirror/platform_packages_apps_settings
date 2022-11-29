/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ControlsTrivialPrivacyPreferenceControllerTest {

    private static final String TEST_KEY = "test_key";
    private static final String SETTING_KEY = Settings.Secure.LOCKSCREEN_ALLOW_TRIVIAL_CONTROLS;
    private static final String DEPENDENCY_SETTING_KEY = Settings.Secure.LOCKSCREEN_SHOW_CONTROLS;

    private Context mContext;
    private ContentResolver mContentResolver;
    private ControlsTrivialPrivacyPreferenceController mController;

    @Mock
    private Preference mPreference;

    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mContentResolver = spy(mContext.getContentResolver());
        when(mContext.getContentResolver()).thenReturn(mContentResolver);

        setCustomizableLockScreenQuickAffordancesEnabled(false);

        mController = new ControlsTrivialPrivacyPreferenceController(mContext, TEST_KEY);
    }

    @Test
    public void isCheckedWhenSettingIsTrue() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 1);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isCheckedWhenSettingIsFalse() {
        Settings.Secure.putInt(mContentResolver, SETTING_KEY, 0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isCheckedWhenSettingIsNull() {
        Settings.Secure.putString(mContentResolver, SETTING_KEY, null);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void checkedMeansSettingIsTrue() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isNotEqualTo(0);
    }

    @Test
    public void uncheckedMeansSettingIsFalse() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContentResolver, SETTING_KEY, 0)).isEqualTo(0);
    }

    @Test
    public void getSummaryRequireDeviceControls() {
        Settings.Secure.putInt(mContentResolver, DEPENDENCY_SETTING_KEY, 0);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getText(R.string.lockscreen_trivial_disabled_controls_summary));
    }

    @Test
    public void getSummaryDefault() {
        Settings.Secure.putInt(mContentResolver, DEPENDENCY_SETTING_KEY, 1);

        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getText(R.string.lockscreen_trivial_controls_summary));
    }

    @Test
    public void updateState() {
        Settings.Secure.putInt(mContentResolver, DEPENDENCY_SETTING_KEY, 1);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(anyBoolean());
        verify(mPreference, atLeastOnce()).setSummary(mController.getSummary());
    }

    @Test
    public void updateStateWithCustomizableLockScreenQuickAffordancesEnabled() {
        setCustomizableLockScreenQuickAffordancesEnabled(true);
        Settings.Secure.putInt(mContentResolver, DEPENDENCY_SETTING_KEY, 0);

        mController.updateState(mPreference);

        verify(mPreference).setEnabled(true);
        verify(mPreference, atLeastOnce()).setSummary(
                mContext.getString(R.string.lockscreen_trivial_controls_summary));
    }

    @Test
    public void getAvailabilityStatusWithoutDeviceControls() {
        Settings.Secure.putInt(mContentResolver, DEPENDENCY_SETTING_KEY, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatusWithCustomizableLockScreenQuickAffordancesEnabled() {
        setCustomizableLockScreenQuickAffordancesEnabled(true);
        Settings.Secure.putInt(mContentResolver, DEPENDENCY_SETTING_KEY, 0);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatusWithDeviceControls() {
        Settings.Secure.putInt(mContentResolver, DEPENDENCY_SETTING_KEY, 1);


        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void setDependency() {
        Mockito.when(mPreferenceScreen
                .findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        verify(mPreference).setDependency(anyString());
    }

    private void setCustomizableLockScreenQuickAffordancesEnabled(boolean isEnabled) {
        when(
                mContentResolver.query(
                        CustomizableLockScreenUtils.FLAGS_URI, null, null, null))
                .thenAnswer((Answer<Cursor>) invocation -> {
                    final MatrixCursor cursor = new MatrixCursor(
                            new String[] {
                                    CustomizableLockScreenUtils.NAME,
                                    CustomizableLockScreenUtils.VALUE
                            });
                    cursor.addRow(
                            new Object[] {
                                    CustomizableLockScreenUtils.ENABLED_FLAG, isEnabled ? 1 : 0
                            });
                    return cursor;
                });
    }
}
