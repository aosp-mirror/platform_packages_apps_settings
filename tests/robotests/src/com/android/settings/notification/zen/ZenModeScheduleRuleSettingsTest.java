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

import static android.platform.test.flag.junit.SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Looper;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenModeConfig;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.Calendar;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class ZenModeScheduleRuleSettingsTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(DEVICE_DEFAULT);

    @Mock
    private FragmentActivity mActivity;
    @Mock
    private Intent mIntent;
    @Mock
    private ZenModeBackend mBackend;

    private ZenModeScheduleRuleSettings mFragment;
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

        mFragment = spy(new ZenModeScheduleRuleSettings());
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
    @EnableFlags({Flags.FLAG_MODES_API, Flags.FLAG_MODES_UI})
    public void updateScheduleRule_updatesConditionAndTriggerDescription() {
        mFragment.setBackend(mBackend);
        mFragment.mId = "id";
        mFragment.mRule = new AutomaticZenRule.Builder("name", Uri.parse("condition")).build();

        ZenModeConfig.ScheduleInfo scheduleInfo = new ZenModeConfig.ScheduleInfo();
        scheduleInfo.days = new int[] { Calendar.MONDAY };
        scheduleInfo.startHour = 1;
        scheduleInfo.endHour = 2;
        mFragment.updateScheduleRule(scheduleInfo);

        ArgumentCaptor<AutomaticZenRule> updatedRuleCaptor = ArgumentCaptor.forClass(
                AutomaticZenRule.class);
        verify(mBackend).updateZenRule(eq("id"), updatedRuleCaptor.capture());
        assertThat(updatedRuleCaptor.getValue().getConditionId())
                .isEqualTo(ZenModeConfig.toScheduleConditionId(scheduleInfo));
        assertThat(updatedRuleCaptor.getValue().getTriggerDescription()).isNotEmpty();
    }
}
