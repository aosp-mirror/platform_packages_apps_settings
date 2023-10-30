/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.notification;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;


@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class NotificationAssistantPreferenceControllerTest {

    private static final String KEY = "TEST_KEY";
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private ConfigureNotificationSettings mFragment;
    @Mock
    private FragmentManager mFragmentManager;
    @Mock
    private FragmentTransaction mFragmentTransaction;
    @Mock
    private NotificationBackend mBackend;
    private NotificationAssistantPreferenceController mPreferenceController;
    ComponentName mNASComponent = new ComponentName("pkgname", "clsname");
    private TwoStatePreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mPreference = spy(new SwitchPreferenceCompat(mContext));
        doReturn(mContext).when(mFragment).getContext();
        when(mFragment.getFragmentManager()).thenReturn(mFragmentManager);
        when(mFragmentManager.beginTransaction()).thenReturn(mFragmentTransaction);
        when(mBackend.getDefaultNotificationAssistant()).thenReturn(mNASComponent);
        mPreferenceController = new NotificationAssistantPreferenceController(mContext);
        mPreferenceController.setBackend(mBackend);
        mPreferenceController.setFragment(mFragment);
        mPreferenceController.getDefaultNASIntent();

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mPreference.setKey(NotificationAssistantPreferenceController.KEY_NAS);
        screen.addPreference(mPreference);
        mPreferenceController.displayPreference(screen);
    }

    @Test
    public void testIsChecked() throws Exception {
        when(mBackend.getAllowedNotificationAssistant()).thenReturn(mNASComponent);
        assertTrue(mPreferenceController.isChecked());

        when(mBackend.getAllowedNotificationAssistant()).thenReturn(null);
        assertFalse(mPreferenceController.isChecked());
    }

    @Test
    public void testSetChecked() throws Exception {
        // Verify a dialog is shown when the switch is to be enabled.
        assertFalse(mPreferenceController.setChecked(true));
        verify(mFragmentTransaction).add(
                any(NotificationAssistantDialogFragment.class), anyString());
        verify(mBackend, times(0)).setNotificationAssistantGranted(any());

        // Verify no dialog is shown and NAS set to null when disabled
        assertTrue(mPreferenceController.setChecked(false));
        verify(mBackend, times(1)).setNotificationAssistantGranted(null);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testMigrationFromSetting_userEnable_multiProfile() throws Exception {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAS_SETTINGS_UPDATED, 0, 0);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAS_SETTINGS_UPDATED, 0, 10);

        //Test user enable for the first time
        mPreferenceController.setNotificationAssistantGranted(mNASComponent);
        verify(mBackend, times(1))
                .setNASMigrationDoneAndResetDefault(eq(0), eq(true));
        verify(mBackend, never())
                .setNASMigrationDoneAndResetDefault(eq(10), anyBoolean());
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testMigrationFromSetting_userEnable_multiUser() throws Exception {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAS_SETTINGS_UPDATED, 0, 0);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAS_SETTINGS_UPDATED, 0, 20);

        //Test user 0 enable for the first time
        mPreferenceController.setNotificationAssistantGranted(mNASComponent);
        verify(mBackend, times(1))
                .setNASMigrationDoneAndResetDefault(eq(0), eq(true));
        verify(mBackend, never())
                .setNASMigrationDoneAndResetDefault(eq(20), anyBoolean());
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testMigrationFromSetting_userDisable_multiProfile() throws Exception {
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAS_SETTINGS_UPDATED, 0, 0);
        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAS_SETTINGS_UPDATED, 0, 10);

        //Test user disable for the first time
        mPreferenceController.setChecked(false);
        verify(mBackend, times(1))
                .setNASMigrationDoneAndResetDefault(eq(0), eq(false));
        verify(mBackend, never())
                .setNASMigrationDoneAndResetDefault(eq(10), anyBoolean());
    }
}
