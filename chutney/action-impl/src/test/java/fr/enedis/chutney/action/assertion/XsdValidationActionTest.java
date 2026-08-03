/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.ActionExecutionResult.Status;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class XsdValidationActionTest {

    XsdValidationAction action;

    @Test
    public void should_validate_simple_xsd() {
        Logger logger = new TestLogger();
        String xml = "<?xml version=\"1.0\"?>\r\n" +
            "<Employee xmlns=\"https://www.chutneytesting.com/Employee\">\r\n" +
            "    <name>my Name</name>\r\n" +
            "    <age>29</age>\r\n" +
            "    <role>Java Developer</role>\r\n" +
            "    <gender>Male</gender>\r\n" +
            "</Employee>";

        String xsd = "/xsd_samples/employee.xsd";

        action = new XsdValidationAction(logger, xml, xsd);

        //When
        ActionExecutionResult result = action.execute();

        //Then
        assertThat(result.status).isEqualTo(Status.Success);
    }

    @Test
    public void should_not_validate_simple_xsd() {
        Logger logger = new TestLogger();
        String xml = "<?xml version=\"1.0\"?>\r\n" +
            "<Employee xmlns=\"https://www.chutneytesting.com/Employee\">\r\n" +
            "    <name>my name</name>\r\n" +
            "    <age>29</age>\r\n" +
            "    <role>Java Developer</role>\r\n" +
            "    <gender>ERROR</gender>\r\n" +
            "</Employee>";

        String xsd = "/xsd_samples/employee.xsd";

        action = new XsdValidationAction(logger, xml, xsd);

        //When
        ActionExecutionResult result = action.execute();

        //Then
        assertThat(result.status).isEqualTo(Status.Failure);
    }

    @Test
    public void should_validate_xsd_with_multiple_import() {
        Logger logger = new TestLogger();
        String xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <shipTo xmlns="http://chutney/test/ship"
                    xmlns:addr="http://chutney/test/address"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://chutney/test/ship shipTo.xsd">
                <name>string</name>
                <address>
                    <addr:street>voltaire</addr:street>
                    <addr:type>Rue</addr:type>
                    <addr:city>Paris</addr:city>
                    <addr:country>France</addr:country>
                </address>
            </shipTo>
            """;

        String xsd = "/xsd_samples/shipTo.xsd";

        action = new XsdValidationAction(logger, xml, xsd);

        //When
        ActionExecutionResult result = action.execute();

        //Then
        assertThat(result.status).isEqualTo(Status.Success);


    }

    @Test
    public void should_validate_xsd_from_classpath() {
        Logger logger = new TestLogger();
        String xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <shipTo xmlns="http://chutney/test/ship"
                    xmlns:addr="http://chutney/test/address"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://chutney/test/ship shipTo.xsd">
                <name>string</name>
                <address>
                    <addr:street>voltaire</addr:street>
                    <addr:type>Rue</addr:type>
                    <addr:city>Paris</addr:city>
                    <addr:country>France</addr:country>
                </address>
            </shipTo>""";

        String xsd = "classpath:/xsd_samples/shipTo.xsd";

        action = new XsdValidationAction(logger, xml, xsd);

        //When
        ActionExecutionResult result = action.execute();

        //Then
        assertThat(result.status).isEqualTo(Status.Success);


    }

    @Test
    public void should_validate_xsd_from_file_system() {
        Logger logger = new TestLogger();
        String xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <shipTo xmlns="http://chutney/test/ship"
                    xmlns:addr="http://chutney/test/address"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://chutney/test/ship shipTo.xsd">
                <name>string</name>
                <address>
                    <addr:street>voltaire</addr:street>
                    <addr:type>Rue</addr:type>
                    <addr:city>Paris</addr:city>
                    <addr:country>France</addr:country>
                </address>
            </shipTo>""";

        Path executionPath = Paths.get("").toAbsolutePath();
        String xsd = "file:" + executionPath.resolve("src/test/resources/xsd_samples/shipTo.xsd");

        action = new XsdValidationAction(logger, xml, xsd);

        //When
        ActionExecutionResult result = action.execute();

        //Then
        assertThat(result.status).isEqualTo(Status.Success);


    }

    @Test
    public void should_validate_xsd_from_file_system_with_root_file_in_sub_dir() {
        Logger logger = new TestLogger();
        String xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <shipTo xmlns="http://chutney/test/ship"
                    xmlns:addr="http://chutney/test/address"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://chutney/test/ship shipTo.xsd">
                <name>string</name>
                <address>
                    <addr:street>voltaire</addr:street>
                    <addr:type>Rue</addr:type>
                    <addr:city>Paris</addr:city>
                    <addr:country>France</addr:country>
                </address>
            </shipTo>""";

        Path executionPath = Paths.get("").toAbsolutePath();
        String xsd = "file:" + executionPath.resolve("src/test/resources/xsd_samples/ship/subShip/shipTo.xsd");

        action = new XsdValidationAction(logger, xml, xsd);

        //When
        ActionExecutionResult result = action.execute();

        //Then
        assertThat(result.status).isEqualTo(Status.Success);

    }

    @Test
    public void should_validate_xsd_from_jar_in_classpath() {
        Logger logger = new TestLogger();
        String xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <shipTo xmlns="http://chutney/test/ship"
                    xmlns:addr="http://chutney/test/address"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://chutney/test/ship shipTo.xsd">
                <name>string</name>
                <address>
                    <addr:street>voltaire</addr:street>
                    <addr:type>Rue</addr:type>
                    <addr:city>Paris</addr:city>
                    <addr:country>France</addr:country>
                </address>
            </shipTo>""";

        String xsd = "/xsd/shipTo.xsd";

        action = new XsdValidationAction(logger, xml, xsd);

        //When
        ActionExecutionResult result = action.execute();

        //Then
        assertThat(result.status).isEqualTo(Status.Success);

    }
}
