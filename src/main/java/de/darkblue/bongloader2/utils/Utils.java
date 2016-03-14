/*
 * Copyright (C) 2016 Florian Frankenberger.
 *
 * This library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, see <http://www.gnu.org/licenses/>.
 */
package de.darkblue.bongloader2.utils;

import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class
 *
 * @author Florian Frankenberger
 */
public final class Utils {

    private Utils() {
    }

    private static final String[] FILE_SIZE_NAMES = {"Bytes", "KB", "MB", "GB"};
    private static final Pattern HOUR_DURATION_PATTERN = Pattern.compile("([0-9]+):([0-9]{2})");

    /**
     * transforms the given file size in bytes into a human readable
     * string like "15 MB" or "256 KB"
     *
     * @param fileSize
     * @return
     */
    public static String toHumanReadableFileSize(long fileSize) {
        for (int i = FILE_SIZE_NAMES.length - 1; i > 0; --i) {
            float thisValue = (float) Math.pow(1024, i);
            if (fileSize / thisValue > 1) {
                return String.format("%3.2f %s", fileSize / thisValue, FILE_SIZE_NAMES[i]);
            }
        }

        return "0";
    }

    /**
     * Converts a hour duration (hh:mm) to an integer representing the minutes
     *
     * @param raw
     * @return
     * @throws ParseException
     */
    public static int hourDurationToMinutes(String raw) throws ParseException {
        Matcher matcher = HOUR_DURATION_PATTERN.matcher(raw);
        if (!matcher.matches()) {
            throw new ParseException("Could not parse duration", 0);
        }

        try {
            return Integer.valueOf(matcher.group(1)) * 60 + Integer.valueOf(matcher.group(2));
        } catch (NumberFormatException e) {
            throw new ParseException("Could not parse duration", 0);
        }
    }

    /**
     * @param name
     * @return
     */
    public static String toCamelCase(String name) {
        String[] parts = name.split("[ _]+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; ++i) {
            sb.append((i == 0 ? parts[i].toLowerCase() : ucFirst(parts[i].toLowerCase())));
        }
        return sb.toString();
    }

    /**
     * upper cases the first letter of the given
     * string
     *
     * @param string
     * @return
     */
    public static String ucFirst(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    /**
     * tries to parse the value as int or in case of an error returns null
     *
     * @param string
     * @return
     */
    public static Integer toIntOrNull(String string) {
        Integer i = null;
        try {
            i = Integer.valueOf(string);
        } catch (NumberFormatException e) {
            //ignore
        }
        return i;
    }

    public static URL toURLOrNull(String string) {
        URL url = null;
        try {
            url = new URL(string);
        } catch (MalformedURLException e) {
            //ignore
        }

        return url;
    }

    /**
     * returns the file extension. For example "bla.mp4" would
     * return ".mp4"
     *
     * @param fileName
     * @return
     */
    public static String getFileNameExt(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    /**
     * returns the base name (so everything before the extension and the dot)
     * of a file name.
     *
     * @param fileName
     * @return
     */
    public static String getBaseName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public static int[] reverse(int[] array) {
        int[] result = new int[array.length];
        for (int i = 0; i < array.length; ++i) {
            result[i] = array[array.length - (i + 1)];
        }

        return result;
    }

    public static <T> T[] reverse(T[] array) {
        if (array.length > 0) {
            T[] result = (T[]) Array.newInstance(array[0].getClass(), array.length);

            for (int i = 0; i < array.length; ++i) {
                result[i] = array[array.length - (i + 1)];
            }

            return result;
        } else {
            return array;
        }
    }

}
