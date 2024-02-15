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

package com.android.settings.development;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.lifecycle.LifecycleOwner;

import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class, ShadowUserManager.class})
public class DevelopmentSwitchBarControllerTest {

    @Mock
    private DevelopmentSettingsDashboardFragment mSettings;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private SettingsMainSwitchBar mSwitchBar;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        ShadowUserManager.getShadow().setIsAdminUser(true);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mSwitchBar = new SettingsMainSwitchBar(context);
        when(mSettings.getContext()).thenReturn(context);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void runThroughLifecycle_v2_isMonkeyRun_shouldNotRegisterListener() {
        ShadowUtils.setIsUserAMonkey(true);
        new DevelopmentSwitchBarController(mSettings, mSwitchBar,
                true /* isAvailable */, mLifecycle);
        final List<OnCheckedChangeListener> listeners =
                ReflectionHelpers.getField(mSwitchBar, "mSwitchChangeListeners");

        mLifecycle.handleLifecycleEvent(ON_START);
        assertThat(listeners).doesNotContain(mSettings);

        mLifecycle.handleLifecycleEvent(ON_STOP);
        assertThat(listeners).doesNotContain(mSettings);
    }

    @Test
    public void runThroughLifecycle_isNotMonkeyRun_shouldRegisterAndRemoveListener() {
        ShadowUtils.setIsUserAMonkey(false);
        new DevelopmentSwitchBarController(mSettings, mSwitchBar,
                true /* isAvailable */, mLifecycle);
        final List<OnCheckedChangeListener> listeners =
                ReflectionHelpers.getField(mSwitchBar, "mSwitchChangeListeners");

        mLifecycle.handleLifecycleEvent(ON_START);
        assertThat(listeners).contains(mSettings);

        mLifecycle.handleLifecycleEvent(ON_STOP);
        assertThat(listeners).doesNotContain(mSettings);
    }

    @Test
    public void runThroughLifecycle_v2_isNotMonkeyRun_shouldRegisterAndRemoveListener() {
        when(mSettings.getContext()).thenReturn(RuntimeEnvironment.application);
        ShadowUtils.setIsUserAMonkey(false);
        new DevelopmentSwitchBarController(mSettings, mSwitchBar,
                true /* isAvailable */, mLifecycle);
        final List<OnCheckedChangeListener> listeners =
                ReflectionHelpers.getField(mSwitchBar, "mSwitchChangeListeners");

        mLifecycle.handleLifecycleEvent(ON_START);
        assertThat(listeners).contains(mSettings);

        mLifecycle.handleLifecycleEvent(ON_STOP);
        assertThat(listeners).doesNotContain(mSettings);
    }

    @Test
    @Ignore
    public void buildController_unavailable_shouldDisableSwitchBar() {
        ShadowUtils.setIsUserAMonkey(false);
        new DevelopmentSwitchBarController(mSettings, mSwitchBar,
                false /* isAvailable */, mLifecycle);

        assertThat(mSwitchBar.isEnabled()).isFalse();
    }
}
