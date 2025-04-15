/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.assertion;

import static com.chutneytesting.action.spi.ActionExecutionResult.Status.Failure;
import static com.chutneytesting.action.spi.ActionExecutionResult.Status.Success;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.chutneytesting.action.TestLogger;
import com.chutneytesting.action.assertion.JsonCompareAction.COMPARE_MODE;
import com.chutneytesting.action.spi.ActionExecutionResult;
import com.chutneytesting.action.spi.injectable.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class JsonAssertActionTest {

    @Test
    void take_zoned_date_when_asserting_dates() {
        // Given
        Map<String, Object> expected = Map.of(
            "$.something.onedate", "$isBeforeDate:2020-08-14T15:07:46.621Z",
            "$.something.seconddate", "$isAfterDate:2020-08-14T16:56:56+02:00",
            "$.something.thirddate", "$isEqualDate:2020-08-14T17:07:46.621+02:00"
        );

        String fakeActualResult = """
            {
                "something": {
                    "onedate": "2020-08-14T16:56:56+02:00",
                    "seconddate": "2020-08-14T15:07:46.621Z",
                    "thirddate": "2020-08-14T15:07:46.621Z"
                }
            }""".stripIndent();

        // When
        JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
        ActionExecutionResult result = jsonAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Success);
    }

    @Test
    void execute_4_successful_assertions_on_comparing_actual_result_to_expected() {
        // Given
        Map<String, Object> expected = new HashMap<>();
        expected.put("$.something.value", 3);
        expected.put("$.something_else.value", 5);
        expected.put("$.a_thing.type", "my_type");
        expected.put("$.a_thing.not.existing", null);

        String fakeActualResult = """
            {
                "something": { "value": 3 },
                "something_else": { "value": 5 },
                "a_thing": {
                    "type": "my_type"
                 }
             }""".stripIndent();

        // When
        JsonAssertAction jsonAssertAction = new JsonAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = jsonAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Success);
    }

    @Test
    void execute_a_failing_assertion_on_comparing_actual_result_to_expected() {
        // Given
        Map<String, Object> expected = Map.of("$.something.value", 42);
        String fakeActualResult = """
            { "something": { "value": 3 } }
            """.stripIndent();

        // When
        JsonAssertAction jsonAssertAction = new JsonAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = jsonAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
    }

    @Test
    void execute_a_failing_assertion_on_invalid_JSON_content_in_actual() {
        // Given
        Map<String, Object> expected = Map.of("$.something.value", 1);
        String fakeInvalidJson = """
            { "EXCEPTION 42 - BSOD"}
            """.stripIndent();

        // When
        JsonAssertAction jsonAssertAction = new JsonAssertAction(mock(Logger.class), fakeInvalidJson, expected);
        ActionExecutionResult result = jsonAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
    }

    @Test
    void execute_a_failing_assertion_on_wrong_XPath_value_in_expected() {
        // Given
        Map<String, Object> expected = Map.of("$.wrong.json.x.xpath", 1);
        String fakeActualResult = """
            { "xpath": { "to": "value" } }
            """.stripIndent();

        // When
        JsonAssertAction jsonAssertAction = new JsonAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = jsonAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
    }

    @Test
    void do_not_convert_int_as_long() {
        // Given
        Map<String, Object> expected = Map.of("$.something.value", 400.0);
        String fakeActualResult = """
            { "something": { "value": 400.0 } }
            """.stripIndent();

        // When
        JsonAssertAction jsonAssertAction = new JsonAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = jsonAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Success);
    }

    @Test
    void assert_enum_as_string() {
        // Given
        Map<String, Object> expected = Map.of("$.something.value", COMPARE_MODE.STRICT);
        String fakeActualResult = """
            { "something": { "value": "STRICT" } }
            """.stripIndent();

        // When
        JsonAssertAction jsonAssertAction = new JsonAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = jsonAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Success);
    }

    @Test
    void execute_a_successful_assertions_on_comparing_expected_value_as_string_and_actual_value_as_number() {
        // Given
        Map<String, Object> expected = Map.of("$.something.value", "my_value");
        String fakeActualResult = """
            { "something": { "value": my_value } }
            """.stripIndent();

        // When
        JsonAssertAction jsonAssertAction = new JsonAssertAction(mock(Logger.class), fakeActualResult, expected);
        ActionExecutionResult result = jsonAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Success);
    }

    @Test
    void execute_all_assertions_after_assertion_fail() {
        // Given
        Map<String, Object> expected = new HashMap<>();
        expected.put("$.something.value", 2);
        expected.put("$.something_else.value", 5);
        expected.put("$.a_thing.type", "my_type");
        expected.put("$.a_thing.not.existing", null);

        String fakeActualResult = "{\"something\":{\"value\":3},\"something_else\":{\"value\":5},\"a_thing\":{\"type\":\"my_type\"}}";
        Logger logger = mock(Logger.class);

        // When
        JsonAssertAction jsonAssertAction = new JsonAssertAction(logger, fakeActualResult, expected);
        ActionExecutionResult result = jsonAssertAction.execute();

        // Then
        assertThat(result.status).isEqualTo(Failure);
        verify(logger).error("On path [$.something.value], found [3], expected was [2]");
        verify(logger).info("On path [$.something_else.value], found [5]");
        verify(logger).info("On path [$.a_thing.type], found [my_type]");
        verify(logger).info("On path [$.a_thing.not.existing], found [null]");
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Assert json using placeholders")
    class AssertWithPlaceholders {

        @ParameterizedTest
        @ValueSource(strings = {
            "$.something.notpresent $contains:abcdef",
            "$.something.null $contains:abcdef",
            "$.something.notpresent $matches:\\d{4}-\\d{2}-\\d{2}",
            "$.something.null $matches:\\d{4}-\\d{2}-\\d{2}",
            "$.something.notpresent $isBeforeDate:2010-01-01T11:12:13.1230Z",
            "$.something.null $isBeforeDate:2010-01-01T11:12:13.1230Z",
            "$.something.notpresent $isAfterDate:1998-07-14T02:03:04.456Z",
            "$.something.null $isAfterDate:1998-07-14T02:03:04.456Z",
            "$.something.notpresent $isEqualDate:2000-01-01T11:11:12.123+01:00",
            "$.something.null $isEqualDate:2000-01-01T11:11:12.123+01:00",
            "$.something.notpresent $isLessThan:42000",
            "$.something.null $isLessThan:42000",
            "$.something.notpresent $isGreaterThan:45",
            "$.something.null $isGreaterThan:45",
            "$.something.notpresent $value:first",
            "$.something.null $value:first",
            "$.something.notpresent $isEmpty",
            "$.something.null $isEmpty",
            "$.something.notpresent $lenientEqual:abcd",
            "$.something.null $lenientEqual:abcd"
        })
        void handle_null_actual(String expectation) {
            // Given
            String[] expect = expectation.split(" ");
            Map<String, Object> expected = Map.of(expect[0], expect[1]);
            String fakeActualResult = "{ \"something\": { \"null\": null } }";

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(Failure);
        }

        @Test
        @DisplayName("isNull")
        public void assert_with_isNull_placeholder() {
            // Given
            Map<String, Object> expected = Map.of(
                "$.something.notexist", "$isNull",
                "$.something[?(@.notexist=='noop')]", "$isNull",
                "$.valuenull", "$isNull"
            );

            String fakeActualResult = """
                { "valuenull": null }
                """.stripIndent();

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(Success);
        }

        @Test
        @DisplayName("isNotNull")
        public void assert_with_isNotNull_placeholder() {
            // Given
            Map<String, Object> expected = Map.of(
                "$.something", "$isNotNull",
                "$.something.value", "$isNotNull",
                "$.something.emptyObject", "$isNotNull",
                "$.something.emptyArray", "$isNotNull",
                "$.something.emptyString", "$isNotNull"
            );

            String fakeActualResult = """
                {
                    "something": {
                        "value": 3,
                        "emptyObject": {},
                        "emptyArray": [],
                        "emptyString": ""
                    }
                }""".stripIndent();

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(Success);
        }

        @Test
        @DisplayName("contains")
        public void assert_with_contains_placeholder() {
            // Given
            Map<String, Object> expected = Map.of(
                "$.something.alphabet", "$contains:abcdefg",
                "$.something.value", "$contains:3",
                "$", "$contains:636, alpha"
            );

            String fakeActualResult = """
                {
                    "something": {
                        "value": 636,
                        "alphabet": "abcdefg",
                    }
                }""".stripIndent();

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(Success);
        }

        @Test
        @DisplayName("matches")
        public void assert_with_matches_placeholder() {
            // Given
            Map<String, Object> expected = Map.of(
                "$.something.matchregexp", "$matches:\\d{4}-\\d{2}-\\d{2}",
                "$", "$matches:.*something=\\{alpha.*"
            );

            String fakeActualResult = """
                {
                    "something": {
                        "alphabet": "abcdefg",
                        "matchregexp": "1983-10-26"
                    }
                }""".stripIndent();

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(Success);
        }

        @Test
        @DisplayName("is[Before|Equal|After]Date")
        public void assert_with_comparison_date_placeholders() {
            // Given
            Map<String, Object> expected = Map.of(
                "$.something.onedate", "$isBeforeDate:2010-01-01T11:12:13.1230Z",
                "$.something.seconddate", "$isAfterDate:1998-07-14T02:03:04.456Z",
                "$.something.thirddate", "$isEqualDate:2000-01-01T11:11:12.123+01:00"
            );

            String fakeActualResult = """
                {
                    "something": {
                        "onedate": "2000-01-01T11:11:12.123+01:00",
                        "seconddate": "2000-01-01T10:11:12.123Z",
                        "thirddate": "2000-01-01T10:11:12.123Z"
                    }
                }""".stripIndent();

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(Success);
        }

        @Test
        @DisplayName("is[Less|Greater]Than")
        public void assert_with_comparison_number_placeholders() {
            // Given
            Map<String, Object> expected = Map.of(
                "$.something.anumber", "$isLessThan:42000",
                "$.something.thenumber", "$isGreaterThan:45"
            );

            String fakeActualResult = """
                {
                    "something": {
                        "anumber": 4 100,
                        "thenumber": 46
                    }
                }""".stripIndent();

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(Success);
        }

        @Test
        @DisplayName("value (json array)")
        public void assert_with_json_array_value_placeholder() {
            // Given
            Map<String, Object> expected = Map.of(
                "$.something.objectArray[?(@.name=='obj2')].array[0]", "$value:first",
                "$.something.objectArray[?(@.name=='obj2')].array", "$value:[\"first\",\"second\",\"three\"]",
                "$.something.objectArray[?(@.name=='obj1')].array[2]", "$value[0]:three",
                "$.something.objectArray[?(@.name=='obj3')].array", "$value:[]"
            );

            String fakeActualResult = """
                {
                    "something": {
                        "objectArray": [
                            { "name": "obj1", "array": [ "first", "second", "three" ] },
                            { "name": "obj2", "array": [ "first", "second", "three" ] },
                            { "name": "obj3", "array": [ ] }
                        ]
                    }
                }""".stripIndent();

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(Success);
        }

        @Test
        @DisplayName("isEmpty")
        public void assert_with_isEmpty_placeholder() {
            // Given
            Map<String, Object> expected = Map.of(
                "$.something.emptyArray", "$isEmpty",
                "$.something.emptyString", "$isEmpty",
                "$.something.objectArray[?(@.name=='obj3')].array", "$isEmpty",
                "$.something.objectArray[?(@.name=='obj4')].emptyString", "$isEmpty"
            );

            String fakeActualResult = """
                {
                    "something": {
                        "objectArray": [
                            { "name": "obj3", "array": [ ] },
                            { "name": "obj4", "emptyString": "" }
                        ],
                        "emptyArray": [ ],
                        "emptyString": ""
                    }
                }""".stripIndent();

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), fakeActualResult, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(Success);
        }

        @ParameterizedTest
        @MethodSource("lenientEqual")
        @DisplayName("lenientEqual")
        public void assert_with_lenientEqual_placeholder(String doc, String expectedString, ActionExecutionResult.Status expectedStatus) {
            // Given
            Map<String, Object> expected = Map.of("$", "$lenientEqual:" + ofNullable(expectedString).orElse(doc));

            // When
            JsonAssertAction jsonAssertAction = new JsonAssertAction(new TestLogger(), doc, expected);
            ActionExecutionResult result = jsonAssertAction.execute();

            // Then
            assertThat(result.status).isEqualTo(expectedStatus);
        }

        Stream<Arguments> lenientEqual() {
            return Stream.of(
                // Classic equals
                Arguments.of("{}", null, Success),
                Arguments.of("[1, \"value\"]", null, Success),
                Arguments.of("{\"string\": \"value\"}", null, Success),
                Arguments.of("{\"number\": 123}", null, Success),
                Arguments.of("{\"array\": [1, 2, 3]}", null, Success),
                Arguments.of("{\"object\":{\"string\":\"value\"}}", null, Success),

                // Extra attributes
                Arguments.of("{\"number\": 123}", "{}", Success),
                Arguments.of("{\"string\": \"val\"}", "{}", Success),
                Arguments.of("{\"array\": [123, \"val\", {\"att\": \"val\"}]}", "{}", Success),
                Arguments.of("{\"object\": {\"att\": \"val\"}}", "{}", Success),

                Arguments.of("{\"string\": \"val\", \"extra_number\": 123}", "{\"string\": \"val\"}", Success),
                Arguments.of("{\"string\": \"val\", \"extra_string\": \"value\"}", "{\"string\": \"val\"}", Success),
                Arguments.of("{\"string\": \"val\", \"extra_array\": [123, \"val\", {\"att\": \"val\"}]}", "{\"string\": \"val\"}", Success),
                Arguments.of("{\"string\": \"val\", \"extra_object\": {\"att\": \"val\"}}", "{\"string\": \"val\"}", Success),

                Arguments.of("{\"object\": {\"att\": \"val\", \"extra_att\": \"extra_val\"}}", "{\"object\": {\"att\": \"val\"}}", Success),

                Arguments.of("{\"string\": \"val\", \"extra_number_one\": 123}", "{\"string\": \"val\", \"extra_number_two\": 123}", Failure),
                Arguments.of("{\"string\": \"val\", \"object\": {\"att\": \"val\", \"extra_att_one\": \"val\"}}", "{\"string\": \"val\", \"object\": {\"att\": \"val\", \"extra_att_two\": \"val\"}}", Failure),

                // Array order
                Arguments.of("[1, 2, 3]", "[2, 3, 1]", Success),
                Arguments.of("[1, 2, 3, 1]", "[2, 3, 1]", Failure),
                Arguments.of("{\"array\": [1, null, 3]}", "{\"array\": [null, 3, 1]}", Success),
                Arguments.of("[null, 3]", "[null, 3, null]", Failure),

                Arguments.of("[{\"att\": \"val\"}, 3]}", "[3, {\"att\": \"val\"}]", Success),
                Arguments.of("{\"object\": {\"array\": [null, {\"att\": \"val\"}]}}", "{\"object\": {\"array\": [{\"att\": \"val\"}, null]}}", Success),
                Arguments.of("[{\"att\": \"val\"}]", "[{\"att\": \"val\"}, null]", Failure),

                Arguments.of("[1, [1, 2, 3], 3]", "[1, [3, 2, 1], 3]", Failure)
            );
        }
    }
}
