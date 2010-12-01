
package com.android.settings.accounts;

import com.android.settings.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
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
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Remove the "remove account" menu item
        menu.findItem(MENU_REMOVE_ACCOUNT_ID).setVisible(false);
    }

    public void onClick(View v) {
        finish();
    }
}
