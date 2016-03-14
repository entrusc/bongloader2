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

import de.darkblue.bongloader2.exception.ReportableException;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 *
 * @author Florian Frankenberger
 */
public class ErrorLogHandler extends Handler {

    private volatile Throwable lastError = null;
    public Set<ErrorHandler> errorHandler = new HashSet<ErrorHandler>();

    public void addErrorHandler(ErrorHandler handler) {
        this.errorHandler.add(handler);
    }

    public void removeErrorHandler(ErrorHandler handler) {
        this.errorHandler.remove(handler);
    }

    @Override
    public synchronized void publish(final LogRecord record) {
        if (record.getThrown() != null && !equals(record.getThrown(), lastError)) {
            //only severe problems are blocking to give the user a chace to read
            //the message before the application is shut down
            lastError = record.getThrown();
            if (record.getLevel().equals(Level.SEVERE)) {

                for (ErrorHandler handler : errorHandler) {
                    handler.handleSevereError(record.getThrown());
                }

            }
            
            new Thread() {

                @Override
                public void run() {
                    if (record.getThrown() != null && record.getThrown() instanceof ReportableException) {
                        for (ErrorHandler handler : errorHandler) {
                            handler.handleError(record.getLevel(), (ReportableException) record.getThrown());
                        }
                    }
                }
            }.start();
        }
    }
    
    private static boolean equals(Throwable t1, Throwable t2) {
        try {
            final ByteArrayOutputStream bs1 = new ByteArrayOutputStream();
            final ByteArrayOutputStream bs2 = new ByteArrayOutputStream();
            final PrintWriter pw1 = new PrintWriter(bs1);
            final PrintWriter pw2 = new PrintWriter(bs2);
            
            if (t1 != null) {
                t1.printStackTrace(pw1);
                pw1.flush();
            }
            
            if (t2 != null) {
                t2.printStackTrace(pw2);
                pw2.flush();
            }
            
            return Arrays.equals(bs1.toByteArray(), bs2.toByteArray());
            
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
