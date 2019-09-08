package com.imaginadesarrollo.universalhrm.utils;

import java.util.UUID;

public class Utils {
    public static int decode(byte[] characteristicValue){
        int bmp = characteristicValue[1] & 0xFF;
        return bmp;
    }

    public static UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }
}
