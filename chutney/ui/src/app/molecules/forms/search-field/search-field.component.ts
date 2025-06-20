/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

@Component({
    selector: 'chutney-forms-search-field',
    templateUrl: './search-field.component.html',
    styleUrls: ['./search-field.component.scss'],
    standalone: false
})
export class SearchFieldComponent implements OnInit {

    @Input() id: string;
    @Input() placeholder: string;
    @Output() searchInputEvent = new EventEmitter<string>();
    @Output() enterTriggerEvent = new EventEmitter<void>();
    @Input() searchInput:  string;

  constructor() { }

  ngOnInit() {
  }

  fireChangeEvent() {
    this.searchInputEvent.emit(this.searchInput);
  }

  enterTriggered() {
    this.enterTriggerEvent.emit()
  }

  clearSearchInput() {
    this.searchInputEvent.emit('');
  }
}
