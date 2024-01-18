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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.window.flags.Flags;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class BackAnimationPreferenceControllerTest {

    private static final int SETTING_VALUE_OFF = 0;
    private static final int SETTING_VALUE_ON = 1;

    private SwitchPreference mPreference;

    private Context mContext;
    private BackAnimationPreferenceController mController;
    private Looper mLooper;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = instrumentation.getTargetContext();
        mController = new BackAnimationPreferenceController(mContext);
        mPreference = new SwitchPreference(mContext);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mLooper = Looper.myLooper();

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_BACK_ANIMATION, -1);

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        screen.addPreference(mPreference);
        mController.displayPreference(screen);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PREDICTIVE_BACK_SYSTEM_ANIMS)
    public void controllerNotAvailable_whenAconfigFlagEnabled() {
        assertFalse(mController.isAvailable());
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_PREDICTIVE_BACK_SYSTEM_ANIMS)
    public void controllerAvailable_whenAconfigFlagDisabled() {
        assertTrue(mController.isAvailable());
    }

    @Test
    public void onPreferenceChange_switchEnabled_shouldEnableBackAnimations() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_BACK_ANIMATION, -1 /* default */);
        assertThat(mode).isEqualTo(SETTING_VALUE_ON);
    }

    @Test
    public void onPreferenceChange_switchDisabled_shouldDisableBackAnimations() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_BACK_ANIMATION, -1 /* default */);
        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
    }

    @Test
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_BACK_ANIMATION, SETTING_VALUE_ON);
        mController.updateState(mPreference);
        assertTrue(mPreference.isChecked());
    }

    @Test
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_BACK_ANIMATION, SETTING_VALUE_OFF);

        mController.updateState(mPreference);
        assertFalse(mPreference.isChecked());
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference()
            throws InterruptedException {
        ContentResolver contentResolver = mContext.getContentResolver();
        int mode = doAndWaitForSettingChange(() -> mController.onDeveloperOptionsSwitchDisabled(),
                contentResolver);
        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
        assertFalse(mPreference.isEnabled());
        assertFalse(mPreference.isChecked());
    }

    private int doAndWaitForSettingChange(Runnable runnable, ContentResolver contentResolver) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ContentObserver settingsObserver =
                new ContentObserver(new Handler(mLooper)) {
                    @Override
                    public void onChange(boolean selfChange, Uri uri) {
                        countDownLatch.countDown();
                    }
                };
        contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ENABLE_BACK_ANIMATION),
                false, settingsObserver, UserHandle.USER_SYSTEM
        );
        runnable.run();
        try {
            countDownLatch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        return Settings.Global.getInt(contentResolver,
                Settings.Global.ENABLE_BACK_ANIMATION, -1 /* default */);
    }
}
