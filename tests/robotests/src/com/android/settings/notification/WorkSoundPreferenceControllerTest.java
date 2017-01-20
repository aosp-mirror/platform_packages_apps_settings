/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.telephony.TelephonyManager;

import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WorkSoundPreferenceControllerTest {

    private static final String KEY_WORK_CATEGORY = "sound_work_settings_section";
    private static final String KEY_WORK_USE_PERSONAL_SOUNDS = "work_use_personal_sounds";
    private static final String KEY_WORK_PHONE_RINGTONE = "work_ringtone";
    private static final String KEY_WORK_NOTIFICATION_RINGTONE = "work_notification_ringtone";
    private static final String KEY_WORK_ALARM_RINGTONE = "work_alarm_ringtone";

    @Mock
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private AudioHelper mAudioHelper;
    @Mock
    private SoundSettings mFragment;

    private WorkSoundPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        mController = new WorkSoundPreferenceController(mContext, mFragment, null, mAudioHelper);
    }

    @Test
    public void isAvailable_managedProfileAndNotSingleVolume_shouldReturnTrue() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mAudioHelper.getManagedProfileId(any(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        when(mAudioHelper.isSingleVolume()).thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noManagedProfile_shouldReturnFalse() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mAudioHelper.getManagedProfileId(any(UserManager.class)))
                .thenReturn(UserHandle.USER_NULL);
        when(mAudioHelper.isSingleVolume()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_singleVolume_shouldReturnFalse() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mAudioHelper.getManagedProfileId(any(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        when(mAudioHelper.isSingleVolume()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onResume_available_shouldAddPreferenceCategory() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mAudioHelper.getManagedProfileId(any(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        when(mAudioHelper.isSingleVolume()).thenReturn(false);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mAudioHelper.createPackageContextAsUser(anyInt())).thenReturn(mContext);
        mockWorkCategory();

        mController.onResume();

        verify(mFragment).addPreferencesFromResource(R.xml.sound_work_settings);
    }

    @Test
    public void onManagedProfileAdded_shouldAddPreferenceCategory() {
        // Given a device without any managed profiles:
        when(mAudioHelper.isSingleVolume()).thenReturn(false);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mAudioHelper.createPackageContextAsUser(anyInt())).thenReturn(mContext);
        when(mAudioHelper.getManagedProfileId(any(UserManager.class)))
                .thenReturn(UserHandle.USER_NULL);
        mockWorkCategory();

        // When the fragment first resumes, the category should not appear.
        mController.onResume();

        verify(mFragment, never()).addPreferencesFromResource(R.xml.sound_work_settings);

        // However, when a managed profile is added after resuming, the category should appear.
        when(mAudioHelper.getManagedProfileId(any(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        mController.onManagedProfileAdded(UserHandle.myUserId());

        verify(mFragment).addPreferencesFromResource(R.xml.sound_work_settings);
    }

    @Test
    public void onManagedProfileRemoved_shouldRemovePreferenceCategory() {
        // Given a device with a managed profile:
        when(mAudioHelper.isSingleVolume()).thenReturn(false);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mAudioHelper.createPackageContextAsUser(anyInt())).thenReturn(mContext);
        when(mAudioHelper.getManagedProfileId(any(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        mockWorkCategory();

        // Which is in resumed state:
        mController.onResume();

        // When a managed profile is removed, the category should be removed.
        when(mAudioHelper.getManagedProfileId(any(UserManager.class)))
                .thenReturn(UserHandle.USER_NULL);
        mController.onManagedProfileRemoved(UserHandle.myUserId());

        verify(mScreen).removePreference(mScreen.findPreference(KEY_WORK_CATEGORY));
    }

    @Test
    public void onResume_notAvailable_shouldNotAddPreferenceCategory() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mAudioHelper.getManagedProfileId(any(UserManager.class)))
            .thenReturn(UserHandle.USER_NULL);
        when(mAudioHelper.isSingleVolume()).thenReturn(true);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);

        mController.onResume();

        verify(mFragment, never()).addPreferencesFromResource(anyInt());
    }

    @Test
    public void onPreferenceChange_shouldUpdateSummary() {
        final Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(KEY_WORK_PHONE_RINGTONE);

        mController.onPreferenceChange(preference, "hello");

        verify(preference).setSummary(anyString());
    }

    private void mockWorkCategory() {
        when(mScreen.findPreference(KEY_WORK_CATEGORY))
            .thenReturn(mock(PreferenceGroup.class));
        when(mScreen.findPreference(KEY_WORK_USE_PERSONAL_SOUNDS))
            .thenReturn(mock(TwoStatePreference.class));
        when(mScreen.findPreference(KEY_WORK_PHONE_RINGTONE))
            .thenReturn(mock(DefaultRingtonePreference.class));
        when(mScreen.findPreference(KEY_WORK_NOTIFICATION_RINGTONE))
            .thenReturn(mock(DefaultRingtonePreference.class));
        when(mScreen.findPreference(KEY_WORK_ALARM_RINGTONE))
            .thenReturn(mock(DefaultRingtonePreference.class));
    }
}
