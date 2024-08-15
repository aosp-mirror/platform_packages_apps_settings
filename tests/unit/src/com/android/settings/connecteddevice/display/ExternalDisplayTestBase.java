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
package com.android.settings.connecteddevice.display;

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY;
import static com.android.settings.flags.Flags.FLAG_ROTATION_CONNECTED_DISPLAY_SETTING;
import static com.android.settings.flags.Flags.FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.IDisplayManager;
import android.os.RemoteException;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.DisplayInfo;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.server.testutils.TestHandler;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.flags.FakeFeatureFlagsImpl;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ExternalDisplayTestBase {
    @Mock
    ExternalDisplaySettingsConfiguration.Injector mMockedInjector;
    @Mock
    IDisplayManager mMockedIDisplayManager;
    Resources mResources;
    DisplayManagerGlobal mDisplayManagerGlobal;
    FakeFeatureFlagsImpl mFlags = new FakeFeatureFlagsImpl();
    Context mContext;
    DisplayListener mListener;
    TestHandler mHandler = new TestHandler(null);
    PreferenceManager mPreferenceManager;
    PreferenceScreen mPreferenceScreen;
    Display[] mDisplays;

    /**
     * Setup.
     */
    @Before
    public void setUp() throws RemoteException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        doReturn(mResources).when(mContext).getResources();
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        doReturn(0).when(mMockedIDisplayManager).getPreferredWideGamutColorSpaceId();
        mDisplayManagerGlobal = new DisplayManagerGlobal(mMockedIDisplayManager);
        mFlags.setFlag(FLAG_ROTATION_CONNECTED_DISPLAY_SETTING, true);
        mFlags.setFlag(FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING, true);
        mDisplays = new Display[] {
                createDefaultDisplay(), createExternalDisplay(), createOverlayDisplay()};
        doReturn(mDisplays).when(mMockedInjector).getAllDisplays();
        doReturn(mDisplays).when(mMockedInjector).getEnabledDisplays();
        for (var display : mDisplays) {
            doReturn(display).when(mMockedInjector).getDisplay(display.getDisplayId());
        }
        doReturn(mFlags).when(mMockedInjector).getFlags();
        doReturn(mHandler).when(mMockedInjector).getHandler();
        doReturn("").when(mMockedInjector).getSystemProperty(
                VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY);
        doReturn(true).when(mMockedInjector).isModeLimitForExternalDisplayEnabled();
        doAnswer((arg) -> {
            mListener = arg.getArgument(0);
            return null;
        }).when(mMockedInjector).registerDisplayListener(any());
        doReturn(0).when(mMockedInjector).getDisplayUserRotation(anyInt());
        doReturn(mContext).when(mMockedInjector).getContext();
    }

    Display createDefaultDisplay() throws RemoteException {
        int displayId = 0;
        var displayInfo = new DisplayInfo();
        doReturn(displayInfo).when(mMockedIDisplayManager).getDisplayInfo(displayId);
        displayInfo.displayId = displayId;
        displayInfo.name = "Built-in";
        displayInfo.type = Display.TYPE_INTERNAL;
        displayInfo.supportedModes = new Display.Mode[]{
                new Display.Mode(0, 2048, 1024, 60, 60, new float[0],
                    new int[0])};
        displayInfo.appsSupportedModes = displayInfo.supportedModes;
        return createDisplay(displayInfo);
    }

    Display createExternalDisplay() throws RemoteException {
        int displayId = 1;
        var displayInfo = new DisplayInfo();
        doReturn(displayInfo).when(mMockedIDisplayManager).getDisplayInfo(displayId);
        displayInfo.displayId = displayId;
        displayInfo.name = "HDMI";
        displayInfo.type = Display.TYPE_EXTERNAL;
        displayInfo.supportedModes = new Display.Mode[]{
                new Display.Mode(0, 1920, 1080, 60, 60, new float[0], new int[0]),
                new Display.Mode(1, 800, 600, 60, 60, new float[0], new int[0]),
                new Display.Mode(2, 320, 240, 70, 70, new float[0], new int[0]),
                new Display.Mode(3, 640, 480, 60, 60, new float[0], new int[0]),
                new Display.Mode(4, 640, 480, 50, 60, new float[0], new int[0]),
                new Display.Mode(5, 2048, 1024, 60, 60, new float[0], new int[0]),
                new Display.Mode(6, 720, 480, 60, 60, new float[0], new int[0])};
        displayInfo.appsSupportedModes = displayInfo.supportedModes;
        return createDisplay(displayInfo);
    }

    Display createOverlayDisplay() throws RemoteException {
        int displayId = 2;
        var displayInfo = new DisplayInfo();
        doReturn(displayInfo).when(mMockedIDisplayManager).getDisplayInfo(displayId);
        displayInfo.displayId = displayId;
        displayInfo.name = "Overlay #1";
        displayInfo.type = Display.TYPE_OVERLAY;
        displayInfo.supportedModes = new Display.Mode[]{
                new Display.Mode(0, 1240, 780, 60, 60, new float[0],
                    new int[0])};
        displayInfo.appsSupportedModes = displayInfo.supportedModes;
        return createDisplay(displayInfo);
    }

    Display createDisplay(DisplayInfo displayInfo) {
        return new Display(mDisplayManagerGlobal, displayInfo.displayId, displayInfo,
                (DisplayAdjustments) null);
    }
}
