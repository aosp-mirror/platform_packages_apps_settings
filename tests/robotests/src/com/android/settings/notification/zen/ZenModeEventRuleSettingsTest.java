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

package com.android.settings.notification.zen;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Looper;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ZenModeEventRuleSettingsTest {

    @Mock
    private FragmentActivity mActivity;

    @Mock
    private Intent mIntent;

    private ZenModeEventRuleSettings mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();

        Resources res = mContext.getResources();
        when(mActivity.getTheme()).thenReturn(res.newTheme());
        when(mActivity.getIntent()).thenReturn(mIntent);
        when(mActivity.getResources()).thenReturn(res);
        when(mActivity.getMainLooper()).thenReturn(mock(Looper.class));

        mFragment = spy(new ZenModeEventRuleSettings());
        when(mFragment.getActivity()).thenReturn(mActivity);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getResources()).thenReturn(res);
        mFragment.onAttach(mContext);
    }

    @Test
    public void onAttach_noRuleId_shouldToastAndFinishAndNoCrash() {
        final String expected = mContext.getString(R.string.zen_mode_rule_not_found_text);

        // verify the toast
        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(expected);

        // verify the finish
        verify(mActivity).finish();

        //should not crash
    }

    @Test
    public void testNoDuplicateCalendars() {
        List<ZenModeEventRuleSettings.CalendarInfo> calendarsList = new ArrayList<>();
        mFragment.addCalendar(1234, "calName", 1, calendarsList);
        mFragment.addCalendar(1234, "calName", 2, calendarsList);
        mFragment.addCalendar(1234, "calName", 3, calendarsList);
        assertThat(calendarsList.size()).isEqualTo(1);
    }
}
