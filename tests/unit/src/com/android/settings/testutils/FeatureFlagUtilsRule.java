/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.testutils;

import android.content.Context;
import android.util.FeatureFlagUtils;

import androidx.test.core.app.ApplicationProvider;

import org.junit.rules.ExternalResource;

import java.util.HashMap;
import java.util.Map;

/**
 * A test rule that is used to automatically recover the FeatureFlagUtils resource after testing.
 *
 * Example:
 * <pre class="code"><code class="java">
 * public class ExampleTest {
 *
 *     &#064;Rule
 *     public final FeatureFlagUtilsRule mFeatureFlagUtilsRule = new FeatureFlagUtilsRule();
 *
 * }
 * </code></pre>
 */
public class FeatureFlagUtilsRule extends ExternalResource {

    private Context mContext;
    private Map<String, Boolean> mBackupFeatureFlags = new HashMap<String, Boolean>();

    @Override
    protected void before() throws Throwable {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Override
    protected void after() {
        mBackupFeatureFlags.forEach((k, v) -> FeatureFlagUtils.setEnabled(mContext, k, v));
    }

    public void setEnabled(String feature, boolean enabled) {
        if (enabled == FeatureFlagUtils.isEnabled(mContext, feature)) {
            return;
        }
        mBackupFeatureFlags.putIfAbsent(feature, !enabled);
        FeatureFlagUtils.setEnabled(mContext, feature, enabled);
    }

    public void setProviderModelEnabled(boolean enabled) {
        setEnabled(FeatureFlagUtils.SETTINGS_PROVIDER_MODEL, enabled);
    }
}
