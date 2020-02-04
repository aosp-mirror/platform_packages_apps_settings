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

package com.android.settings.homepage.contextualcards;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleOwner;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowSubscriptionManager;
import org.robolectric.shadows.ShadowTelephonyManager;

@RunWith(RobolectricTestRunner.class)
public class ControllerRendererPoolTest {
    private static final int SUB_ID = 1;

    private static final int UNSUPPORTED_CARD_TYPE = -99999;
    private static final int UNSUPPORTED_VIEW_TYPE = -99999;

    private ControllerRendererPool mPool;
    private Context mContext;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;

    @Mock
    private TelephonyManager mTelephonyMgr;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);

        // SubscriptionManager and TelephonyManager for CellularDataConditionController
        ShadowSubscriptionManager shadowSubscriptionMgr = shadowOf(
                mContext.getSystemService(SubscriptionManager.class));
        shadowSubscriptionMgr.setDefaultDataSubscriptionId(SUB_ID);

        ShadowTelephonyManager shadowTelephonyMgr = Shadow.extract(
                mContext.getSystemService(TelephonyManager.class));
        shadowTelephonyMgr.setTelephonyManagerForSubscriptionId(SUB_ID, mTelephonyMgr);
        when(mTelephonyMgr.createForSubscriptionId(anyInt())).thenReturn(mTelephonyMgr);

        mPool = new ControllerRendererPool();
    }

    @Test
    public void getController_hasSupportedCardType_shouldReturnCorrespondingController() {
        ContextualCardLookupTable.LOOKUP_TABLE.stream().forEach(mapping -> assertThat(
                mPool.getController(mContext, mapping.mCardType).getClass()).isEqualTo(
                mapping.mControllerClass));
    }

    @Test
    public void getController_hasSupportedCardType_shouldHaveTwoControllersInPool() {
        final long count = ContextualCardLookupTable.LOOKUP_TABLE.stream().map(
                mapping -> mapping.mControllerClass).distinct().count();

        ContextualCardLookupTable.LOOKUP_TABLE.stream().forEach(
                mapping -> mPool.getController(mContext, mapping.mCardType));

        assertThat(mPool.getControllers()).hasSize((int) count);
    }

    @Test
    public void getController_hasUnsupportedCardType_shouldReturnNullAndPoolIsEmpty() {
        final ContextualCardController controller = mPool.getController(mContext,
                UNSUPPORTED_CARD_TYPE);

        assertThat(controller).isNull();
        assertThat(mPool.getControllers()).isEmpty();
    }

    @Test
    public void getRenderer_hasSupportedViewType_shouldReturnCorrespondingRenderer() {
        ContextualCardLookupTable.LOOKUP_TABLE.stream().forEach(mapping -> assertThat(
                mPool.getRendererByViewType(mContext, mLifecycleOwner,
                        mapping.mViewType).getClass()).isEqualTo(mapping.mRendererClass));
    }

    @Test
    public void getRenderer_hasSupportedViewType_shouldHaveDistinctRenderersInPool() {
        final long count = ContextualCardLookupTable.LOOKUP_TABLE.stream().map(
                mapping -> mapping.mRendererClass).distinct().count();

        ContextualCardLookupTable.LOOKUP_TABLE.stream().forEach(
                mapping -> mPool.getRendererByViewType(mContext, mLifecycleOwner,
                        mapping.mViewType));

        assertThat(mPool.getRenderers()).hasSize((int) count);
    }

    @Test
    public void getRenderer_hasUnsupportedViewType_shouldReturnNullAndPoolIsEmpty() {
        final ContextualCardRenderer renderer = mPool.getRendererByViewType(mContext,
                mLifecycleOwner,
                UNSUPPORTED_VIEW_TYPE);

        assertThat(renderer).isNull();
        assertThat(mPool.getRenderers()).isEmpty();
    }
}
