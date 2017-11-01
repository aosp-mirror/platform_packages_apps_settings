package com.android.settings.search;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Parcel;
import android.provider.Settings;
import com.android.settings.TestConfig;
import com.android.settings.search.ResultPayload.SettingsSource;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class InlinePayloadTest {

    private Context mContext;

    private final String KEY = "key";

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testGetSecure_returnsSecureSetting() {
        InlinePayload payload = getDummyPayload(SettingsSource.SECURE);
        int currentValue = 2;
        Settings.Secure.putInt(mContext.getContentResolver(), KEY, currentValue);

        int newValue = payload.getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }

    @Test
    public void testGetGlobal_returnsGlobalSetting() {
        InlinePayload payload = getDummyPayload(SettingsSource.GLOBAL);
        int currentValue = 2;
        Settings.Global.putInt(mContext.getContentResolver(), KEY, currentValue);

        int newValue = payload.getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }

    @Test
    public void testGetSystem_returnsSystemSetting() {
        InlinePayload payload = getDummyPayload(SettingsSource.SYSTEM);
        int currentValue = 2;
        Settings.System.putInt(mContext.getContentResolver(), KEY, currentValue);

        int newValue = payload.getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }

    @Test
    public void testSetSecure_updatesSecureSetting() {
        InlinePayload payload = getDummyPayload(SettingsSource.SECURE);
        int newValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, KEY, 0);

        payload.setValue(mContext, newValue);
        int updatedValue = Settings.System.getInt(resolver, KEY, -1);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    public void testSetGlobal_updatesGlobalSetting() {
        InlinePayload payload = getDummyPayload(SettingsSource.GLOBAL);
        int newValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Global.putInt(resolver, KEY, 0);

        payload.setValue(mContext, newValue);
        int updatedValue = Settings.Global.getInt(resolver, KEY, -1);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    public void testSetSystem_updatesSystemSetting() {
        InlinePayload payload = getDummyPayload(SettingsSource.SECURE);
        int newValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putInt(resolver, SCREEN_BRIGHTNESS_MODE, 0);

        payload.setValue(mContext, newValue);
        int updatedValue = Settings.System.getInt(resolver, KEY, -1);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    private InlinePayload getDummyPayload(int source) {
        return new ConcreteInlinePayload(KEY, source, null /* intent */,
                true /* isDeviceSupported */);
    }

    class ConcreteInlinePayload extends InlinePayload {

        public ConcreteInlinePayload(String key, @SettingsSource int source, Intent intent,
                boolean isDeviceSupported) {
            super(key, source, intent, isDeviceSupported, 0 /* defaultValue */);
        }

        @Override
        public int getType() {
            return 0;
        }

        @Override
        protected int standardizeInput(int input) throws IllegalArgumentException {
            return input;
        }
    }
}
