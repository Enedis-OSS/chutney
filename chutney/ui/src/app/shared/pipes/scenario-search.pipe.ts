/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { ScenarioIndex } from '@model';
import { Pipe, PipeTransform } from '@angular/core';
import { intersection } from '@shared/tools/array-utils';

@Pipe({
    name: 'scenarioSearch',
    standalone: false
})
export class ScenarioSearchPipe implements PipeTransform {

    transform(input: any, tags: String[], noTag: boolean, all: boolean) {
        return all ? input : input.filter((item: ScenarioIndex) => {
            return (this.tagPresent(tags, item) || this.noTagPresent(noTag, item));
        });
    }

    private tagPresent(tags: String[], scenario: ScenarioIndex): boolean {
        return intersection(tags, scenario.tags).length > 0;
    }

    private noTagPresent(noTag: boolean, scenario: ScenarioIndex): boolean {
        return noTag && scenario.tags.length === 0;
    }
}
