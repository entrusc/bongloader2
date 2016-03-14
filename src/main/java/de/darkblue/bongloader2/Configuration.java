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
package de.darkblue.bongloader2;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class Configuration {

    private static final Logger logger = Logger.getLogger(Configuration.class.getCanonicalName());
    private Properties configuration = new Properties();
    private final Properties defaults;
    private File file;
    private Set<ConfigurationUpdateListener> configurationUpdateListeners = new HashSet<ConfigurationUpdateListener>();

    public Configuration(Properties defaults, File file) throws IOException {
        this.file = file;
        this.defaults = defaults;
        this.configuration.putAll(defaults);

        if (file.exists()) {
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(file);
                this.configuration.load(fileReader);
            } finally {
                if (fileReader != null) {
                    fileReader.close();
                }
            }
        }

        this.save();
    }

    public void clear(ConfigurationKey key) {
        this.configuration.remove(key.getKey());
        try {
            this.save();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save config.", e);
        }
    }

    public void set(ConfigurationKey key, String value) {
        String oldValue = configuration.getProperty(key.getKey());
        this.configuration.setProperty(key.getKey(), value);
        
        try {
            this.save();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not save config.", e);
        }
        
        if (oldValue == null || !oldValue.equals(value)) {
            this.notifyUpdate(key);
        }
    }

    public void setBoolean(ConfigurationKey key, boolean value) {
        this.set(key, Boolean.toString(value));
    }

    public void setFile(ConfigurationKey key, File file) {
        this.set(key, file.getAbsolutePath());
    }

    public void setInt(ConfigurationKey key, int i) {
        this.set(key, String.valueOf(i));
    }

    public void setLong(ConfigurationKey key, long i) {
        this.set(key, String.valueOf(i));
    }

    public boolean isConfigured(ConfigurationKey key) {
        return this.configuration.containsKey(key.getKey());
    }

    /**
     * Forces the existence of the given key - if the key is not existent
     * a runtimeException is thrown
     *
     * @param key
     * @return
     */
    public synchronized String get(ConfigurationKey key) {
        String value = this.configuration.getProperty(key.getKey());
        if (value == null) {
            throw new RuntimeException("Neccessary value in properties " + key + " was not found.");
        }
        return value;
    }

    /**
     * Returns the value for the given key or the default if non is set.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public synchronized String get(ConfigurationKey key, String defaultValue) {
        return this.configuration.getProperty(key.getKey(), defaultValue);
    }

    public synchronized String get(String key, String defaultValue) {
        return this.configuration.getProperty(key, defaultValue);
    }

    public int getAsInt(ConfigurationKey key) {
        try {
            return Integer.valueOf(this.get(key));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Non int value in property " + key + ".");
        }
    }

    public int getAsInt(ConfigurationKey key, int defaultValue) {
        try {
            return Integer.valueOf(this.get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warning("Non int value in property " + key + " using default (" + defaultValue + ")");
            return defaultValue;
        }
    }

    public long getAsLong(ConfigurationKey key) {
        try {
            return Long.valueOf(this.get(key));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Non int value in property " + key + ".");
        }
    }

    public long getAsLong(ConfigurationKey key, long defaultValue) {
        try {
            return Long.valueOf(this.get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            logger.warning("Non int value in property " + key + " using default (" + defaultValue + ")");
            return defaultValue;
        }
    }

    public boolean getAsBoolean(ConfigurationKey key) {
        return Boolean.valueOf(this.get(key));
    }

    public boolean getAsBoolean(ConfigurationKey key, boolean defaultValue) {
        return Boolean.valueOf(this.get(key, Boolean.toString(defaultValue)));
    }

    public File getAsFile(ConfigurationKey key) {
        return new File(this.get(key));
    }

    public File getAsFile(ConfigurationKey key, File defaultValue) {
        return new File(this.get(key, defaultValue.getAbsolutePath()));
    }

    public List<String> getAsList(ConfigurationKey key, String separator) {
        return toList(this.get(key), separator);
    }

    public List<String> getAsList(ConfigurationKey key, String separator, List<String> defaults) {
        String raw = this.get(key, null);
        if (raw == null) {
            return defaults;
        } else {
            return toList(raw, separator);
        }
    }

    private List<String> toList(String string, String separator) {
        String[] items = string.replaceAll("[ ]*" + Pattern.quote(separator) + "[ ]*", separator).split(separator);
        return Arrays.asList(items);
    }

    public Map<Object, Object> asMap() {
        return this.configuration;
    }

    public boolean contains(ConfigurationKey key) {
        return this.configuration.containsKey(key.getKey());
    }

    public List<String> getSetKeysStartingWith(String prefix) {
        List<String> result = new ArrayList<String>();
        for (Object key : this.configuration.keySet()) {
            if (key.toString().startsWith(prefix)) {
                result.add(key.toString());
            }
        }

        return result;
    }

    public synchronized void save() throws IOException {
        Map<ConfigurationKey, String> values = new EnumMap<ConfigurationKey, String>(ConfigurationKey.class);
        for (ConfigurationKey key : ConfigurationKey.values()) {
            if (key.isVolatile() && this.contains(key)) {
                //volatile keys are only not saved if the defaults contains a different value
                //as the currently set value
                if (!this.defaults.containsKey(key.getKey()) 
                        || this.defaults.get(key.getKey()).equals(this.get(key))) {
                    values.put(key, this.get(key));
                    this.configuration.remove(key.getKey());
                }
            }
        }

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(this.file);
            this.configuration.store(fileWriter, "BongLoader config file");
        } finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }

        for (Entry<ConfigurationKey, String> entry : values.entrySet()) {
            this.configuration.setProperty(entry.getKey().getKey(), entry.getValue());
        }
    }

    public void addConfigurationUpdateListener(ConfigurationUpdateListener listener) {
        this.configurationUpdateListeners.add(listener);
    }

    public void removeConfigurationUpdateListener(ConfigurationUpdateListener listener) {
        this.configurationUpdateListeners.remove(listener);
    }

    private void notifyUpdate(final ConfigurationKey key) {
        for (final ConfigurationUpdateListener listener : configurationUpdateListeners) {
            new Thread() {

                public void run() {
                    listener.onUpdate(key);
                }
            }.start();
        }
    }
}
