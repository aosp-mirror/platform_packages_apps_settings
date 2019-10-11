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
package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiScanningRequiredFragmentTest {

    private WifiScanningRequiredFragment mFragment;
    private Context mContext;
    private ContentResolver mResolver;
    @Mock
    private Fragment mCallbackFragment;
    @Mock
    private AlertDialog.Builder mBuilder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = spy(WifiScanningRequiredFragment.newInstance());
        mContext = RuntimeEnvironment.application;
        mResolver = mContext.getContentResolver();

        doReturn(mContext).when(mFragment).getContext();
        mFragment.setTargetFragment(mCallbackFragment, 1000);
        Settings.Global.putInt(mResolver, Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0);
    }

    @Test
    public void onClick_positiveButtonSetsWifiScanningOn()
            throws Settings.SettingNotFoundException {
        mFragment.onClick(null, DialogInterface.BUTTON_POSITIVE);

        assertThat(Settings.Global.getInt(mResolver, Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE))
                .isEqualTo(1);
    }

    @Test
    public void onClick_positiveButtonCallsBackToActivity() {
        mFragment.onClick(null, DialogInterface.BUTTON_POSITIVE);

        verify(mCallbackFragment).onActivityResult(anyInt(), anyInt(), isNull());
    }

    @Test
    public void learnMore_launchesHelpWhenIntentFound() {
        Context context = mock(Context.class);
        doReturn(context).when(mFragment).getContext();
        doReturn("").when(context).getString(eq(R.string.help_uri_wifi_scanning_required));
        mFragment.addButtonIfNeeded(mBuilder);
        verify(mBuilder, never())
                .setNeutralButton(anyInt(), nullable(DialogInterface.OnClickListener.class));

        doReturn("help").when(context).getString(eq(R.string.help_uri_wifi_scanning_required));
        mFragment.addButtonIfNeeded(mBuilder);
        verify(mBuilder, times(1))
                .setNeutralButton(anyInt(), nullable(DialogInterface.OnClickListener.class));
    }
}
