/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.notification.app;

import static android.app.NotificationChannel.DEFAULT_CHANNEL_ID;
import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AttributeSet;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class SoundPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;
    @Mock
    private SettingsPreferenceFragment mFragment;
    @Mock
    private NotificationSettings.DependentFieldListener mDependentFieldListener;

    private SoundPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;
        mController = spy(new SoundPreferenceController(
                mContext, mFragment, mDependentFieldListener, mBackend));
    }

    @Test
    public void testNoCrashIfNoOnResume() {
        mController.isAvailable();
        mController.updateState(mock(NotificationSoundPreference.class));
        mController.onPreferenceChange(mock(NotificationSoundPreference.class), Uri.EMPTY);
        mController.handlePreferenceTreeClick(mock(NotificationSoundPreference.class));
        mController.onActivityResult(1, 1, null);
        SoundPreferenceController.hasValidSound(null);
    }

    @Test
    public void testIsAvailable_notIfChannelNull() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        mController.onResume(appRow, null, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfNotImportant() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_LOW);
        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable_notIfDefaultChannel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "", IMPORTANCE_DEFAULT);
        mController.onResume(appRow, channel, null, null, null, null);
        assertFalse(mController.isAvailable());
    }

    @Test
    public void testIsAvailable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_DEFAULT);
        mController.onResume(appRow, channel, null, null, null, null);
        assertTrue(mController.isAvailable());
    }

    @Test
    public void testDisplayPreference_savesPreference() {
        NotificationSoundPreference pref = mock(NotificationSoundPreference.class);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);

        mController.onActivityResult(SoundPreferenceController.CODE, 1, new Intent());
        verify(pref, times(1)).onActivityResult(anyInt(), anyInt(), any());
    }

    @Test
    public void testUpdateState_disabledByAdmin() {
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        mController.onResume(new NotificationBackend.AppRow(), channel, null, null, null, mock(
                RestrictedLockUtils.EnforcedAdmin.class));

        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        Preference pref = new NotificationSoundPreference(mContext, attributeSet);
        mController.updateState(pref);

        assertFalse(pref.isEnabled());
    }

    @Test
    public void testUpdateState_notBlockable() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.isImportanceLockedByOEM()).thenReturn(true);
        mController.onResume(appRow, channel, null, null, null, null);

        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        Preference pref = new NotificationSoundPreference(mContext, attributeSet);
        mController.updateState(pref);

        assertTrue(pref.isEnabled());
    }

    @Test
    public void testUpdateState_configurable() {
        Uri sound = Settings.System.DEFAULT_ALARM_ALERT_URI;
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn("something");
        when(channel.getSound()).thenReturn(sound);
        mController.onResume(appRow, channel, null, null, null, null);

        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        NotificationSoundPreference pref = new NotificationSoundPreference(mContext, attributeSet);
        mController.updateState(pref);

        assertEquals(sound, pref.onRestoreRingtone());
        assertTrue(pref.isEnabled());
    }

    @Test
    public void testOnPreferenceChange() {
        Uri sound = Settings.System.DEFAULT_ALARM_ALERT_URI;
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_HIGH);
        channel.setSound(sound, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        mController.onResume(appRow, channel, null, null, null, null);

        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        NotificationSoundPreference pref =
                new NotificationSoundPreference(mContext, attributeSet);
        mController.updateState(pref);

        mController.onPreferenceChange(pref, Uri.EMPTY);
        assertEquals(Uri.EMPTY, channel.getSound());
        assertEquals(Notification.AUDIO_ATTRIBUTES_DEFAULT, channel.getAudioAttributes());
        verify(mBackend, times(1)).updateChannel(any(), anyInt(), any());
    }

    @Test
    public void testOnPreferenceTreeClick_incorrectPref() {
        NotificationSoundPreference pref = mock(NotificationSoundPreference.class);
        mController.handlePreferenceTreeClick(pref);

        verify(pref, never()).onPrepareRingtonePickerIntent(any());
        verify(mFragment, never()).startActivityForResult(any(), anyInt());
    }

    @Test
    public void testOnPreferenceTreeClick_correctPref() {
        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        NotificationSoundPreference pref =
                spy(new NotificationSoundPreference(mContext, attributeSet));
        pref.setKey(mController.getPreferenceKey());
        mController.handlePreferenceTreeClick(pref);

        verify(pref, times(1)).onPrepareRingtonePickerIntent(any());
        verify(mFragment, times(1)).startActivityForResult(any(), anyInt());
    }

    @Test
    public void testOnPreferenceTreeClick_alarmSound() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_HIGH);
        channel.setSound(null, new AudioAttributes.Builder().setUsage(
                AudioAttributes.USAGE_ALARM).build());
        mController.onResume(appRow, channel, null, null, null, null);

        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        NotificationSoundPreference pref =
                spy(new NotificationSoundPreference(mContext, attributeSet));
        pref.setKey(mController.getPreferenceKey());
        mController.handlePreferenceTreeClick(pref);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(pref, times(1)).onPrepareRingtonePickerIntent(intentArgumentCaptor.capture());
        assertEquals(RingtoneManager.TYPE_ALARM,
                intentArgumentCaptor.getValue().getIntExtra(
                        RingtoneManager.EXTRA_RINGTONE_TYPE, 0));
    }

    @Test
    public void testOnPreferenceTreeClick_ringtoneSound() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_HIGH);
        channel.setSound(null, new AudioAttributes.Builder().setUsage(
                AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
        mController.onResume(appRow, channel, null, null, null, null);

        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        NotificationSoundPreference pref =
                spy(new NotificationSoundPreference(mContext, attributeSet));
        pref.setKey(mController.getPreferenceKey());
        mController.handlePreferenceTreeClick(pref);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(pref, times(1)).onPrepareRingtonePickerIntent(intentArgumentCaptor.capture());
        assertEquals(RingtoneManager.TYPE_RINGTONE,
                intentArgumentCaptor.getValue().getIntExtra(
                        RingtoneManager.EXTRA_RINGTONE_TYPE, 0));
    }

    @Test
    public void testOnPreferenceTreeClick_otherSound() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        NotificationChannel channel = new NotificationChannel("", "", IMPORTANCE_HIGH);
        channel.setSound(null, new AudioAttributes.Builder().setUsage(
                AudioAttributes.USAGE_UNKNOWN).build());
        mController.onResume(appRow, channel, null, null, null, null);

        AttributeSet attributeSet = Robolectric.buildAttributeSet().build();
        NotificationSoundPreference pref =
                spy(new NotificationSoundPreference(mContext, attributeSet));
        pref.setKey(mController.getPreferenceKey());
        mController.handlePreferenceTreeClick(pref);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(pref, times(1)).onPrepareRingtonePickerIntent(intentArgumentCaptor.capture());
        assertEquals(RingtoneManager.TYPE_NOTIFICATION,
                intentArgumentCaptor.getValue().getIntExtra(
                        RingtoneManager.EXTRA_RINGTONE_TYPE, 0));
    }

    @Test
    public void testOnActivityResult() {
        NotificationSoundPreference pref = mock(NotificationSoundPreference.class);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(pref);
        mController.displayPreference(mScreen);

        mController.onActivityResult(SoundPreferenceController.CODE, 1, new Intent("hi"));
        verify(pref, times(1)).onActivityResult(anyInt(), anyInt(), any());
        verify(mDependentFieldListener, times(1)).onFieldValueChanged();
    }

    @Test
    public void testHasValidSound() {
        NotificationChannel channel =
                new NotificationChannel(DEFAULT_CHANNEL_ID, "a", IMPORTANCE_HIGH);
        assertTrue(SoundPreferenceController.hasValidSound(channel));

        channel.setSound(Uri.EMPTY, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        assertFalse(SoundPreferenceController.hasValidSound(channel));

        channel.setSound(null, Notification.AUDIO_ATTRIBUTES_DEFAULT);
        assertFalse(SoundPreferenceController.hasValidSound(channel));
    }
}
