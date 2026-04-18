package com.vikas.gtr2e.watchFeatureUtilities;

public class GTR2eDisplayUtil {

    /**
     * Set the auto brightness mode in the watch
     * @param enabled if true sets to Auto Brightness, else Manually controlled brightness on the watch
     * @return byte array of the command
     */
    public static byte[] setAutoBrightness(boolean enabled) {
        byte[] cmd = new byte[7];

        if(enabled) {
            //0307 5900 0900 0000 0A00 0501 0101 0001 010B 01
        } else {
            //0307 5800 0900 0000 0A00 0501 0101 0001 010B 00
        }

        return cmd;
    }

}
