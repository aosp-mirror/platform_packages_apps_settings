package com.android.settings.notification.zen;

import android.content.ComponentName;
import android.net.Uri;

public class ZenRuleInfo {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZenRuleInfo that = (ZenRuleInfo) o;

        if (isSystem != that.isSystem) return false;
        if (ruleInstanceLimit != that.ruleInstanceLimit) return false;
        if (packageName != null ? !packageName.equals(that.packageName) : that.packageName != null)
            return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (settingsAction != null ? !settingsAction.equals(
                that.settingsAction) : that.settingsAction != null) return false;
        if (configurationActivity != null ? !configurationActivity.equals(
                that.configurationActivity) : that.configurationActivity != null) return false;
        if (defaultConditionId != null ? !defaultConditionId.equals(
                that.defaultConditionId) : that.defaultConditionId != null) return false;
        if (serviceComponent != null ? !serviceComponent.equals(
                that.serviceComponent) : that.serviceComponent != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null)
            return false;
        return packageLabel != null ? packageLabel.equals(
                that.packageLabel) : that.packageLabel == null;

    }

    public String packageName;
    public String title;
    public String settingsAction;
    public ComponentName configurationActivity;
    public Uri defaultConditionId;
    public ComponentName serviceComponent;
    public boolean isSystem;
    public CharSequence packageLabel;
    public int ruleInstanceLimit = -1;
    public String id;
}
