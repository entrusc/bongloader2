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
package de.darkblue.bongloader2.controller;

import de.darkblue.bongloader2.Application;
import de.darkblue.bongloader2.Configuration;
import de.darkblue.bongloader2.ConfigurationKey;
import de.darkblue.bongloader2.model.Download;
import de.darkblue.bongloader2.model.data.AbstractUpdateable;
import java.util.List;

/**
 *
 * @author Florian Frankenberger
 */
public class CruiseController extends AbstractUpdateable<CruiseController> {

    private static final int MEASUREMENT_TIME_FRAME = 200;
    
    private int runningDownloads = 0;
    
    private long speedLimit = 0;
    private boolean hasSpeedLimit = false;
    
    private float meanSpeedLimit = 0;
    
    private float meanSpeed = 0;
    private int meanSpeedCounter = 0;
    
    private volatile boolean paused = false;
    private final Application application;

    public CruiseController(Application application) {
        this.application = application;
        
        final Configuration config = application.getConfig();
        this.setSpeedLimit(config.getAsLong(ConfigurationKey.MAX_DOWNLOAD_SPEED, 0L));
        this.setPaused(config.getAsBoolean(ConfigurationKey.DOWNLOAD_PAUSED, false));
        notifyOnUpdate();
    }
    
    public synchronized void addDownload() {
        this.runningDownloads++;
        recalculateSpeedLimit();
    }

    public synchronized void removeDownload() {
        if (this.runningDownloads > 0) {
            this.runningDownloads--;
            if (this.runningDownloads == 0) {
                this.meanSpeedCounter = 0;
                this.meanSpeed = 0;
            }
            recalculateETAs();
            recalculateSpeedLimit();
            this.notifyOnUpdate();
        }
    }
    
    public boolean areDownloadsRunning() {
        return this.runningDownloads > 0;
    }
    
    public void shutdown() {
        this.paused = true;
        notifyOnUpdate();
        while (areDownloadsRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        final Configuration config = application.getConfig();
        config.setBoolean(ConfigurationKey.DOWNLOAD_PAUSED, paused);
        notifyOnUpdate();
    }

    public boolean isPaused() {
        return paused;
    }
    
    private synchronized void recalculateETAs() {
        final DownloadController downloadController = application.getDownloadController();
        final Configuration config = application.getConfig();
        final List<Download> downloads = downloadController.getPriorizisedDownloadList();
        
        final int maxSimultaneousDownloads = config.getAsInt(ConfigurationKey.MAX_SIMULTANEOUS_DOWNLOADS);
        final float slotSpeed = this.getMeanSpeed() / maxSimultaneousDownloads;
        int downloadSlotInList = 0;
        final long[] slotLengths = new long[maxSimultaneousDownloads];
        
        for (Download download : downloads) {
            
            if (download.isDownloading()) {
                if (downloadSlotInList < slotLengths.length) { //might happen when speed is changed concurrently!
                    final long bytesToGo = download.getTotalBytes() - download.getDownloadedBytes();
                    slotLengths[downloadSlotInList++] = bytesToGo;
                    long eta = (long) Math.ceil(bytesToGo / slotSpeed);
                    download.setEta(eta);
                }
            } else 
                if (!download.isDownloaded()) {
                    int minSlot = 0;
                    long minSlotLength = Long.MAX_VALUE;
                    for (int i = 0; i < slotLengths.length; ++i) {
                        final long slotLength = slotLengths[i];
                        final long currentPossibilityLength = slotLength + download.getTotalBytes();
                        if (currentPossibilityLength < minSlotLength) {
                            minSlotLength = currentPossibilityLength;
                            minSlot = i;
                        }
                    }

                    slotLengths[minSlot] = minSlotLength;
                    long eta = (long) Math.ceil(minSlotLength / slotSpeed);
                    
                    download.setEta(eta);
                } else {
                    download.setEta(null);
                }
        }
    }
    
    public void setSpeedLimit(long speed) {
        synchronized (this) {
            if (speed > 0) {
                this.speedLimit = speed;
                this.hasSpeedLimit = true;
            } else {
                this.speedLimit = 0;
                this.hasSpeedLimit = false;
            }
        }
        
        final Configuration config = application.getConfig();
        config.setLong(ConfigurationKey.MAX_DOWNLOAD_SPEED, this.speedLimit);
        
        this.meanSpeed = speedLimit;
        this.meanSpeedCounter = 0;
        recalculateSpeedLimit();
        notifyOnUpdate();
    }
    
    public synchronized float getSpeedLimit() {
        return this.speedLimit;
    }

    public boolean hasSpeedLimit() {
        return hasSpeedLimit;
    }

    public float getMeanSpeedLimit() {
        return this.meanSpeedLimit;
    }
    
    private void recalculateSpeedLimit() {
        if (this.hasSpeedLimit() && this.runningDownloads > 0) {
            this.meanSpeedLimit = this.speedLimit / (float) this.runningDownloads;
        } else {
            this.meanSpeedLimit = 0;
        }
    }
    
    public void addSpeed(float speed) {
        synchronized (this) {
            this.meanSpeed = (this.meanSpeed * this.meanSpeedCounter + speed) / (float) (this.meanSpeedCounter + 1);
            if (this.meanSpeedCounter < this.runningDownloads * MEASUREMENT_TIME_FRAME) {
                this.meanSpeedCounter++;
            } else {
                this.meanSpeedCounter = this.runningDownloads * MEASUREMENT_TIME_FRAME;
            }
        }
        
        recalculateETAs();
        this.notifyOnUpdate();
    }
    
    public synchronized float getMeanSpeed() {
        return this.meanSpeed * this.runningDownloads;
    }

    
}
