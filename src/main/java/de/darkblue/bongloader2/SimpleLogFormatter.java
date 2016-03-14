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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A simple log formatter
 *
 * @author Florian Frankenberger
 */
public class SimpleLogFormatter extends Formatter {

    /* (non-Javadoc)
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        String message = (record.getParameters() != null ? MessageFormat.format(record.getMessage(), record.getParameters()) : record.getMessage());

        sb.append(
                String.format(
                "%25s %2$7s %3$50s  %4$s",
                new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z").format(new Date(record.getMillis())),
                record.getLevel(),
                getSimpleName(record.getSourceClassName()) + "." + record.getSourceMethodName() + "()",
                message));

        if (record.getThrown() != null) {
            sb.append("\n");
            sb.append("\t");
            sb.append(record.getThrown().getMessage());
            sb.append("\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            pw.flush();

            for (String line : sw.getBuffer().toString().split("\n")) {
                sb.append("\t\t" + line + "\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String getSimpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }
}
