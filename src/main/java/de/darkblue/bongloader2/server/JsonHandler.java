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
package de.darkblue.bongloader2.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * Handles JSON marshalling and unmarshalling
 * 
 * @author Florian Frankenberger
 */
class JsonHandler extends AbstractHandler {

    private static final Logger LOGGER = Logger.getLogger(JsonHandler.class.getCanonicalName());

    private final Map<String, PathInfo<?>> pathMapping = new HashMap<String, PathInfo<?>>();
    private final ObjectMapper mapper = new ObjectMapper();

    public static interface JsonRequestHandler<T> {

        Object call(T value);
    }

    private static class PathInfo<T> {

        final Class<T> requestClass;
        final JsonRequestHandler<T> callback;

        public PathInfo(Class<T> requestClass, JsonRequestHandler<T> callback) {
            this.requestClass = requestClass;
            this.callback = callback;
        }
    }

    public <T> void putMapping(String path, Class<T> requestClass, JsonRequestHandler<T> requestHandler) {
        this.pathMapping.put(path, new PathInfo<T>(requestClass, requestHandler));
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (pathMapping.containsKey(target) && baseRequest.getMethod().equalsIgnoreCase("POST")) {
            final PathInfo<Object> pathInfo = (PathInfo<Object>) pathMapping.get(target);

            Object value = mapper.readValue(request.getInputStream(), pathInfo.requestClass);
            Object result = null;
            JsonCommandResult<Object> commandResult = new JsonCommandResult<Object>();
            commandResult.ok = true;
            try {
                result = pathInfo.callback.call(value);
            } catch (Exception e) {
                commandResult.ok = false;
                commandResult.errorMsg = e.getMessage();
                LOGGER.log(Level.WARNING, "Request of " + target + " throw an exception", e);
            }

            commandResult.result = result;

            response.setContentType("application/javascript;charset=utf-8");
            mapper.writeValue(response.getOutputStream(), commandResult);

            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
        }
    }

    public static class JsonCommandResult<T> {
        public boolean ok;
        public String errorMsg;
        public T result = null;
    }


}
