/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { AgentInfo } from '@model';

@Component({
    selector: 'chutney-agent',
    templateUrl: './agent.component.html',
    styleUrls: ['./agent.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AgentComponent {

  @Input() configurationAgent: AgentInfo;
  @Output() AgentRemoved = new EventEmitter<AgentInfo>();

  removeAgent() {
    this.AgentRemoved.emit(this.configurationAgent);
  }
}
