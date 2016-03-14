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

import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * No description given.
 *
 * @author Florian Frankenberger
 */
public final class XmlUtils {

    private XmlUtils() {
    }

    public static Node getRequiredTag(Node parentNode, String tagName) {
        return getTag(parentNode, tagName, true);
    }

    public static Node getTag(Node parentNode, String tagName) {
        return getTag(parentNode, tagName, false);
    }

    public static String nodeToString(Node node) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(node);
            transformer.transform(source, result);

            return result.getWriter().toString();
        } catch (TransformerConfigurationException ex) {
            return "";
        } catch (TransformerException ex) {
            return "";
        }
    }

    private static Node getTag(Node parentNode, String tagName, boolean required) throws IllegalArgumentException {
        NodeList nodeList = parentNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node itemNode = nodeList.item(i);
            if (itemNode.getNodeName().equals(tagName)) {
                return itemNode;
            }
            Node targetNode = getTag(itemNode, tagName, false);
            if (targetNode != null) {
                return targetNode;
            }
        }

        if (!required) {
            return null;
        } else {
            throw new IllegalArgumentException("Required tag \"" + tagName + "\" not found in xml");
        }
    }

}
