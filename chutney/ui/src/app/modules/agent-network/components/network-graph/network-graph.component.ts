/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, OnDestroy } from '@angular/core';
import { Agent, AgentGraphe, Environment } from '@model';
import { EnvironmentService } from '@core/services';
import { Subscription } from 'rxjs';

@Component({
    selector: 'chutney-network-graph',
    templateUrl: './network-graph.component.html',
    styleUrls: ['./network-graph.component.scss'],
})
export class NetworkGraphComponent implements OnDestroy {
    private serviceSubscription: Subscription = null;
    agentNodes: Array<Agent>;

    environments: Array<Environment> = [];
    targetReachByAgent = new Map<string, Array<string>>();
    targetFilter = '';

    public constructor(private environmentAdminService: EnvironmentService) {}

    ngOnDestroy(): void {
        this.serviceSubscription?.unsubscribe();
    }

    @Input() message: string;
    @Input()
    set agentGraphe(agentGraphe: AgentGraphe) {
        this.loadDescription(agentGraphe);
        this.loadUnreachableTarget();
    }

    private loadDescription(agentGraphe: AgentGraphe): void {
        this.agentNodes = agentGraphe.agents;
    }

    loadUnreachableTarget() {
        this.serviceSubscription = this.environmentAdminService.list().subscribe({
            next: (res) => {
                this.environments = res.sort((t1, t2) =>
                    t1.name.toUpperCase() > t2.name.toUpperCase() ? 1 : 0
                );
                this.targetReachByAgent = new Map<string, Array<string>>();

                this.agentNodes.forEach((agent) => {
                    agent.reachableTargets.forEach((target) => {
                        if (this.targetReachByAgent.has(target.name)) {
                            this.targetReachByAgent
                                .get(target.name)
                                .push(agent.info.name);
                        } else {
                            this.targetReachByAgent.set(target.name, [
                                agent.info.name
                            ]);
                        }
                    });
                });
            },
            error: (error) => console.log(error),
        });
    }
}
