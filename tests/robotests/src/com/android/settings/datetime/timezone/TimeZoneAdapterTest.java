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
package com.android.settings.datetime.timezone;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.icu.util.TimeZone;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Collections;
import java.util.Locale;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = TimeZoneAdapterTest.ShadowDataFormat.class)
public class TimeZoneAdapterTest {

    @Mock
    private View.OnClickListener mOnClickListener;

    private TimeZoneAdapter mTimeZoneAdapter;

    private Context mContext;
    private Locale mDefaultLocale;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mTimeZoneAdapter = new TimeZoneAdapter(mOnClickListener, mContext);
        mDefaultLocale = Locale.getDefault();
    }

    @After
    public void tearDown() {
        Locale.setDefault(mDefaultLocale);
    }

    @Test
    public void getItemViewType_onDefaultTimeZone_returnsTypeSelected() {
        final TimeZoneInfo tzi = dummyTimeZoneInfo(TimeZone.getDefault());
        mTimeZoneAdapter.setTimeZoneInfos(Collections.singletonList(tzi));
        assertThat(mTimeZoneAdapter.getItemViewType(0))
            .isEqualTo(TimeZoneAdapter.VIEW_TYPE_SELECTED);
    }

    @Test
    public void getItemViewType_onNonDefaultTimeZone_returnsTypeNormal() {
        final TimeZoneInfo tzi = dummyTimeZoneInfo(getNonDefaultTimeZone());
        mTimeZoneAdapter.setTimeZoneInfos(Collections.singletonList(tzi));
        assertThat(mTimeZoneAdapter.getItemViewType(0)).isEqualTo(TimeZoneAdapter.VIEW_TYPE_NORMAL);
    }

    @Test
    public void bindViewHolder_onDstTimeZone_showsDstLabel() {
        final TimeZoneInfo tzi = dummyTimeZoneInfo(TimeZone.getTimeZone("America/Los_Angeles"));
        mTimeZoneAdapter.setTimeZoneInfos(Collections.singletonList(tzi));

        final FrameLayout parent = new FrameLayout(RuntimeEnvironment.application);

        final ViewHolder viewHolder =
            (ViewHolder) mTimeZoneAdapter.createViewHolder(parent, TimeZoneAdapter.VIEW_TYPE_NORMAL);
        mTimeZoneAdapter.bindViewHolder(viewHolder, 0);
        assertThat(viewHolder.mDstView).isNotNull();
        assertThat(viewHolder.mDstView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewHolder_onNonDstTimeZone_hidesDstLabel() {
        final TimeZoneInfo tzi = dummyTimeZoneInfo(TimeZone.getTimeZone("Etc/UTC"));
        mTimeZoneAdapter.setTimeZoneInfos(Collections.singletonList(tzi));

        final FrameLayout parent = new FrameLayout(RuntimeEnvironment.application);

        final ViewHolder viewHolder =
            (ViewHolder) mTimeZoneAdapter.createViewHolder(parent, TimeZoneAdapter.VIEW_TYPE_NORMAL);
        mTimeZoneAdapter.bindViewHolder(viewHolder, 0);
        assertThat(viewHolder.mDstView).isNotNull();
        assertThat(viewHolder.mDstView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewHolder_on24Hour() {
        Locale.setDefault(Locale.US);
        ShadowDataFormat.mTimeFormatString = "HH:mm";
        mTimeZoneAdapter = new TimeZoneAdapter(mOnClickListener, mContext);

        final TimeZoneInfo tzi = dummyTimeZoneInfo(TimeZone.getTimeZone("Etc/UTC"));
        mTimeZoneAdapter.setTimeZoneInfos(Collections.singletonList(tzi));

        final FrameLayout parent = new FrameLayout(RuntimeEnvironment.application);

        final ViewHolder viewHolder =
            (ViewHolder) mTimeZoneAdapter.createViewHolder(parent, TimeZoneAdapter.VIEW_TYPE_NORMAL);
        mTimeZoneAdapter.bindViewHolder(viewHolder, 0);
        assertThat(viewHolder.mTimeView).isNotNull();
        assertThat(viewHolder.mTimeView.getText().toString()).hasLength(5);
    }

    @Test
    public void bindViewHolder_on12Hour() {
        Locale.setDefault(Locale.US);
        ShadowDataFormat.mTimeFormatString = "hh:mm a";
        mTimeZoneAdapter = new TimeZoneAdapter(mOnClickListener, mContext);

        final TimeZoneInfo tzi = dummyTimeZoneInfo(TimeZone.getTimeZone("Etc/UTC"));
        mTimeZoneAdapter.setTimeZoneInfos(Collections.singletonList(tzi));

        final FrameLayout parent = new FrameLayout(RuntimeEnvironment.application);

        final ViewHolder viewHolder =
            (ViewHolder) mTimeZoneAdapter.createViewHolder(parent, TimeZoneAdapter.VIEW_TYPE_NORMAL);
        mTimeZoneAdapter.bindViewHolder(viewHolder, 0);
        assertThat(viewHolder.mTimeView).isNotNull();
        assertThat(viewHolder.mTimeView.getText().toString()).hasLength(8);
    }

    // Pick an arbitrary time zone that's not the current default.
    private static TimeZone getNonDefaultTimeZone() {
        final String[] availableIDs = TimeZone.getAvailableIDs();
        int index = 0;
        if (TextUtils.equals(availableIDs[index], TimeZone.getDefault().getID())) {
            index++;
        }
        return TimeZone.getTimeZone(availableIDs[index]);
    }

    private TimeZoneInfo dummyTimeZoneInfo(TimeZone timeZone) {
        return new TimeZoneInfo.Builder(timeZone).setGmtOffset("GMT+0").setItemId(1).build();
    }

    @Implements(android.text.format.DateFormat.class)
    public static class ShadowDataFormat {

        private static String mTimeFormatString = "";

        @Implementation
        public static String getTimeFormatString(Context context) {
            return mTimeFormatString;
        }
    }
}
