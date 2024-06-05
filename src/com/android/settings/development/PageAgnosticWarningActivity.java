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

package com.android.settings.development;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.settings.R;

public class PageAgnosticWarningActivity extends Activity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        String title =
                Enable16kUtils.isUsing16kbPages()
                        ? getString(R.string.page_agnostic_16k_pages_title)
                        : getString(R.string.page_agnostic_4k_pages_title);

        String warningText =
                Enable16kUtils.isUsing16kbPages()
                        ? getString(R.string.page_agnostic_16k_pages_text)
                        : getString(R.string.page_agnostic_4k_pages_text);
        showWarningDialog(title, warningText);
    }

    // Create warning dialog and make links clickable
    private void showWarningDialog(String title, String warningText) {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(Html.fromHtml(warningText, Html.FROM_HTML_MODE_COMPACT))
                        .setCancelable(false)
                        .setPositiveButton(
                                android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            @NonNull DialogInterface dialog, int which) {
                                        dialog.cancel();
                                        finish();
                                    }
                                })
                        .create();
        dialog.show();

        ((TextView) dialog.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }
}
