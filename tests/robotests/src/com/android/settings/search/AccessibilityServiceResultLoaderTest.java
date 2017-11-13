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

package com.android.settings.search;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.TestConfig;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AccessibilityServiceResultLoaderTest {

    private static final String QUERY = "test_query";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private AccessibilityManager mAccessibilityManager;
    @Mock
    private SiteMapManager mSiteMapManager;

    private AccessibilityServiceResultLoader mLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.ACCESSIBILITY_SERVICE))
                .thenReturn(mAccessibilityManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        mLoader = new AccessibilityServiceResultLoader(mContext, QUERY, mSiteMapManager);
    }

    @Test
    public void query_noService_shouldNotReturnAnything() {
        assertThat(mLoader.loadInBackground()).isEmpty();
    }

    @Test
    public void query_hasServiceMatchingTitle_shouldReturnResult() {
        addFakeAccessibilityService();

        List<? extends SearchResult> results = new ArrayList<>(mLoader.loadInBackground());
        assertThat(results).hasSize(1);

        SearchResult result = results.get(0);
        assertThat(result.title).isEqualTo(QUERY);
    }

    @Test
    public void query_serviceDoesNotMatchTitle_shouldReturnResult() {
        addFakeAccessibilityService();

        mLoader = new AccessibilityServiceResultLoader(mContext,
                QUERY + "no_match", mSiteMapManager);

        assertThat(mLoader.loadInBackground()).isEmpty();
    }

    private void addFakeAccessibilityService() {
        final List<AccessibilityServiceInfo> services = new ArrayList<>();
        final AccessibilityServiceInfo info = mock(AccessibilityServiceInfo.class);
        final ResolveInfo resolveInfo = mock(ResolveInfo.class);
        when(info.getResolveInfo()).thenReturn(resolveInfo);
        when(resolveInfo.loadIcon(mPackageManager)).thenReturn(new ColorDrawable(Color.BLUE));
        when(resolveInfo.loadLabel(mPackageManager)).thenReturn(QUERY);
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = "pkg";
        resolveInfo.serviceInfo.name = "class";
        services.add(info);

        when(mAccessibilityManager.getInstalledAccessibilityServiceList()).thenReturn(services);
    }
}
