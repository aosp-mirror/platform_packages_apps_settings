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

package com.android.settings.fingerprint;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.fingerprint.FingerprintSettings.FingerprintPreference;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class FingerprintPreferenceTest {

    @Mock
    private FingerprintPreference.OnDeleteClickListener mOnDeleteClickListener;

    private Context mContext;
    private FingerprintPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mPreference = new FingerprintPreference(mContext, mOnDeleteClickListener);
    }

    @Test
    public void shouldShowDeleteButton() {
        assertThat(mPreference.getSecondTargetResId()).isEqualTo(R.layout.preference_widget_delete);
    }

    @Test
    public void bindAndClickDeleteButton_shouldInvokeOnDeleteListener() {
        final FrameLayout layout = new FrameLayout(mContext);
        final View deleteButton = LayoutInflater.from(mContext)
                .inflate(mPreference.getSecondTargetResId(), layout, true);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(layout);
        mPreference.onBindViewHolder(holder);

        layout.findViewById(R.id.delete_button).performClick();

        verify(mOnDeleteClickListener).onDeleteClick(mPreference);
    }
}
