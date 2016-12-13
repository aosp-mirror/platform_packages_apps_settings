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

package com.android.settings.webview;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.shadows.ShadowView.clickOn;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.view.View;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class WebViewAppPickerTest {

  private static final String DEFAULT_PACKAGE_NAME = "DEFAULT_PACKAGE_NAME";

  private static ApplicationInfo createApplicationInfo(String packageName) {
      ApplicationInfo ai = new ApplicationInfo();
      ai.packageName = packageName;
      return ai;
  }

  @Test
  public void testClickingItemChangesProvider() {
      ActivityController<WebViewAppPicker> controller =
              Robolectric.buildActivity(WebViewAppPicker.class);
      WebViewAppPicker webviewAppPicker = controller.get();

      WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
      when(wvusWrapper.getValidWebViewApplicationInfos(any())).thenReturn(
              Arrays.asList(createApplicationInfo(DEFAULT_PACKAGE_NAME)));
      when(wvusWrapper.setWebViewProvider(eq(DEFAULT_PACKAGE_NAME))).thenReturn(true);

      webviewAppPicker.setWebViewUpdateServiceWrapper(wvusWrapper);

      controller.create().start().postCreate(null).resume().visible();
      WebViewApplicationInfo firstItem =
              (WebViewApplicationInfo) webviewAppPicker.getListView().getItemAtPosition(0);
      assertThat(firstItem.info.packageName).isEqualTo(DEFAULT_PACKAGE_NAME);

      webviewAppPicker.onListItemClick(webviewAppPicker.getListView(), null, 0, 0);

      verify(wvusWrapper, times(1)).setWebViewProvider(eq(DEFAULT_PACKAGE_NAME));
      assertThat(shadowOf(webviewAppPicker).getResultCode()).isEqualTo(Activity.RESULT_OK);
      verify(wvusWrapper, never()).showInvalidChoiceToast(any());
  }

  @Test
  public void testFailingPackageChangeReturnsCancelled() {
      ActivityController<WebViewAppPicker> controller =
              Robolectric.buildActivity(WebViewAppPicker.class);
      WebViewAppPicker webviewAppPicker = controller.get();

      WebViewUpdateServiceWrapper wvusWrapper = mock(WebViewUpdateServiceWrapper.class);
      when(wvusWrapper.getValidWebViewApplicationInfos(any())).thenReturn(
              Arrays.asList(createApplicationInfo(DEFAULT_PACKAGE_NAME)));
      when(wvusWrapper.setWebViewProvider(eq(DEFAULT_PACKAGE_NAME))).thenReturn(false);

      webviewAppPicker.setWebViewUpdateServiceWrapper(wvusWrapper);

      controller.create().start().postCreate(null).resume().visible();
      WebViewApplicationInfo firstItem =
              (WebViewApplicationInfo) webviewAppPicker.getListView().getItemAtPosition(0);
      assertThat(firstItem.info.packageName).isEqualTo(DEFAULT_PACKAGE_NAME);

      webviewAppPicker.onListItemClick(webviewAppPicker.getListView(), null, 0, 0);

      verify(wvusWrapper, times(1)).setWebViewProvider(eq(DEFAULT_PACKAGE_NAME));
      assertThat(shadowOf(webviewAppPicker).getResultCode()).isEqualTo(Activity.RESULT_CANCELED);
      verify(wvusWrapper, times(1)).showInvalidChoiceToast(any());
  }
}
