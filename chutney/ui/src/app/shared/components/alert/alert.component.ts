/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

 import { Component, inject } from '@angular/core';
 import { AlertService } from '@shared/alert.service';

 @Component({
   selector: 'chutney-alert',
   templateUrl: './alert.component.html',
   styleUrl: './alert.component.scss',
   standalone: false
 })
 export class AlertComponent {

   readonly alertService = inject(AlertService);
 }
