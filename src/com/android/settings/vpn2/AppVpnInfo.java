package com.android.settings.vpn2;

import androidx.annotation.NonNull;

import com.android.internal.util.Preconditions;

import java.util.Objects;

/**
 * Holds packageName:userId pairs without any heavyweight fields.
 * {@see ApplicationInfo}
 */
class AppVpnInfo implements Comparable<AppVpnInfo> {
    public final int userId;
    public final String packageName;

    public AppVpnInfo(int userId, @NonNull String packageName) {
        this.userId = userId;
        this.packageName = Preconditions.checkNotNull(packageName);
    }

    @Override
    public int compareTo(AppVpnInfo other) {
        int result = packageName.compareTo(other.packageName);
        if (result == 0) {
            result = other.userId - userId;
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof AppVpnInfo) {
            AppVpnInfo that = (AppVpnInfo) other;
            return userId == that.userId && Objects.equals(packageName, that.packageName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, userId);
    }
}
