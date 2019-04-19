/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.settings.development.gup;

import static com.android.settings.development.gup.GupEnableForAllAppsPreferenceController.GUP_DEFAULT;
import static com.android.settings.development.gup.GupEnableForAllAppsPreferenceController.GUP_OFF;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.widget.SwitchWidgetController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GupGlobalSwitchBarControllerTest {
    @Mock
    private SwitchBar mSwitchBar;
    @Mock
    private SwitchWidgetController mSwitchWidgetController;
    @Mock
    private GameDriverContentObserver mGameDriverContentObserver;

    private Context mContext;
    private ContentResolver mResolver;
    private GupGlobalSwitchBarController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mResolver = mContext.getContentResolver();
    }

    @Test
    public void constructor_gupOn_shouldCheckSwitchBar() {
        Settings.Global.putInt(mResolver, Settings.Global.GUP_DEV_ALL_APPS, GUP_DEFAULT);
        mController =
                new GupGlobalSwitchBarController(mContext, new SwitchBarController(mSwitchBar));

        verify(mSwitchBar).setChecked(true);
    }

    @Test
    public void constructor_gupOff_shouldUncheckSwitchBar() {
        Settings.Global.putInt(mResolver, Settings.Global.GUP_DEV_ALL_APPS, GUP_OFF);
        mController =
                new GupGlobalSwitchBarController(mContext, new SwitchBarController(mSwitchBar));

        verify(mSwitchBar).setChecked(false);
    }

    @Test
    public void onStart_shouldStartListeningAndRegister() {
        mController =
                new GupGlobalSwitchBarController(mContext, new SwitchBarController(mSwitchBar));
        mController.mSwitchWidgetController = mSwitchWidgetController;
        mController.mGameDriverContentObserver = mGameDriverContentObserver;
        mController.onStart();

        verify(mSwitchWidgetController).startListening();
        verify(mGameDriverContentObserver).register(mResolver);
    }

    @Test
    public void onStop_shouldStopListeningAndUnregister() {
        mController =
                new GupGlobalSwitchBarController(mContext, new SwitchBarController(mSwitchBar));
        mController.mSwitchWidgetController = mSwitchWidgetController;
        mController.mGameDriverContentObserver = mGameDriverContentObserver;
        mController.onStop();

        verify(mSwitchWidgetController).stopListening();
        verify(mGameDriverContentObserver).unregister(mResolver);
    }

    @Test
    public void onSwitchToggled_checked_shouldTurnOnGup() {
        Settings.Global.putInt(mResolver, Settings.Global.GUP_DEV_ALL_APPS, GUP_OFF);
        mController =
                new GupGlobalSwitchBarController(mContext, new SwitchBarController(mSwitchBar));
        mController.onSwitchToggled(true);

        assertThat(Settings.Global.getInt(mResolver, Settings.Global.GUP_DEV_ALL_APPS, GUP_DEFAULT))
                .isEqualTo(GUP_DEFAULT);
    }

    @Test
    public void onSwitchToggled_unchecked_shouldTurnOffGup() {
        Settings.Global.putInt(mResolver, Settings.Global.GUP_DEV_ALL_APPS, GUP_DEFAULT);
        mController =
                new GupGlobalSwitchBarController(mContext, new SwitchBarController(mSwitchBar));
        mController.onSwitchToggled(false);

        assertThat(Settings.Global.getInt(mResolver, Settings.Global.GUP_DEV_ALL_APPS, GUP_DEFAULT))
                .isEqualTo(GUP_OFF);
    }
}
