/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import '@angular/localize/init';
import { enableProdMode } from '@angular/core';
import { platformBrowser } from '@angular/platform-browser';

import { ChutneyAppModule } from './app/app.module';
import { environment } from '@env/environment';

if (environment.production) {
  enableProdMode();
}

platformBrowser().bootstrapModule(ChutneyAppModule)
  .catch(err => console.error(err));
