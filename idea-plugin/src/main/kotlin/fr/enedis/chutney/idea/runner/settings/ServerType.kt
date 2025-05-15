/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.idea.runner.settings

/**
 * Is the server being run somewhere else by the user, or are they running a server in the IDE?
 * @author alexeagle@google.com (Alex Eagle)
 */
enum class ServerType {
    EXTERNAL, INTERNAL
}
