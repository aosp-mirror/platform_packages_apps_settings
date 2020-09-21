/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for {@link DividerAllowedBelowPreference}. */
@RunWith(RobolectricTestRunner.class)
public class DividerAllowedBelowPreferenceTest {
    private final Context mContext = RuntimeEnvironment.application;
    private PreferenceViewHolder mViewHolder;

    @Before
    public void setUp() {
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View rootView =
                inflater.inflate(R.layout.preference, /* root= */ null);
        mViewHolder = spy(PreferenceViewHolder.createInstanceForTests(rootView));
    }

    @Test
    public void onBindViewHolder_dividerAllowedBelow() {
        final DividerAllowedBelowPreference dividerAllowedBelowPreference =
                new DividerAllowedBelowPreference(mContext);

        dividerAllowedBelowPreference.onBindViewHolder(mViewHolder);

        // One time was in parent, the other time was in child.
        verify(mViewHolder, times(2)).setDividerAllowedBelow(true);
    }
}
