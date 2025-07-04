/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PaginationInstance } from 'ngx-pagination';

import { Table } from '@model';

@Component({
    selector: 'chutney-table-panel',
    templateUrl: './table-panel.component.html',
    styleUrls: ['./table-panel.component.scss'],
    standalone: false
})
export class TablePanelComponent {

  @Input() table: Table;
  @Input() paginationInstanceConfig: PaginationInstance;
  @Input() paginationControlConfig: Object = {
    maxSize: 7,
    directionLinks: true,
    autoHide: false,
    previousLabel: '',
    nextLabel: '',
    screenReaderPaginationLabel: 'pagination',
    screenReaderPageLabel: 'page',
    screenReaderCurrentLabel: 'current'
  };

  @Output() pageChangeEvent = new EventEmitter();

  pageChange(event: number) {
    this.pageChangeEvent.emit(event);
  }
}
