/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { RolesService } from '@core/services';

import { delay } from '@shared/tools';
import { Subject, takeUntil } from 'rxjs';

@Component({
    selector: 'chutney-roles',
    templateUrl: './roles.component.html',
    styleUrls: ['./roles.component.scss'],
    standalone: false
})
export class RolesComponent implements OnInit {

    rolesContent: string;
    message: string;
    help: boolean;
    error: boolean;


    private saving: string;
    private saved: string;
    private modifiedRoles: string;
    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(
        private rolesService: RolesService,
        private translate: TranslateService,
    ) {
        translate.get('global.actions.ongoing.saving').subscribe((res: string) => {
            this.saving = res;
        });
        translate.get('global.actions.done.saved').subscribe((res: string) => {
            this.saved = res;
        });
    }

    ngOnInit() {
        this.loadRoles();
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    saveRoles() {
        try {
            const content = JSON.parse(this.modifiedRoles);
            (async () => {
                this.printMessage(this.saving);
                await delay(1000);
                this.rolesService.save(content)
                    .pipe(takeUntil(this.unsubscribeSub$))
                    .subscribe({
                        next: res => {
                            this.printMessage(this.saved);
                            this.loadRoles();
                        },
                        error: err => {
                            this.printMessage((err.error || `${err.status} ${err.statusText}`), true);
                        }
                    });
            })();
        } catch(e) {
            this.printMessage(e, true);
        }
    }

    onRoleContentChange(data) {
        this.modifiedRoles = data;
    }

    private loadRoles() {
        this.rolesService.read()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.rolesContent = JSON.stringify(res, undefined, '\t');
                    //this.rolesAceEditor && this.rolesAceEditor.forceContentChange(this.rolesContent);
                },
                error: (err) => {
                    this.printMessage(err.error || `${err.status} ${err.statusText}`, true);
                }
            });
    }

    private printMessage(message: string, err: boolean = false) {
        this.error = err;
        this.message = message;
    }
}
