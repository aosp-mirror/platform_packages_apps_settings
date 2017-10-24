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
 * limitations under the License
 */

package com.android.settings.notification;

import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import org.robolectric.annotation.Config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class NotificationBackendTest {

    @Test
    public void testMarkAppRow_unblockablePackage() {
        AppRow appRow = new AppRow();
        String packageName = "foo.bar.unblockable";
        appRow.pkg = packageName;
        String[] nonBlockablePkgs = new String[2];
        nonBlockablePkgs[0] = packageName;
        nonBlockablePkgs[1] = "some.other.package";
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, appRow, packageName);

        // This package has a package lock but no locked channels
        assertTrue(appRow.lockedImportance);
        assertNull(appRow.lockedChannelId);
    }

    @Test
    public void testMarkAppRow_unblockableChannelOrPkg() {
        String channelBlockName = "foo.bar.pkgWithChannel";
        String pkgBlockName = "foo.bar.pkgBlock";
        String[] nonBlockablePkgs = new String[2];
        nonBlockablePkgs[0] = pkgBlockName;
        nonBlockablePkgs[1] = channelBlockName + ":SpecificChannel";

        // This package has a channel level lock but no full package lock
        AppRow channelBlockApp = new AppRow();
        channelBlockApp.pkg = channelBlockName;
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, channelBlockApp,
                channelBlockName);
        assertFalse(channelBlockApp.lockedImportance);
        assertEquals("SpecificChannel", channelBlockApp.lockedChannelId);

        // This other package has the reverse
        AppRow pkgBlock = new AppRow();
        pkgBlock.pkg = pkgBlockName;
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, pkgBlock, pkgBlockName);
        assertTrue(pkgBlock.lockedImportance);
        assertNull(pkgBlock.lockedChannelId);

        // This third package has no locks at all
        AppRow otherAppRow = new AppRow();
        otherAppRow.pkg ="foo.bar.nothingBlocked";
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, otherAppRow,
                "foo.bar.nothingBlocked");
        assertFalse(otherAppRow.lockedImportance);
        assertNull(otherAppRow.lockedChannelId);
    }

    @Test
    public void testMarkAppRow_unblockableChannelAndPkg() {
        AppRow appRow = new AppRow();
        String packageName = "foo.bar.unblockable";
        appRow.pkg = packageName;
        String[] nonBlockablePkgs = new String[2];
        nonBlockablePkgs[0] = "foo.bar.unblockable";
        nonBlockablePkgs[1] = "foo.bar.unblockable:SpecificChannel";
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, appRow, packageName);

        // This package has both a channel lock and a package lock
        assertTrue(appRow.lockedImportance);
        assertEquals("SpecificChannel", appRow.lockedChannelId);
    }

    @Test
    public void testMarkAppRow_channelNameWithColons() {
        AppRow appRow = new AppRow();
        String packageName = "foo.bar.unblockable";
        String channelName = "SpecificChannel:1234:abc:defg";
        appRow.pkg = packageName;
        String[] nonBlockablePkgs = new String[1];
        nonBlockablePkgs[0] = packageName + ":" + channelName;
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, appRow, packageName);

        assertEquals(channelName, appRow.lockedChannelId);
    }


    @Test
    public void testMarkAppRow_blocklistWithNullEntries() {
        AppRow appRow = new AppRow();
        String packageName = "foo.bar.unblockable";
        appRow.pkg = packageName;
        String[] nonBlockablePkgs = new String[6]; // extra long list with some entries left null
        nonBlockablePkgs[2] = "foo.bar.unblockable";
        nonBlockablePkgs[4] = "foo.bar.unblockable:SpecificChannel";
        NotificationBackend.markAppRowWithBlockables(nonBlockablePkgs, appRow, packageName);

        assertTrue(appRow.lockedImportance);
        assertEquals("SpecificChannel", appRow.lockedChannelId);
    }
}
