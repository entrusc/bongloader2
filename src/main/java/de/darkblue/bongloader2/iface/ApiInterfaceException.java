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

/**
 *
 * @author Florian Frankenberger
 */
public class ApiInterfaceException extends Exception {

    public ApiInterfaceException() {
    }

    public ApiInterfaceException(String message) {
        super(message);
    }

    public ApiInterfaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiInterfaceException(Throwable cause) {
        super(cause);
    }

}
