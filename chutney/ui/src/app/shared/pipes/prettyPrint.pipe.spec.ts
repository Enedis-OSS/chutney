/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { PrettyPrintPipe } from './prettyPrint.pipe';

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
        const input = ['{"a":1}', '{"b":2}'];
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

    it('should return escaped HTML when escapeHtml is true', () => {
        const input = '<script>alert("XSS")</script>';
        const result = pipe.transform(input, true);
        expect(result.trim()).toBe('&lt;script&gt;alert(&quot;XSS&quot;)&lt;/script&gt;');
    });

    it('should return an <img> tag for base64 image data', () => {
        const input = 'data:image/png;base64,someBase64String';
        const result = pipe.transform(input);
        expect(result).toBe('<img src="data:image/png;base64,someBase64String" />');
    });

    it('should return a download link for other base64 data', () => {
        const input = 'data:application/json;base64,eyJrZXkiOiJ2YWx1ZSJ9';
        const result = pipe.transform(input);
        expect(result).toBe('<a href="data:application/json;base64,eyJrZXkiOiJ2YWx1ZSJ9" >download information data</a>');
    });

    it('should format XML content', () => {
        const xml = '<note><to>User</to><from>Dev</from></note>';
        const result = pipe.transform(xml);
        expect(result).toContain('<note>');
        expect(result).toContain('<to>User</to>');
        expect(result).toContain('<from>Dev</from>');
    });

    it('should return empty string for null or undefined', () => {
        expect(pipe.transform(null)).toBe('');
        expect(pipe.transform(undefined)).toBe('');
    });
});
