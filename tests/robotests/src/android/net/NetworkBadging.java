package android.net;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Implementation for {@link android.net.NetworkBadging}.
 *
 * <p>Can be removed once Robolectric supports Android O.
 */
public class NetworkBadging {
    @IntDef({BADGING_NONE, BADGING_SD, BADGING_HD, BADGING_4K})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Badging {}

    public static final int BADGING_NONE = 0;
    public static final int BADGING_SD = 10;
    public static final int BADGING_HD = 20;
    public static final int BADGING_4K = 30;

    private static Drawable drawable;

    public static Drawable getWifiIcon(
            int signalLevel, @NetworkBadging.Badging int badging, @Nullable Resources.Theme theme) {
        return new ColorDrawable(Color.GREEN);
    }
}
