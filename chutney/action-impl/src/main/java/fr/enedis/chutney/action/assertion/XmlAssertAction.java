/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notBlankStringValidation;
import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notEmptyMapValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;

import fr.enedis.chutney.action.assertion.placeholder.PlaceholderAsserter;
import fr.enedis.chutney.action.assertion.placeholder.PlaceholderAsserterUtils;
import fr.enedis.chutney.action.common.XmlUtils;
import fr.enedis.chutney.action.jakarta.domain.XmlContent;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jdom2.Attribute;
import org.jdom2.CDATA;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Text;
import org.jdom2.filter.ContentFilter;
import org.jdom2.filter.Filter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;

public class XmlAssertAction implements Action {

    private final Logger logger;
    private final String documentAsString;
    private final Map<String, Object> xpathsAndExpectedResults;

    public XmlAssertAction(Logger logger, @Input("document") String documentAsString, @Input("expected") Map<String, Object> xpathsAndExpectedResults) {
        this.logger = logger;
        this.documentAsString = documentAsString;
        this.xpathsAndExpectedResults = xpathsAndExpectedResults;
    }

    @Override
    public List<String> validateInputs() {
        return getErrorsFrom(
            notBlankStringValidation(documentAsString, "document"),
            notEmptyMapValidation(xpathsAndExpectedResults, "expected")
        );
    }

    @Override
    public ActionExecutionResult execute() {
        try {
            SAXBuilder saxBuilder = XmlUtils.saxBuilder();
            Document document = new XmlContent(saxBuilder, documentAsString).buildDocumentWithoutNamespaces();
            boolean assertTrue = xpathsAndExpectedResults.entrySet().stream().map(xpathAndExpected -> {
                String xpath = xpathAndExpected.getKey();
                Object expected = xpathAndExpected.getValue();
                try {
                    return assertXpathMatchExpectation(document, xpath, expected);
                } catch (XmlUtils.InvalidXPathException e) {
                    logger.error(e.getMessage());
                    return false;
                }
            }).reduce(true, Boolean::logicalAnd);
            return assertTrue ? ActionExecutionResult.ok() : ActionExecutionResult.ko();
        } catch (XmlContent.InvalidXmlDocumentException e) {
            logger.error(e.getMessage());
            return ActionExecutionResult.ko();
        }
    }

    private boolean assertXpathMatchExpectation(Document document, String xpath, Object expectedResult) throws XmlUtils.InvalidXPathException {
        XPathExpression<Object> xpathExpression = XmlUtils.compileXPath(xpath);
        String actualResult = convertEvaluationResultToString(xpathExpression.evaluateFirst(document));

        Optional<PlaceholderAsserter> asserts = PlaceholderAsserterUtils.getAsserterMatching(expectedResult);
        if (asserts.isPresent()) {
            return asserts.get().assertValue(logger, actualResult, expectedResult);
        } else if (String.valueOf(expectedResult).equals(actualResult)) {
            logger.info(xpath + " = " + actualResult);
            return true;
        } else {
            logger.error(xpath + " != " + expectedResult + " (found " + actualResult + ")");
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String convertEvaluationResultToString(Object evaluationResult) {
        if (evaluationResult == null) {
            return null;
        }
        return switch (evaluationResult) {
            case Text text -> text.getText();
            case Element element -> {
                List<Content> contents = element.getContent((Filter<Content>) new ContentFilter(ContentFilter.COMMENT).negate());
                List<Content> cdata = element.getContent(new ContentFilter(ContentFilter.CDATA));

                if (contents.size() == 1) {
                    yield convertEvaluationResultToString(contents.getFirst());
                } else if (cdata.size() == 1) {
                    yield ((CDATA) cdata.getFirst()).getText();
                } else if (contents.isEmpty()) {
                    yield null;
                } else {
                    yield "!!!MULTIPLE!";
                }
            }
            case Attribute attribute -> attribute.getValue();
            default -> String.valueOf(evaluationResult);
        };
    }
}
