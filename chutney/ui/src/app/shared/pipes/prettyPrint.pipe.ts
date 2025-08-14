/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { Pipe, PipeTransform } from '@angular/core';
import { escapeHtml } from '@shared/tools/string-utils';
import { isLosslessNumber, parse, stringify } from 'lossless-json';

@Pipe({
    name: 'prettyPrint',
    standalone: false
})
export class PrettyPrintPipe implements PipeTransform {
    transform(value: any, escapeHtmlFlag: boolean = false): string {
        if (Array.isArray(value)) {
            return this.formatArray(value, escapeHtmlFlag);
        }
        return value != null ? this.format(value, escapeHtmlFlag) : '';
    }

    private formatArray(array: any[], escapeHtmlFlag: boolean): string {
        return `[\n${array.map(item => this.format(item, escapeHtmlFlag)).join(',\n')}\n]`;
    }

    private format(content: any, escapeHtmlFlag: boolean = false): string {
        let result: any;
        try {
            const parsed = parse(content);
            if (typeof parsed === 'string') {
                content = parsed;
                result = this.formatPrimitives(content, escapeHtmlFlag);
            } else if (isLosslessNumber(parsed)) {
                result = content.valueOf();
            } else if (Array.isArray(parsed)) {
                result = this.formatArray(parsed.map(item => stringify(item)), escapeHtmlFlag);
            } else {
                result = this.formatObject(parsed, escapeHtmlFlag);
            }
        } catch {
            result = this.formatPrimitives(content, escapeHtmlFlag);
        }

        return result;
    }


    private formatObject(obj: Record<string, any>, escapeHtmlFlag: boolean): string {
        const beautified = Object.fromEntries(
            Object.entries(obj).map(([key, value]) => [key, this.format(value, escapeHtmlFlag)])
        );
        return stringify(beautified, null, '  ');
    }


    private formatPrimitives(content: any, escapeHtmlFlag: boolean): any {
        let result: any;
        if (typeof content === 'string') {
            result = this.formatString(content, escapeHtmlFlag);
        } else if (isLosslessNumber(content)) {
            result = content.valueOf();
        } else {
            result = content;
        }
        return result;

    }

    private formatString(content: string, escapeHtmlFlag: boolean): string {
        let result = content;
        let escapeHtmlLocalFlag = escapeHtmlFlag;
        if (content.startsWith('data:image')) {
            result = `<img src='${content}' />`;
            escapeHtmlLocalFlag = false;
        } else if (content.startsWith('data:')) {
            result = `<a href='${content}' download='data'><span class='fa fa-fw fa-download'></span></a>`;
            escapeHtmlLocalFlag = false;
        } else if (content.startsWith('<') || content.includes('<?xml')) {
            result = this.formatXml(content, '  ');
        }

        return escapeHtmlLocalFlag ? escapeHtml(result) : result;
    }

    private formatXml(input: string, indent: string = '\t'): string {
        const xmlString = input
            .replace(/(<([a-zA-Z]+\b)[^>]*>)(?!<\/\2>|[\w\s])/g, '$1\n')
            .replace(/(<\/[a-zA-Z]+[^>]*>)/g, '$1\n')
            .replace(/>\s+(.+?)\s+<(?!\/)/g, '>\n$1\n<')
            .replace(/>(.+?)<([a-zA-Z])/g, '>\n$1\n<$2')
            .replace(/\?></, '?>\n<');

        const xmlLines = xmlString.split('\n');
        let tabs = '';
        let start = /^<\?xml/.test(xmlLines[0]) ? 1 : 0;

        for (let i = start; i < xmlLines.length; i++) {
            const line = xmlLines[i].trim();

            if (/^<\/.*/.test(line)) {
                tabs = tabs.slice(0, -indent.length);
            } else if (/^<.*>.*<\/.*>|<.*[^>]*\/>/.test(line)) {
                // No change to tabs
            } else if (/^<.*>/.test(line)) {
                tabs += indent;
            }
            xmlLines[i] = tabs + line;
        }

        return xmlLines.join('\n');
    }
}
