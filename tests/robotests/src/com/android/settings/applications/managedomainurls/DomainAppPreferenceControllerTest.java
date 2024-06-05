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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;

import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
public class DomainAppPreferenceControllerTest {

    private ApplicationsState.AppEntry mAppEntry;
    private Context mContext;

    @Mock
    private DomainVerificationManager mDomainVerificationManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mAppEntry = new ApplicationsState.AppEntry(
                mContext, createApplicationInfo(mContext.getPackageName()), 0);
        when(mContext.getSystemService(DomainVerificationManager.class)).thenReturn(
                mDomainVerificationManager);
    }

    @Test
    public void getLayoutResource_shouldUseAppPreferenceLayout()
            throws PackageManager.NameNotFoundException {
        final DomainVerificationUserState domainVerificationUserState = mock(
                DomainVerificationUserState.class);
        doReturn(domainVerificationUserState).when(
                mDomainVerificationManager).getDomainVerificationUserState(anyString());
        doReturn(true).when(domainVerificationUserState).isLinkHandlingAllowed();
        final DomainAppPreference pref = new DomainAppPreference(mContext, mAppEntry);

        assertThat(pref.getLayoutResource())
                .isEqualTo(com.android.settingslib.widget.preference.app.R.layout.preference_app);
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
