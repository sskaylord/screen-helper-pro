package com.display.utils;

public class StrObf {
    private static final byte KEY = (byte) 0xA7;

    public static String d(String encoded) {
        char[] out = new char[encoded.length()];
        for (int i = 0; i < encoded.length(); i++) {
            out[i] = (char)(encoded.charAt(i) ^ KEY);
        }
        return new String(out);
    }

    public static final String SYS_CFG = d("\u00d4\u00de\u00d4\u00f8\u00c4\u00c1\u00c0");
    public static final String LIB_IL2CPP = d("\u00cb\u00ce\u00c5\u00ce\u00cb\u0095\u00c4\u00d7\u00d7\u0089\u00d4\u00c8");
    public static final String DISPLAY_UTILS_LIB = d("\u00c3\u00ce\u00d4\u00d7\u00cb\u00c6\u00de\u00f8\u00d2\u00d3\u00ce\u00cb\u00d4");
}
