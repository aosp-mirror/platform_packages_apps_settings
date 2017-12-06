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
import static com.android.settings.search.InputDeviceResultLoader.PHYSICAL_KEYBOARD_FRAGMENT;
import static com.android.settings.search.InputDeviceResultLoader.VIRTUAL_KEYBOARD_FRAGMENT;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SiteMapManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowInputDevice;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowInputDevice.class
        })
public class InputDeviceResultLoaderTest {

    private static final String QUERY = "test_query";
    private static final List<String> PHYSICAL_KEYBOARD_BREADCRUMB;
    private static final List<String> VIRTUAL_KEYBOARD_BREADCRUMB;

    static {
        PHYSICAL_KEYBOARD_BREADCRUMB = new ArrayList<>();
        VIRTUAL_KEYBOARD_BREADCRUMB = new ArrayList<>();
        PHYSICAL_KEYBOARD_BREADCRUMB.add("Settings");
        PHYSICAL_KEYBOARD_BREADCRUMB.add("physical keyboard");
        VIRTUAL_KEYBOARD_BREADCRUMB.add("Settings");
        VIRTUAL_KEYBOARD_BREADCRUMB.add("virtual keyboard");
    }

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private SiteMapManager mSiteMapManager;
    @Mock
    private InputManager mInputManager;
    @Mock
    private InputMethodManager mImm;
    @Mock
    private PackageManager mPackageManager;

    private InputDeviceResultLoader mLoader;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(Context.INPUT_SERVICE))
                .thenReturn(mInputManager);
        when(mContext.getSystemService(INPUT_METHOD_SERVICE))
                .thenReturn(mImm);
        when(mContext.getPackageManager())
                .thenReturn(mPackageManager);
        when(mContext.getString(anyInt()))
                .thenAnswer(invocation -> RuntimeEnvironment.application.getString(
                        (Integer) invocation.getArguments()[0]));
        mLoader = new InputDeviceResultLoader(mContext, QUERY, mSiteMapManager);
    }

    @After
    public void tearDown() {
        ShadowInputDevice.reset();
    }

    @Test
    public void query_noKeyboard_shouldNotReturnAnything() {
        assertThat(mLoader.loadInBackground()).isEmpty();
    }

    @Test
    public void query_hasPhysicalKeyboard_match() {
        addPhysicalKeyboard(QUERY);
        when(mSiteMapManager.buildBreadCrumb(mContext, PHYSICAL_KEYBOARD_FRAGMENT,
                RuntimeEnvironment.application.getString(R.string.physical_keyboard_title)))
                .thenReturn(PHYSICAL_KEYBOARD_BREADCRUMB);

        final List<SearchResult> results = new ArrayList<>(mLoader.loadInBackground());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title).isEqualTo(QUERY);
        assertThat(results.get(0).breadcrumbs)
                .containsExactlyElementsIn(PHYSICAL_KEYBOARD_BREADCRUMB);
    }

    @Test
    public void query_hasVirtualKeyboard_match() {
        addVirtualKeyboard(QUERY);
        when(mSiteMapManager.buildBreadCrumb(mContext, VIRTUAL_KEYBOARD_FRAGMENT,
                RuntimeEnvironment.application.getString(R.string.add_virtual_keyboard)))
                .thenReturn(VIRTUAL_KEYBOARD_BREADCRUMB);

        final List<SearchResult> results = new ArrayList<>(mLoader.loadInBackground());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).title).isEqualTo(QUERY);
        assertThat(results.get(0).breadcrumbs)
                .containsExactlyElementsIn(VIRTUAL_KEYBOARD_BREADCRUMB);
    }

    @Test
    public void query_hasPhysicalVirtualKeyboard_doNotMatch() {
        addPhysicalKeyboard("abc");
        addVirtualKeyboard("def");

        assertThat(mLoader.loadInBackground()).isEmpty();
        verifyZeroInteractions(mSiteMapManager);
    }

    private void addPhysicalKeyboard(String name) {
        final InputDevice device = mock(InputDevice.class);
        when(device.isVirtual()).thenReturn(false);
        when(device.isFullKeyboard()).thenReturn(true);
        when(device.getName()).thenReturn(name);
        ShadowInputDevice.sDeviceIds = new int[]{0};
        ShadowInputDevice.addDevice(0, device);
    }

    private void addVirtualKeyboard(String name) {
        final List<InputMethodInfo> imis = new ArrayList<>();
        final InputMethodInfo info = mock(InputMethodInfo.class);
        imis.add(info);
        when(info.getServiceInfo()).thenReturn(new ServiceInfo());
        when(info.loadLabel(mPackageManager)).thenReturn(name);
        info.getServiceInfo().packageName = "pkg";
        info.getServiceInfo().name = "class";
        when(mImm.getInputMethodList()).thenReturn(imis);
    }

}
