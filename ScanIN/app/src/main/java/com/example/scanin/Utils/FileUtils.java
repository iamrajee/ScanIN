package com.example.scanin.Utils;

public class FileUtils {

    public static boolean isAlphabet (char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    public static boolean isNumber (char c) {
        return (c >= '0' && c <= '9');
    }

    /*
     * Must contain only a-zA-Z0-9_
     */
    public static boolean validateFileName (String fileName) {
        for (int i = 0; i < fileName.length(); i++) {
             char c = fileName.charAt(i);
             if (isAlphabet(c) || isNumber(c) || c == '_') {

             } else {
                 return false;
             }
        }
        return isAlphabet(fileName.charAt(0));
    }
}
