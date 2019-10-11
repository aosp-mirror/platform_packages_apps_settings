/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import com.android.settingslib.widget.CandidateInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class NotificationAssistantPickerTest {

    private static final String TEST_PKG = "test.package";
    private static final String TEST_SRV = "test.component";
    private static final String TEST_CMP = TEST_PKG + "/" + TEST_SRV;
    private static final String TEST_NAME = "Test name";
    private static final ComponentName TEST_COMPONENT = ComponentName.unflattenFromString(TEST_CMP);
    private NotificationAssistantPicker mFragment;
    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private NotificationBackend mNotificationBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mFragment = new TestNotificationAssistantPicker(mContext, mPackageManager,
                mNotificationBackend);
    }

    @Test
    public void getCurrentAssistant() {
        when(mNotificationBackend.getAllowedNotificationAssistant()).thenReturn(TEST_COMPONENT);
        String key = mFragment.getDefaultKey();
        assertEquals(key, TEST_CMP);
    }

    @Test
    public void getCurrentAssistant_None() {
        when(mNotificationBackend.getAllowedNotificationAssistant()).thenReturn(null);
        String key = mFragment.getDefaultKey();
        assertEquals(key, "");
    }

    @Test
    public void setAssistant() {
        mFragment.setDefaultKey(TEST_CMP);
        verify(mNotificationBackend).setNotificationAssistantGranted(TEST_COMPONENT);
    }

    @Test
    public void setAssistant_None() {
        mFragment.setDefaultKey("");
        verify(mNotificationBackend).setNotificationAssistantGranted(null);
    }

    @Test
    public void candidateListHasNoneAtEnd() {
        List<ServiceInfo> list = new ArrayList<>();
        ServiceInfo serviceInfo = mock(ServiceInfo.class, RETURNS_SMART_NULLS);
        serviceInfo.packageName = TEST_PKG;
        serviceInfo.name = TEST_SRV;
        list.add(serviceInfo);
        mFragment.onServicesReloaded(list);
        List<? extends CandidateInfo> candidates = mFragment.getCandidates();
        assertTrue(candidates.size() > 0);
        assertEquals(candidates.get(candidates.size() - 1).getKey(), "");
    }

    @Test
    public void candidateListHasCorrectCandidate() {
        List<ServiceInfo> list = new ArrayList<>();
        ServiceInfo serviceInfo = mock(ServiceInfo.class, RETURNS_SMART_NULLS);
        serviceInfo.packageName = TEST_PKG;
        serviceInfo.name = TEST_SRV;
        list.add(serviceInfo);
        mFragment.onServicesReloaded(list);
        List<? extends CandidateInfo> candidates = mFragment.getCandidates();
        boolean found = false;
        for (CandidateInfo c : candidates) {
            if (TEST_CMP.equals(c.getKey())) {
                found = true;
                break;
            }
        }
        if (!found) fail();
    }

    @Test
    public void noDialogOnNoAssistantSelected() {
        when(mContext.getString(anyInt(), anyString())).thenAnswer(
                (InvocationOnMock invocation) -> {
                    return invocation.getArgument(1);
                });
        assertNull(mFragment.getConfirmationMessage(
                new NotificationAssistantPicker.CandidateNone(mContext)));
    }

    @Test
    public void dialogTextHasAssistantName() {
        CandidateInfo c = mock(CandidateInfo.class);
        when(mContext.getString(anyInt(), anyString())).thenAnswer(
                (InvocationOnMock invocation) -> {
                    return invocation.getArgument(1);
                });
        when(c.loadLabel()).thenReturn(TEST_NAME);
        when(c.getKey()).thenReturn(TEST_CMP);
        CharSequence text = mFragment.getConfirmationMessage(c);
        assertNotNull(text);
        assertTrue(text.toString().contains(TEST_NAME));
    }


    private static class TestNotificationAssistantPicker extends NotificationAssistantPicker {
        TestNotificationAssistantPicker(Context context, PackageManager packageManager,
                NotificationBackend notificationBackend) {
            mContext = context;
            mPm = packageManager;
            mNotificationBackend = notificationBackend;
        }
    }

}
