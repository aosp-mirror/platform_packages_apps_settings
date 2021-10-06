/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.content.Context.MODE_PRIVATE;

import static com.android.settings.accessibility.ToggleAutoclickCustomSeekbarController.KEY_CUSTOM_DELAY_VALUE;
import static com.android.settings.accessibility.ToggleAutoclickPreferenceController.KEY_DELAY_MODE;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.lifecycle.LifecycleObserver;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ToggleAutoclickCustomSeekbarController}. */
@RunWith(RobolectricTestRunner.class)
public class ToggleAutoclickCustomSeekbarControllerTest {

    @Mock
    private PreferenceScreen mScreen;

    @Mock
    private LayoutPreference mLayoutPreference;

    @Mock
    private Lifecycle mLifecycle;

    private SharedPreferences mSharedPreferences;
    private TextView mDelayLabel;
    private ImageView mShorter;
    private ImageView mLonger;
    private SeekBar mSeekBar;
    private ToggleAutoclickCustomSeekbarController mController;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final String mPrefKey = "prefKey";
        mContext = ApplicationProvider.getApplicationContext();
        mSharedPreferences = mContext.getSharedPreferences(mContext.getPackageName(), MODE_PRIVATE);
        mDelayLabel = new TextView(mContext);
        mShorter = new ImageView(mContext);
        mLonger = new ImageView(mContext);
        mSeekBar = new SeekBar(mContext);
        mController =
                new ToggleAutoclickCustomSeekbarController(mContext, mLifecycle, mPrefKey);

        doReturn(mLayoutPreference).when(mScreen).findPreference(mPrefKey);
        doReturn(mSeekBar).when(mLayoutPreference).findViewById(R.id.autoclick_delay);
        doReturn(mDelayLabel).when(mLayoutPreference).findViewById(R.id.current_label);
        doReturn(mShorter).when(mLayoutPreference).findViewById(R.id.shorter);
        doReturn(mLonger).when(mLayoutPreference).findViewById(R.id.longer);
    }

    @Test
    public void getAvailabilityStatus_available() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void constructor_hasLifecycle_addObserver() {
        verify(mLifecycle).addObserver(any(LifecycleObserver.class));
    }

    @Test
    public void displayPreference_initSeekBar() {
        mSharedPreferences.edit().putInt(KEY_CUSTOM_DELAY_VALUE, 700).apply();

        mController.onResume();
        mController.displayPreference(mScreen);
        mController.onPause();
        final SeekBar.OnSeekBarChangeListener mListener =
                shadowOf(mSeekBar).getOnSeekBarChangeListener();

        assertThat(mSeekBar.getMax()).isEqualTo(8);
        assertThat(mSeekBar.getProgress()).isEqualTo(5);
        assertThat(mListener).isNotNull();
    }

    @Test
    public void displayPreference_initDelayLabel() {
        mSharedPreferences.edit().putInt(KEY_CUSTOM_DELAY_VALUE, 700).apply();

        mController.onResume();
        mController.displayPreference(mScreen);
        mController.onPause();

        assertThat(mDelayLabel.getText()).isEqualTo("0.7 seconds");
    }

    @Test
    public void onSharedPreferenceChanged_delayMode_updateCustomDelayValue() {
        mSharedPreferences.edit().putInt(KEY_CUSTOM_DELAY_VALUE, 700).apply();

        mController.displayPreference(mScreen);
        mController.onSharedPreferenceChanged(mSharedPreferences, KEY_DELAY_MODE);
        final int actualDelayValue =
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY, /* def= */ 0);
        final int actualCustomDelayValue =
                mSharedPreferences.getInt(KEY_CUSTOM_DELAY_VALUE, /* defValue= */ 0);

        assertThat(mDelayLabel.getText()).isEqualTo("0.7 seconds");
        assertThat(mSeekBar.getProgress()).isEqualTo(5);
        assertThat(actualDelayValue).isEqualTo(700);
        assertThat(actualCustomDelayValue).isEqualTo(700);
    }

    @Test
    public void onSeekBarProgressChanged_updateCustomDelayValue() {
        mSharedPreferences.edit().putInt(KEY_CUSTOM_DELAY_VALUE, 700).apply();

        mController.displayPreference(mScreen);
        mController.mSeekBarChangeListener.onProgressChanged(mock(SeekBar.class),
                /* value= */ 8,
                true);
        final int actualDelayValue =
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY, /* def= */ 0);
        final int actualCustomDelayValue =
                mSharedPreferences.getInt(KEY_CUSTOM_DELAY_VALUE, /* defValue= */ 0);

        assertThat(mDelayLabel.getText()).isEqualTo("1 second");
        assertThat(mSeekBar.getProgress()).isEqualTo(8);
        assertThat(actualDelayValue).isEqualTo(1000);
        assertThat(actualCustomDelayValue).isEqualTo(1000);
    }

    @Test
    public void onShorterClicked_updateCustomDelayValue() {
        mSharedPreferences.edit().putInt(KEY_CUSTOM_DELAY_VALUE, 700).apply();

        mController.displayPreference(mScreen);
        mShorter.callOnClick();
        final int actualDelayValue =
                Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY, /* def= */ 0);
        final int actualCustomDelayValue =
                mSharedPreferences.getInt(KEY_CUSTOM_DELAY_VALUE, /* defValue= */ 0);

        assertThat(mSeekBar.getProgress()).isEqualTo(4);
        assertThat(mDelayLabel.getText()).isEqualTo("0.6 seconds");
        assertThat(actualDelayValue).isEqualTo(600);
        assertThat(actualCustomDelayValue).isEqualTo(600);
    }

    @Test
    public void onLongerClicked_updateCustomDelayValue() {
        mSharedPreferences.edit().putInt(KEY_CUSTOM_DELAY_VALUE, 700).apply();

        mController.displayPreference(mScreen);
        mLonger.callOnClick();
        final int actualDelayValue =
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY, /* def= */ 0);
        final int actualCustomDelayValue =
                mSharedPreferences.getInt(KEY_CUSTOM_DELAY_VALUE, /* defValue= */ 0);

        assertThat(mSeekBar.getProgress()).isEqualTo(6);
        assertThat(mDelayLabel.getText()).isEqualTo("0.8 seconds");
        assertThat(actualDelayValue).isEqualTo(800);
        assertThat(actualCustomDelayValue).isEqualTo(800);
    }
}
