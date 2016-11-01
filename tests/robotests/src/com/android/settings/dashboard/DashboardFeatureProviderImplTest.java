/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.dashboard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DashboardFeatureProviderImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserManager mUserManager;

    private DashboardFeatureProviderImpl mImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mImpl = new DashboardFeatureProviderImpl(mActivity);
    }

    @Test
    public void bindPreference_shouldBindAllData() {
        final Preference preference = new Preference(
                ShadowApplication.getInstance().getApplicationContext());
        final Tile tile = new Tile();
        tile.title = "title";
        tile.summary = "summary";
        tile.icon = Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565));
        tile.metaData = new Bundle();
        tile.metaData.putString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS, "HI");
        tile.priority = 10;
        mImpl.bindPreferenceToTile(mActivity, preference, tile, "123");

        assertThat(preference.getTitle()).isEqualTo(tile.title);
        assertThat(preference.getSummary()).isEqualTo(tile.summary);
        assertThat(preference.getIcon()).isNotNull();
        assertThat(preference.getFragment())
                .isEqualTo(tile.metaData.getString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS));
        assertThat(preference.getOrder()).isEqualTo(-tile.priority);
    }

    @Test
    public void bindPreference_noFragmentMetadata_shouldBindIntent() {
        final Preference preference = new Preference(
                ShadowApplication.getInstance().getApplicationContext());
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.priority = 10;
        tile.intent = new Intent();
        mImpl.bindPreferenceToTile(mActivity, preference, tile, "123");

        assertThat(preference.getFragment()).isNull();
        assertThat(preference.getOnPreferenceClickListener()).isNotNull();
        assertThat(preference.getOrder()).isEqualTo(-tile.priority);
    }

    @Test
    public void bindPreference_noFragmentMetadata_shouldBindToProfileSelector() {
        final Preference preference = new Preference(
                ShadowApplication.getInstance().getApplicationContext());
        final Tile tile = new Tile();
        tile.metaData = new Bundle();
        tile.userHandle = new ArrayList<>();
        tile.userHandle.add(mock(UserHandle.class));
        tile.userHandle.add(mock(UserHandle.class));
        tile.intent = new Intent();

        when(mActivity.getApplicationContext().getSystemService(Context.USER_SERVICE))
                .thenReturn(mUserManager);

        mImpl.bindPreferenceToTile(mActivity, preference, tile, "123");
        preference.getOnPreferenceClickListener().onPreferenceClick(null);

        verify(mActivity).getFragmentManager();
    }

    @Test
    public void bindPreference_withNullKeyNullPriority_shouldGenerateKeyAndPriority() {
        final Preference preference = new Preference(
                ShadowApplication.getInstance().getApplicationContext());
        final Tile tile = new Tile();
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName("pkg", "class"));
        mImpl.bindPreferenceToTile(mActivity, preference, tile, null /* key */);

        assertThat(preference.getKey()).isNotNull();
        assertThat(preference.getOrder()).isEqualTo(Preference.DEFAULT_ORDER);
    }
}
