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

package com.android.settings.notification.zen;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Looper;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
public class ZenModeScheduleRuleSettingsTest {

    @Mock
    private FragmentActivity mActivity;

    @Mock
    private Intent mIntent;

    @Mock
    private NotificationManager mNotificationManager;

    private TestFragment mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);
        mContext = application;

        mFragment = spy(new TestFragment());
        mFragment.onAttach(application);

        doReturn(mActivity).when(mFragment).getActivity();

        Resources res = application.getResources();

        doReturn(res).when(mFragment).getResources();
        when(mActivity.getTheme()).thenReturn(res.newTheme());
        when(mActivity.getIntent()).thenReturn(mIntent);
        when(mActivity.getResources()).thenReturn(res);
        when(mActivity.getMainLooper()).thenReturn(mock(Looper.class));
        when(mFragment.getContext()).thenReturn(mContext);
    }

    @Test
    public void onCreate_noRuleId_shouldToastAndFinishAndNoCrash() {
        final String expected = mContext.getString(R.string.zen_mode_rule_not_found_text);

        mFragment.onCreate(null);

        // verify the toast
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expected);

        // verify the finish
        verify(mActivity).finish();

        //should not crash
    }

    public static class TestFragment extends ZenModeScheduleRuleSettings {

        @Override
        protected Object getSystemService(final String name) {
            return null;
        }
    }
}
