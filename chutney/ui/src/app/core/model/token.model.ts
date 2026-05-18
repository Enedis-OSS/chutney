/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

export class AccessToken {

    constructor(
            public note: string = '',
            public expiresAt: Date) {
    }
}