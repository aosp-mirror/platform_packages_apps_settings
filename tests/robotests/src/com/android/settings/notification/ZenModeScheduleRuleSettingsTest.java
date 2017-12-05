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
 * limitations under the License
 */

package com.android.settings.notification;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                SettingsShadowResources.class,
                SettingsShadowResources.SettingsShadowTheme.class,
        })
public class ZenModeScheduleRuleSettingsTest {

    @Mock
    private Activity mActivity;

    @Mock
    private Intent mIntent;

    @Mock
    private UserManager mUserManager;

    private TestFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFragment = spy(new TestFragment());
        mFragment.onAttach(application);

        doReturn(mActivity).when(mFragment).getActivity();

        Resources res = application.getResources();

        doReturn(res).when(mFragment).getResources();
        when(mActivity.getTheme()).thenReturn(res.newTheme());
        when(mActivity.getIntent()).thenReturn(mIntent);
        when(mActivity.getResources()).thenReturn(res);
        when(mFragment.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
    }

    @Test
    public void onCreate_noRuleId_shouldToastAndFinishAndNoCrash() {
        final Context ctx = application.getApplicationContext();
        final String expected = ctx.getResources().getString(R.string.zen_mode_rule_not_found_text);

        mFragment.onCreate(null);

        // verify the toast
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expected);

        // verify the finish
        verify(mActivity).finish();

        //shoud not crash
    }

    public static class TestFragment extends ZenModeScheduleRuleSettings {

        @Override
        protected Object getSystemService(final String name) {
            return null;
        }
    }

}
