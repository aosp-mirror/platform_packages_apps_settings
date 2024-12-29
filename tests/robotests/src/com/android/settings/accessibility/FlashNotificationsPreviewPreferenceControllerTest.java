/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.settings.accessibility.FlashNotificationsUtil.ACTION_FLASH_NOTIFICATION_START_PREVIEW;
import static com.android.settings.accessibility.FlashNotificationsUtil.EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE;
import static com.android.settings.accessibility.FlashNotificationsUtil.TYPE_LONG_PREVIEW;
import static com.android.settings.accessibility.FlashNotificationsUtil.TYPE_SHORT_PREVIEW;
import static com.android.settings.accessibility.ShadowFlashNotificationsUtils.setFlashNotificationsState;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.ButtonPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowFlashNotificationsUtils.class)
public class FlashNotificationsPreviewPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "preference_key";

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private ContentResolver mContentResolver = mContext.getContentResolver();
    @Mock
    private PreferenceScreen mPreferenceScreen;
    private ButtonPreference mPreference;
    private FlashNotificationsPreviewPreferenceController mController;

    @Before
    public void setUp() {
        when(mContext.getContentResolver()).thenReturn(mContentResolver);

        mPreference = new ButtonPreference(mContext);
        mPreference.setKey(PREFERENCE_KEY);
        final View rootView = View.inflate(mContext, mPreference.getLayoutResource(), null);
        mPreference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(rootView));
        when(mPreferenceScreen.findPreference(PREFERENCE_KEY)).thenReturn(mPreference);

        mController = new FlashNotificationsPreviewPreferenceController(mContext, PREFERENCE_KEY);
        mController.displayPreference(mPreferenceScreen);
    }

    @After
    public void tearDown() {
        ShadowFlashNotificationsUtils.reset();
    }

    @Test
    public void testGetAvailabilityStatus() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updateState_cameraOff_screenOff_notVisible() {
        setFlashNotificationsState(FlashNotificationsUtil.State.OFF);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_cameraOn_screenOff_isVisible() {
        setFlashNotificationsState(FlashNotificationsUtil.State.CAMERA);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_cameraOff_screenOn_isVisible() {
        setFlashNotificationsState(FlashNotificationsUtil.State.SCREEN);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_cameraOn_screenOn_isVisible() {
        setFlashNotificationsState(FlashNotificationsUtil.State.CAMERA_SCREEN);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void clickOnButton_assertAction() {
        mPreference.getButton().callOnClick();

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).sendBroadcastAsUser(captor.capture(), any());
        Intent captured = captor.getValue();
        assertThat(captured.getAction()).isEqualTo(ACTION_FLASH_NOTIFICATION_START_PREVIEW);
    }

    @Test
    public void clickOnButton_assertExtra() {
        mPreference.getButton().callOnClick();

        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).sendBroadcastAsUser(captor.capture(), any());
        Intent captured = captor.getValue();
        assertThat(captured.getIntExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE, TYPE_LONG_PREVIEW))
                .isEqualTo(TYPE_SHORT_PREVIEW);
    }

    @Test
    public void onStateChanged_onResume_cameraUri_verifyRegister() {
        mController.onStateChanged(mock(LifecycleOwner.class), Lifecycle.Event.ON_RESUME);

        verify(mContentResolver).registerContentObserver(
                eq(Settings.System.getUriFor(Settings.System.CAMERA_FLASH_NOTIFICATION)),
                anyBoolean(), eq(mController.mContentObserver));
    }

    @Test
    public void onStateChanged_onResume_screenUri_verifyRegister() {
        mController.onStateChanged(mock(LifecycleOwner.class), Lifecycle.Event.ON_RESUME);

        verify(mContentResolver).registerContentObserver(
                eq(Settings.System.getUriFor(Settings.System.SCREEN_FLASH_NOTIFICATION)),
                anyBoolean(), eq(mController.mContentObserver));
    }

    @Test
    public void onStateChanged_onPause_verifyUnregister() {
        mController.onStateChanged(mock(LifecycleOwner.class), Lifecycle.Event.ON_PAUSE);

        verify(mContentResolver).unregisterContentObserver(eq(mController.mContentObserver));
    }
}
