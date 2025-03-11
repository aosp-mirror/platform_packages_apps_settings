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

package com.android.settings.notification;

import static android.service.notification.Adjustment.KEY_TYPE;
import static android.service.notification.Adjustment.TYPE_CONTENT_RECOMMENDATION;
import static android.service.notification.Adjustment.TYPE_NEWS;
import static android.service.notification.Adjustment.TYPE_PROMOTION;
import static android.service.notification.Adjustment.TYPE_SOCIAL_MEDIA;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.INotificationManager;
import android.content.Context;
import android.os.RemoteException;
import android.platform.test.flag.junit.SetFlagsRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BundleTypePreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    BundleTypePreferenceController mController;
    @Mock
    INotificationManager mInm;

    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        when(mInm.getAllowedAssistantAdjustments(any())).thenReturn(List.of(KEY_TYPE));
        when(mInm.getUnsupportedAdjustmentTypes()).thenReturn(List.of());
        mSetFlagsRule.enableFlags(
                android.service.notification.Flags.FLAG_NOTIFICATION_CLASSIFICATION,
                Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI);
        mContext = RuntimeEnvironment.application;
        mController = new BundleTypePreferenceController(mContext, PREFERENCE_KEY);
        mController.mBackend.setNm(mInm);
    }

    @Test
    public void isAvailable() throws RemoteException {
        when(mInm.getAllowedAssistantAdjustments(any())).thenReturn(List.of(KEY_TYPE));
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_flagEnabledNasDoesNotSupport_shouldReturnFalse()
            throws RemoteException {
        when(mInm.getUnsupportedAdjustmentTypes()).thenReturn(List.of(KEY_TYPE));
        when(mInm.getAllowedAssistantAdjustments(any())).thenReturn(List.of(KEY_TYPE));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_flagDisabledNasSupports_shouldReturnFalse() throws RemoteException {
        mSetFlagsRule.disableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI);
        when(mInm.getUnsupportedAdjustmentTypes()).thenReturn(List.of());
        when(mInm.getAllowedAssistantAdjustments(any())).thenReturn(List.of(KEY_TYPE));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_flagEnabledNasDisabled_shouldReturnFalse() throws RemoteException {
        when(mInm.getUnsupportedAdjustmentTypes()).thenReturn(List.of());
        when(mInm.getAllowedAssistantAdjustments(any())).thenReturn(List.of());
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isChecked_promotions() throws RemoteException {
        mController = new BundleTypePreferenceController(mContext,
                BundleTypePreferenceController.PROMO_KEY);

        when(mInm.getAllowedAdjustmentKeyTypes()).thenReturn(new int[]{TYPE_PROMOTION});
        assertThat(mController.isChecked()).isTrue();

        when(mInm.getAllowedAdjustmentKeyTypes()).thenReturn(new int[]{});
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_news() throws RemoteException {
        mController = new BundleTypePreferenceController(mContext,
                BundleTypePreferenceController.NEWS_KEY);

        when(mInm.getAllowedAdjustmentKeyTypes()).thenReturn(new int[]{TYPE_NEWS});
        assertThat(mController.isChecked()).isTrue();

        when(mInm.getAllowedAdjustmentKeyTypes()).thenReturn(new int[]{});
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_social() throws RemoteException {
        mController = new BundleTypePreferenceController(mContext,
                BundleTypePreferenceController.SOCIAL_KEY);

        when(mInm.getAllowedAdjustmentKeyTypes()).thenReturn(new int[]{TYPE_SOCIAL_MEDIA});
        assertThat(mController.isChecked()).isTrue();

        when(mInm.getAllowedAdjustmentKeyTypes()).thenReturn(new int[]{});
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_recs() throws RemoteException {
        mController = new BundleTypePreferenceController(mContext,
                BundleTypePreferenceController.RECS_KEY);

        when(mInm.getAllowedAdjustmentKeyTypes()).thenReturn(
                new int[]{TYPE_CONTENT_RECOMMENDATION});
        assertThat(mController.isChecked()).isTrue();

        when(mInm.getAllowedAdjustmentKeyTypes()).thenReturn(new int[]{});
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_mixed() throws RemoteException {
        mController = new BundleTypePreferenceController(mContext,
                BundleTypePreferenceController.RECS_KEY);

        when(mInm.getAllowedAdjustmentKeyTypes()).thenReturn(
                new int[]{TYPE_PROMOTION, TYPE_CONTENT_RECOMMENDATION});
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked() throws RemoteException {
        mController = new BundleTypePreferenceController(mContext,
                BundleTypePreferenceController.PROMO_KEY);
        mController.setChecked(false);
        verify(mInm).setAssistantAdjustmentKeyTypeState(TYPE_PROMOTION, false);

        mController.setChecked(true);
        verify(mInm).setAssistantAdjustmentKeyTypeState(TYPE_PROMOTION, true);
    }
}
