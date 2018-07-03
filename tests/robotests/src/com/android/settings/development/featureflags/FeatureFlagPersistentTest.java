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
package com.android.settings.development.featureflags;

import static com.google.common.truth.Truth.assertThat;

import com.android.settings.development.featureflags.FeatureFlagPersistent;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.FeatureFlagUtils;
import android.util.Log;

@RunWith(SettingsRobolectricTestRunner.class)
public class FeatureFlagPersistentTest {

    private static final String TEST_FEATURE_NAME = "test_feature";

    private static final String PERSISTENT_FALSE_NAME = "false_persistent";
    private static final String PERSISTENT_TRUE_NAME = "true_persistent";
    private static final String VOLATILE_FALSE_NAME = "volatile_false_volatile";
    private static final String VOLATILE_TRUE_NAME = "volatile_true_volatile";

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        FeatureFlagPersistent.getAllPersistentFlags().add(TEST_FEATURE_NAME);
        FeatureFlagUtils.getAllFeatureFlags().put(TEST_FEATURE_NAME, "false");

        FeatureFlagUtils.getAllFeatureFlags().put(VOLATILE_FALSE_NAME, "false");
        FeatureFlagUtils.getAllFeatureFlags().put(VOLATILE_TRUE_NAME, "true");

        FeatureFlagPersistent.getAllPersistentFlags().add(PERSISTENT_FALSE_NAME);
        FeatureFlagUtils.getAllFeatureFlags().put(PERSISTENT_FALSE_NAME, "false");

        FeatureFlagPersistent.getAllPersistentFlags().add(PERSISTENT_TRUE_NAME);
        FeatureFlagUtils.getAllFeatureFlags().put(PERSISTENT_TRUE_NAME, "true");
    }

    @After
    public void tearDown() {
        cleanup(PERSISTENT_FALSE_NAME);
        cleanup(PERSISTENT_TRUE_NAME);
        cleanup(VOLATILE_FALSE_NAME);
        cleanup(VOLATILE_TRUE_NAME);
        cleanup(TEST_FEATURE_NAME);
    }

    private void cleanup(String flagName) {
        Settings.Global.putString(mContext.getContentResolver(), flagName, "");
        SystemProperties.set(FeatureFlagUtils.FFLAG_PREFIX + flagName, "");
        SystemProperties.set(FeatureFlagUtils.FFLAG_OVERRIDE_PREFIX + flagName, "");
        SystemProperties.set(FeatureFlagUtils.PERSIST_PREFIX + flagName, "");
    }

    /**
     * Test to verify a non-persistent flag is indeed not persistent.
     */
    @Test
    public void isPersistent_notPersistent_shouldReturnFalse() {
        assertThat(FeatureFlagPersistent.isPersistent(VOLATILE_FALSE_NAME)).isFalse();
    }

    /**
     * Test to verify a persistent flag is indeed persistent.
     */
    @Test
    public void isPersistent_persistent_shouldReturnTrue() {
        assertThat(FeatureFlagPersistent.isPersistent(PERSISTENT_TRUE_NAME)).isTrue();
    }

    /**
     * Test to verify a persistent flag that is enabled should return true.
     */
    @Test
    public void isEnabled_enabled_shouldReturnTrue() {
        assertThat(FeatureFlagPersistent.isEnabled(mContext, PERSISTENT_TRUE_NAME)).isTrue();
    }

    /**
     * Test to verify a persistent flag that is disabled should return false.
     */
    @Test
    public void isEnabled_disabled_shouldReturnFalse() {
        assertThat(FeatureFlagPersistent.isEnabled(mContext, PERSISTENT_FALSE_NAME)).isFalse();
    }

    /**
     * Test to verify a persistent flag that has an enabled in system property should return true.
     */
    @Test
    public void isEnabled_sysPropEnabled_shouldReturnTrue() {
        SystemProperties.set(FeatureFlagUtils.PERSIST_PREFIX + TEST_FEATURE_NAME, "true");
        FeatureFlagUtils.setEnabled(mContext, TEST_FEATURE_NAME, false);

        assertThat(FeatureFlagPersistent.isEnabled(mContext, TEST_FEATURE_NAME)).isTrue();
    }

    /**
     * Test to verify a persistent flag that is disabled in system property should return false.
     */
    @Test
    public void isEnabled_sysPropDisabled_shouldReturnFalse() {
        SystemProperties.set(FeatureFlagUtils.PERSIST_PREFIX + TEST_FEATURE_NAME, "false");
        FeatureFlagUtils.setEnabled(mContext, TEST_FEATURE_NAME, true);

        assertThat(FeatureFlagPersistent.isEnabled(mContext, TEST_FEATURE_NAME)).isFalse();
    }

    /**
     * Test to verify setting persistent flag to enable works.
     */
    @Test
    public void setEnabled_sysPropTrue_shouldSetValues() {
        SystemProperties.set(FeatureFlagUtils.PERSIST_PREFIX + TEST_FEATURE_NAME, "");

        FeatureFlagPersistent.setEnabled(mContext, TEST_FEATURE_NAME, true);

        assertThat(SystemProperties.get(FeatureFlagUtils.PERSIST_PREFIX + TEST_FEATURE_NAME)).isEqualTo("true");
        assertThat(FeatureFlagUtils.isEnabled(mContext, TEST_FEATURE_NAME)).isTrue();
    }

    /**
     * Test to verify setting persistent flag to disable works.
     */
    @Test
    public void setEnabled_sysPropFalse_shouldSetValues() {
        SystemProperties.set(FeatureFlagUtils.PERSIST_PREFIX + TEST_FEATURE_NAME, "");

        FeatureFlagPersistent.setEnabled(mContext, TEST_FEATURE_NAME, false);

        assertThat(SystemProperties.get(FeatureFlagUtils.PERSIST_PREFIX + TEST_FEATURE_NAME)).isEqualTo("false");
        assertThat(FeatureFlagUtils.isEnabled(mContext, TEST_FEATURE_NAME)).isFalse();
    }
}

