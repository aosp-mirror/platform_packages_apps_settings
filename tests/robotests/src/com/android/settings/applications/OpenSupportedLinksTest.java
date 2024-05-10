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
import android.content.res.Resources;
import android.util.ArraySet;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.widget.FooterPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = {
        ShadowUtils.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class OpenSupportedLinksTest {
    private static final String TEST_FOOTER_TITLE = "FooterTitle";
    private static final String TEST_DOMAIN_LINK = "aaa.bbb.ccc";
    private static final String TEST_SUMMARY = "TestSummary";
    private static final String TEST_PACKAGE = "ssl.test.package.com";

    @Mock
    private PackageManager mPackageManager;
    @Mock
    private Resources mResources;

    private Context mContext;
    private TestFragment mSettings;
    private FooterPreference mFooter;
    private PreferenceCategory mCategory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mSettings = spy(new TestFragment(mContext, mPackageManager));
        mCategory = spy(new PreferenceCategory(mContext));
        mFooter = new FooterPreference.Builder(mContext).setTitle(TEST_FOOTER_TITLE).build();
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void addLinksToFooter_noHandledDomains_returnDefaultFooterTitle() {
        mSettings.addLinksToFooter(mFooter);

        assertThat(mFooter.getTitle()).isEqualTo(TEST_FOOTER_TITLE);
    }

    @Test
    public void addLinksToFooter_oneHandledDomains_returnDomainsFooterTitle() {
        final ArraySet<String> domainLinks = new ArraySet<>();
        domainLinks.add(TEST_DOMAIN_LINK);
        ShadowUtils.setHandledDomains(domainLinks);

        mSettings.addLinksToFooter(mFooter);

        assertThat(mFooter.getTitle().toString()).contains(TEST_DOMAIN_LINK);
    }

    @Test
    public void initRadioPreferencesGroup_statusAlways_allowOpenChecked() {
        init(INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS);

        mSettings.initRadioPreferencesGroup();

        assertThat(mSettings.mAllowOpening.isChecked()).isTrue();
        assertThat(mSettings.mAskEveryTime.isChecked()).isFalse();
        assertThat(mSettings.mNotAllowed.isChecked()).isFalse();
    }

    @Test
    public void initRadioPreferencesGroup_statusAsk_askEveryTimeChecked() {
        init(INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ASK);

        mSettings.initRadioPreferencesGroup();

        assertThat(mSettings.mAllowOpening.isChecked()).isFalse();
        assertThat(mSettings.mAskEveryTime.isChecked()).isTrue();
        assertThat(mSettings.mNotAllowed.isChecked()).isFalse();
    }

    @Test
    public void initRadioPreferencesGroup_statusNever_notAllowedChecked() {
        init(INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER);

        mSettings.initRadioPreferencesGroup();

        assertThat(mSettings.mAllowOpening.isChecked()).isFalse();
        assertThat(mSettings.mAskEveryTime.isChecked()).isFalse();
        assertThat(mSettings.mNotAllowed.isChecked()).isTrue();
    }

    @Test
    public void getEntriesNo_oneHandledDomains_returnOne() {
        initHandledDomains();

        assertThat(mSettings.getEntriesNo()).isEqualTo(1);
    }

    private void init(int status) {
        doReturn(status).when(mPackageManager).getIntentVerificationStatusAsUser(anyString(),
                anyInt());
        doReturn(mCategory).when(mSettings).findPreference(any(CharSequence.class));
        doReturn(mResources).when(mSettings).getResources();
        doReturn(true).when(mCategory).addPreference(any(Preference.class));
        when(mResources.getString(R.string.app_link_open_always_summary))
                .thenReturn("App claims to handle # links");
    }

    public static class TestFragment extends OpenSupportedLinks {
        private final Context mContext;

        public TestFragment(Context context, PackageManager packageManager) {
            mContext = context;
            mPackageManager = packageManager;
            mPackageName = TEST_PACKAGE;
        }
    }

    private void initHandledDomains() {
        final ArraySet<String> domainLinks = new ArraySet<>();
        domainLinks.add(TEST_DOMAIN_LINK);
        ShadowUtils.setHandledDomains(domainLinks);
    }
}
