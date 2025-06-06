/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.function;

import fr.enedis.chutney.action.spi.SpelFunction;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecUsernameToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoapFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoapFunction.class);

    @Deprecated
    @SpelFunction
    public static String getSoapBody(final String login, final String password, final String body) {
        return soapInsertWSUsernameToken(login, password, body);
    }

    @SpelFunction
    public static String soapInsertWSUsernameToken(final String user, final String password, final String envelope) {
        if (!user.isEmpty()) {
            try {
                WSSecUsernameToken builder = new WSSecUsernameToken();
                builder.setUserInfo(user, password);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builderDoc = factory.newDocumentBuilder();
                org.w3c.dom.Document doc = builderDoc.parse(new ByteArrayInputStream(envelope.getBytes()));
                WSSecHeader secHeader = new WSSecHeader();
                secHeader.insertSecurityHeader(doc);
                org.w3c.dom.Document signedDoc = builder.build(doc, secHeader);
                return fromDocumentToString(signedDoc);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(),e);
            }
        }
        return envelope;
    }

    private static String fromDocumentToString(final org.w3c.dom.Document doc) {
        StringWriter buffer = new StringWriter();
        if (doc != null) {
            try {
                TransformerFactory transFactory = TransformerFactory.newInstance();
                Transformer transformer = transFactory.newTransformer();
                transformer.transform(new DOMSource(doc), new StreamResult(buffer));
            } catch (TransformerException e) {
                LOGGER.error(e.getMessage(),e);
            }
        }
        return buffer.toString();
    }

}
