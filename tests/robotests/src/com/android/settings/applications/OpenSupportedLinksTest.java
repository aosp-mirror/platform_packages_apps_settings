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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.ArraySet;

import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.widget.FooterPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class OpenSupportedLinksTest {
    private static final String TEST_FOOTER_TITLE = "FooterTitle";
    private static final String TEST_DOMAIN_LINK = "aaa.bbb.ccc";

    private Context mContext;
    private TestFragment mSettings;
    private FooterPreference mFooter;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mSettings = spy(new TestFragment(mContext));
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

    public static class TestFragment extends OpenSupportedLinks {
        private final Context mContext;

        public TestFragment(Context context) {
            mContext = context;
            mPackageInfo = new PackageInfo();
            mPackageInfo.packageName = "ssl.test.package.com";
        }

        @Override
        protected PackageManager getPackageManager() {
            return mContext.getPackageManager();
        }
    }
}
