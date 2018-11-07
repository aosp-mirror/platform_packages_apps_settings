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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.wifi.AccessPoint;
import java.util.ArrayList;
import java.util.List;

public class NetworkRequestDialogFragment extends InstrumentedDialogFragment implements
    DialogInterface.OnClickListener {

  /** Message sent to us to stop scanning wifi and pop up timeout dialog. */
  private static final int MESSAGE_STOP_SCAN_WIFI_LIST = 0;

  /** Delayed time to stop scanning wifi. */
  private static final int DELAY_TIME_STOP_SCAN_MS = 30*1000;

  private List<AccessPoint> mAccessPointList;

  public static NetworkRequestDialogFragment newInstance() {
    NetworkRequestDialogFragment dialogFragment = new NetworkRequestDialogFragment();
    return dialogFragment;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context context = getContext();

    // Prepares title.
    LayoutInflater inflater = LayoutInflater.from(context);
    View customTitle = inflater.inflate(R.layout.network_request_dialog_title, null);

    TextView title = customTitle.findViewById(R.id.network_request_title_text);
    title.setText(R.string.network_connection_request_dialog_title);
    ProgressBar progressBar = customTitle.findViewById(R.id.network_request_title_progress);
    progressBar.setVisibility(View.VISIBLE);

    // Prepares adapter.
    AccessPointAdapter adapter = new AccessPointAdapter(context,
        R.layout.preference_access_point, getAccessPointList());

    AlertDialog.Builder builder = new AlertDialog.Builder(context)
        .setCustomTitle(customTitle)
        .setAdapter(adapter, this)
        .setPositiveButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
    return builder.create();
  }

  List<AccessPoint> getAccessPointList() {
    // Initials list for adapter, in case of display crashing.
    if (mAccessPointList == null) {
      mAccessPointList = new ArrayList<>();
    }
    return mAccessPointList;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
  }

  @Override
  public void onPause() {
    super.onPause();

    mHandler.removeMessages(MESSAGE_STOP_SCAN_WIFI_LIST);
  }

  @Override
  public void onResume() {
    super.onResume();

    // TODO(b/117399926): Starts to scan current WiFi.

    // Sets time-out to stop scanning.
    mHandler.sendEmptyMessageDelayed(MESSAGE_STOP_SCAN_WIFI_LIST, DELAY_TIME_STOP_SCAN_MS);
  }

  private Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MESSAGE_STOP_SCAN_WIFI_LIST:
          removeMessages(MESSAGE_STOP_SCAN_WIFI_LIST);
          stopScanningAndPopTimeoutDialog();
          break;
        default:
          // Do nothing.
          break;
      }
    }
  };

  protected void stopScanningAndPopTimeoutDialog() {
    dismiss();
    NetworkRequestTimeoutDialogFragment fragment = NetworkRequestTimeoutDialogFragment.newInstance();
    fragment.show(getActivity().getSupportFragmentManager(), null);
  }

  @Override
  public int getMetricsCategory() {
    return MetricsProto.MetricsEvent.WIFI_SCANNING_NEEDED_DIALOG;
  }

  private class AccessPointAdapter extends ArrayAdapter<AccessPoint> {

    private final int mResourceId;
    private final LayoutInflater mInflater;

    public AccessPointAdapter(Context context, int resourceId, List<AccessPoint> objects) {
      super(context, resourceId, objects);
      mResourceId = resourceId;
      mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
      if (view == null) {
        view = mInflater.inflate(mResourceId, parent, false);
      }

      // TODO: Sets correct information to list item.
      final View divider = view.findViewById(com.android.settingslib.R.id.two_target_divider);
      divider.setVisibility(View.GONE);

      return view;
    }
  }
}

