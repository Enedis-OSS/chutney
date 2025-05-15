/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jakarta;

import fr.enedis.chutney.action.spi.injectable.Target;
import jakarta.jms.InvalidSelectorException;
import jakarta.jms.JMSException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

@SuppressWarnings("serial")
class UncheckedJakartaException extends RuntimeException {

    public UncheckedJakartaException(InvalidSelectorException e) {
        super("Cannot parse selector " + e.getMessage(), e);
    }

    public UncheckedJakartaException(NameNotFoundException e, Target target) {
        super("Cannot find destination " + e.getMessage() + " on jms server " + target.name() + " (" + target.uri().toString() + ")", e);
    }

    public UncheckedJakartaException(JMSException e, Target target) {
        super("Cannot connect to jms server " + target.name() + " (" + target.uri().toString() + "): " + e.getMessage(), e);
    }

    public UncheckedJakartaException(NamingException e, Target target) {
        super("Cannot connect to jms server " + target.name() + " (" + target.uri().toString() + "): " + e.getMessage(), e);
    }
}
