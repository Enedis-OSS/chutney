/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Pipe, PipeTransform } from '@angular/core';
import { SafeHtml, SafeValue } from '@angular/platform-browser';

@Pipe({
    name: 'thumbnail',
    standalone: false
})
export class ThumbnailPipe implements PipeTransform {

    constructor() {}

    public transform(value: SafeValue): SafeHtml {
        const doc = new DOMParser().parseFromString(value.toString(), 'text/html');
        const imgElements = doc.getElementsByTagName('img');
        for (let i = 0; i < imgElements.length; i++) {
            const imgElement = imgElements.item(i);
            imgElement.classList.add('img-thumbnail');
            imgElement.insertAdjacentHTML('beforebegin', '<a href="' + imgElement.src + '" target="_blank">' + imgElement.outerHTML + '</a>');
            imgElement.remove();
        }
        return doc.documentElement.innerHTML;
    }
}
