package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class MiBandCoordinator extends AbstractDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandCoordinator.class);
    private final MiBandSampleProvider sampleProvider;

    public MiBandCoordinator() {
        sampleProvider = new MiBandSampleProvider();
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        String macAddress = candidate.getMacAddress().toUpperCase();
        if (macAddress.startsWith(MiBandService.MAC_ADDRESS_FILTER_1_1A)
                || macAddress.startsWith(MiBandService.MAC_ADDRESS_FILTER_1S)) {
            return true;
        }
        if (candidate.supportsService(MiBandService.UUID_SERVICE_MIBAND_SERVICE)) {
            return true;
        }
        // and a heuristic
        try {
            if (isHealthWearable(candidate.getDevice())) {
                return candidate.getName().toUpperCase().startsWith(MiBandConst.MI_GENERAL_NAME_PREFIX.toUpperCase());
            }
        } catch (Exception ex) {
            LOG.error("unable to check device support", ex);
        }
        return false;
    }

    @Override
    public boolean supports(GBDevice device) {
        return getDeviceType().equals(device.getType());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MIBAND;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return MiBandPairingActivity.class;
    }

    public Class<? extends Activity> getPrimaryActivity() {
        return ChartsActivity.class;
    }

    @Override
    public SampleProvider getSampleProvider() {
        return sampleProvider;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        MiBandFWInstallHandler handler = new MiBandFWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsAlarmConfiguration() {
        return true;
    }

    @Override
    public int getTapString() {
        return R.string.tap_connected_device_for_activity;
    }

    public static boolean hasValidUserInfo() {
        String dummyMacAddress = MiBandService.MAC_ADDRESS_FILTER_1_1A + ":00:00:00";
        try {
            UserInfo userInfo = getConfiguredUserInfo(dummyMacAddress);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Returns the configured user info, or, if that is not available or invalid,
     * a default user info.
     *
     * @param miBandAddress
     */
    public static UserInfo getAnyUserInfo(String miBandAddress) {
        try {
            return getConfiguredUserInfo(miBandAddress);
        } catch (Exception ex) {
            LOG.error("Error creating user info from settings, using default user instead: " + ex);
            return UserInfo.getDefault(miBandAddress);
        }
    }

    /**
     * Returns the user info from the user configured data in the preferences.
     *
     * @param miBandAddress
     * @throws IllegalArgumentException when the user info can not be created
     */
    public static UserInfo getConfiguredUserInfo(String miBandAddress) throws IllegalArgumentException {
        ActivityUser activityUser = new ActivityUser();
        Prefs prefs = GBApplication.getPrefs();

        UserInfo info = UserInfo.create(
                miBandAddress,
                prefs.getString(MiBandConst.PREF_USER_ALIAS, null),
                activityUser.getActivityUserGender(),
                activityUser.getActivityUserAge(),
                activityUser.getActivityUserHeightCm(),
                activityUser.getActivityUserWeightKg(),
                0
        );
        return info;
    }

    public static int getWearLocation(String miBandAddress) throws IllegalArgumentException {
        int location = 0; //left hand
        Prefs prefs = GBApplication.getPrefs();
        if ("right".equals(prefs.getString(MiBandConst.PREF_MIBAND_WEARSIDE, "left"))) {
            location = 1; // right hand
        }
        return location;
    }

	public static int getDeviceTimeOffsetHours() throws IllegalArgumentException {
		Prefs prefs = GBApplication.getPrefs();
		return prefs.getInt(MiBandConst.PREF_MIBAND_DEVICE_TIME_OFFSET_HOURS, 0);
	}

    public static boolean getHeartrateSleepSupport(String miBandAddress) throws IllegalArgumentException {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getBoolean(MiBandConst.PREF_MIBAND_USE_HR_FOR_SLEEP_DETECTION, false);
    }

    public static int getFitnessGoal(String miBandAddress) throws IllegalArgumentException {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getInt(MiBandConst.PREF_MIBAND_FITNESS_GOAL, 10000);
    }

    public static int getReservedAlarmSlots(String miBandAddress) throws IllegalArgumentException {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getInt(MiBandConst.PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR, 0);
    }
}
