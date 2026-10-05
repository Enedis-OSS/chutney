/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { ElementRef, SimpleChange } from '@angular/core';
import { Ace } from 'ace-builds';

import { ChutneyEditorComponent } from './chutney-editor.component';

describe('ChutneyEditorComponent', () => {
    let component: ChutneyEditorComponent;
    let editorElement: HTMLElement;
    let aceEditor: Ace.Editor;

    beforeEach(() => {
        editorElement = document.createElement('div');
        document.body.appendChild(editorElement);

        component = new ChutneyEditorComponent();
        component['editorHtmlElement'] = new ElementRef(editorElement);
        component.modes = ['hjson'];
        component.content = 'initial content';
        component.ngOnInit();
        component.ngAfterViewInit();
        aceEditor = component['aceEditor'];
    });

    afterEach(() => {
        aceEditor.destroy();
        editorElement.remove();
    });

    it('should replace editor content when content changes from outside', () => {
        setContent('new content');

        expect(aceEditor.getValue()).toEqual('new content');
    });

    it('should keep cursor when content given back is already in the editor', () => {
        aceEditor.moveCursorTo(0, 7);
        aceEditor.insert('abc');
        spyOn(aceEditor.session, 'setValue').and.callThrough();

        setContent(aceEditor.getValue());

        expect(aceEditor.session.setValue).not.toHaveBeenCalled();
        expect(aceEditor.getValue()).toEqual('initialabc content');
        expect(aceEditor.getCursorPosition()).toEqual({ row: 0, column: 10 });
    });

    it('should emit content once per change', () => {
        setContent('new content');
        setContent('other content');
        const emitted: string[] = [];
        component.onContentChange.subscribe(content => emitted.push(content));

        aceEditor.insert('!');

        expect(emitted).toEqual(['!other content']);
    });

    function setContent(content: string) {
        const previousContent = component.content;
        component.content = content;
        component.ngOnChanges({ content: new SimpleChange(previousContent, content, false) });
    }
});
