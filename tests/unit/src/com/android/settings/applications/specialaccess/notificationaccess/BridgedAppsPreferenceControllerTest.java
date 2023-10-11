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

package com.android.settings.applications.specialaccess.notificationaccess;

import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_CONVERSATIONS;
import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_ONGOING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.VersionedPackage;
import android.os.Looper;
import android.service.notification.NotificationListenerFilter;
import android.util.ArraySet;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.notification.NotificationBackend;
import com.android.settings.widget.AppCheckBoxPreference;
import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class BridgedAppsPreferenceControllerTest {

    private Context mContext;
    private BridgedAppsPreferenceController mController;
    @Mock
    NotificationBackend mNm;
    ComponentName mCn = new ComponentName("a", "b");
    PreferenceScreen mScreen;
    @Mock
    ApplicationsState mAppState;

    private ApplicationsState.AppEntry mAppEntry;
    private ApplicationsState.AppEntry mAppEntry2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);

        ApplicationInfo ai = new ApplicationInfo();
        ai.packageName = "pkg";
        ai.uid = 12300;
        ai.sourceDir = "";
        ApplicationInfo ai2 = new ApplicationInfo();
        ai2.packageName = "another";
        ai2.uid = 18800;
        ai2.sourceDir = "";
        mAppEntry = new ApplicationsState.AppEntry(mContext, ai, 0);
        mAppEntry2 = new ApplicationsState.AppEntry(mContext, ai2, 1);

        mAppEntry.info = ai;
        mAppEntry.label = "hi";

        mController = new BridgedAppsPreferenceController(mContext, "key");
        mController.setCn(mCn);
        mController.setNm(mNm);
        mController.setUserId(0);
        mController.setAppState(mAppState);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onRebuildComplete_AddsToScreen() {
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());

        ArrayList<ApplicationsState.AppEntry> entries = new ArrayList<>();
        entries.add(mAppEntry);
        entries.add(mAppEntry2);

        mController.onRebuildComplete(entries);

        assertThat(mScreen.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onRebuildComplete_doesNotReaddToScreen() {
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());

        AppCheckBoxPreference p = mock(AppCheckBoxPreference.class);
        when(p.getKey()).thenReturn("pkg|12300");
        mScreen.addPreference(p);

        ArrayList<ApplicationsState.AppEntry> entries = new ArrayList<>();
        entries.add(mAppEntry);
        entries.add(mAppEntry2);

        mController.onRebuildComplete(entries);

        assertThat(mScreen.getPreferenceCount()).isEqualTo(2);
    }

    @Test
    public void onRebuildComplete_removesExtras() {
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());

        Preference p = mock(Preference.class);
        when(p.getKey()).thenReturn("pkg|123");
        mScreen.addPreference(p);

        ArrayList<ApplicationsState.AppEntry> entries = new ArrayList<>();
        entries.add(mAppEntry);
        entries.add(mAppEntry2);

        mController.onRebuildComplete(entries);

        assertThat((Preference) mScreen.findPreference("pkg|123")).isNull();
    }

    @Test
    public void onRebuildComplete_buildsSetting() {
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());

        ArrayList<ApplicationsState.AppEntry> entries = new ArrayList<>();
        entries.add(mAppEntry);

        mController.onRebuildComplete(entries);

        AppCheckBoxPreference actual = mScreen.findPreference("pkg|12300");

        assertThat(actual.isChecked()).isTrue();
        assertThat(actual.getTitle()).isEqualTo("hi");
        assertThat(actual.getIcon()).isNotNull();
    }

    @Test
    public void onPreferenceChange_false() {
        VersionedPackage vp = new VersionedPackage("pkg", 10567);
        ArraySet<VersionedPackage> vps = new ArraySet<>();
        vps.add(vp);
        NotificationListenerFilter nlf = new NotificationListenerFilter(FLAG_FILTER_TYPE_ONGOING
                | FLAG_FILTER_TYPE_CONVERSATIONS, vps);
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        AppCheckBoxPreference pref = new AppCheckBoxPreference(mContext);
        pref.setKey("pkg|567");

        mController.onPreferenceChange(pref, false);

        ArgumentCaptor<NotificationListenerFilter> captor =
                ArgumentCaptor.forClass(NotificationListenerFilter.class);
        verify(mNm).setListenerFilter(eq(mCn), eq(0), captor.capture());
        assertThat(captor.getValue().getDisallowedPackages()).contains(
                new VersionedPackage("pkg", 567));
        assertThat(captor.getValue().getDisallowedPackages()).contains(
                new VersionedPackage("pkg", 10567));
    }

    @Test
    public void onPreferenceChange_true() {
        VersionedPackage vp = new VersionedPackage("pkg", 567);
        VersionedPackage vp2 = new VersionedPackage("pkg", 10567);
        ArraySet<VersionedPackage> vps = new ArraySet<>();
        vps.add(vp);
        vps.add(vp2);
        NotificationListenerFilter nlf = new NotificationListenerFilter(FLAG_FILTER_TYPE_ONGOING
                | FLAG_FILTER_TYPE_CONVERSATIONS, vps);
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        AppCheckBoxPreference pref = new AppCheckBoxPreference(mContext);
        pref.setKey("pkg|567");

        mController.onPreferenceChange(pref, true);

        ArgumentCaptor<NotificationListenerFilter> captor =
                ArgumentCaptor.forClass(NotificationListenerFilter.class);
        verify(mNm).setListenerFilter(eq(mCn), eq(0), captor.capture());
        assertThat(captor.getValue().getDisallowedPackages().size()).isEqualTo(1);
        assertThat(captor.getValue().getDisallowedPackages()).contains(
                new VersionedPackage("pkg", 10567));
    }
}
