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

package com.android.settings.print;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Looper;
import android.view.View;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class PrintSettingsFragmentTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();

    private PrintSettingsFragment mFragment;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            mFragment = spy(new PrintSettingsFragment());
            doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        });
    }

    @Test
    public void setupPreferences_uiIsRestricted_doNotAddPreferences() {
        mFragment.mIsUiRestricted = true;

        mFragment.setupPreferences();

        verify(mFragment, never()).findPreference(any(CharSequence.class));
    }

    @Test
    public void setupEmptyViews_uiIsRestricted_doNotSetEmptyView() {
        mFragment.mIsUiRestricted = true;

        mFragment.setupEmptyViews();

        verify(mFragment, never()).setEmptyView(any(View.class));
    }

    @Test
    public void startSettings_uiIsRestricted_removeAllPreferences() {
        mFragment.mIsUiRestricted = true;

        mFragment.startSettings();

        assertThat(mPreferenceScreen.getPreferenceCount()).isEqualTo(0);
        verify(mFragment, never()).setHasOptionsMenu(true);
    }
}
