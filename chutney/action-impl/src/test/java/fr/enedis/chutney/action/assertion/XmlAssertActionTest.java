/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.assertion;

import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Failure;
import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Success;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.spi.Action;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import fr.enedis.chutney.action.spi.injectable.Logger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class XmlAssertActionTest {

    @Test
    public void execute_2_successful_assertions_on_comparing_actual_result_to_expected() {
        // Given
        Map<String, Object> expected = Map.of(
            "/root/node1/leaf1", "val1",
            "//leaf2", 5,
            "//node1/leaf3", "val2",
            "//node1/@at1", "val3"
        );

        String fakeActualResult = "<root><node1 at1=\"val3\"><leaf1>val1</leaf1><leaf2>5</leaf2><leaf3><![CDATA[val2]]></leaf3></node1></root>";

        // When
        Action action = new XmlAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = action.execute();

        // Then
        assertThat(result.status).isEqualTo(Success);
    }

    @Test
    public void execute_a_failing_assertion_on_comparing_actual_result_to_expected() {
        // Given
        Map<String, Object> expected = Map.of("/root/node1/leaf1", "val1");
        String fakeActualResult = "<root><node1><leaf1>incorrrectValue</leaf1></node1></root>";

        // When
        Action action = new XmlAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = action.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
    }

    @Test
    public void execute_a_failing_assertion_on_invalid_XML_content_in_actual() {
        // Given
        Map<String, Object> expected = Map.of("/root/node1/leaf1", "val1");
        String fakeActualResult = "broken xml";

        // When
        Action action = new XmlAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = action.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
    }

    @Test
    public void execute_a_failing_assertion_on_wrong_XPath_value_in_expected() {
        // Given
        Map<String, Object> expected = Map.of("//missingnode", "val1");
        String fakeActualResult = "<root><node1><leaf1>val1</leaf1></node1></root>";

        // When
        Action action = new XmlAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = action.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
    }

    @Test
    public void xpath_accesses_value_whatever_the_namepace() {
        // Given
        Map<String, Object> expected = Map.of(
            "/descriptionComplete/test1/test2/number", "5072899",
            "/descriptionComplete/test1/test2/num", "5072899"
        );

        String fakeActualResult = loadFileFromClasspath("xml_samples/with_default_and_tag_namespaces.xml");

        // When
        Action action = new XmlAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = action.execute();

        // Then
        assertThat(result.status).isEqualTo(Success);
        //verify(stepContext, times(1)).success(eq("/descriptionComplete/test1/test2/number = 5072899"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/something/value $isNotNull <value>3</value>",
        "/something/notexist $isNull <a>b</a>",
        "/something/empty $isNull <empty></empty>",
        "/something/alphabet $contains:abcdefg <alphabet>abcdefg</alphabet>",
        "/something/matchregexp $matches:\\d{4}-\\d{2}-\\d{2} <matchregexp>1983-10-26</matchregexp>",
        "/something/onedate $isBeforeDate:2010-01-01T11:12:13.1230Z <onedate>2000-01-01T10:11:12.123Z</onedate>",
        "/something/seconddate $isAfterDate:1998-07-14T02:03:04.456Z <seconddate>2000-01-01T10:11:12.123Z</seconddate>",
        "/something/thirddate $isEqualDate:2000-01-01T10:11:12.123Z <thirddate>2000-01-01T10:11:12.123Z</thirddate>",
        "/something/anumber $isLessThan:42000 <anumber>4100</anumber>",
        "/something/thenumber $isGreaterThan:45 <thenumber>46</thenumber>"
    })
    public void execute_successful_assertions_with_placeholder(String expectation) {
        // Given
        String[] expect = expectation.split(" ");
        Map<String, Object> expected = Map.of(expect[0], expect[1]);
        String fakeActualResult = "<something>" + expect[2] + "</something>";

        // When
        Action xmlAssertAction = new XmlAssertAction(new TestLogger(), fakeActualResult, expected);
        ActionExecutionResult result = xmlAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Success);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/something/notpresent $isNotNull",
            "/something/notpresent $contains:abcdefg",
            "/something/notpresent $matches:\\d{4}-\\d{2}-\\d{2}",
            "/something/notpresent $isBeforeDate:2010-01-01T11:12:13.1230Z",
            "/something/notpresent $isAfterDate:1998-07-14T02:03:04.456Z",
            "/something/notpresent $isEqualDate:2000-01-01T10:11:12.123Z",
            "/something/notpresent $isLessThan:42000",
            "/something/notpresent $isGreaterThan:45"
        }
    )
    public void handle_null_actual_with_placeholder(String expectation) {
        // Given
        String[] expect = expectation.split(" ");
        Map<String, Object> expected = Map.of(expect[0], expect[1]);
        String fakeActualResult = "<something></something>";

        // When
        Action xmlAssertAction = new XmlAssertAction(new TestLogger(), fakeActualResult, expected);
        ActionExecutionResult result = xmlAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
    }

    @Test
    public void fails_when_xml_contains_doctype_declaration() {
        // Given
        String xml = loadFileFromClasspath("xml_samples/with_dtd.xml");

        // When
        Action task = new XmlAssertAction(mock(Logger.class), xml, new LinkedHashMap<>());
        ActionExecutionResult result = task.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
    }

    @Test
    public void execute_all_assertions_not_failing_at_the_first_one() {
        // Given
        Map<String, Object> expected = new LinkedHashMap<>(Map.of(
            "/something", "$isNotNull", // ok
            "/something/fail", "aa", // ko
            "/something/value", "3" // ok
        ));
        String fakeActualResult = "<something><value>3</value></something>";

        // When
        TestLogger testLogger = new TestLogger();
        Action xmlAssertAction = new XmlAssertAction(testLogger, fakeActualResult, expected);
        ActionExecutionResult result = xmlAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
        assertThat(testLogger.info).hasSize(2);
        assertThat(testLogger.errors).hasSize(1);
    }

    private String loadFileFromClasspath(String filePath) {
        return new Scanner(Objects.requireNonNull(XmlAssertAction.class.getClassLoader().getResourceAsStream(filePath)), StandardCharsets.UTF_8).useDelimiter("\\A").next();
    }
}
