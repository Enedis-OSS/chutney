/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.micrometer;

import static fr.enedis.chutney.action.micrometer.MicrometerActionTestHelper.assertSuccessAndOutputObjectType;
import static fr.enedis.chutney.action.micrometer.MicrometerActionTestHelper.buildMeterName;
import static fr.enedis.chutney.action.micrometer.MicrometerGaugeAction.OUTPUT_GAUGE;
import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Failure;
import static fr.enedis.chutney.action.spi.ActionExecutionResult.Status.Success;
import static io.micrometer.core.instrument.Metrics.globalRegistry;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.TestLogger;
import fr.enedis.chutney.action.spi.ActionExecutionResult;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;
import org.apache.groovy.util.Maps;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class MicrometerGaugeActionTest {

    private final String METER_NAME_PREFIX = "gaugeName";

    @Test
    public void gauge_name_is_mandatory() {
        MicrometerGaugeAction micrometerGaugeAction = new MicrometerGaugeAction(null, null, null, null, null, null, null, "function", null);

        List<String> errors = micrometerGaugeAction.validateInputs();

        assertThat(errors.size()).isEqualTo(2);
        assertThat(errors.getFirst()).isEqualTo("No name provided (String)");
        assertThat(errors.get(1)).isEqualTo("name should not be blank");
    }

    @Test
    public void gauge_object_and_gauge_function_must_not_be_both_null() {
        MicrometerGaugeAction micrometerGaugeAction = new MicrometerGaugeAction(null, buildMeterName(METER_NAME_PREFIX), null, null, null, null, null, null, null);
        List<String> errors = micrometerGaugeAction.validateInputs();

        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.getFirst()).isEqualTo("gaugeObject and gaugeFunction cannot be both null");

    }

    @Test
    public void gauge_function_must_be_fully_qualified_when_gauge_object_null() {
        // Given
        TestLogger logger = new TestLogger();
        MicrometerGaugeAction sut = new MicrometerGaugeAction(logger, buildMeterName(METER_NAME_PREFIX), null, null, null, null, null, "size", null);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertOneError(result, logger, "cannot be resolved");
    }

    @Test
    public void gauge_function_must_be_static_when_gauge_object_null() {
        // Given
        TestLogger logger = new TestLogger();
        MicrometerGaugeAction sut = new MicrometerGaugeAction(logger, buildMeterName(METER_NAME_PREFIX), null, null, null, null, null, "java.util.ArrayList.size", null);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertOneError(result, logger, "must be static");
    }

    @Test
    public void gauge_function_must_have_no_parameters() {
        // Given
        TestLogger logger = new TestLogger();
        MicrometerGaugeAction sut = new MicrometerGaugeAction(logger, buildMeterName(METER_NAME_PREFIX), null, null, null, null, new ArrayList<>(), "java.util.ArrayList.get", null);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertOneError(result, logger, "must not have parameters");
    }

    @Test
    public void unqualified_gauge_function_should_be_search_for_gauge_object() {
        // Given
        TestLogger logger = new TestLogger();
        MicrometerGaugeAction sut = new MicrometerGaugeAction(logger, buildMeterName(METER_NAME_PREFIX), null, null, null, null, new ArrayList<>(), "unknown", null);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertOneError(result, logger, "Cannot find", "unknown", "java.util.ArrayList");
    }

    @Test
    public void gauge_function_should_return_a_number() {
        // Given
        TestLogger logger = new TestLogger();
        MicrometerGaugeAction sut = new MicrometerGaugeAction(logger, buildMeterName(METER_NAME_PREFIX), null, null, null, null, new ArrayList<>(), "trimToSize", null);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertOneError(result, logger, "must return a Number");
    }

    @Test
    public void gauge_object_must_be_a_collection_map_or_number_when_gauge_function_null() {
        // Given
        TestLogger logger = new TestLogger();
        MicrometerGaugeAction sut = new MicrometerGaugeAction(logger, buildMeterName(METER_NAME_PREFIX), null, null, null, null, new Object(), null, null);

        // When
        List<String> errors = sut.validateInputs();

        // Then
        assertThat(errors.size()).isEqualTo(1);
        assertThat(errors.getFirst()).isEqualTo("gaugeObject must be a Number, a Collection or a Map if no gaugeFunction supplied");
    }

    @Test
    public void should_create_gauge_from_a_number_object_when_gauge_function_null() {
        // Given
        MeterRegistry registry = new SimpleMeterRegistry();
        AtomicInteger gaugeObject = new AtomicInteger(8);
        String meterName = buildMeterName(METER_NAME_PREFIX);
        MicrometerGaugeAction sut = new MicrometerGaugeAction(new TestLogger(), meterName, null, null, null, null, gaugeObject, null, registry);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertSuccessAndGaugeObjectType(result, AtomicInteger.class);

        AtomicInteger outputGauge = (AtomicInteger) result.outputs.get(OUTPUT_GAUGE);
        assertThat(outputGauge).isEqualTo(gaugeObject);

        assertGaugeValue(gaugeObject, meterName, registry);

        // When
        gaugeObject.set(90);
        // Then
        assertGaugeValue(gaugeObject, meterName, registry);
    }

    @Test
    public void should_create_gauge_from_a_collection_object_measuring_its_size_when_gauge_function_null() {
        // Given
        MeterRegistry registry = new SimpleMeterRegistry();
        ArrayList<String> gaugeObject = new ArrayList<>();
        String meterName = buildMeterName(METER_NAME_PREFIX);
        MicrometerGaugeAction sut = new MicrometerGaugeAction(new TestLogger(), meterName, null, null, null, null, gaugeObject, null, registry);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertSuccessAndGaugeObjectType(result, List.class);

        ArrayList outputGauge = (ArrayList) result.outputs.get(OUTPUT_GAUGE);
        assertThat(outputGauge).isEqualTo(gaugeObject);

        assertGaugeValue(gaugeObject.size(), meterName, registry);

        // When
        gaugeObject.addAll(Lists.list("", "", ""));
        // Then
        assertGaugeValue(gaugeObject.size(), meterName, registry);
    }

    @Test
    public void should_create_gauge_from_a_map_object_measuring_its_size_when_gauge_function_null() {
        // Given
        MeterRegistry registry = new SimpleMeterRegistry();
        HashMap<String, Object> gaugeObject = new HashMap<>();
        String meterName = buildMeterName(METER_NAME_PREFIX);
        MicrometerGaugeAction sut = new MicrometerGaugeAction(new TestLogger(), meterName, null, null, null, null, gaugeObject, null, registry);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertSuccessAndGaugeObjectType(result, Map.class);

        Map outputGauge = (Map) result.outputs.get(OUTPUT_GAUGE);
        assertThat(outputGauge).isEqualTo(gaugeObject);

        assertGaugeValue(gaugeObject.size(), meterName, registry);

        // When
        gaugeObject.putAll(Maps.of("", "", "", ""));
        // Then
        assertGaugeValue(gaugeObject.size(), meterName, registry);
    }

    @Test
    public void should_create_gauge_from_an_object_and_a_public_function_defined_on_it() {
        // Given
        class TestObject {
            private int state;

            private TestObject(int initialValue) {
                this.state = initialValue;
            }

            public double measure() {
                return 2 * state;
            }

            private void changeState(int v) {
                state /= v;
            }
        }

        MeterRegistry registry = new SimpleMeterRegistry();
        TestObject gaugeObject = new TestObject(6);
        String meterName = buildMeterName(METER_NAME_PREFIX);
        MicrometerGaugeAction sut = new MicrometerGaugeAction(new TestLogger(), meterName, null, null, null, null, gaugeObject, "measure", registry);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertSuccessAndGaugeObjectType(result, TestObject.class);

        TestObject outputGauge = (TestObject) result.outputs.get(OUTPUT_GAUGE);
        assertThat(outputGauge).isEqualTo(gaugeObject);

        assertGaugeValue(gaugeObject.measure(), meterName, registry);

        // When
        gaugeObject.changeState(2);
        // Then
        assertGaugeValue(gaugeObject.measure(), meterName, registry);
    }

    @Test
    public void should_create_gauge_from_a_static_public_function() {
        // Given
        staticFunction(); // Avoid auto clean unused method
        MeterRegistry registry = new SimpleMeterRegistry();
        String meterName = buildMeterName(METER_NAME_PREFIX);
        MicrometerGaugeAction sut = new MicrometerGaugeAction(new TestLogger(), meterName, null, null, null, null, null, this.getClass().getName() + ".staticFunction", registry);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertThat(result.status).isEqualTo(Success);
        assertThat(result.outputs).isEmpty();

        Iterable<Measurement> firstMeasure = requireNonNull(registry.find(meterName).gauge()).measure();
        assertThat(firstMeasure).hasSize(1);
        double firstValue = getFirstValueFromMeasurements(firstMeasure).getValue();

        // When
        double secondValue = requireNonNull(registry.find(meterName).gauge()).value();
        // Then
        assertThat(requireNonNull(registry.find(meterName).gauge()).measure()).hasSize(1);
        assertThat(secondValue).isNotEqualTo(firstValue);
    }

    @Test
    public void should_create_gauge_and_register_it_on_given_registry() {
        // Given
        MeterRegistry givenMeterRegistry = new SimpleMeterRegistry();
        String meterName = buildMeterName(METER_NAME_PREFIX);
        MicrometerGaugeAction sut = new MicrometerGaugeAction(new TestLogger(), meterName, "description", "my unit", null, Lists.list("tag", "my tag value"), new AtomicInteger(), null, givenMeterRegistry);

        // When
        ActionExecutionResult result = sut.execute();

        // Then
        assertSuccessAndGaugeObjectType(result, AtomicInteger.class);

        assertThat(globalRegistry.find(meterName).gauges()).isEmpty();
        Gauge gauge = givenMeterRegistry.find(meterName).gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.getId().getDescription()).isEqualTo("description");
        assertThat(gauge.getId().getBaseUnit()).isEqualTo("my unit");
        assertThat(gauge.getId().getTag("tag")).isEqualTo("my tag value");
    }

    private void assertGaugeValue(Number gaugeObject, String meterName, MeterRegistry registry) {
        assertThat(requireNonNull(registry.find(meterName).gauge()).value()).isEqualTo(gaugeObject.doubleValue());
    }

    private void assertOneError(ActionExecutionResult result, TestLogger logger, String... messages) {
        assertThat(result.status).isEqualTo(Failure);
        assertThat(logger.errors).hasSize(1);
        for (String message : messages) {
            assertThat(logger.errors.getFirst()).contains(message);
        }
    }

    private void assertSuccessAndGaugeObjectType(ActionExecutionResult result, Class clazz) {
        assertSuccessAndOutputObjectType(result, OUTPUT_GAUGE, clazz);
    }

    private Measurement getFirstValueFromMeasurements(Iterable<Measurement> measurements) {
        return StreamSupport.stream(measurements.spliterator(), false)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Measurements should not be empty !!"));
    }

    public static Number staticFunction() {
        return Objects.hashCode(new Object());
    }
}
