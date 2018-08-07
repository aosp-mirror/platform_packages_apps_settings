package com.android.settings.development;

import android.content.Intent;

/**
 * Interface for activity result callbacks in the {@link DevelopmentSettingsDashboardFragment}
 */
public interface OnActivityResultListener {
    /**
     * Called when an activity returns to the {@link DevelopmentSettingsDashboardFragment}.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @return true if the controller handled the result.
     */
    boolean onActivityResult(int requestCode, int resultCode, Intent data);
}
