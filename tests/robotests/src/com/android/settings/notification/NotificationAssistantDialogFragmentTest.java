/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.notification;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;

import androidx.fragment.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class NotificationAssistantDialogFragmentTest {

    private Context mContext;
    @Mock
    private ConfigureNotificationSettings mFragment;
    private NotificationAssistantDialogFragment mDialogFragment;
    @Mock
    private FragmentActivity mActivity;

    ComponentName mComponentName = new ComponentName("a", "b");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mDialogFragment =
                spy(NotificationAssistantDialogFragment.newInstance(mFragment, mComponentName));
        doReturn(mActivity).when(mDialogFragment).getActivity();
        doReturn(mContext).when(mDialogFragment).getContext();

    }


    @Test
    public void testClickOK_callEnableNAS() {
        mDialogFragment.onClick(null, DialogInterface.BUTTON_POSITIVE);

        verify(mFragment, times(1)).enableNAS(eq(mComponentName));
    }
}
