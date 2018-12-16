/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.datausage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CellDataPreferenceTest {

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubInfo;

    private Context mContext;
    private PreferenceViewHolder mHolder;
    private CellDataPreference mPreference;
    private SubscriptionManager.OnSubscriptionsChangedListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPreference = new CellDataPreference(mContext, null);
        mListener = mPreference.mOnSubscriptionsChangeListener;

        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(mPreference.getLayoutResource(),
                new LinearLayout(mContext), false);

        mHolder = PreferenceViewHolder.createInstanceForTests(view);
    }

    @Test
    public void noActiveSub_shouldDisable() {
        mPreference.mSubscriptionManager = mSubscriptionManager;
        mListener.onSubscriptionsChanged();
        assertThat(mPreference.isEnabled()).isFalse();

        when(mSubscriptionManager.getActiveSubscriptionInfo(anyInt())).thenReturn(mSubInfo);
        mListener.onSubscriptionsChanged();
        assertThat(mPreference.isEnabled()).isTrue();
    }
}
