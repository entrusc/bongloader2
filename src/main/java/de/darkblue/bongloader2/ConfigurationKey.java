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

import de.darkblue.bongloader2.utils.Utils;

/**
 * No description given.
 *
 * @author Florian Frankenberger
 */
public enum ConfigurationKey {

        DELETE_AFTER_DOWNLOAD,
        DIRECTORY_TEMPLATES(true),
        VERSION(true),
	CURRENT_DIR(true),
        RECORDLIST_UPDATE_TIME(true),
	MAX_SIMULTANEOUS_DOWNLOADS,
        DOWNLOAD_PARTS(true),
	QUALITY_LEVELS_TO_DOWNLOAD(true),
        AUTODOWNLOAD_LEVEL,
        MAX_DOWNLOAD_SPEED,
        DOWNLOAD_PAUSED,
	FILE_NAME_PATTERN,
        WINDOW_WIDTH,
        WINDOW_HEIGHT,
        WINDOW_X,
        WINDOW_Y,
        WINDOW_MODE,
        WINDOW_HIDDEN,
        DISPLAY_TRAY_HINT,
        ALLOW_AUTO_UPDATE,
	USERNAME,
	PASSWORD,
        INVALID_CHAR_REPLACEMENT(true),
        DELETE_AFTER_DOWNLOAD_GRACE_PERIOD(true);

	private boolean isVolatile;

	private ConfigurationKey() {
		this(false);
	}

	private ConfigurationKey(boolean isVolatile) {
		this.isVolatile = isVolatile;
	}

	public String getKey() {
		return Utils.toCamelCase(this.name());
	}

	public boolean isVolatile() {
		return this.isVolatile;
	}

}
