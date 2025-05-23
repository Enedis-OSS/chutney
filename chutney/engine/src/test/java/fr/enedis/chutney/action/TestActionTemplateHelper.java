/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action;

import fr.enedis.chutney.action.domain.ActionTemplate;
import fr.enedis.chutney.action.domain.parameter.AnnotationSet;
import fr.enedis.chutney.action.domain.parameter.Parameter;
import fr.enedis.chutney.action.spi.injectable.Input;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.mockito.Mockito;

public class TestActionTemplateHelper {
    private TestActionTemplateHelper() {
    }

    public static ActionTemplate mockActionTemplate(String identifier, Set<Parameter> parameters) {
        ActionTemplate actionTemplate = Mockito.mock(ActionTemplate.class);
        Mockito.when(actionTemplate.identifier()).thenReturn(identifier);
        Mockito.when(actionTemplate.parameters()).thenReturn(parameters);
        return actionTemplate;
    }

    public static Parameter mockParameter(Class type) {
        return mockParameter(type, null);
    }

    public static Parameter mockParameter(Class type, String inputValue) {
        Parameter parameter = Mockito.mock(Parameter.class);
        Mockito.when(parameter.rawType()).thenReturn(type);
        if (inputValue != null) {
            Input newInput = new Input() {

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Input.class;
                }

                @Override
                public String value() {
                    return inputValue;
                }
            };
            Mockito.when(parameter.annotations()).thenReturn(new AnnotationSet(new HashSet<>(Collections.singletonList(newInput))));
        } else {
            Mockito.when(parameter.annotations()).thenReturn(new AnnotationSet(Collections.emptySet()));
        }
        return parameter;
    }
}
