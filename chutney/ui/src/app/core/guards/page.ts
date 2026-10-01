/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Directive, HostListener } from '@angular/core';

@Directive()
export abstract class CanDeactivatePage {
  abstract canDeactivatePage(): boolean;

  @HostListener('window:beforeunload', ['$event'])
  unloadNotification($event: any) {
    if (!this.canDeactivatePage()) {
      $event.preventDefault();
      $event.returnValue = true;
    }
  }
}
