/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.jms;

import fr.enedis.chutney.action.spi.injectable.Target;
import javax.jms.InvalidSelectorException;
import javax.jms.JMSException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

@SuppressWarnings("serial")
class UncheckedJmsException extends RuntimeException {

    public UncheckedJmsException(InvalidSelectorException e) {
        super("Cannot parse selector " + e.getMessage(), e);
    }

    public UncheckedJmsException(NameNotFoundException e, Target target) {
        super("Cannot find destination " + e.getMessage() + " on jms server " + target.name() + " (" + target.uri().toString() + ")", e);
    }

    public UncheckedJmsException(JMSException e, Target target) {
        super("Cannot connect to jms server " + target.name() + " (" + target.uri().toString() + "): " + e.getMessage(), e);
    }

    public UncheckedJmsException(NamingException e, Target target) {
        super("Cannot connect to jms server " + target.name() + " (" + target.uri().toString() + "): " + e.getMessage(), e);
    }
}
