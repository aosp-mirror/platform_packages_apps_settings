/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class NetworkRequestTimeoutDialogFragment extends InstrumentedDialogFragment implements
    DialogInterface.OnClickListener {

  public static NetworkRequestTimeoutDialogFragment newInstance() {
    NetworkRequestTimeoutDialogFragment fragment = new NetworkRequestTimeoutDialogFragment();
    return fragment;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
        .setMessage(R.string.network_connection_timeout_dialog_message)
        .setPositiveButton(R.string.network_connection_timeout_dialog_ok, this)
        .setNegativeButton(R.string.cancel, null);
    return builder.create();
  }

  @Override
  public int getMetricsCategory() {
    return MetricsProto.MetricsEvent.WIFI_SCANNING_NEEDED_DIALOG;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    switch (which) {
      case DialogInterface.BUTTON_POSITIVE:
        startScanningDialog();
        break;
      case DialogInterface.BUTTON_NEGATIVE:
      default:
        // Do nothing.
        break;
    }
  }

  protected void startScanningDialog() {
    NetworkRequestDialogFragment fragment = NetworkRequestDialogFragment.newInstance();
    fragment.show(getActivity().getSupportFragmentManager(), null);
  }
}
