/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion;

import static fr.enedis.chutney.action.spi.validation.ActionValidatorsUtils.notBlankStringValidation;
import static fr.enedis.chutney.action.spi.validation.Validator.getErrorsFrom;
import static fr.enedis.chutney.action.spi.validation.Validator.of;

import fr.enedis.chutney.action.common.ResourceResolver;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Input;
import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.xml.sax.SAXException;

public class XsdValidationAction implements Action {

    private String xml;
    private String xsdPath;
    private Logger logger;
    private ResourceLoader resourceLoader = new DefaultResourceLoader(XsdValidationAction.class.getClassLoader());

    public XsdValidationAction(Logger logger, @Input("xml") String xml, @Input("xsd") String xsdPath) {
        this.logger = logger;
        this.xml = xml;
        this.xsdPath = xsdPath;
    }

    @Override
    public List<String> validateInputs() {
        Validator<String> xmlValidation = of(xsdPath)
            .validate(Objects::nonNull, "No xsd provided")
            .validate(x -> resourceLoader.getResource(x), resource -> resource.exists(), "Cannot find xsd");
        return getErrorsFrom(xmlValidation, notBlankStringValidation(xml, "xml"));
    }

    @Override
    public ActionExecutionResult execute() {
        try {

            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(new ResourceResolver(xsdPath));

            Resource resource = resourceLoader.getResource(xsdPath);
            Source schemaSource = new StreamSource(resource.getInputStream());
            Schema schema = factory.newSchema(schemaSource);
            javax.xml.validation.Validator validator = schema.newValidator();
            try (StringReader sr = new StringReader(xml)) {
                StreamSource ss = new StreamSource(sr);
                validator.validate(ss);
            }
        } catch (SAXException | IOException | UncheckedIOException e ) {
            logger.error("Exception: " + e.getMessage());
            return ActionExecutionResult.ko();
        }
        return ActionExecutionResult.ok();
    }

}
