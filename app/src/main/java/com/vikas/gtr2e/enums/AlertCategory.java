package com.vikas.gtr2e.enums;

/*
 * Based on code from Gadgetbridge:
 * https://codeberg.org/Freeyourgadget/Gadgetbridge
 * Licensed under AGPLv3
 *
 * Modifications by Vikas Tiwari
 */

import lombok.Getter;

/**
 * <a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id.xml">...</a>
 * <a href="https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_category_id_bit_mask.xml">...</a>
 */
@Getter
public enum AlertCategory {
    Email(1),
    IncomingCall(3),
    MissedCall(4),
    SMS(5),
    Any(255),
    CustomHuami(-6);

    private final int id;
    AlertCategory(int id) {
        this.id = id;
    }
}
