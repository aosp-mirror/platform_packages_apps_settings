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

import android.app.ListActivity;
import android.content.pm.PackageInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;
import android.webkit.WebViewFactory;
import android.widget.ListView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.instrumentation.Instrumentable;
import com.android.settings.core.instrumentation.VisibilityLoggerMixin;

public class WebViewAppPicker extends ListActivity implements Instrumentable {
    private static final String TAG = WebViewAppPicker.class.getSimpleName();
    private WebViewAppListAdapter mAdapter;
    private WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;

    private final VisibilityLoggerMixin mVisibilityLoggerMixin =
            new VisibilityLoggerMixin(getMetricsCategory());

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (mWebViewUpdateServiceWrapper == null) {
            setWebViewUpdateServiceWrapper(createDefaultWebViewUpdateServiceWrapper());
        }
        mAdapter = new WebViewAppListAdapter(this, mWebViewUpdateServiceWrapper);
        setListAdapter(mAdapter);

        mVisibilityLoggerMixin.onAttach(this);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        WebViewApplicationInfo app = mAdapter.getItem(position);

        if (mWebViewUpdateServiceWrapper.setWebViewProvider(app.info.packageName)) {
            Intent intent = new Intent();
            intent.setAction(app.info.packageName);
            setResult(RESULT_OK, intent);
        } else {
            mWebViewUpdateServiceWrapper.showInvalidChoiceToast(this);
        }
        finish();
    }

    private WebViewUpdateServiceWrapper createDefaultWebViewUpdateServiceWrapper() {
        return new WebViewUpdateServiceWrapper();
    }

    @VisibleForTesting
    void setWebViewUpdateServiceWrapper(WebViewUpdateServiceWrapper wvusWrapper) {
        mWebViewUpdateServiceWrapper = wvusWrapper;
    }

    @Override
    public void onResume() {
        super.onResume();
        mVisibilityLoggerMixin.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mVisibilityLoggerMixin.onPause();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.WEBVIEW_IMPLEMENTATION;
    }
}
