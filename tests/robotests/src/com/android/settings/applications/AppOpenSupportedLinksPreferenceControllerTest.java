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
package com.android.settings.applications;

import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ASK;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.ArraySet;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class AppOpenSupportedLinksPreferenceControllerTest {
    private static final String TEST_KEY = "test_key";
    private static final String TEST_DOMAIN_LINK = "aaa.bbb.ccc";
    private static final String TEST_PACKAGE = "ssl.test.package.com";

    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mScreen;
    private PreferenceCategory mCategory;
    private AppOpenSupportedLinksPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = spy(mPreferenceManager.createPreferenceScreen(mContext));
        mCategory = spy(new PreferenceCategory(mContext));
        mController = spy(new AppOpenSupportedLinksPreferenceController(mContext, TEST_KEY));
        mController.setInit(TEST_PACKAGE);
    }

    @Test
    public void displayPreference_statusAlways_allowOpenChecked() {
        init(INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS);

        mController.displayPreference(mScreen);

        assertThat(mController.mAllowOpening.isChecked()).isTrue();
        assertThat(mController.mAskEveryTime.isChecked()).isFalse();
        assertThat(mController.mNotAllowed.isChecked()).isFalse();
    }

    @Test
    public void displayPreference_statusAsk_askEveryTimeChecked() {
        init(INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ASK);

        mController.displayPreference(mScreen);

        assertThat(mController.mAllowOpening.isChecked()).isFalse();
        assertThat(mController.mAskEveryTime.isChecked()).isTrue();
        assertThat(mController.mNotAllowed.isChecked()).isFalse();
    }

    @Test
    public void displayPreference_statusNever_notAllowedChecked() {
        init(INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER);

        mController.displayPreference(mScreen);

        assertThat(mController.mAllowOpening.isChecked()).isFalse();
        assertThat(mController.mAskEveryTime.isChecked()).isFalse();
        assertThat(mController.mNotAllowed.isChecked()).isTrue();
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void getEntriesNo_oneHandledDomains_returnOne() {
        initHandledDomains();

        assertThat(mController.getEntriesNo()).isEqualTo(1);
    }

    private void init(int status) {
        doReturn(mCategory).when(mScreen).findPreference(any(CharSequence.class));
        doReturn(true).when(mCategory).addPreference(any(Preference.class));
        when(mPackageManager.getIntentVerificationStatusAsUser(anyString(), anyInt())).thenReturn(
                status);
    }

    private void initHandledDomains() {
        final ArraySet<String> domainLinks = new ArraySet<>();
        domainLinks.add(TEST_DOMAIN_LINK);
        ShadowUtils.setHandledDomains(domainLinks);
    }
}
