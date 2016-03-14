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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents a template string with placeholders that get replaced
 * to form a usable string
 *
 * @author Florian Frankenberger
 */
public class StringTemplate {

    private static final Logger logger = Logger.getLogger(StringTemplate.class.getCanonicalName());
    private static final FilterFunction<String> IDENTITY_FILTER_FUNCTION = new FilterFunction<String>() {

        @Override
        public String filter(String item) {
            return item;
        }

    };

    private static final Pattern pattern = Pattern.compile("\\{([^}]+?)(?:,([^\\/^}]+))?(?:\\/([^}]*))?\\}");
    private final String templateRaw;
    private final Matcher template;

    /**
     * A valid template consists of {}-elements e.g.: blafoo{sth}bar
     *
     * @param template
     */
    public StringTemplate(String template) {
        this.templateRaw = template;
        this.template = pattern.matcher(template);
    }

    /**
     * Builds a usable string
     *
     * Note, that the mapping keys are expected to be in lower-case!
     *
     * @param mapping
     * @return
     */
    public synchronized String apply(Map<Object, Object> mapping) {
        return apply(mapping, false);
    }

    public synchronized String apply(Map<Object, Object> mapping, boolean acceptMissingKeys) {
        return apply(mapping, IDENTITY_FILTER_FUNCTION, acceptMissingKeys);
    }

    public synchronized String apply(Map<Object, Object> mapping, FilterFunction<String> filterFunction, boolean acceptMissingKeys) {
        this.template.reset();

        StringBuffer sb = new StringBuffer();

        while (this.template.find()) {
            String key = this.template.group(1);
            String additional = this.template.group(2) != null ? this.template.group(2) : null;
            String defaultValue = this.template.group(3) != null ? this.template.group(3).trim() : this.template.group();

            this.template.appendReplacement(sb,
                    filterFunction.filter(getReplacement(key, additional, defaultValue,
                        mapping, acceptMissingKeys)));
        }

        this.template.appendTail(sb);

        return sb.toString();
    }

    public StringTemplate applyPartly(Map<Object, Object> mapping) {
        return new StringTemplate(this.apply(mapping, true));
    }

    private static String getReplacement(String key, String additional, String defaultValue,
            Map<Object, Object> mapping,
            boolean acceptMissingKeys) {

        //additional object mapping?
        Object replacementObject = mapping.get(key);
        if (replacementObject == null && key.contains(".")) {
            String[] callParts = key.split("\\.");
            if (callParts.length == 2) {
                try {
                    Object obj = mapping.get(callParts[0]);
                    if (obj != null) {
                        final String getterMethodName = "get" + Utils.ucFirst(callParts[1]);
                        final Method method = obj.getClass().getMethod(getterMethodName);
                        if (method != null) {
                            replacementObject = method.invoke(obj);
                        }
                    }
                } catch (IllegalAccessException ex) {
                } catch (IllegalArgumentException ex) {
                } catch (InvocationTargetException ex) {
                } catch (NoSuchMethodException ex) {
                } catch (SecurityException ex) {
                }
            }
        }

        if (replacementObject == null) {
            if (acceptMissingKeys) {
                return defaultValue;
            } else {
                logger.log(Level.WARNING, "No replacement for key {0} found.", key);
                return key + "_NA";
            }
        }

        if (replacementObject instanceof Date) {
            Date date = (Date) replacementObject;
            final String aPattern = (additional != null ? additional
                    : "dd.MM.yyyy_HH:mm:ss");
            try {
                final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(aPattern);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
                return simpleDateFormat.format(date);
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Pattern for date replacement {0} was invalid", key);
                return key + "_ERROR";
            }
        } else {
            if (additional != null) {
                try {
                    return String.format(additional, replacementObject);
                } catch (IllegalFormatException e) {
                    logger.log(Level.WARNING, "Pattern {0}could not be applied to the datatype (which was a {1})",
                            new Object[]{pattern, replacementObject.getClass().getCanonicalName()});
                }
            }
            return replacementObject.toString();
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.templateRaw != null ? this.templateRaw.hashCode() : 0);
        return hash;
    }

    /*
     * (non-Javadoc) @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StringTemplate)) {
            return false;
        }
        StringTemplate other = (StringTemplate) obj;
        return other.templateRaw.equals(this.templateRaw);
    }
}
