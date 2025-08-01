/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnInit } from '@angular/core';
import { ValidationService } from '../../../../molecules/validation/validation.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LinkifierService } from '@core/services';
import { delay } from '@shared/tools';
import { Linkifier } from '@model';
import { Subject, takeUntil } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'chutney-config-linkifier',
    templateUrl: './linkifier.component.html',
    styleUrls: ['./linkifier.component.scss'],
    standalone: false
})
export class LinkifierComponent implements OnInit {

    linkifierForm: FormGroup;

    message;
    private savedMessage: string;
    private deletedMessage: string;
    isErrorNotification: boolean = false;

    linkifiers: Array<Linkifier> = [];

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(private fb: FormBuilder,
                private translate: TranslateService,
                private linkifierService: LinkifierService,
                private validationService: ValidationService) {
    }

    ngOnInit() {
        this.linkifierForm = this.fb.group({
            pattern: ['', Validators.required],
            link: ['', Validators.required],
        });

        this.loadLinkifiers();
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

    private loadLinkifiers() {
        this.linkifierService.loadLinkifiers()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (linkifiers: Array<Linkifier>) => {
                    this.linkifiers = linkifiers;
                },
                error: (error) => {
                    this.notify(error.error, true);
                }
            });
    }

    isValid(): boolean {
        return this.validationService.isValidPattern(this.linkifierForm.value['pattern'])
            && this.validationService.isNotEmpty(this.linkifierForm.value['pattern'])
            && this.validationService.isValidUrl(this.linkifierForm.value['link'])
            && this.validationService.isNotEmpty(this.linkifierForm.value['link']);
    }

    addLinkifier() {
        const linkifier = new Linkifier(this.linkifierForm.value['pattern'], this.linkifierForm.value['link']);
        this.linkifierService.add(linkifier)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.notify(this.savedMessage, false);
                    this.loadLinkifiers();
                },
                error: (error) => {
                    this.notify(error.error, true);
                }
            });
    }

    remove(linkifier: Linkifier, i: number) {
        this.linkifiers.splice(i);
        this.linkifierService.remove(linkifier)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.notify(this.deletedMessage, false);
                    this.loadLinkifiers();
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
}
