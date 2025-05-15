/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.index.api.dto;

import java.util.List;

public record Hit(
    String id,
    String title,
    String description,
    String content,
    List<String> tags,
    String what
) {}
