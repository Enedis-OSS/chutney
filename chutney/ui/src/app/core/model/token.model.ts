/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

export class AccessToken {

    constructor(
            public id: string = '',
            public note: string = '',
            public expiresAt: Date) {
    }
}

export class CreatedAccessToken {

    constructor(
            public id: string,
            public note: string,
            public token: string,
            public expiresAt: Date) {
    }
}