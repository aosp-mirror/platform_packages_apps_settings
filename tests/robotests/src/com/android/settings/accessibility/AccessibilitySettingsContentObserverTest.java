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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

import java.util.Collection;
import java.util.List;

/** Test for {@link AccessibilitySettingsContentObserver}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilitySettingsContentObserverTest {

    private static final String SPECIFIC_KEY_A_1 = "SPECIFIC_KEY_A_1";
    private static final String SPECIFIC_KEY_A_2 = "SPECIFIC_KEY_A_2";
    private static final String SPECIFIC_KEY_B_1 = "SPECIFIC_KEY_B_1";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final AccessibilitySettingsContentObserver mObserver =
            new AccessibilitySettingsContentObserver(new Handler());
    private final AccessibilitySettingsContentObserver.ContentObserverCallback mObserverCallbackA =
            spy(new TestableContentObserverCallback());
    private final AccessibilitySettingsContentObserver.ContentObserverCallback mObserverCallbackB =
            spy(new TestableContentObserverCallback());
    private final ContentResolver mContentResolver = mContext.getContentResolver();

    @Test
    public void register_shouldRegisterContentObserverForDefaultKeys() {
        mObserver.register(mContentResolver);

        ShadowContentResolver shadowContentResolver = Shadow.extract(mContentResolver);
        assertObserverToUri(shadowContentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, mObserver);
        assertObserverToUri(shadowContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, mObserver);
    }

    @Test
    public void unregister_shouldUnregisterContentObserver() {
        mObserver.register(mContentResolver);

        mObserver.unregister(mContentResolver);

        ShadowContentResolver shadowContentResolver = Shadow.extract(mContentResolver);
        assertNotObserverToUri(shadowContentResolver, Settings.Secure.ACCESSIBILITY_ENABLED);
        assertNotObserverToUri(shadowContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    }

    @Test
    public void register_addSpecificKeys_shouldRegisterContentObserverForSpecificAndDefaultKeys() {
        mObserver.registerKeysToObserverCallback(List.of(SPECIFIC_KEY_A_1), mObserverCallbackA);

        mObserver.register(mContentResolver);

        ShadowContentResolver shadowContentResolver = Shadow.extract(mContentResolver);
        assertObserverToUri(shadowContentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, mObserver);
        assertObserverToUri(shadowContentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, mObserver);
        assertObserverToUri(shadowContentResolver, SPECIFIC_KEY_A_1, mObserver);
    }

    @Test
    public void onChange_shouldTriggerCallbackOnDefaultKey() {
        mObserver.registerObserverCallback(mObserverCallbackA);
        mObserver.register(mContentResolver);

        mObserver.onChange(/* selfChange= */ false,
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED));

        verify(mObserverCallbackA).onChange(Settings.Secure.ACCESSIBILITY_ENABLED);
    }

    @Test
    public void onChange_registerSpecificKeys_shouldTriggerSpecificCallback() {
        mObserver.registerKeysToObserverCallback(
                List.of(SPECIFIC_KEY_A_1, SPECIFIC_KEY_A_2), mObserverCallbackA);
        mObserver.register(mContentResolver);

        mObserver.onChange(/* selfChange= */ false,
                Settings.Secure.getUriFor(SPECIFIC_KEY_A_2));

        verify(mObserverCallbackA).onChange(SPECIFIC_KEY_A_2);
    }

    @Test
    public void onChange_registerSpecificKeys_withoutTriggerOtherCallback() {
        mObserver.registerKeysToObserverCallback(
                List.of(SPECIFIC_KEY_A_1, SPECIFIC_KEY_A_2), mObserverCallbackA);
        mObserver.registerKeysToObserverCallback(
                List.of(SPECIFIC_KEY_B_1), mObserverCallbackB);
        mObserver.register(mContentResolver);

        mObserver.onChange(/* selfChange= */ false,
                Settings.Secure.getUriFor(SPECIFIC_KEY_B_1));

        verify(mObserverCallbackA, never()).onChange(SPECIFIC_KEY_A_1);
        verify(mObserverCallbackA, never()).onChange(SPECIFIC_KEY_A_2);
        verify(mObserverCallbackA, never()).onChange(SPECIFIC_KEY_B_1);
        verify(mObserverCallbackB).onChange(SPECIFIC_KEY_B_1);
    }

    @After
    public void tearDown() {
        mObserver.unregister(mContentResolver);
    }

    private void assertNotObserverToUri(ShadowContentResolver resolver, String key) {
        assertThat(resolver.getContentObservers(Settings.Secure.getUriFor(key))).isEmpty();
    }

    private void assertObserverToUri(ShadowContentResolver resolver,
            String key, ContentObserver observer) {
        Collection<ContentObserver> observers =
                resolver.getContentObservers(Settings.Secure.getUriFor(key));
        assertThat(observers).contains(observer);
    }

    private static class TestableContentObserverCallback implements
            AccessibilitySettingsContentObserver.ContentObserverCallback {
        @Override
        public void onChange(String key) {
            // do nothing
        }
    }
}
