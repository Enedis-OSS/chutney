/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.spi.injectable;

import fr.enedis.chutney.action.spi.Action;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a {@link Action} constructor parameter as retrieved from Step inputs
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Input {

    /**
     * Name of the input parameter.
     * <p>
     * Mandatory to retrieve constructor parameter name since it's lost at compilation
     */
    String value();
}
