package godau.fynn.dsbdirect.download;

import java.util.Random;
import java.util.UUID;

/**
 * Class which generates fun metadata for app. endpoint (legacy)
 */
public abstract class DsbAppQueryMetadata {

    /**
     * @return Some existing device model
     */
    public static String getDeviceModel() {
        final String[] DEVICE_LIST = {
                "GM1910", "GM1911", "GM1913", "GM1915", "GM1917", "G020E", "G020F", "G020G", "G020H",
                "PH-1",
                "VTR-AL00", "VTR-L09", "VTR-L29", "VTR-TL00",
                "TA-1181", "TA-1196",
                "J9150",
                "I3113", "I3123", "I4113", "I4193",
                "HTV33",
                "2PZC5"
        };

        return DEVICE_LIST[new Random().nextInt(DEVICE_LIST.length)];
    }

    /**
     * @return Some valid combination of API level and Android version
     */
    public static String getAndroidVersion() {
        final String[] VERSION_LIST = {
                "14 4.0.2", "15 4.0.4", "16 4.1.1", "17 4.2.2", "18 4.3", "19 4.4",
                "21 5.0", "22 5.1",
                "23 6.0.1",
                "24 7.0", "25 7.1.1",
                "26 8.0", "27 8.1",
                "28 9",
                "29 10.0"
        };

        return VERSION_LIST[new Random().nextInt(VERSION_LIST.length)];

    }

    /**
     * @return A language String, either en or (more likely) de
     */
    public static String getLanguage() {
        if (Math.random() > 0.9) return "en"; else return "de";
    }

    public static String getAppId() {
        // Generate AppId
        return UUID.randomUUID().toString();
    }
}
