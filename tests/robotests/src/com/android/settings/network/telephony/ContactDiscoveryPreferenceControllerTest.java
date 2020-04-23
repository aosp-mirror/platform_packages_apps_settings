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

package com.android.settings.network.telephony;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsRcsManager;
import android.telephony.ims.RcsUceAdapter;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.SwitchPreference;

import com.android.settings.network.SubscriptionUtil;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class ContactDiscoveryPreferenceControllerTest {

    private static final int TEST_SUB_ID = 2;
    private static final Uri UCE_URI = Uri.withAppendedPath(Telephony.SimInfo.CONTENT_URI,
            Telephony.SimInfo.COLUMN_IMS_RCS_UCE_ENABLED);

    @Mock private ImsManager mImsManager;
    @Mock private ImsRcsManager mImsRcsManager;
    @Mock private RcsUceAdapter mRcsUceAdapter;
    @Mock private CarrierConfigManager mCarrierConfigManager;
    @Mock private ContentResolver mContentResolver;
    @Mock private FragmentManager mFragmentManager;
    @Mock private FragmentTransaction mFragmentTransaction;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private ContactDiscoveryPreferenceController mPreferenceControllerUT;
    private SwitchPreference mSwitchPreferenceUT;
    private PersistableBundle mCarrierConfig = new PersistableBundle();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mImsManager).when(mContext).getSystemService(ImsManager.class);
        doReturn(mImsRcsManager).when(mImsManager).getImsRcsManager(anyInt());
        doReturn(mRcsUceAdapter).when(mImsRcsManager).getUceAdapter();
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(eq(TEST_SUB_ID));
        // Start all tests with presence being disabled.
        setRcsPresenceConfig(false);
        doReturn(mContentResolver).when(mContext).getContentResolver();
        doReturn(mFragmentTransaction).when(mFragmentManager).beginTransaction();

        mPreferenceControllerUT = new ContactDiscoveryPreferenceController(mContext,
                "ContactDiscovery");
        // Ensure subscriptionInfo check doesn't fail.
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Collections.emptyList());
        mPreferenceControllerUT.init(mFragmentManager, TEST_SUB_ID, mLifecycle);
        mSwitchPreferenceUT = spy(new SwitchPreference(mContext));
        mSwitchPreferenceUT.setKey(mPreferenceControllerUT.getPreferenceKey());
        mPreferenceControllerUT.preference = mSwitchPreferenceUT;
    }

    @Test
    public void testGetAvailabilityStatus() {
        assertEquals("Availability status should not be available.", CONDITIONALLY_UNAVAILABLE,
                mPreferenceControllerUT.getAvailabilityStatus(TEST_SUB_ID));
        setRcsPresenceConfig(true);
        assertEquals("Availability status should available.", AVAILABLE,
                mPreferenceControllerUT.getAvailabilityStatus(TEST_SUB_ID));
    }

    @Test
    public void testIsChecked() throws Exception {
        doReturn(false).when(mRcsUceAdapter).isUceSettingEnabled();
        assertFalse(mPreferenceControllerUT.isChecked());

        doReturn(true).when(mRcsUceAdapter).isUceSettingEnabled();
        assertTrue(mPreferenceControllerUT.isChecked());
    }

    @Test
    public void testRegisterObserver() {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        verify(mContentResolver).registerContentObserver(eq(UCE_URI), anyBoolean(), any());

        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        verify(mContentResolver).unregisterContentObserver(any());
    }

    @Test
    public void testContentObserverChanged() throws Exception {
        assertFalse(mSwitchPreferenceUT.isChecked());
        ContentObserver observer = getUceChangeObserver();
        assertNotNull(observer);

        doReturn(true).when(mRcsUceAdapter).isUceSettingEnabled();
        observer.onChange(false, UCE_URI);
        assertTrue(mSwitchPreferenceUT.isChecked());
    }

    @Test
    public void testSetChecked() throws Exception {
        // Verify a dialog is shown when the switch is enabled (but the switch is not enabled).
        assertFalse(mPreferenceControllerUT.setChecked(true /*isChecked*/));
        verify(mFragmentTransaction).add(any(), anyString());
        // Verify content discovery is disabled when the user disables it.
        assertTrue(mPreferenceControllerUT.setChecked(false /*isChecked*/));
        verify(mRcsUceAdapter).setUceSettingEnabled(false);
    }

    private void setRcsPresenceConfig(boolean isEnabled) {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_USE_RCS_PRESENCE_BOOL, isEnabled);
    }

    private ContentObserver getUceChangeObserver() {
        ArgumentCaptor<ContentObserver> observerCaptor =
                ArgumentCaptor.forClass(ContentObserver.class);
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        verify(mContentResolver).registerContentObserver(eq(UCE_URI), anyBoolean(),
                observerCaptor.capture());
        return observerCaptor.getValue();
    }
}
