/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.target.dto;

public final class NoTargetDto {

    public static final TargetDto NO_TARGET_DTO = new TargetDto("", "", null);

    private NoTargetDto() {
    }
}
