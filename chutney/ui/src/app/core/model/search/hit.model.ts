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
        return Object.entries(this)
            .map(([key, value]) => {
                let valueStr: string;
                if (typeof value === 'string') {
                    valueStr = value;
                } else if (value !== null && value !== undefined) {
                    valueStr = typeof value === 'object' ? JSON.stringify(value) : value.toString();
                } else {
                    return null;
                }
                return { key, valueStr }
            })
            .filter(res => res !== null)
            .filter(res => res.key != 'id' && res.key != 'title' && res.key != 'tags')
            .map((res) => {
                let index = res.valueStr.indexOf(searchTerm);
                if (index !== -1) {
                    const start = Math.max(0, index - 40);
                    const end = Math.min(res.valueStr.length, index + searchTerm.length + 40);
                    return {
                        attribute: res.key,
                        snippet: res.valueStr.substring(start, end),
                    };
                }
                return null
            })
            .filter(res => res !== null)
    }


    static fromJson(json: any): Hit {
        return new Hit(json.id, json.title, json.description, json.content, json.tags, json.what);
    }
}


export interface SearchResult {
    attribute: string;
    snippet: string;
}


