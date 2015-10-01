package com.android.settings.notification;

import android.content.ComponentName;
import android.net.Uri;

public class ZenRuleInfo {
    public String packageName;
    public String title;
    public String settingsAction;
    public ComponentName configurationActivity;
    public Uri defaultConditionId;
    public ComponentName serviceComponent;
    public boolean isSystem;
}
