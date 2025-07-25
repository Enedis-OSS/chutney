/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { TestBed, waitForAsync } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TranslateModule } from '@ngx-translate/core';
import { ScenariosComponent } from './scenarios.component';
import { SharedModule } from '@shared/shared.module';

import { MoleculesModule } from '../../../../molecules/molecules.module';

import { MomentModule } from 'ngx-moment';
import { NgbModule, NgbPopoverConfig } from '@ng-bootstrap/ng-bootstrap';
import { EMPTY, of, Subject } from 'rxjs';
import { ScenarioIndex } from '@core/model';
import { ScenarioService } from '@core/services';

import { JiraPluginService } from '@core/services/jira-plugin.service';
import { JiraPluginConfigurationService } from '@core/services/jira-plugin-configuration.service';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ActivatedRouteStub } from '../../../../testing/activated-route-stub';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { NgMultiSelectDropDownModule } from 'ng-multiselect-dropdown';
import { DROPDOWN_SETTINGS, DropdownSettings } from '@core/model/dropdown-settings';
import { OAuthService } from "angular-oauth2-oidc";
import { AlertService } from '@shared';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

function getScenarios(html: HTMLElement) {
    return html.querySelectorAll('.scenario-title');
}

function sendInput(input: HTMLInputElement, value: string) {
    input.value = value;
    input.dispatchEvent(new Event('input'));
}

describe('ScenariosComponent', () => {
    let activatedRouteStub;

    beforeEach(waitForAsync(() => {
        TestBed.resetTestingModule();
        const eventsSubject = new Subject<any>();
        const scenarioService = jasmine.createSpyObj('ScenarioService', ['findScenarios', 'search']);
        const oAuthService = jasmine.createSpyObj('OAuthService', ['loadDiscoveryDocumentAndTryLogin', 'hasValidAccessToken', 'setupAutomaticSilentRefresh', 'configure', 'initCodeFlow', 'logOut', 'getAccessToken'], {events: eventsSubject.asObservable()});
        const alertService = jasmine.createSpyObj('AlertService', ['error']);
        const jiraPluginService = jasmine.createSpyObj('JiraPluginService', ['findScenarios', 'findCampaigns']);
        const jiraPluginConfigurationService = jasmine.createSpyObj('JiraPluginConfigurationService', ['getUrl']);
        const mockScenarioIndex = [new ScenarioIndex('1', 'title1', 'description', 'source', new Date(), new Date(), 1, 'guest', [], []),
                                   new ScenarioIndex('2', 'title2', 'description', 'source', new Date(), new Date(), 1, 'guest', [], []),
                                   new ScenarioIndex('3', 'another scenario', 'description', 'source', new Date(), new Date(), 1, 'guest', [], [])];
        scenarioService.findScenarios.and.returnValue(of(mockScenarioIndex));
        scenarioService.search.and.returnValue(of(mockScenarioIndex));
        jiraPluginConfigurationService.getUrl.and.returnValue(EMPTY);
        jiraPluginService.findScenarios.and.returnValue(EMPTY);
        jiraPluginService.findCampaigns.and.returnValue(EMPTY);
        activatedRouteStub = new ActivatedRouteStub();
        TestBed.configureTestingModule({
    declarations: [
        ScenariosComponent
    ],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [RouterModule.forRoot([]),
        TranslateModule.forRoot(),
        MoleculesModule,
        SharedModule,
        MomentModule,
        NgbModule,
        NgMultiSelectDropDownModule.forRoot()],
    providers: [
        NgbPopoverConfig,
        { provide: ScenarioService, useValue: scenarioService },
        { provide: OAuthService, useValue: oAuthService },
        { provide: AlertService, useValue: alertService },
        { provide: JiraPluginService, useValue: jiraPluginService },
        { provide: JiraPluginConfigurationService, useValue: jiraPluginConfigurationService },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: DROPDOWN_SETTINGS, useClass: DropdownSettings },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
}).compileComponents();
    }));

    it('should create the component ScenariosComponent with three scenarios', waitForAsync(() => {
        const fixture = TestBed.createComponent(ScenariosComponent);
        activatedRouteStub.setParamMap({orderBy: 'id'});
        fixture.detectChanges();
        fixture.whenStable().then(() => {
            fixture.detectChanges();
            const app = fixture.debugElement.componentInstance;
            expect(app).toBeTruthy();
            const html: HTMLElement = fixture.nativeElement;
            const scenarios = getScenarios(html);
            expect(scenarios.length).toBe(3);
            expect(scenarios[0].textContent).toBe('title1');
            expect(scenarios[1].textContent).toBe('title2');
            expect(scenarios[2].textContent).toBe('another scenario');
            expect(fixture.componentInstance.scenarios.length).toBe(3);
        });
    }));

    it('should filter the list of scenario', waitForAsync(() => {
        const fixture = TestBed.createComponent(ScenariosComponent);
        fixture.detectChanges();
        fixture.whenStable().then(() => {
            const html: HTMLElement = fixture.nativeElement;

            const searchInput: HTMLInputElement = html.querySelector('#scenario-search');
            sendInput(searchInput, 'another');
            fixture.detectChanges();

            const scenarios = getScenarios(html);
            expect(scenarios.length).toBe(1);
            expect(scenarios[0].textContent).toBe('another scenario');
        });
    }));

    it('should apply filters from the URL',  waitForAsync(() => {
        const fixture = TestBed.createComponent(ScenariosComponent);
        activatedRouteStub.setParamMap({ text: 'title', orderBy: 'title', reverseOrder: 'true'});
        fixture.detectChanges();
        fixture.whenStable().then(() => {
            fixture.detectChanges();
            const app = fixture.debugElement.componentInstance;
            expect(app).toBeTruthy();
            const html: HTMLElement = fixture.nativeElement;
            const scenarios = getScenarios(html);

            expect(scenarios.length).toBe(2);
            expect(scenarios[0].textContent).toBe('title2');
            expect(scenarios[1].textContent).toBe('title1');
            expect(fixture.componentInstance.scenarios.length).toBe(3);
        });
    }));

});


