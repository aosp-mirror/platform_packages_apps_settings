package com.android.settings.search;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class InlineListPayloadTest {

    private static final String DUMMY_SETTING = "inline_list_key";

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testConstructor_DataRetained() {
        final String uri = "test.com";
        final int type = ResultPayload.PayloadType.INLINE_LIST;
        final int source = ResultPayload.SettingsSource.SYSTEM;
        final String intentKey = "key";
        final String intentVal = "value";
        final Intent intent = new Intent();
        intent.putExtra(intentKey, intentVal);

        InlineListPayload payload = new InlineListPayload(uri, source,
                intent, true /* isAvailable */, 1 /* numOptions */, 1 /* default */);

        final Intent retainedIntent = payload.getIntent();
        assertThat(payload.mSettingKey).isEqualTo(uri);
        assertThat(payload.getType()).isEqualTo(type);
        assertThat(payload.mSettingSource).isEqualTo(source);
        assertThat(payload.getAvailability()).isEqualTo(ResultPayload.Availability.AVAILABLE);
        assertThat(retainedIntent.getStringExtra(intentKey)).isEqualTo(intentVal);
    }

    @Test
    public void testParcelConstructor_DataRetained() {
        String uri = "test.com";
        int type = ResultPayload.PayloadType.INLINE_LIST;
        int source = ResultPayload.SettingsSource.SYSTEM;
        final String intentKey = "key";
        final String intentVal = "value";
        final Intent intent = new Intent();
        intent.putExtra(intentKey, intentVal);

        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(intent, 0);
        parcel.writeString(uri);
        parcel.writeInt(source);
        parcel.writeInt(InlineSwitchPayload.TRUE);
        parcel.writeInt(InlineSwitchPayload.TRUE);
        parcel.setDataPosition(0);

        InlineListPayload payload = InlineListPayload
                .CREATOR.createFromParcel(parcel);

        final Intent builtIntent = payload.getIntent();
        assertThat(payload.mSettingKey).isEqualTo(uri);
        assertThat(payload.getType()).isEqualTo(type);
        assertThat(payload.mSettingSource).isEqualTo(source);
        assertThat(payload.getAvailability()).isEqualTo(ResultPayload.Availability.AVAILABLE);
        assertThat(builtIntent.getStringExtra(intentKey)).isEqualTo(intentVal);
    }

    @Test
    public void testInputStandardization_inputDoesntChange() {
        InlineListPayload payload = new InlineListPayload(DUMMY_SETTING,
                ResultPayload.SettingsSource.SYSTEM, null /* intent */, true /* isDeviceSupport */,
                3 /* numOptions */, 0 /* default */);
        int input = 2;

        assertThat(payload.standardizeInput(input)).isEqualTo(input);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSystem_negativeValue_throwsError() {
        InlineListPayload payload = new InlineListPayload(DUMMY_SETTING,
                ResultPayload.SettingsSource.SYSTEM, null /* intent */, true /* isDeviceSupport */,
                3 /* numOptions */, 0 /* default */);

        payload.setValue(mContext, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSystem_exceedsMaxValue_throwsError() {
        int maxOptions = 4;
        InlineListPayload payload = new InlineListPayload(DUMMY_SETTING,
                ResultPayload.SettingsSource.SYSTEM, null /* intent */, true /* isDeviceSupport */,
                maxOptions /* numOptions */, 0 /* default */);

        payload.setValue(mContext, maxOptions + 1);
    }
}
