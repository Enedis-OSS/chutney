/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import fr.enedis.chutney.action.common.XmlUtils;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class XPathFunctionTest {

    private static final String STANDARD_XML =
        """
        <node1>
            <node2 attr1="val4"/>
            <node3 attr1="val7">text12</node3>
            <node4/>
            <node5>
                <![CDATA[some stuff]]>
            </node5>
        </node1>""";

    private static final String XMl_WITH_NS =
        """
          <soap12:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                           xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
             <soap12:Body>
               <tns:Demande xmlns:tns="http://demande.org"\
                          xmlns:tns1="http://dico.org"\
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                   <tns:Entete xsi:type="tns1:EnteteExtType">
                       <tns1:IdExterne>CHUTNEY</tns1:IdExterne>
                   </tns:Entete>
                   <tns:Id>265426252465</tns:Id>
                   <tns:Options>
                       <tns:status>true</tns:status>
                   </tns:Options>
               </tns:Demande>
             </soap12:Body>
         </soap12:Envelope>""";

    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForXpath_matching_returns_value")
    public void xpath_matching_returns_value(String title, String xpath, Object expectedValue) throws XmlUtils.InvalidXPathException, XmlUtils.InvalidXmlDocumentException {
        Object result = XPathFunction.xpath(STANDARD_XML, xpath);
        assertThat(result).as("Result of XPath[" + xpath + "] evaluation").isEqualTo(expectedValue);
    }

    @Test
    public void xpath_matching_returns_value() throws XmlUtils.InvalidXPathException, XmlUtils.InvalidXmlDocumentException {
        Map<String, String> nsMap = new HashMap<>();
        nsMap.put("soap", "http://www.w3.org/2003/05/soap-envelope");
        nsMap.put("rep", "http://demande.org");
        Object result = XPathFunction.xpathNs(XMl_WITH_NS, "//soap:Body/rep:Demande/rep:Id", nsMap);
        Object resultB = XPathFunction.xpathNs(XMl_WITH_NS, "boolean(//soap:Body/rep:Demande/rep:toto)", nsMap);
        assertThat(result).isEqualTo("265426252465");
        assertThat(resultB).isEqualTo(false);
    }

    public static Object[] parametersForXpath_matching_returns_value() {
        return new Object[] {
            new Object[] {"Attribute value", "/node1/node2/@attr1", "val4"},
            new Object[] {"Text value", "/node1/node3/text()", "text12"},
            new Object[] {"Single node containing text", "/node1/node3", "text12"},
            new Object[] {"Single node containing CDATA", "/node1/node5", "some stuff"}
        };
    }

    @Test
    public void invalid_document_throws() {
        assertThatExceptionOfType(XmlUtils.InvalidXmlDocumentException.class).isThrownBy(
            () -> XPathFunction.xpath("broken xml", "/node1/node2/@attr1")
        ).withMessage("Unable to parse XML document: broken xml");
    }

    @Test
    public void invalid_xpath_throws() {
        assertThatExceptionOfType(XmlUtils.InvalidXPathException.class).isThrownBy(
            () -> XPathFunction.xpath(STANDARD_XML, "?@hjy1")
        ).withMessage("Unable to compile XPath: ?@hjy1");
    }
}
