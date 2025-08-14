/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { PrettyPrintPipe } from './prettyPrint.pipe';
import { stringify } from 'lossless-json';

const escapedLessThanSign = '&lt;';
const escapedGreaterThanSign = '&gt;';

describe('PrettyPrintPipe', () => {
    let pipe: PrettyPrintPipe;

    beforeEach(() => {
        pipe = new PrettyPrintPipe();
    });

    it('should return a pretty JSON string for a valid JSON object', () => {
        const input = '{"name":"John","age":30}';
        const result = pipe.transform(input);
        expect(result).toBe(`{
  "name": "John",
  "age": 30
}`);
    });

    it('should return a pretty array for an array of JSON strings', () => {
        const input = ['{"a": 1}', '{"b": 2 }'];
        const result = pipe.transform(input);
        expect(result).toBe(`[
{
  "a": 1
},
{
  "b": 2
}
]`);
    });

    it('should return a pretty array for an array of JSON strings that contains numbers as strings', () => {
        const input = ['{"a": "1"}', '{"b": "2" }'];
        const result = pipe.transform(input);
        expect(result).toBe(`[
{
  "a": "1"
},
{
  "b": "2"
}
]`);
    });

    it('should return escaped HTML when escapeHtml is true', () => {
        const input = '<script>alert("XSS")</script>';
        const result = pipe.transform(input, true);
        expect(result.trim()).toBe(`${escapedLessThanSign}script${escapedGreaterThanSign}alert(&quot;XSS&quot;)${escapedLessThanSign}/script${escapedGreaterThanSign}`);
    });

    it('should return a valid <img> tag for base64 image data', () => {
        const input = 'data:image/png;base64,someBase64String';
        const result = pipe.transform(input, true);
        expect(result).toBe(`<img src='data:image/png;base64,someBase64String' />`);
    });

    it('should return a valid download link for other base64 data', () => {
        const input = 'data:application/json;base64,eyJrZXkiOiJ2YWx1ZSJ9';
        const result = pipe.transform(input, true);
        expect(result).toBe(`<a href='data:application/json;base64,eyJrZXkiOiJ2YWx1ZSJ9' download='data'><span class='fa fa-fw fa-download'></span></a>`);
    });

    it('should return a valid <img> and <a> tags even in json object', () => {
        const input = {
            "img": "data:image/png;base64,someBase64String",
            "link": "data:application/json;base64,eyJrZXkiOiJ2YWx1ZSJ9",
        };
        const result = pipe.transform(stringify(input), true);
        expect(result).toContain(`<img src='data:image/png;base64,someBase64String' />`);
        expect(result).toContain(`<a href='data:application/json;base64,eyJrZXkiOiJ2YWx1ZSJ9' download='data'><span class='fa fa-fw fa-download'></span></a>`);
    });


    it('should format XML content', () => {
        const xml = '<note><to>User</to><from>Dev</from></note>';
        const result = pipe.transform(xml);
        expect(result).toContain('<note>');
        expect(result).toContain('<to>User</to>');
        expect(result).toContain('<from>Dev</from>');
    });

    it('should format and escape XML content', () => {
        const xml = '<note><to>User</to><from>Dev</from></note>';
        const result = pipe.transform(xml, true);
        expect(result).toContain(`${escapedLessThanSign}note${escapedGreaterThanSign}`);
        expect(result).toContain(`${escapedLessThanSign}to${escapedGreaterThanSign}User${escapedLessThanSign}/to${escapedGreaterThanSign}`);
        expect(result).toContain(`${escapedLessThanSign}from${escapedGreaterThanSign}Dev${escapedLessThanSign}/from${escapedGreaterThanSign}`);
    });

    it('should format and escape XML inside json ', () => {
        const xml = '<note><to>User</to><from>Dev</from></note>';
        const json = `
                      {
                      "note": "${xml}"
                      }
                      `
        const result = pipe.transform(json, true);
        expect(result).toContain(`${escapedLessThanSign}note${escapedGreaterThanSign}`);
        expect(result).toContain(`${escapedLessThanSign}to${escapedGreaterThanSign}User${escapedLessThanSign}/to${escapedGreaterThanSign}`);
        expect(result).toContain(`${escapedLessThanSign}from${escapedGreaterThanSign}Dev${escapedLessThanSign}/from${escapedGreaterThanSign}`);
    });

    it('should format and escape XML inside json inside array ', () => {
        const xml = '<note><to>User</to><from>Dev</from></note>';
        const array = `[
                          {
                          "note": "${xml}"
                          }
                      ]
                      `
        const result = pipe.transform(array, true);
        expect(result).toContain(`${escapedLessThanSign}note${escapedGreaterThanSign}`);
        expect(result).toContain(`${escapedLessThanSign}to${escapedGreaterThanSign}User${escapedLessThanSign}/to${escapedGreaterThanSign}`);
        expect(result).toContain(`${escapedLessThanSign}from${escapedGreaterThanSign}Dev${escapedLessThanSign}/from${escapedGreaterThanSign}`);
    });

    it('should format and escape XML inside array ', () => {
        const xml = '<note><to>User</to><from>Dev</from></note>';
        const array = `[
                         "${xml}"
                      ]
                      `
        const result = pipe.transform(array, true);
        expect(result).toContain(`${escapedLessThanSign}note${escapedGreaterThanSign}`);
        expect(result).toContain(`${escapedLessThanSign}to${escapedGreaterThanSign}User${escapedLessThanSign}/to${escapedGreaterThanSign}`);
        expect(result).toContain(`${escapedLessThanSign}from${escapedGreaterThanSign}Dev${escapedLessThanSign}/from${escapedGreaterThanSign}`);
    });

    it('should return empty string for null or undefined', () => {
        expect(pipe.transform(null)).toBe('');
        expect(pipe.transform(undefined)).toBe('');
    });
});
