/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Pipe, PipeTransform } from '@angular/core';
import { stringify } from 'lossless-json'

@Pipe({
  name: 'stringify'
})
export class StringifyPipe implements PipeTransform {

  transform(value: any, args?: any): any {
    if (value instanceof Object) {
        return stringify(value, args?.replacer, args?.space);
    } else {
        return value;
    }
  }
}
