/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { AgentNetwork, NetworkConfiguration } from '@model';
import { AgentNetworkService } from '@core/services';
import { Subscription } from 'rxjs';

@Component({
    selector: 'chutney-agent-network',
    templateUrl: './agent-network.component.html',
    styleUrls: ['./agent-network.component.scss'],
})
export class AgentNetworkComponent implements OnInit, OnDestroy {
    private agentSubscription: Subscription = null;
    currentConfiguration = new NetworkConfiguration([]);
    description: AgentNetwork;
    errorMessage: any;
    messages: string;

    constructor(private agentService: AgentNetworkService) {}

    ngOnInit(): void {
        this.loadAll();
    }

    ngOnDestroy(): void {
        this.agentSubscription?.unsubscribe();
    }

    loadAll(): void {
        this.agentSubscription = this.agentService.getDescription().subscribe({
            next: (description) => {
                this.description = description;
            },
            error: (error) => {
                this.messages = error.error;
            }
        });
    }

    propagationDone(message: string) {
        this.messages = message;
        this.loadAll();
    }

    loadDescription(description: AgentNetwork): void {
        this.description = description;
    }
}
