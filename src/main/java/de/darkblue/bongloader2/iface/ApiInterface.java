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
package de.darkblue.bongloader2.iface;

import de.darkblue.bongloader2.exception.ReportableException;
import de.darkblue.bongloader2.model.Recording;
import java.util.List;

/**
 * A general interface with the Bong.tv Service
 *
 * @author Florian Frankenberger
 */
public interface ApiInterface {

    /**
     * returns a list of recordings
     *
     * @param username
     * @param password
     * @return
     * @throws de.darkblue.bongloader2.iface.ApiInterfaceException
     * @throws de.darkblue.bongloader2.exception.ReportableException
     */
    List<Recording> downloadRecordingsList(String username, String password) throws ApiInterfaceException, ReportableException;

    /**
     * permanently deletes a recording from the servers of bong.tv
     *
     * @param username
     * @param password
     * @param recording
     * @throws de.darkblue.bongloader2.iface.ApiInterfaceException
     * @throws de.darkblue.bongloader2.exception.ReportableException
     */
    void deleteRecording(String username, String password, Recording recording) throws ApiInterfaceException, ReportableException;

    /**
     * returns the version of this api interface as a string
     * 
     * @return
     */
    String getVersion();

}
