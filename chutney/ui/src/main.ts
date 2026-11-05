/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import '@angular/localize/init';
import { enableProdMode } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { ChutneyAppModule } from './app/app.module';
import { environment } from '@env/environment';

registerLocaleData(localeFr);

if (environment.production) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(ChutneyAppModule)
  .catch(err => console.error(err));
