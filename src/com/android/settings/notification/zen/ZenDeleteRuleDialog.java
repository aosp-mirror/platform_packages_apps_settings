/*
 * Copyright (c) 2017 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.BidiFormatter;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class ZenDeleteRuleDialog extends InstrumentedDialogFragment {
    protected static final String TAG = "ZenDeleteRuleDialog";
    private static final String EXTRA_ZEN_RULE_NAME = "zen_rule_name";
    private static final String EXTRA_ZEN_RULE_ID = "zen_rule_id";
    protected static PositiveClickListener mPositiveClickListener;

    /**
     * The interface we expect a listener to implement.
     */
    public interface PositiveClickListener {
        void onOk(String id);
    }

    public static void show(Fragment parent, String ruleName, String id, PositiveClickListener
            listener) {
        final BidiFormatter bidi = BidiFormatter.getInstance();
        final Bundle args = new Bundle();
        args.putString(EXTRA_ZEN_RULE_NAME, bidi.unicodeWrap(ruleName));
        args.putString(EXTRA_ZEN_RULE_ID, id);
        mPositiveClickListener = listener;

        ZenDeleteRuleDialog dialog = new ZenDeleteRuleDialog();
        dialog.setArguments(args);
        dialog.setTargetFragment(parent, 0);
        dialog.show(parent.getFragmentManager(), TAG);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ZEN_MODE_DELETE_RULE_DIALOG;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        String ruleName = arguments.getString(EXTRA_ZEN_RULE_NAME);
        String id = arguments.getString(EXTRA_ZEN_RULE_ID);

        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setMessage(getString(R.string.zen_mode_delete_rule_confirmation, ruleName))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.zen_mode_delete_rule_button,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (arguments != null) {
                            mPositiveClickListener.onOk(id);
                        }
                    }
                }).create();
        final View messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }
        return dialog;
    }

}
