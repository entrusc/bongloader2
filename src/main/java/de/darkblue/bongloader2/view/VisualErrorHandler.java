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
package de.darkblue.bongloader2.view;

import de.darkblue.bongloader2.ErrorHandler;
import de.darkblue.bongloader2.exception.ReportableException;
import java.awt.Frame;
import java.util.logging.Level;

/**
 *
 * @author Florian Frankenberger
 */
public class VisualErrorHandler implements ErrorHandler {
 
    private Frame parent = null;

    public void setParent(Frame parent) {
        this.parent = parent;
    }
    
    @Override
    public void handleSevereError(Throwable t) {
        final String msg = t.getLocalizedMessage() + "\n\n" + "Fehlertyp: " + t.getClass().getSimpleName() + "\n\nBongloader muss beendet werden.\nWeiter informationen finden Sie im Log-File.";
        new InfoDialog(this.parent, "Ein schwerwiegender Fehler ist aufgetreten", msg, InfoDialog.InfoType.ERROR).setVisible(true);
    }

    @Override
    public void handleError(Level level, ReportableException e) {
        new InfoDialog(this.parent, e.getTitle(), e.getMessage(), InfoDialog.InfoType.WARNING).setVisible(true);
    }
    
}
