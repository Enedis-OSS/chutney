/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { areEquals, Clonable, cloneAsPossible } from '@shared';

export class Dataset {

    public static CUSTOM_ID = '__CUSTOM__';
    public static CUSTOM_LABEL = 'Custom';

    constructor(
        public name: string = '',
        public description: string = '',
        public tags: Array<string> = [],
        public lastUpdated: Date,
        public uniqueValues: Array<KeyValue>,
        public multipleValues: Array<Array<KeyValue>>,
        public id?: string,
        public scenarioUsage?: Array<string>,
        public campaignUsage?: Array<string>,
        public scenarioInCampaignUsage?: { [key: string]: string[] }) {
    }

    getMultipleValueHeader(): Array<string> {
        if (this.multipleValues.length > 0) {
            return this.multipleValues[0].map(v => v.key);
        }
        return [];
    }

    public equals(obj: Dataset): boolean {
        return obj
            && areEquals(this.name, obj.name)
            && areEquals(this.description, obj.description)
            && areEquals(this.tags, obj.tags)
            && areEquals(this.uniqueValues, obj.uniqueValues)
            && areEquals(this.multipleValues, obj.multipleValues);
    }
}

export class KeyValue implements Clonable<KeyValue> {

    constructor(
        public key: string,
        public value: any
    ) {
    }

    public clone(): KeyValue {
        return new KeyValue(
            cloneAsPossible(this.key),
            cloneAsPossible(this.value)
        );
    }

    public equals(obj: KeyValue): boolean {
        return obj
            && areEquals(this.key, obj.key)
            && areEquals(this.value, obj.value);
    }
}
