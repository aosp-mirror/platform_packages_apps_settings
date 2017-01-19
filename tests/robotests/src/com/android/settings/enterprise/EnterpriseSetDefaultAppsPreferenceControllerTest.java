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

package com.android.settings.enterprise;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v7.preference.Preference;
import android.util.ArraySet;

import com.android.settings.R;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.applications.ApplicationFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EnterpriseSetDefaultAppsPreferenceController}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class EnterpriseSetDefaultAppsPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    private EnterpriseSetDefaultAppsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mController = new EnterpriseSetDefaultAppsPreferenceController(mContext);
    }

    private static Intent buildIntent(String action, String category, String protocol,
            String type) {
        final Intent intent = new Intent(action);
        if (category != null) {
            intent.addCategory(category);
        }
        if (protocol != null) {
            intent.setData(Uri.parse(protocol));
        }
        if (type != null) {
            intent.setType(type);
        }
        return intent;
    }

    private void setEnterpriseSetDefaultApps(Intent[] intents, int number) {
        final Set<ApplicationFeatureProvider.PersistentPreferredActivityInfo> apps
                = new ArraySet<>(number);
        for (int i = 0; i < number; i++) {
            apps.add(new ApplicationFeatureProvider.PersistentPreferredActivityInfo("app", i));
        }
        when(mFeatureFactory.applicationFeatureProvider.findPersistentPreferredActivities(
                argThat(new MatchesIntents(intents)))).thenReturn(apps);
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        preference.setVisible(true);

        when(mFeatureFactory.applicationFeatureProvider.findPersistentPreferredActivities(
                anyObject())).thenReturn(
                        new ArraySet<ApplicationFeatureProvider.PersistentPreferredActivityInfo>());
        mController.updateState(preference);
        assertThat(preference.isVisible()).isFalse();

        setEnterpriseSetDefaultApps(new Intent[] {buildIntent(Intent.ACTION_VIEW,
                Intent.CATEGORY_BROWSABLE, "http:", null)}, 1);
        setEnterpriseSetDefaultApps(new Intent[] {new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                new Intent(MediaStore.ACTION_VIDEO_CAPTURE)}, 2);
        setEnterpriseSetDefaultApps(new Intent[] {buildIntent(Intent.ACTION_VIEW, null, "geo:",
                null)}, 4);
        setEnterpriseSetDefaultApps(new Intent[] {new Intent(Intent.ACTION_SENDTO),
                new Intent(Intent.ACTION_SEND), new Intent(Intent.ACTION_SEND_MULTIPLE)}, 8);
        setEnterpriseSetDefaultApps(new Intent[] {buildIntent(Intent.ACTION_INSERT, null, null,
                "vnd.android.cursor.dir/event")}, 16);
        setEnterpriseSetDefaultApps(new Intent[] {buildIntent(Intent.ACTION_PICK, null, null,
                ContactsContract.Contacts.CONTENT_TYPE)}, 32);
        setEnterpriseSetDefaultApps(new Intent[] {new Intent(Intent.ACTION_DIAL),
                new Intent(Intent.ACTION_CALL)}, 64);
        when(mContext.getResources().getQuantityString(
                R.plurals.enterprise_privacy_number_enterprise_set_default_apps, 127, 127))
                .thenReturn("127 apps");
        mController.updateState(preference);
        assertThat(preference.getTitle()).isEqualTo("127 apps");
        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey())
                .isEqualTo("number_enterprise_set_default_apps");
    }

    private static class MatchesIntents extends ArgumentMatcher<Intent[]> {
        private final Intent[] mExpectedIntents;

        MatchesIntents(Intent[] intents) {
            mExpectedIntents = intents;
        }

        @Override
        public boolean matches(Object object) {
            final Intent[] actualIntents = (Intent[]) object;
            if (actualIntents == null) {
                return false;
            }
            if (actualIntents.length != mExpectedIntents.length) {
                return false;
            }
            for (int i = 0; i < mExpectedIntents.length; i++) {
                if (!mExpectedIntents[i].filterEquals(actualIntents[i])) {
                    return false;
                }
            }
            return true;
        }
    }
}
