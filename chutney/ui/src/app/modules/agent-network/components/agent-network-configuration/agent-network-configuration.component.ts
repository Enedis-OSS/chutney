/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';
import { AgentInfo, NetworkConfiguration } from '@model';
import { AgentNetworkService } from '@core/services';
import { Subscription } from 'rxjs';

@Component({
    selector: 'chutney-agent-network-configuration',
    templateUrl: './agent-network-configuration.component.html',
    styleUrls: ['./agent-network-configuration.component.scss'],
    standalone: false
})
export class AgentNetworkConfigurationComponent implements OnDestroy {
    @Input() currentConfiguration: NetworkConfiguration;
    @Output() configurationUpdate = new EventEmitter();

    private agentSubscription: Subscription = null;

    constructor(private agentService: AgentNetworkService) {}

    ngOnDestroy(): void {
       this.agentSubscription?.unsubscribe();
    }

    removeAgent(configurationAgent) {
        const index =
            this.currentConfiguration.agentNetworkConfiguration.indexOf(
                configurationAgent
            );
        this.currentConfiguration.agentNetworkConfiguration.splice(index, 1);
    }

    save() {
        this.configurationUpdate.emit('Propagation en cours');
        this.agentSubscription = this.agentService
            .sendAndSaveConfiguration(this.currentConfiguration)
            .subscribe({
                next: (res) =>
                    this.configurationUpdate.emit('Propagation terminée'),
                error: (error) =>
                    this.configurationUpdate.emit('Erreur: ' + error),
            });
    }

    addServer() {
        this.currentConfiguration.agentNetworkConfiguration.push(
            new AgentInfo('', '', 8350)
        );
    }
}
