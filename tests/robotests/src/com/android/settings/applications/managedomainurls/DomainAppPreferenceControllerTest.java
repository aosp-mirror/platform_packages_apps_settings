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
 * limitations under the License.
 */

package com.android.settings.applications.managedomainurls;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.IconDrawableFactory;
import android.view.View;
import android.widget.ProgressBar;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
public class DomainAppPreferenceControllerTest {

    private ApplicationsState.AppEntry mAppEntry;
    private Context mContext;
    private IconDrawableFactory mIconDrawableFactory;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mIconDrawableFactory = IconDrawableFactory.newInstance(mContext);
        mAppEntry = new ApplicationsState.AppEntry(
                mContext, createApplicationInfo(mContext.getPackageName()), 0);
    }

    @Test
    public void getLayoutResource_shouldUseAppPreferenceLayout() {
        final DomainAppPreference pref = new DomainAppPreference(
                mContext, mIconDrawableFactory, mAppEntry);

        assertThat(pref.getLayoutResource()).isEqualTo(R.layout.preference_app);
    }

    @Test
    public void onBindViewHolder_shouldSetAppendixViewToGone() {
        final DomainAppPreference pref = new DomainAppPreference(
                mContext, mIconDrawableFactory, mAppEntry);
        final View holderView = mock(View.class);
        final View appendixView = mock(View.class);
        when(holderView.findViewById(R.id.summary_container)).thenReturn(mock(View.class));
        when(holderView.findViewById(android.R.id.progress)).thenReturn(mock(ProgressBar.class));
        when(holderView.findViewById(R.id.appendix)).thenReturn(appendixView);

        pref.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(holderView));

        verify(appendixView).setVisibility(View.GONE);
    }

    private ApplicationInfo createApplicationInfo(String packageName) {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.sourceDir = "foo";
        appInfo.flags |= ApplicationInfo.FLAG_INSTALLED;
        appInfo.storageUuid = UUID.randomUUID();
        appInfo.packageName = packageName;
        return appInfo;
    }
}
