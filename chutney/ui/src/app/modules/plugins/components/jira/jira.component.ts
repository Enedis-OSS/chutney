/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { JiraPluginConfiguration } from '@core/model/jira-plugin-configuration.model';
import { JiraPluginConfigurationService } from '@core/services/jira-plugin-configuration.service';
import { TranslateService } from '@ngx-translate/core';
import { delay } from '@shared/tools';
import { ValidationService } from '../../../../molecules/validation/validation.service';
import { Subject, takeUntil } from 'rxjs';


@Component({
    selector: 'chutney-config-jira',
    templateUrl: './jira.component.html',
    styleUrls: ['./jira.component.scss'],
    standalone: false
})
export class JiraComponent implements OnInit, OnDestroy {

    configuration: JiraPluginConfiguration = new JiraPluginConfiguration('', '', '', '', '', '');
    configurationForm: FormGroup;

    message;
    private savedMessage: string;
    private deletedMessage: string;
    private unsubscribeSub$: Subject<void> = new Subject();

    isErrorNotification: boolean = false;

    constructor(private configurationService: JiraPluginConfigurationService,
                private translate: TranslateService,
                private validationService: ValidationService,
                private formBuilder: FormBuilder) {
    }

    ngOnInit() {
        this.configurationForm = this.formBuilder.group({
            url: ['', Validators.required],
            username: '',
            password: '',
            urlProxy: '',
            userProxy: '',
            passwordProxy: ''
        });

        this.loadConfiguration();
        this.initTranslation();
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    private initTranslation() {
        this.translate.get('global.actions.done.saved').subscribe((res: string) => {
            this.savedMessage = res;
        });
        this.translate.get('global.actions.done.deleted').subscribe((res: string) => {
            this.deletedMessage = res;
        });
    }

    loadConfiguration() {
        this.configurationService.get()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (config: JiraPluginConfiguration) => {
                    this.configuration = config;
                    this.configurationForm.controls['url'].patchValue(config.url);
                    this.configurationForm.controls['username'].patchValue(config.username);
                    this.configurationForm.controls['password'].patchValue(config.password);
                    this.configurationForm.controls['urlProxy'].patchValue(config.urlProxy);
                    this.configurationForm.controls['userProxy'].patchValue(config.userProxy);
                    this.configurationForm.controls['passwordProxy'].patchValue(config.passwordProxy);
                },
                error: (error) => {
                    this.notify(error.error, true);
                }
            });
    }

    save() {
        const url = this.configurationForm.value['url'] ? this.configurationForm.value['url'] : '';
        const username = this.configurationForm.value['username'] ? this.configurationForm.value['username'] : '';
        const password = this.configurationForm.value['password'] ? this.configurationForm.value['password'] : '';
        const urlProxy = this.configurationForm.value['urlProxy'] ? this.configurationForm.value['urlProxy'] : '';
        const userProxy = this.configurationForm.value['userProxy'] ? this.configurationForm.value['userProxy'] : '';
        const passwordProxy = this.configurationForm.value['passwordProxy'] ? this.configurationForm.value['passwordProxy'] : '';
        this.configuration = new JiraPluginConfiguration(url, username, password, urlProxy, userProxy, passwordProxy);

        this.configurationService.save(this.configuration)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.notify(this.savedMessage, false);
                },
                error: (error) => {
                    this.notify(error.error, true);
                }
            });
    }

    delete() {
        this.configurationService.delete()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.notify(this.deletedMessage, false);
                    this.configurationForm.reset();
                },
                error: (error) => {
                    this.notify(error.error, true);
                }
            });
    }

    notify(message: string, isErrorNotification: boolean) {
        (async () => {
            this.isErrorNotification = isErrorNotification;
            this.message = message;
            await delay(3000);
            this.message = null;
        })();
    }

    isValid(): boolean {
        return this.validationService.isValidUrl(this.configurationForm.value['url'])
            && this.validationService.isNotEmpty(this.configurationForm.value['url']);
    }

}
