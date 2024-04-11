package com.siller.rohlik.store.testSupport;

public class Utils {

    public static String nChars(char c, int length) {
        return new String(new char[length]).replace('\0', c);
    }
}
