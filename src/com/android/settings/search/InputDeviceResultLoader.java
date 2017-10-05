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
 * limitations under the License.
 */

package com.android.settings.search;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.android.settings.R;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settingslib.inputmethod.InputMethodAndSubtypeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Search result for input devices (physical/virtual keyboard, game controllers, etc)
 */

public class InputDeviceResultLoader extends FutureTask<List<? extends SearchResult>> {

    private static final String TAG = "InputResultFutureTask";

    @VisibleForTesting
    static final String PHYSICAL_KEYBOARD_FRAGMENT = PhysicalKeyboardFragment.class.getName();
    @VisibleForTesting
    static final String VIRTUAL_KEYBOARD_FRAGMENT =
            AvailableVirtualKeyboardFragment.class.getName();

    public InputDeviceResultLoader(Context context, String query, SiteMapManager manager) {
        super(new InputDeviceResultCallable(context, query, manager));
    }

    static class InputDeviceResultCallable implements
            Callable<List<? extends SearchResult>> {
        private static final int NAME_NO_MATCH = -1;

        private final Context mContext;
        private final SiteMapManager mSiteMapManager;
        private final InputManager mInputManager;
        private final InputMethodManager mImm;
        private final PackageManager mPackageManager;
        @VisibleForTesting
        final String mQuery;

        private List<String> mPhysicalKeyboardBreadcrumb;
        private List<String> mVirtualKeyboardBreadcrumb;

        public InputDeviceResultCallable(Context context, String query, SiteMapManager mapManager) {
            mContext = context;
            mQuery = query;
            mSiteMapManager = mapManager;
            mInputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
            mImm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
            mPackageManager = context.getPackageManager();
        }

        @Override
        public List<? extends SearchResult> call() {
            long startTime = System.currentTimeMillis();
            final List<SearchResult> results = new ArrayList<>();
            results.addAll(buildPhysicalKeyboardSearchResults());
            results.addAll(buildVirtualKeyboardSearchResults());
            Collections.sort(results);
            Log.i(TAG, "Input search loading took:" + (System.currentTimeMillis() - startTime));
            return results;
        }

        private Set<SearchResult> buildPhysicalKeyboardSearchResults() {
            final Set<SearchResult> results = new HashSet<>();
            final String screenTitle = mContext.getString(R.string.physical_keyboard_title);

            for (final InputDevice device : getPhysicalFullKeyboards()) {
                final String deviceName = device.getName();
                final int wordDiff = InstalledAppResultLoader.getWordDifference(deviceName,
                        mQuery);
                if (wordDiff == NAME_NO_MATCH) {
                    continue;
                }
                final String keyboardLayoutDescriptor = mInputManager
                        .getCurrentKeyboardLayoutForInputDevice(device.getIdentifier());
                final KeyboardLayout keyboardLayout = (keyboardLayoutDescriptor != null)
                        ? mInputManager.getKeyboardLayout(keyboardLayoutDescriptor) : null;
                final String summary = (keyboardLayout != null)
                        ? keyboardLayout.toString()
                        : mContext.getString(R.string.keyboard_layout_default_label);

                final Intent intent = DatabaseIndexingUtils.buildSearchResultPageIntent(mContext,
                        PHYSICAL_KEYBOARD_FRAGMENT, deviceName, screenTitle);
                results.add(new SearchResult.Builder()
                        .setTitle(deviceName)
                        .setPayload(new ResultPayload(intent))
                        .setStableId(Objects.hash(PHYSICAL_KEYBOARD_FRAGMENT, deviceName))
                        .setSummary(summary)
                        .setRank(wordDiff)
                        .addBreadcrumbs(getPhysicalKeyboardBreadCrumb())
                        .build());
            }
            return results;
        }

        private Set<SearchResult> buildVirtualKeyboardSearchResults() {
            final Set<SearchResult> results = new HashSet<>();
            final String screenTitle = mContext.getString(R.string.add_virtual_keyboard);
            final List<InputMethodInfo> inputMethods = mImm.getInputMethodList();
            for (InputMethodInfo info : inputMethods) {
                final String title = info.loadLabel(mPackageManager).toString();
                final String summary = InputMethodAndSubtypeUtil
                        .getSubtypeLocaleNameListAsSentence(getAllSubtypesOf(info), mContext, info);
                int wordDiff = InstalledAppResultLoader.getWordDifference(title, mQuery);
                if (wordDiff == NAME_NO_MATCH) {
                    wordDiff = InstalledAppResultLoader.getWordDifference(summary, mQuery);
                }
                if (wordDiff == NAME_NO_MATCH) {
                    continue;
                }
                final ServiceInfo serviceInfo = info.getServiceInfo();
                final String key = new ComponentName(serviceInfo.packageName, serviceInfo.name)
                        .flattenToString();
                final Intent intent = DatabaseIndexingUtils.buildSearchResultPageIntent(mContext,
                        VIRTUAL_KEYBOARD_FRAGMENT, key, screenTitle);
                results.add(new SearchResult.Builder()
                        .setTitle(title)
                        .setSummary(summary)
                        .setRank(wordDiff)
                        .setStableId(Objects.hash(VIRTUAL_KEYBOARD_FRAGMENT, key))
                        .addBreadcrumbs(getVirtualKeyboardBreadCrumb())
                        .setPayload(new ResultPayload(intent))
                        .build());
            }
            return results;
        }

        private List<String> getPhysicalKeyboardBreadCrumb() {
            if (mPhysicalKeyboardBreadcrumb == null || mPhysicalKeyboardBreadcrumb.isEmpty()) {
                mPhysicalKeyboardBreadcrumb = mSiteMapManager.buildBreadCrumb(
                        mContext, PHYSICAL_KEYBOARD_FRAGMENT,
                        mContext.getString(R.string.physical_keyboard_title));
            }
            return mPhysicalKeyboardBreadcrumb;
        }


        private List<String> getVirtualKeyboardBreadCrumb() {
            if (mVirtualKeyboardBreadcrumb == null || mVirtualKeyboardBreadcrumb.isEmpty()) {
                final Context context = mContext;
                mVirtualKeyboardBreadcrumb = mSiteMapManager.buildBreadCrumb(
                        context, VIRTUAL_KEYBOARD_FRAGMENT,
                        context.getString(R.string.add_virtual_keyboard));
            }
            return mVirtualKeyboardBreadcrumb;
        }

        private List<InputDevice> getPhysicalFullKeyboards() {
            final List<InputDevice> keyboards = new ArrayList<>();
            final int[] deviceIds = InputDevice.getDeviceIds();
            if (deviceIds != null) {
                for (int deviceId : deviceIds) {
                    final InputDevice device = InputDevice.getDevice(deviceId);
                    if (device != null && !device.isVirtual() && device.isFullKeyboard()) {
                        keyboards.add(device);
                    }
                }
            }
            return keyboards;
        }

        private static List<InputMethodSubtype> getAllSubtypesOf(final InputMethodInfo imi) {
            final int subtypeCount = imi.getSubtypeCount();
            final List<InputMethodSubtype> allSubtypes = new ArrayList<>(subtypeCount);
            for (int index = 0; index < subtypeCount; index++) {
                allSubtypes.add(imi.getSubtypeAt(index));
            }
            return allSubtypes;
        }
    }
}
