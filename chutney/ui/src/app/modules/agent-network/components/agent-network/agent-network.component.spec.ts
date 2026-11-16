/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Agent, AgentGraphe, AgentInfo, AgentNetwork, Environment, NetworkConfiguration, Target, TargetId } from '@model';
import { toAgentRows, toTargetMatrix, toTargetRows } from './agent-network.component';

describe('Agent network rows', () => {
    const local = new AgentInfo('local', 'localhost', 8443);
    const zoneB = new AgentInfo('zone-b', 'b', 8443);
    const zoneC = new AgentInfo('zone-c', 'c', 8443);
    const network = new AgentNetwork(
        new AgentGraphe([
            new Agent(local, ['zone-b'], [new TargetId('api', 'DEV')]),
            new Agent(zoneB, [], [new TargetId('db', 'DEV')]),
            new Agent(zoneC, [], [])
        ]),
        new NetworkConfiguration([local, zoneB, zoneC]),
        'local'
    );
    const environments = [
        new Environment('DEV', '', [new Target('api', 'http://api'), new Target('db', 'tcp://db:5432'), new Target('lost', 'http://lost')]),
        new Environment('PROD', '', [new Target('api', 'http://api')])
    ];

    it('gives each agent its status', () => {
        expect(toAgentRows(network).map(row => row.status)).toEqual(['local', 'reachable', 'unreachable']);
    });

    it('tells where each target runs, per environment', () => {
        const rows = toTargetRows(network, environments);
        expect(rows.map(row => [row.environment, row.name, row.reachable, row.runsOn])).toEqual([
            ['DEV', 'api', true, ''],
            ['DEV', 'db', true, 'zone-b'],
            ['DEV', 'lost', false, ''],
            ['PROD', 'api', false, '']
        ]);
    });

    it('groups targets in one row per name, one cell per environment', () => {
        const matrix = toTargetMatrix(toTargetRows(network, environments));
        expect(matrix.map(row => [row.name, Object.keys(row.cellByEnvironment), row.hasUnreachable])).toEqual([
            ['api', ['DEV', 'PROD'], true],
            ['db', ['DEV'], false],
            ['lost', ['DEV'], true]
        ]);
    });

    it('runs everything on this server when no network is saved', () => {
        const empty = new AgentNetwork(new AgentGraphe([]), new NetworkConfiguration([]));
        expect(toTargetRows(empty, environments).every(row => row.reachable && row.runsOn === '')).toBeTrue();
    });
});
