/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

export class Hit {
    constructor(
        public id: string,
        public title: string,
        public description: string,
        public content: string,
        public tags: string[],
        public what: string
    ) { }

    search(searchTerm: string): { attribute: string; snippet: string }[] {
        const results: { attribute: string; snippet: string }[] = [];

        Object.entries(this).forEach(([key, value]) => {
            let valueStr: string;
            if (typeof value === "string") {
                valueStr = value;
            } else if (value !== null && value !== undefined) {
                valueStr = typeof value === "object" ? JSON.stringify(value) : value.toString();
            } else {
                return;
            }

            let index = valueStr.indexOf(searchTerm);
            if (index !== -1) {
                const start = Math.max(0, index - 40);
                const end = Math.min(valueStr.length, index + searchTerm.length + 40);
                results.push({
                    attribute: key,
                    snippet: valueStr.substring(start, end),
                });
                index = valueStr.indexOf(searchTerm, index + searchTerm.length);
            }
        });

        return results;
    }

}

export interface SearchResult {
    attribute: string;
    snippet: string;
}


