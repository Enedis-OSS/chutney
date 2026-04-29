/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

export class AgentInfo {
    constructor(
        public name: string,
        public host: string,
        public port: number,
    ) { }
}
