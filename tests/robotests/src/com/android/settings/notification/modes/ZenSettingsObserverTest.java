/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.provider.Settings.Global.ZEN_MODE_CONFIG_ETAG;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.Flags;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
public class ZenSettingsObserverTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final Uri SETTINGS_URI = Settings.Global.getUriFor(
            ZEN_MODE_CONFIG_ETAG);

    private Context mContext;
    private ZenSettingsObserver mObserver;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mObserver = new ZenSettingsObserver(mContext);
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void register_withFlagEnabled_registersAndCallsBack() {
        AtomicInteger someValue = new AtomicInteger();
        mObserver.setOnChangeListener(someValue::incrementAndGet);
        assertThat(getSettingsContentObservers()).isEmpty();

        mObserver.register();
        assertThat(getSettingsContentObservers()).hasSize(1);

        getSettingsContentObservers().forEach(o -> o.dispatchChange(false, SETTINGS_URI));
        ShadowLooper.idleMainLooper();
        assertThat(someValue.get()).isEqualTo(1);

        mObserver.unregister();
        assertThat(getSettingsContentObservers()).isEmpty();
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void register_withFlagDisabled_doesNotRegister() {
        mObserver.register();
        assertThat(getSettingsContentObservers()).isEmpty();
        mObserver.unregister();
        assertThat(getSettingsContentObservers()).isEmpty();
    }

    private ImmutableList<ContentObserver> getSettingsContentObservers() {
        return ImmutableList.copyOf(
                shadowOf(mContext.getContentResolver())
                        .getContentObservers(SETTINGS_URI));
    }
}
