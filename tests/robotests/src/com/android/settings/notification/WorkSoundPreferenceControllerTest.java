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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.DefaultRingtonePreference;
import com.android.settings.R;
import com.android.settings.RingtonePreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
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
    private PreferenceCategory mWorkCategory;
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
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mScreen.findPreference(KEY_WORK_CATEGORY))
                .thenReturn(mWorkCategory);
        when(mWorkCategory.findPreference(KEY_WORK_USE_PERSONAL_SOUNDS))
                .thenReturn(mock(TwoStatePreference.class));
        when(mWorkCategory.findPreference(KEY_WORK_PHONE_RINGTONE))
                .thenReturn(mock(DefaultRingtonePreference.class));
        when(mWorkCategory.findPreference(KEY_WORK_NOTIFICATION_RINGTONE))
                .thenReturn(mock(DefaultRingtonePreference.class));
        when(mWorkCategory.findPreference(KEY_WORK_ALARM_RINGTONE))
                .thenReturn(mock(DefaultRingtonePreference.class));

        mController = new WorkSoundPreferenceController(mContext, mFragment, null, mAudioHelper);
    }

    @Test
    public void isAvailable_managedProfileAndNotSingleVolume_shouldReturnTrue() {
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        when(mAudioHelper.isUserUnlocked(nullable(UserManager.class), anyInt())).thenReturn(true);
        when(mAudioHelper.isSingleVolume()).thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noManagedProfile_shouldReturnFalse() {
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.USER_NULL);
        when(mAudioHelper.isUserUnlocked(nullable(UserManager.class), anyInt())).thenReturn(true);
        when(mAudioHelper.isSingleVolume()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_singleVolume_shouldReturnFalse() {
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        when(mAudioHelper.isUserUnlocked(nullable(UserManager.class), anyInt())).thenReturn(true);
        when(mAudioHelper.isSingleVolume()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onManagedProfileAdded_shouldDisplayPreferenceCategory() {
        // Given a device without any managed profiles:
        when(mAudioHelper.isSingleVolume()).thenReturn(false);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mAudioHelper.createPackageContextAsUser(anyInt())).thenReturn(mContext);
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.USER_NULL);

        // When the fragment first displays, the category should not appear.
        mController.displayPreference(mScreen);
        verify(mWorkCategory).setVisible(false);

        // However, when a managed profile is added later, the category should appear.
        mController.onResume();
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        mController.onManagedProfileAdded(UserHandle.myUserId());

        verify(mWorkCategory).setVisible(true);
    }

    @Test
    public void onManagedProfileRemoved_shouldHidePreferenceCategory() {
        // Given a device with a managed profile:
        when(mAudioHelper.isSingleVolume()).thenReturn(false);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mAudioHelper.createPackageContextAsUser(anyInt())).thenReturn(mContext);
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        when(mAudioHelper.isUserUnlocked(nullable(UserManager.class), anyInt())).thenReturn(true);

        // Which is in resumed state:
        mController.displayPreference(mScreen);
        mController.onResume();

        verify(mWorkCategory, times(2)).setVisible(true);

        // When a managed profile is removed, the category should be hidden.
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.USER_NULL);
        mController.onManagedProfileRemoved(UserHandle.myUserId());

        verify(mWorkCategory).setVisible(false);
    }

    @Test
    public void displayPreference_isAvailable_shouldShowPreferenceCategory() {
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        when(mAudioHelper.isUserUnlocked(nullable(UserManager.class), anyInt())).thenReturn(true);
        when(mAudioHelper.isSingleVolume()).thenReturn(false);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mAudioHelper.createPackageContextAsUser(anyInt())).thenReturn(mContext);

        mController.displayPreference(mScreen);
        verify(mWorkCategory).setVisible(true);
    }

    @Test
    public void displayPreference_notAvailable_shouldHidePreferenceCategory() {
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.USER_NULL);
        when(mAudioHelper.isSingleVolume()).thenReturn(true);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);

        mController.displayPreference(mScreen);
        verify(mWorkCategory).setVisible(false);
    }

    @Test
    public void onPreferenceChange_shouldUpdateSummary() {
        final Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(KEY_WORK_PHONE_RINGTONE);

        mController.onPreferenceChange(preference, "hello");

        verify(preference).setSummary(nullable(String.class));
    }

    @Test
    public void onResume_noVoiceCapability_shouldHidePhoneRingtone() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);
        mController = new WorkSoundPreferenceController(mContext, mFragment, null, mAudioHelper);

        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        when(mAudioHelper.isUserUnlocked(nullable(UserManager.class), anyInt())).thenReturn(true);
        when(mAudioHelper.isSingleVolume()).thenReturn(false);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mAudioHelper.createPackageContextAsUser(anyInt())).thenReturn(mContext);

        // Precondition: work profile is available.
        assertThat(mController.isAvailable()).isTrue();

        mController.displayPreference(mScreen);
        mController.onResume();

        verify((Preference) mWorkCategory.findPreference(KEY_WORK_PHONE_RINGTONE))
                .setVisible(false);
    }

    @Test
    public void onResume_availableButLocked_shouldRedactPreferences() {
        final String notAvailable = "(not available)";
        when(mContext.getString(R.string.managed_profile_not_available_label))
                .thenReturn(notAvailable);

        // Given a device with a managed profile:
        when(mAudioHelper.isSingleVolume()).thenReturn(false);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mAudioHelper.createPackageContextAsUser(anyInt())).thenReturn(mContext);
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(UserHandle.myUserId());
        when(mAudioHelper.isUserUnlocked(nullable(UserManager.class), anyInt())).thenReturn(false);

        // When resumed:
        mController.displayPreference(mScreen);
        mController.onResume();

        verify(mWorkCategory, times(2)).setVisible(true);

        // Sound preferences should explain that the profile isn't available yet.
        verify((Preference) mWorkCategory.findPreference(KEY_WORK_PHONE_RINGTONE))
                .setSummary(eq(notAvailable));
        verify((Preference) mWorkCategory.findPreference(KEY_WORK_NOTIFICATION_RINGTONE))
                .setSummary(eq(notAvailable));
        verify((Preference) mWorkCategory.findPreference(KEY_WORK_ALARM_RINGTONE))
                .setSummary(eq(notAvailable));
    }

    @Test
    public void onResume_shouldSetUserIdToPreference() {
        final int managedProfileUserId = 10;
        when(mAudioHelper.getManagedProfileId(nullable(UserManager.class)))
                .thenReturn(managedProfileUserId);
        when(mAudioHelper.isUserUnlocked(nullable(UserManager.class), anyInt())).thenReturn(true);
        when(mAudioHelper.isSingleVolume()).thenReturn(false);
        when(mFragment.getPreferenceScreen()).thenReturn(mScreen);
        when(mAudioHelper.createPackageContextAsUser(anyInt())).thenReturn(mContext);

        mController.displayPreference(mScreen);
        mController.onResume();

        verify((RingtonePreference) mWorkCategory.findPreference(KEY_WORK_PHONE_RINGTONE))
                .setUserId(managedProfileUserId);
        verify((RingtonePreference) mWorkCategory.findPreference(KEY_WORK_NOTIFICATION_RINGTONE))
                .setUserId(managedProfileUserId);
        verify((RingtonePreference) mWorkCategory.findPreference(KEY_WORK_ALARM_RINGTONE))
                .setUserId(managedProfileUserId);
    }
}
