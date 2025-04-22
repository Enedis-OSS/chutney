/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, OnChanges, OnDestroy } from '@angular/core';

import { CampaignService } from '@core/services';
import { Authorization, Campaign } from '@model';
import { Subscription } from 'rxjs';

@Component({
    selector: 'chutney-scenario-campaigns',
    templateUrl: './scenario-campaigns.component.html',
    styleUrls: ['./scenario-campaigns.component.scss']
})
export class ScenarioCampaignsComponent implements OnChanges, OnDestroy {

    @Input() idScenario: string;
    campaignsForScenario: Array<Campaign> = [];

    Authorization = Authorization;
    private campaignServiceSubscription: Subscription;

    constructor(private campaignService: CampaignService) {
    }

    ngOnChanges() {
        if (this.idScenario) {
            this.load(this.idScenario);
        }
    }

    ngOnDestroy(): void {
        this.campaignServiceSubscription?.unsubscribe();
    }

    load(id) {
        this.campaignServiceSubscription = this.campaignService.findAllCampaignsForScenario(id).subscribe({
            next: (response) => {
                this.campaignsForScenario = response;
            },
            error: (error) => {
                console.log(error);
            }
        });
    }
}
