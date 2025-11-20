/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.server.core.domain.security.Authorization;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.support.ReflectionSupport;
import org.springframework.security.access.prepost.PreAuthorize;

public class SpringPreAuthorizeTest {

    private static final String SPRING_PREAUTHORIZE_VALUE_REGEX = "'(?<value>[^']*)'";
    private static final Pattern SPRING_PREAUTHORIZE_VALUE_PATTERN = Pattern.compile(SPRING_PREAUTHORIZE_VALUE_REGEX);

    @Test
    void preAuthorize_annotations_reference_existing_authorizations() {
        ReflectionSupport.findAllClassesInPackage(
            this.getClass().getPackageName(),
            truePredicate(),
            truePredicate()
        ).forEach(c -> {
            var isPreAuthorizeAnnotationOnClass = preAuthorizeMethod().test(c);
            Arrays.stream(c.getMethods())
                .filter(m -> isPreAuthorizeAnnotationOnClass || preAuthorizeMethod().test(m))
                .forEach(m -> {
                    var preAuthorize = m.getAnnotation(PreAuthorize.class);
                    var preAuthorizeValues = extractAuthorizationsFromPreAuthorizeAnnotation(preAuthorize.value());
                    preAuthorizeValues.forEach(v ->
                        Assertions.assertDoesNotThrow(() -> {
                            Authorization.valueOf(v);
                        }, "Found preAuthorize value [" + v + "] in class [" + c.getName() + "] !!"));
                });
        });
    }

    private static @NotNull Predicate<? super AnnotatedElement> preAuthorizeMethod() {
        return m -> m.getAnnotation(PreAuthorize.class) != null;
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "hasAuthority('AUTHORITY')",
        "hasAuthority('AUTHORITY') or hasAuthority('AUTHORITY')",
        "hasAnyAuthority('AUTHORITY', 'AUTHORITY') or hasAuthority('AUTHORITY')",
        "hasAnyAuthority('AUTHORITY', 'AUTHORITY') or hasAnyAuthority('AUTHORITY', 'AUTHORITY')"
    })
    void extractAuthorizationsFromPreAuthorizeAnnotationTest(String preAuthorizeValue) {
        var expectedSize = StringUtils.countMatches(preAuthorizeValue, "AUTHORITY");
        List<String> result = extractAuthorizationsFromPreAuthorizeAnnotation(preAuthorizeValue);
        assertThat(result)
            .hasSize(expectedSize)
            .containsOnly("AUTHORITY")
        ;
    }

    private List<String> extractAuthorizationsFromPreAuthorizeAnnotation(String preAuthorizeValue) {
        var matcher = SPRING_PREAUTHORIZE_VALUE_PATTERN.matcher(preAuthorizeValue);
        var result = matcher.results()
            .map(r -> r.group("value"))
            .toList();
        if (result.isEmpty()) {
            throw new IllegalArgumentException("preAuthorize value without value match !!");
        }
        return result;
    }

    private static @NotNull <T> Predicate<T> truePredicate() {
        return s -> true;
    }
}
