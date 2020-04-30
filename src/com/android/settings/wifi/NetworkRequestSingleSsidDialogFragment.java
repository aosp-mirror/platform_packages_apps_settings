package com.android.settings.wifi;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;

/**
 * This is similar fragment with {@link NetworkRequestDialogFragment} but only for single SSID mode.
 */
public class NetworkRequestSingleSsidDialogFragment extends
        NetworkRequestDialogBaseFragment {
    public static final String EXTRA_SSID = "DIALOG_REQUEST_SSID";
    public static final String EXTRA_TRYAGAIN = "DIALOG_IS_TRYAGAIN";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean isTryAgain = false;
        String requestSsid = "";
        if (getArguments() != null) {
            isTryAgain = getArguments().getBoolean(EXTRA_TRYAGAIN, true);
            requestSsid = getArguments().getString(EXTRA_SSID, "");
        }

        final Context context = getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);

        final View customTitle = inflater.inflate(R.layout.network_request_dialog_title, null);
        final TextView title = customTitle.findViewById(R.id.network_request_title_text);
        title.setText(getTitle());
        final TextView summary = customTitle.findViewById(R.id.network_request_summary_text);
        summary.setText(getSummary());
        final ProgressBar progressBar = customTitle
                .findViewById(R.id.network_request_title_progress);
        progressBar.setVisibility(View.GONE);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setCustomTitle(customTitle)
                .setMessage(requestSsid)
                .setPositiveButton(isTryAgain ? R.string.network_connection_timeout_dialog_ok
                        : R.string.wifi_connect, (dialog, which) -> onUserClickConnectButton())
                .setNeutralButton(R.string.cancel, (dialog, which) -> onCancel(dialog));

        // Don't dismiss dialog when touching outside. User reports it is easy to touch outside.
        // This causes dialog to close.
        setCancelable(false);

        return builder.create();
    }

    private void onUserClickConnectButton() {
        if (mActivity != null) {
            mActivity.onClickConnectButton();
        }
    }
}
