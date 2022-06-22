/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.ims.ImsRcsManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.Settings.MobileNetworkActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.network.telephony.MobileNetworkSettings;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class MobileNetworkIntentConverterTest {

    private static final String ACTIONS_ALLOWED [] = {
        Intent.ACTION_MAIN,
        Settings.ACTION_NETWORK_OPERATOR_SETTINGS,
        Settings.ACTION_DATA_ROAMING_SETTINGS,
        Settings.ACTION_MMS_MESSAGE_SETTING,
        ImsRcsManager.ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN
    };

    private static final int TEST_SUBSCRIPTION_ID = 3;

    private static final CharSequence TEST_TITLE_CHAR_SEQUENCE =
            "Test Title".subSequence(0, 10);

    @Mock
    private Activity mActivity;

    private MobileNetworkIntentConverter mIntentConverter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = spy(ApplicationProvider.getApplicationContext());
        ComponentName componentName = ComponentName.createRelative(Utils.SETTINGS_PACKAGE_NAME,
                MobileNetworkActivity.class.getTypeName());

        doReturn(context).when(mActivity).getApplicationContext();
        doReturn(componentName).when(mActivity).getComponentName();

        mIntentConverter = new MobileNetworkIntentConverter(mActivity) {
            @Override
            protected boolean isAttachedToExposedComponents() {
                return false;
            }
        };
    }

    @Test
    public void converter_returnNull_whenNotInterested() {
        Intent intent = new Intent(Intent.ACTION_USER_INITIALIZE);
        assertThat(mIntentConverter.apply(intent)).isEqualTo(null);
    }

    @Test
    public void converter_acceptableIntent_whenInterested() {
        Arrays.stream(ACTIONS_ALLOWED).forEach(action -> {
            Intent intent = new Intent(action);
            assertThat(mIntentConverter.apply(intent)).isNotEqualTo(null);
        });
    }

    @Test
    public void convertSubscriptionId_fromIntentExtra_copyToBundleArgument() {
        Intent intent = new Intent(ACTIONS_ALLOWED[0]);
        intent.putExtra(Settings.EXTRA_SUB_ID, TEST_SUBSCRIPTION_ID);

        Intent convertedIntent = mIntentConverter.apply(intent);

        Bundle args = convertedIntent.getBundleExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(args.getInt(Settings.EXTRA_SUB_ID)).isEqualTo(TEST_SUBSCRIPTION_ID);

        int intExtra = convertedIntent.getIntExtra(
                Settings.EXTRA_SUB_ID, TEST_SUBSCRIPTION_ID - 1);
        assertThat(intExtra).isEqualTo(TEST_SUBSCRIPTION_ID);
    }

    @Test
    public void supportMms_addExtra_whenIntentForMms() {
        Intent intent = new Intent(Settings.ACTION_MMS_MESSAGE_SETTING);

        Intent convertedIntent = mIntentConverter.apply(intent);

        Bundle args = convertedIntent.getBundleExtra(
                    SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(args).isNotEqualTo(null);
        assertThat(args.getString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY)).isEqualTo(
                    MobileNetworkActivity.EXTRA_MMS_MESSAGE);
    }

    @Test
    public void supportContacts_addExtra_whenIntentForContacts() {
        Intent intent = new Intent(ImsRcsManager.ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN);
        MobileNetworkIntentConverter converter = new MobileNetworkIntentConverter(mActivity) {
            @Override
            protected boolean isAttachedToExposedComponents() {
                return false;
            }
            @Override
            protected boolean mayShowContactDiscoveryDialog(Context context, int subId) {
                return true;
            }
        };

        Intent convertedIntent = converter.apply(intent);

        Bundle args = convertedIntent.getBundleExtra(
                    SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(args).isNotEqualTo(null);
        assertThat(args.getBoolean(MobileNetworkActivity.EXTRA_SHOW_CAPABILITY_DISCOVERY_OPT_IN))
                .isEqualTo(true);
    }

    @Test
    public void convertFormat_forSettings_fragmentPresentation() {
        MobileNetworkIntentConverter converter = new MobileNetworkIntentConverter(mActivity) {
            @Override
            protected boolean isAttachedToExposedComponents() {
                return false;
            }
            @Override
            protected CharSequence getFragmentTitle(Context context, int subId) {
                return TEST_TITLE_CHAR_SEQUENCE;
            }
        };

        Intent intent = new Intent(ACTIONS_ALLOWED[0]);

        Intent convertedIntent = converter.apply(intent);

        assertThat(convertedIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE))
                .isEqualTo(TEST_TITLE_CHAR_SEQUENCE.toString());
        assertThat(convertedIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(MobileNetworkSettings.class.getTypeName());
    }

    @Test
    public void convertFormat_deepLink_unwrapIntent() {
        MobileNetworkIntentConverter converter = new MobileNetworkIntentConverter(mActivity) {
            @Override
            protected boolean isAttachedToExposedComponents() {
                return true;
            }
        };

        Intent intent = new Intent(ACTIONS_ALLOWED[0]);
        String intentUri = intent.toUri(Intent.URI_INTENT_SCHEME);

        Intent deepLinkIntent = new Intent(Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY);
        deepLinkIntent.putExtra(Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI, intentUri);

        assertThat(converter.apply(deepLinkIntent)).isNotEqualTo(null);
    }
}
