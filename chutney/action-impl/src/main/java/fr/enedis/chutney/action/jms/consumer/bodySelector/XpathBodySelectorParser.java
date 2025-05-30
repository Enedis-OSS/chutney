/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jms.consumer.bodySelector;

import fr.enedis.chutney.action.common.XmlUtils;
import fr.enedis.chutney.action.jms.domain.XmlContent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

class XpathBodySelectorParser implements BodySelectorParser {

    private static final String BODY_SELECTOR_REGEX = "^XPATH '(?<xpath>.+)'$";
    private static final Pattern BODY_SELECTOR_PATTERN = Pattern.compile(BODY_SELECTOR_REGEX);

    @Override
    public String description() {
        return "XPath selector: " + BODY_SELECTOR_REGEX;
    }

    /**
     * @throws IllegalArgumentException when given XPATH does not compile
     */
    @Override
    public Optional<BodySelector> tryParse(String selector) throws IllegalArgumentException {
        Matcher matcher = BODY_SELECTOR_PATTERN.matcher(selector);
        final Optional<BodySelector> optionalBodySelector;
        if (matcher.matches()) {
            String xpath = matcher.group("xpath");
            XPathExpression<Boolean> xPathExpression = XPathFactory.instance().compile(xpath, Filters.fboolean());
            optionalBodySelector = Optional.of(new XpathBodySelector(xPathExpression));
        } else {
            optionalBodySelector = Optional.empty();
        }
        return optionalBodySelector;
    }

    private static class XpathBodySelector extends TextMessageBodySelector {
        private final XPathExpression<Boolean> xPathExpression;
        private final SAXBuilder saxBuilder = XmlUtils.saxBuilder();

        XpathBodySelector(XPathExpression<Boolean> xPathExpression) {
            this.xPathExpression = xPathExpression;
        }

        @Override
        public boolean match(String messageBody) {
            XmlContent xmlContent = new XmlContent(saxBuilder, messageBody);
            return xmlContent
                .tryBuildDocumentWithoutNamespaces()
                .map(xPathExpression::evaluateFirst)
                .orElse(Boolean.FALSE);
        }
    }
}
