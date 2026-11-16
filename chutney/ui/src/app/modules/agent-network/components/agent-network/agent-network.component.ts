/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { AgentInfo, AgentNetwork, Environment, NetworkConfiguration } from '@model';
import { AgentNetworkService, EnvironmentService } from '@core/services';

export type AgentStatus = 'local' | 'reachable' | 'unreachable' | 'unchecked';

export interface AgentRow {
    info: AgentInfo;
    status: AgentStatus;
}

export interface TargetRow {
    environment: string;
    name: string;
    url: string;
    reachable: boolean;
    // Empty when the step runs on this server
    runsOn: string;
}

export interface TargetMatrixRow {
    name: string;
    cellByEnvironment: Record<string, TargetRow>;
    hasUnreachable: boolean;
}

export function toTargetMatrix(targets: TargetRow[]): TargetMatrixRow[] {
    const rowByName = new Map<string, TargetMatrixRow>();
    targets.forEach(target => {
        const row = rowByName.get(target.name) ?? {name: target.name, cellByEnvironment: {}, hasUnreachable: false};
        row.cellByEnvironment[target.environment] = target;
        row.hasUnreachable ||= !target.reachable;
        rowByName.set(target.name, row);
    });
    return [...rowByName.values()].sort((a, b) => a.name.localeCompare(b.name));
}

export function toAgentRows(network: AgentNetwork): AgentRow[] {
    const reached = new Set(network.graphe.agents.flatMap(agent => agent.reachableAgents));
    return network.networkConfiguration.agentNetworkConfiguration.map(info => ({
        info,
        status: info.name === network.localAgentName ? 'local' : reached.has(info.name) ? 'reachable' : 'unreachable'
    }));
}

export function toTargetRows(network: AgentNetwork, environments: Environment[]): TargetRow[] {
    const agents = network.graphe.agents;
    return environments.flatMap(environment => environment.targets.map(target => {
        const reachedBy = agents
            .filter(agent => agent.reachableTargets.some(t => t.name === target.name && t.environment === environment.name))
            .map(agent => agent.info.name);
        const local = agents.length === 0 || reachedBy.includes(network.localAgentName);
        return {
            environment: environment.name,
            name: target.name,
            url: target.url,
            reachable: local || reachedBy.length > 0,
            runsOn: local ? '' : reachedBy.join(', ')
        };
    }));
}

@Component({
    selector: 'chutney-agent-network',
    templateUrl: './agent-network.component.html',
    standalone: false
})
export class AgentNetworkComponent implements OnInit {
    private readonly agentService = inject(AgentNetworkService);
    private readonly environmentService = inject(EnvironmentService);
    private readonly destroyRef = inject(DestroyRef);

    private configuration: NetworkConfiguration;
    agents: AgentRow[] = [];
    environmentNames: string[] = [];
    targets: TargetMatrixRow[] = [];
    lastChecked: string = null;

    help = false;
    saving = false;
    error: string = null;
    targetFilter = '';
    onlyUnreachable = false;

    ngOnInit(): void {
        this.load();
    }

    get canSave(): boolean {
        return !this.saving && this.agents.every(({info}) => info.name && info.host && info.port);
    }

    addAgent(): void {
        this.agents.push({info: new AgentInfo('', '', 8443), status: 'unchecked'});
    }

    removeAgent(row: AgentRow): void {
        this.agents = this.agents.filter(agent => agent !== row);
    }

    save(): void {
        this.saving = true;
        this.error = null;
        this.agentService
            .sendAndSaveConfiguration({...this.configuration, agentNetworkConfiguration: this.agents.map(agent => agent.info)})
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: () => this.load(),
                error: (error) => this.fail(error)
            });
    }

    private load(): void {
        forkJoin([this.agentService.getDescription(), this.environmentService.listTargets()])
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: ([network, environments]) => {
                    this.saving = false;
                    this.configuration = network.networkConfiguration;
                    this.agents = toAgentRows(network);
                    this.environmentNames = environments.map(environment => environment.name);
                    this.targets = toTargetMatrix(toTargetRows(network, environments));
                    this.lastChecked = this.agents.length > 0 ? network.networkConfiguration.creationDate : null;
                },
                error: (error) => this.fail(error)
            });
    }

    private fail(error: any): void {
        this.saving = false;
        this.error = typeof error?.error === 'string' && error.error ? error.error : (error?.message ?? String(error));
    }
}
