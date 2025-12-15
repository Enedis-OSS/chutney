/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, inject, Input, OnDestroy, OnInit } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { Dataset } from "@core/model";
import { CampaignService } from "@core/services";
import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { TranslateService } from "@ngx-translate/core";
import { Observable, Subject, switchMap, takeUntil, timer } from "rxjs";

@Component({
    selector: 'replay-execution-with-jira-link',
    templateUrl: './replay-execution-with-jira-link.component.html',
    standalone: false
})
export class ReplayExecutionWithJiraLinkComponent implements OnInit, OnDestroy {

    activeModal = inject(NgbActiveModal);

    @Input() campaignId: number;
    @Input() environment: string;
    @Input() dataset: Dataset;
    @Input() campaignJiraId: string;
    @Input() executionJiraId: string;

    @Input() executeCallback: () => void;

    form: FormGroup;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(private campaignService: CampaignService,
                private formBuilder: FormBuilder,
                private translateService: TranslateService) {
    }

    ngOnInit(): void {
        this.form = this.formBuilder.group({
            selectedJiraId: [[], Validators.required],
            newJiraId: ''
        });
    }

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    onSubmit(): void {
        if (this.form.invalid) {
            return;
        }

        const selectedOption = this.form.get('selectedJiraId').value;

        if(selectedOption == "1") {
            this.campaignService.executeCampaign(this.campaignId, this.environment, this.dataset, this.campaignJiraId)
                        .pipe(takeUntil(this.unsubscribeSub$))
                        .subscribe()
        } else if(selectedOption == "2") {
            this.campaignService.executeCampaign(this.campaignId, this.environment, this.dataset, this.executionJiraId)
                        .pipe(takeUntil(this.unsubscribeSub$))
                        .subscribe()
        } else if(selectedOption == "3") {
            const newJiraId = this.form.get('newJiraId').value;
            if(newJiraId == "") {
                return;
            }
            this.campaignService.executeCampaign(this.campaignId, this.environment, this.dataset, newJiraId)
                        .pipe(takeUntil(this.unsubscribeSub$))
                        .subscribe()
        }

        this.executeCallback();
        this.activeModal.close()
    }
}
