package com.vikas.gtr2e.ble;
/*
 * Based on code from Gadgetbridge:
 * https://codeberg.org/Freeyourgadget/Gadgetbridge
 * Licensed under AGPLv3
 *
 * Modifications by Vikas Tiwari
 */
public interface Huami2021Handler {
    void handle2021Payload(short type, byte[] payload);
}
