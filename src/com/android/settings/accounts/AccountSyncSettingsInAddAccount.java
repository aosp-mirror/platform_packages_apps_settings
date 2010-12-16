
package com.android.settings.accounts;

import android.accounts.Account;
import android.content.ContentResolver;
import android.preference.Preference;
import com.android.settings.R;

import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * This is AccountSyncSettings with 'remove account' button always gone and 
 * a wizard-like button bar to complete the activity.
 */
public class AccountSyncSettingsInAddAccount extends AccountSyncSettings 
        implements OnClickListener {
    private View mFinishArea;
    private View mFinishButton;

    @Override
    protected void initializeUi(final View rootView) {
        super.initializeUi(rootView);

        mFinishArea = (View) rootView.findViewById(R.id.finish_button_area);
        mFinishArea.setVisibility(View.VISIBLE);
        mFinishButton = (View) rootView.findViewById(R.id.finish_button);
        mFinishButton.setOnClickListener(this);

        mUseSyncManagerFeedsState = false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Remove the "remove account" menu item
        menu.findItem(MENU_REMOVE_ACCOUNT_ID).setVisible(false);
    }

    public void onClick(View v) {
        applySyncSettingsToSyncManager();
        finish();
    }

    private void applySyncSettingsToSyncManager() {
        for (int i = 0, count = getPreferenceScreen().getPreferenceCount(); i < count; i++) {
            Preference pref = getPreferenceScreen().getPreference(i);
            if (! (pref instanceof SyncStateCheckBoxPreference)) {
                continue;
            }
            SyncStateCheckBoxPreference syncPref = (SyncStateCheckBoxPreference) pref;

            String authority = syncPref.getAuthority();
            Account account = syncPref.getAccount();

            ContentResolver.setSyncAutomatically(account, authority, syncPref.isChecked());
        }
    }
}
