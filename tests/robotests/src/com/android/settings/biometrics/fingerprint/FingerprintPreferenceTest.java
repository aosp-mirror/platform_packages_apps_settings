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

package com.android.settings.biometrics.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.biometrics.fingerprint.FingerprintSettings.FingerprintPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
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
        LayoutInflater.from(mContext).inflate(mPreference.getSecondTargetResId(), layout, true);
        final PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(layout);
        mPreference.onBindViewHolder(holder);

        final View view = layout.findViewById(R.id.delete_button);
        assertThat(view).isNotNull();
        view.performClick();

        verify(mOnDeleteClickListener).onDeleteClick(mPreference);
    }
}
