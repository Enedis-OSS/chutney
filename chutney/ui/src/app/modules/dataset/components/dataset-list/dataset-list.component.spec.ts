/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { TestBed, waitForAsync } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TranslateModule } from '@ngx-translate/core';
import { SharedModule } from '@shared/shared.module';

import { MoleculesModule } from '../../../../molecules/molecules.module';

import { MomentModule } from 'ngx-moment';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { DatasetListComponent } from './dataset-list.component';
import { DataSetService } from '@core/services';
import { of, Subject } from 'rxjs';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgMultiSelectDropDownModule } from 'ng-multiselect-dropdown';
import { DROPDOWN_SETTINGS, DropdownSettings } from '@core/model/dropdown-settings';
import { RouterModule } from '@angular/router';
import { OAuthService } from "angular-oauth2-oidc";
import { AlertService } from '@shared';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('DatasetListComponent', () => {


  const eventsSubject = new Subject<any>();
  const dataSetService = jasmine.createSpyObj('DataSetService', ['findAll']);
  const oAuthService = jasmine.createSpyObj('OAuthService', ['loadDiscoveryDocumentAndTryLogin', 'setupAutomaticSilentRefresh', 'hasValidAccessToken', 'configure', 'initCodeFlow', 'logOut', 'getAccessToken'], {events: eventsSubject.asObservable()});
  const alertService = jasmine.createSpyObj('AlertService', ['error']);
  dataSetService.findAll.and.returnValue(of([]));
   beforeEach(waitForAsync(() => {
    TestBed.resetTestingModule();

    TestBed.configureTestingModule({
    declarations: [
        DatasetListComponent
    ],
    imports: [RouterModule.forRoot([]),
        TranslateModule.forRoot(),
        MoleculesModule,
        SharedModule,
        MomentModule,
        NgbModule,
        NgMultiSelectDropDownModule.forRoot(),
        FormsModule,
        ReactiveFormsModule],
    providers: [
        { provide: DataSetService, useValue: dataSetService },
        { provide: AlertService, useValue: alertService },
        { provide: OAuthService, useValue: oAuthService },
        { provide: DROPDOWN_SETTINGS, useClass: DropdownSettings },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
}).compileComponents();
  }));

  it('should create the component DatasetListComponent', () => {
    const fixture = TestBed.createComponent(DatasetListComponent);
    fixture.detectChanges();

    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  });

});


