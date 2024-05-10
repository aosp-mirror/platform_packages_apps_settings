/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.activeunlock;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowLooper.idleMainLooper;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;

import com.android.settings.biometrics.activeunlock.ActiveUnlockContentListener.OnContentChangedListener;
import com.android.settings.testutils.ActiveUnlockTestUtils;
import com.android.settings.testutils.shadow.ShadowDeviceConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class ActiveUnlockContentListenerTest {

    @Rule public final MockitoRule mMocks = MockitoJUnit.rule();
    @Mock private PackageManager mPackageManager;

    private Context mContext;
    private ActiveUnlockContentListener mContentListener;
    @Nullable private String mContent;
    private int mUpdateCount;

    @Before
    public void setUp() {
        Robolectric.setupContentProvider(
                FakeContentProvider.class, FakeContentProvider.AUTHORITY);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        OnContentChangedListener listener = new OnContentChangedListener() {
            @Override
            public void onContentChanged(String newValue) {
                mContent = newValue;
                mUpdateCount++;
            }
        };
        ActiveUnlockTestUtils.enable(mContext);
        mContentListener =
                new ActiveUnlockContentListener(
                        mContext,
                        listener,
                        "logTag",
                        FakeContentProvider.METHOD_SUMMARY,
                        FakeContentProvider.KEY_SUMMARY);
        FakeContentProvider.init(mContext);
    }

    @Test
    public void subscribe_contentFetched() {
        String newContent = "newContent";
        FakeContentProvider.setTileSummary(newContent);

        mContentListener.subscribe();
        idleMainLooper();

        assertThat(mContent).isEqualTo(newContent);
    }

    @Test
    public void contentUpdated_contentUpdated() {
        mContentListener.subscribe();
        idleMainLooper();

        String newContent = "newContent";
        updateContent(newContent);

        assertThat(mContent).isEqualTo(newContent);
    }

    @Test
    public void contentUpdated_unsubscribed_contentNotUpdated() {
        mContentListener.subscribe();
        idleMainLooper();

        mContentListener.unsubscribe();
        updateContent("newContent");

        assertThat(mContent).isNull();
    }

    @Test
    public void multipleContentUpdates_contentIsNewestValueAndUpdatedTwice() {
        mContentListener.subscribe();
        idleMainLooper();

        updateContent("temporaryContent");
        String newContent = "newContent";
        updateContent(newContent);

        assertThat(mContent).isEqualTo(newContent);
        assertThat(mUpdateCount).isEqualTo(2);
    }

    @Test
    public void duplicateContentUpdates_onContentChangedOnlyCalledOnce() {
        mContentListener.subscribe();
        idleMainLooper();

        updateContent("newContent");
        updateContent("newContent");

        assertThat(mUpdateCount).isEqualTo(1);
    }

    @Test
    public void noProvider_subscribeDoesntRegisterObserver() {
        when(mPackageManager.getInstalledPackages(any()))
                .thenReturn(new ArrayList<>());
        OnContentChangedListener listener = new OnContentChangedListener() {
            @Override
            public void onContentChanged(String newValue) {}
        };

        ActiveUnlockContentListener contentListener =
                new ActiveUnlockContentListener(
                        mContext,
                        listener,
                        "logTag",
                        FakeContentProvider.METHOD_SUMMARY,
                        FakeContentProvider.KEY_SUMMARY);

        assertThat(contentListener.subscribe()).isFalse();
    }

    private void updateContent(String content) {
        FakeContentProvider.setTileSummary(content);
        mContext.getContentResolver().notifyChange(
                FakeContentProvider.URI, null /* observer */);
        idleMainLooper();
    }
}
