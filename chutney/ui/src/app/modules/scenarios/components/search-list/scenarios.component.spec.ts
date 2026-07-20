/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
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

function anchorByIcon(html: HTMLElement, iconClass: string): HTMLAnchorElement {
    return html.querySelector(`a span.${iconClass}`)?.closest('a');
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

    it('should open scenario as a routerLink anchor so it supports "open in new tab"', waitForAsync(() => {
        const fixture = TestBed.createComponent(ScenariosComponent);
        fixture.componentInstance.isAuthorizedToReadExecutions = true;
        activatedRouteStub.setParamMap({orderBy: 'id'});
        fixture.detectChanges();
        fixture.whenStable().then(() => {
            fixture.detectChanges();
            const html: HTMLElement = fixture.nativeElement;

            // Title cell is an anchor pointing at the executions view with the last execution opened
            const titleAnchor: HTMLAnchorElement = getScenarios(html)[0].querySelector('a');
            expect(titleAnchor).withContext('title should be an <a>').toBeTruthy();
            expect(titleAnchor.textContent.trim()).toBe('title1');
            const titleHref = titleAnchor.getAttribute('href');
            expect(titleHref).toContain('/scenario/1/executions');
            expect(titleHref).toContain('open=last');
            expect(titleHref).toContain('active=last');

            // The eye (show) action targets the same executions link
            const eyeAnchor = anchorByIcon(html, 'fa-eye');
            expect(eyeAnchor).withContext('show action should be an <a>').toBeTruthy();
            expect(eyeAnchor.getAttribute('href')).toContain('/scenario/1/executions');

            // The edit action targets the raw-edition view (no query params)
            const editAnchor = anchorByIcon(html, 'fa-edit');
            expect(editAnchor).withContext('edit action should be an <a>').toBeTruthy();
            expect(editAnchor.getAttribute('href')).toContain('/scenario/1/raw-edition');
        });
    }));

    it('should fall back to raw-edition when not authorized to read executions', waitForAsync(() => {
        const fixture = TestBed.createComponent(ScenariosComponent);
        fixture.componentInstance.isAuthorizedToReadExecutions = false;
        activatedRouteStub.setParamMap({orderBy: 'id'});
        fixture.detectChanges();
        fixture.whenStable().then(() => {
            fixture.detectChanges();
            const html: HTMLElement = fixture.nativeElement;

            const titleAnchor: HTMLAnchorElement = getScenarios(html)[0].querySelector('a');
            expect(titleAnchor).toBeTruthy();
            const href = titleAnchor.getAttribute('href');
            expect(href).toContain('/scenario/1/raw-edition');
            expect(href).not.toContain('open=last');
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


