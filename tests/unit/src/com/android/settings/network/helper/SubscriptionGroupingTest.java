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
package com.android.settings.network.helper;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.ParcelUuid;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.helper.SubscriptionAnnotation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SubscriptionGroupingTest {

    private ParcelUuid mMockUuid;

    private Context mContext;
    private SubscriptionGrouping mTarget;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mMockUuid = ParcelUuid.fromString("1-1-1-1-1");
        mTarget = spy(new SubscriptionGrouping());
    }

    @Test
    public void apply_multipleEntriesWithSameGroupUuid_onlyOneLeft() {
        SubscriptionAnnotation subAnno1 = new TestSubAnnotation(1,
                SubscriptionAnnotation.TYPE_ESIM, true, true, mMockUuid);
        SubscriptionAnnotation subAnno2 = new TestSubAnnotation(2,
                SubscriptionAnnotation.TYPE_PSIM, true, true, mMockUuid);
        SubscriptionAnnotation subAnno3 = new TestSubAnnotation(3,
                SubscriptionAnnotation.TYPE_ESIM, true, true, mMockUuid);
        doReturn(mMockUuid).when(mTarget).getGroupUuid(any());

        List<SubscriptionAnnotation> result = mTarget
                .apply(Arrays.asList(subAnno2, subAnno1, subAnno3));
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0)).isEqualTo(subAnno1);
    }

    @Test
    public void apply_multipleEntriesWithSameGroupUuid_disabledOneIsAvoided() {
        SubscriptionAnnotation subAnno1 = new TestSubAnnotation(1,
                SubscriptionAnnotation.TYPE_ESIM, true, true, mMockUuid);
        SubscriptionAnnotation subAnno2 = new TestSubAnnotation(2,
                SubscriptionAnnotation.TYPE_PSIM, true, true, mMockUuid);
        SubscriptionAnnotation subAnno3 = new TestSubAnnotation(3,
                SubscriptionAnnotation.TYPE_ESIM, false, false, mMockUuid);
        doReturn(mMockUuid).when(mTarget).getGroupUuid(any());

        List<SubscriptionAnnotation> result = mTarget
                .apply(Arrays.asList(subAnno2, subAnno1, subAnno3));
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0)).isEqualTo(subAnno1);
    }

    private class TestSubAnnotation extends SubscriptionAnnotation {
        private int mSubId;
        private int mSimType;
        private boolean mIsActive;
        private boolean mIsDisplayAllowed;
        private ParcelUuid mUuid;

        private TestSubAnnotation(int subId, int simType,
                boolean isActive, boolean isDisplayAllowed, ParcelUuid guuid) {
            super(null, -1, null, null, null, null);
            mSubId = subId;
            mSimType = simType;
            mIsActive = isActive;
            mIsDisplayAllowed = isDisplayAllowed;
            mUuid = guuid;
        }

        @Override
        public int getSubscriptionId() {
            return mSubId;
        }

        @Override
        public int getType() {
            return mSimType;
        }

        @Override
        public boolean isExisted() {
            return true;
        }

        @Override
        public boolean isActive() {
            return mIsActive;
        }

        @Override
        public boolean isDisplayAllowed() {
            return mIsDisplayAllowed;
        }

        @Override
        public ParcelUuid getGroupUuid() {
            return mUuid;
        }
    }
}
