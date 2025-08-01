/*  Copyright (C) 2018-2024 Andreas Shimokawa, gsbhat, José Rebelo, NekoBox

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */

//##### THIS CLASS IS TAKEN FROM GADGETBRIDGE PROJECT, NOT MY WORK #####//
//CREDIT GOES TO GADGETBRIDGE FOR PROVIDING THIS CLASS

package com.vikas.gtr2e.ble;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HuamiService {


    public static final String BASE_UUID = "0000%s-0000-1000-8000-00805f9b34fb";

    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_CONTROL_POINT = UUID.fromString((String.format(BASE_UUID, "2A39")));
    public static final UUID UUID_CHARACTERISTIC_REALTIME_STEPS = UUID.fromString(String.format(BASE_UUID, "FF06"));
    public static final UUID UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT = UUID.fromString((String.format(BASE_UUID, "2A37")));
    public static final UUID UUID_SERVICE_MIBAND_SERVICE = UUID.fromString(String.format(BASE_UUID, "FEE0"));
    public static final UUID UUID_SERVICE_MIBAND2_SERVICE = UUID.fromString(String.format(BASE_UUID, "FEE1"));
    public static final UUID UUID_SERVICE_HEART_RATE = UUID.fromString(String.format(BASE_UUID, "180D"));
    public static final UUID UUID_SERVICE_FIRMWARE_SERVICE = UUID.fromString("00001530-0000-3512-2118-0009af100700");

    public static final UUID UUID_CHARACTERISTIC_FIRMWARE_CONTROL = UUID.fromString("00001531-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_FIRMWARE_DATA = UUID.fromString("00001532-0000-3512-2118-0009af100700");

    public static final UUID UUID_UNKNOWN_CHARACTERISTIC0 = UUID.fromString("00000000-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_RAW_SENSOR_CONTROL = UUID.fromString("00000001-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_RAW_SENSOR_DATA = UUID.fromString("00000002-0000-3512-2118-0009af100700");

    /**
     * Alarms, Display and other configuration.
     */
    public static final UUID UUID_CHARACTERISTIC_3_CONFIGURATION = UUID.fromString("00000003-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_5_ACTIVITY_CONTROL = UUID.fromString("00000004-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_5_ACTIVITY_DATA = UUID.fromString("00000005-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_6_BATTERY_INFO = UUID.fromString("00000006-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_7_REALTIME_STEPS = UUID.fromString("00000007-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_8_USER_SETTINGS = UUID.fromString("00000008-0000-3512-2118-0009af100700");
    // service uuid fee1
    public static final UUID UUID_CHARACTERISTIC_AUTH = UUID.fromString("00000009-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_WORKOUT = UUID.fromString("0000000f-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_DEVICEEVENT = UUID.fromString("00000010-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_AUDIO = UUID.fromString("00000012-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_AUDIODATA = UUID.fromString("00000013-0000-3512-2118-0009af100700");
    public static final UUID UUID_UNKNOWN_CHARACTERISTIC5 = UUID.fromString("00000014-0000-3512-2118-0009af100700");

    public static final UUID UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_WRITE = UUID.fromString("00000016-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ = UUID.fromString("00000017-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_CHUNKEDTRANSFER = UUID.fromString("00000020-0000-3512-2118-0009af100700");

    public static final UUID UUID_BT_SERIAL_SERVICE = UUID.fromString("00000022-0000-3512-2118-0009af100700");

    public static final UUID UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_SEND = UUID.fromString("00000023-0000-3512-2118-0009af100700");
    public static final UUID UUID_CHARACTERISTIC_ZEPP_OS_FILE_TRANSFER_V3_RECEIVE = UUID.fromString("00000024-0000-3512-2118-0009af100700");

    public static final int ALERT_LEVEL_NONE = 0;
    public static final int ALERT_LEVEL_MESSAGE = 1;
    public static final int ALERT_LEVEL_PHONE_CALL = 2;
    public static final int ALERT_LEVEL_VIBRATE_ONLY = 3;


    // set metric distance
    // set 12 hour time mode



    private static final Map<UUID, String> MIBAND_DEBUG;

    /**
     * Mi Band 2 authentication has three steps.
     * This is step 1: sending a "secret" key to the band.
     * This is byte 0, followed by {@link #AUTH_BYTE} and then the key.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_SEND_KEY = 0x01;
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 2: requesting a random authentication key from the band.
     * This is byte 0, followed by {@link #AUTH_BYTE}.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_REQUEST_RANDOM_AUTH_NUMBER = 0x02;
    /**
     * Mi Band 2 authentication has three steps.
     * This is step 3: sending the encrypted random authentication key to the band.
     * This is byte 0, followed by {@link #AUTH_BYTE} and then the encrypted random authentication key.
     * In the response, it is byte 1 in the byte[] value.
     */
    public static final byte AUTH_SEND_ENCRYPTED_AUTH_NUMBER = 0x03;

    /**
     * Received in response to any authentication requests (byte 0 in the byte[] value.
     */
    public static final byte AUTH_RESPONSE = 0x10;
    /**
     * Received in response to any authentication requests (byte 2 in the byte[] value.
     * 0x01 means success.
     */
    public static final byte AUTH_SUCCESS = 0x01;
    /**
     * Received in response to any authentication requests (byte 2 in the byte[] value.
     * 0x04 means failure.
     */
    public static final byte AUTH_FAIL = 0x04;
    /**
     * In some logs it's 0x0...
     */
    public static final byte AUTH_BYTE = 0x08;

    // maybe not really activity data, but steps?
    public static final byte COMMAND_FETCH_DATA = 0x02;
    // delete/drop activity data
    // on Huami it's just the single 03 byte
    // on Huami 2021 it's followed by 09 to keep, 01 to drop from device
    public static final byte COMMAND_ACK_ACTIVITY_DATA = 0x03;

    public static final byte[] COMMAND_SET_FITNESS_GOAL_START = new byte[] { 0x10, 0x0, 0x0 };
    public static final byte[] COMMAND_SET_FITNESS_GOAL_END = new byte[] { 0, 0 };

    public static final byte COMMAND_SET_USERINFO = 0x4f;

    public static final byte ICON_HIGH_PRIORITY = 0x7;

    public static byte ENDPOINT_DISPLAY_ITEMS = 0x0a;

    public static byte DISPLAY_ITEM_BIT_CLOCK = 0x01;
    public static byte DISPLAY_ITEM_BIT_STEPS = 0x02;
    public static byte DISPLAY_ITEM_BIT_DISTANCE = 0x04;
    public static byte DISPLAY_ITEM_BIT_CALORIES= 0x08;
    public static byte DISPLAY_ITEM_BIT_HEART_RATE = 0x10;
    public static byte DISPLAY_ITEM_BIT_BATTERY = 0x20;

    // Second byte must be a bitwise OR combination of the above
    // The clock can't be disabled
    public static int SCREEN_CHANGE_BYTE = 1;
    public static final byte[] COMMAND_CHANGE_SCREENS = new byte[]{ENDPOINT_DISPLAY_ITEMS, DISPLAY_ITEM_BIT_CLOCK, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05};

    public static byte ENDPOINT_DISPLAY = 0x06;

    public static final byte[] DATEFORMAT_DATE_TIME = new byte[] {ENDPOINT_DISPLAY, 0x0a, 0x0, 0x03 };
    public static final byte[] DATEFORMAT_TIME = new byte[] {ENDPOINT_DISPLAY, 0x0a, 0x0, 0x0 };
    public static final byte[] DATEFORMAT_TIME_12_HOURS = new byte[] {ENDPOINT_DISPLAY, 0x02, 0x0, 0x0 };
    public static final byte[] DATEFORMAT_TIME_24_HOURS = new byte[] {ENDPOINT_DISPLAY, 0x02, 0x0, 0x1 };
    public static final byte[] DATEFORMAT_DATE_MM_DD_YYYY = new byte[]{ENDPOINT_DISPLAY, 30, 0x00, 'M', 'M', '/', 'd', 'd', '/', 'y', 'y', 'y', 'y'};
    public static final byte[] COMMAND_ENBALE_HR_CONNECTION = new byte[]{ENDPOINT_DISPLAY, 0x1f, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_HR_CONNECTION = new byte[]{ENDPOINT_DISPLAY, 0x1f, 0x00, 0x00};
    public static final byte[] COMMAND_ENABLE_BT_CONNECTED_ADVERTISEMENT = new byte[]{ENDPOINT_DISPLAY, 0x01, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_BT_CONNECTED_ADVERTISEMENT = new byte[]{ENDPOINT_DISPLAY, 0x01, 0x00, 0x00};
    public static final byte[] COMMAND_ENABLE_DISPLAY_ON_LIFT_WRIST = new byte[]{ENDPOINT_DISPLAY, 0x05, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00};
    public static final byte[] COMMAND_DISABLE_DISPLAY_ON_LIFT_WRIST = new byte[]{ENDPOINT_DISPLAY, 0x05, 0x00, 0x00};
    public static final byte[] COMMAND_SCHEDULE_DISPLAY_ON_LIFT_WRIST = new byte[]{ENDPOINT_DISPLAY, 0x05, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00};
    public static final byte[] COMMAND_DISPLAY_ON_LIFT_WRIST_SPEED_NORMAL = new byte[]{ENDPOINT_DISPLAY, 0x23, 0x00, 0x00};
    public static final byte[] COMMAND_DISPLAY_ON_LIFT_WRIST_SPEED_SENSITIVE = new byte[]{ENDPOINT_DISPLAY, 0x23, 0x00, 0x01};
    public static final byte[] COMMAND_ENABLE_GOAL_NOTIFICATION = new byte[]{ENDPOINT_DISPLAY, 0x06, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_GOAL_NOTIFICATION = new byte[]{ENDPOINT_DISPLAY, 0x06, 0x00, 0x00};
    public static final byte[] COMMAND_ENABLE_ROTATE_WRIST_TO_SWITCH_INFO = new byte[]{ENDPOINT_DISPLAY, 0x0d, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_ROTATE_WRIST_TO_SWITCH_INFO = new byte[]{ENDPOINT_DISPLAY, 0x0d, 0x00, 0x00};
    public static final byte[] COMMAND_ENABLE_DISPLAY_CALLER = new byte[]{ENDPOINT_DISPLAY, 0x10, 0x00, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_DISPLAY_CALLER = new byte[]{ENDPOINT_DISPLAY, 0x10, 0x00, 0x00, 0x00};
    public static final byte[] DISPLAY_YYY = new byte[] {ENDPOINT_DISPLAY, 0x10, 0x0, 0x1, 0x1 };
    public static final byte[] COMMAND_DISTANCE_UNIT_METRIC = new byte[] { ENDPOINT_DISPLAY, 0x03, 0x00, 0x00 };
    public static final byte[] COMMAND_DISTANCE_UNIT_IMPERIAL = new byte[] { ENDPOINT_DISPLAY, 0x03, 0x00, 0x01 };
    public static final byte[] COMMAND_SET_LANGUAGE_NEW_TEMPLATE = new byte[]{ENDPOINT_DISPLAY, 0x17, 0x00, 0, 0, 0, 0, 0};
    public static final byte[] COMMAND_FACTORY_RESET = new byte[]{ENDPOINT_DISPLAY, 0x0b, 0x00, 0x01};
    public static final byte[] COMMAND_ENABLE_DISCONNECT_NOTIFCATION = new byte[]{ENDPOINT_DISPLAY, 0x0c, 0x00, 0x01, 0, 0, 0, 0};
    public static final byte[] COMMAND_DISABLE_DISCONNECT_NOTIFCATION = new byte[]{ENDPOINT_DISPLAY, 0x0c, 0x00, 0x00, 0, 0, 0, 0};

    public static final byte COMMAND_ALARMS = 0x0d;
    public static final byte COMMAND_ALARMS_WITH_TIMES = 0x01;
    public static final byte COMMAND_WORKOUT_ACTIVITY_TYPES = 0x11;
    public static final byte COMMAND_GPS_VERSION = 0x0e;

    public static final byte[] COMMAND_REQUEST_ALARMS = new byte[]{COMMAND_ALARMS};
    public static final byte[] COMMAND_REQUEST_ALARMS_WITH_TIMES = new byte[]{(byte) 0xff, COMMAND_ALARMS_WITH_TIMES, 0x00, 0x00, 0x00};
    public static final byte[] COMMAND_REQUEST_WORKOUT_ACTIVITY_TYPES = new byte[]{(byte) 0xff, COMMAND_WORKOUT_ACTIVITY_TYPES, 0x00, 0x00, 0x00};

    public static final byte[] COMMAND_REQUEST_GPS_VERSION = new byte[]{COMMAND_GPS_VERSION};

    // The third byte controls the threshold, in minutes
    // The last 8 bytes represent 2 separate time intervals for the inactivity warnings
    // If there is no do not disturb interval, the last 4 bytes (the second interval) are 0
    // and only the first interval of the command is used
    public static int INACTIVITY_WARNINGS_THRESHOLD = 2;
    public static int INACTIVITY_WARNINGS_INTERVAL_1_START_HOURS = 4;
    public static int INACTIVITY_WARNINGS_INTERVAL_1_START_MINUTES = 5;
    public static int INACTIVITY_WARNINGS_INTERVAL_1_END_HOURS = 6;
    public static int INACTIVITY_WARNINGS_INTERVAL_1_END_MINUTES = 7;
    public static int INACTIVITY_WARNINGS_INTERVAL_2_START_HOURS = 8;
    public static int INACTIVITY_WARNINGS_INTERVAL_2_START_MINUTES = 9;
    public static int INACTIVITY_WARNINGS_INTERVAL_2_END_HOURS = 10;
    public static int INACTIVITY_WARNINGS_INTERVAL_2_END_MINUTES = 11;
    public static final byte[] COMMAND_ENABLE_INACTIVITY_WARNINGS = new byte[] { 0x08, 0x01, 0x3c, 0x00, 0x04, 0x00, 0x15, 0x00, 0x00, 0x00, 0x00, 0x00 };
    public static final byte[] COMMAND_DISABLE_INACTIVITY_WARNINGS = new byte[] { 0x08, 0x00, 0x3c, 0x00, 0x04, 0x00, 0x15, 0x00, 0x00, 0x00, 0x00, 0x00 };

    public static byte ENDPOINT_DND = 0x09;

    public static final byte[] COMMAND_DO_NOT_DISTURB_AUTOMATIC = new byte[] { ENDPOINT_DND, (byte) 0x83 };
    public static final byte[] COMMAND_DO_NOT_DISTURB_OFF = new byte[] { ENDPOINT_DND, (byte) 0x82 };
    public static final byte[] COMMAND_DO_NOT_DISTURB_SCHEDULED = new byte[] { ENDPOINT_DND, (byte) 0x81, 0x01, 0x00, 0x06, 0x00 };
    // The 4 last bytes set the start and end time in 24h format
    public static byte DND_BYTE_START_HOURS = 2;
    public static byte DND_BYTE_START_MINUTES = 3;
    public static byte DND_BYTE_END_HOURS = 4;
    public static byte DND_BYTE_END_MINUTES = 5;

    public static final byte MUSIC_FLAG_STATE = 0x01;
    public static final byte MUSIC_FLAG_ARTIST = 0x02;
    public static final byte MUSIC_FLAG_ALBUM = 0x04;
    public static final byte MUSIC_FLAG_TRACK = 0x08;
    public static final byte MUSIC_FLAG_DURATION = 0x10;
    public static final byte MUSIC_FLAG_NOTHING_PLAYING = 0x20;
    public static final byte MUSIC_FLAG_VOLUME = 0x40;

    public static final byte RESPONSE = 0x10;

    public static final byte SUCCESS = 0x01;
    public static final byte COMMAND_ACTIVITY_DATA_START_DATE = 0x01;
    public static final byte COMMAND_ACTIVITY_DATA_TYPE_ACTIVTY = 0x01;
    public static final byte COMMAND_ACTIVITY_DATA_XXX_DATE = 0x02; // issued on first connect, followd by COMMAND_XXXX_ACTIVITY_DATA instead of COMMAND_FETCH_DATA

    public static final byte COMMAND_FIRMWARE_INIT = 0x01; // to UUID_CHARACTERISTIC_FIRMWARE, followed by fw file size in bytes
    public static final byte COMMAND_FIRMWARE_START_DATA = 0x03; // to UUID_CHARACTERISTIC_FIRMWARE
    public static final byte COMMAND_FIRMWARE_UPDATE_SYNC = 0x00; // to UUID_CHARACTERISTIC_FIRMWARE
    public static final byte COMMAND_FIRMWARE_CHECKSUM = 0x04; // to UUID_CHARACTERISTIC_FIRMWARE
    public static final byte COMMAND_FIRMWARE_REBOOT = 0x05; // to UUID_CHARACTERISTIC_FIRMWARE

    public static final byte[] WEAR_LOCATION_LEFT_WRIST = new byte[] { 0x20, 0x00, 0x00, 0x02 };
    public static final byte[] WEAR_LOCATION_RIGHT_WRIST = new byte[] { 0x20, 0x00, 0x00, (byte) 0x82};

    public static final byte[] COMMAND_ENABLE_HR_SLEEP_MEASUREMENT = new byte[]{0x15, 0x00, 0x01};
    public static final byte[] COMMAND_DISABLE_HR_SLEEP_MEASUREMENT = new byte[]{0x15, 0x00, 0x00};

    public static final byte COMMAND_SET_PERIODIC_HR_MEASUREMENT_INTERVAL = 0x14;

    public static final byte[] COMMAND_TEXT_NOTIFICATION = new byte[] {0x05, 0x01};

    public static final byte[] COMMAND_ENABLE_HOURLY_CHIME = new byte[] { (byte) 0xfe, 0x0b, 0x00, 0x01, 0x0a, 0x00, 0x16, 0x00 };
    public static final byte[] COMMAND_DISABLE_HOURLY_CHIME = new byte[] { (byte) 0xfe, 0x0b, 0x00, 0x00 };

    static {
        MIBAND_DEBUG = new HashMap<>();
        MIBAND_DEBUG.put(UUID_SERVICE_MIBAND_SERVICE, "MiBand Service");
        MIBAND_DEBUG.put(UUID_SERVICE_HEART_RATE, "MiBand HR Service");
    }

    public static String lookup(UUID uuid, String fallback) {
        String name = MIBAND_DEBUG.get(uuid);
        if (name == null) {
            name = fallback;
        }
        return name;
    }
}
