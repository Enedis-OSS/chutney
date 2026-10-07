/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { TestCase } from '@model';
import { HjsonParserService } from '@shared/hjson-parser/hjson-parser.service';

import { RawEditionComponent } from './raw-edition.component';

describe('RawEditionComponent', () => {
    const scenario = new TestCase('42', 'title', 'description', '{ givens: [] }', 'local',
        null, null, 1, 'author', ['tag'], [], 'dataset');

    it('should allow leaving a new scenario without changes', () => {
        const component = createComponent({});

        expect(component.canDeactivatePage()).toBeTrue();
    });

    it('should not allow leaving after scenario content changed', () => {
        const component = createComponent({});

        component.onScenarioContentChanged('{ givens: [], when: {} }');

        expect(component.canDeactivatePage()).toBeFalse();
    });

    it('should allow leaving when editor gives back loaded content', () => {
        const component = createComponent({ id: '42' });

        component.onScenarioContentChanged(scenario.content);

        expect(component.canDeactivatePage()).toBeTrue();
    });

    it('should allow leaving a duplicated scenario without changes', () => {
        const component = createComponent({ id: '42' }, true);

        expect(component.testCase.defaultDataset).toBeNull();
        expect(component.canDeactivatePage()).toBeTrue();
    });

    function createComponent(params: object, duplicate = false): RawEditionComponent {
        const route = {
            params: of(params),
            snapshot: { queryParamMap: convertToParamMap(duplicate ? { duplicate: 'true' } : {}) }
        };
        const scenarioService = { findRawTestCase: () => of(TestCase.fromRaw(scenario)) };
        const loginService = { hasAuthorization: () => true };
        const component = new RawEditionComponent(
            route as any, null, scenarioService as any, new HjsonParserService(), loginService as any
        );
        component.ngOnInit();
        return component;
    }
});
