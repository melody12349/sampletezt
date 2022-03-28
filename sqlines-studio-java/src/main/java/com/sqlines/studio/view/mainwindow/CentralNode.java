/*
 * Copyright (c) 2021 SQLines
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sqlines.studio.view.mainwindow;

import com.sqlines.studio.view.mainwindow.editor.CodeEditor;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;

import org.jetbrains.annotations.NotNull;

/**
 * Central control in the main window.
 * <p>
 * Contains 2 text input fields for source data and target data, respectively.
 */
class CentralNode extends HBox {

    /**
     * Represents currently focused text input field.
     */
    public enum FieldInFocus { SOURCE, TARGET }

    /**
     * An enumeration denoting the policy to be used by a CentralNode
     * in deciding whether to show a target text input field.
     */
    public enum TargetFieldPolicy { ALWAYS, AS_NEEDED }

    private final CodeEditor sourceEditor = new CodeEditor();
    private final CodeEditor targetEditor = new CodeEditor();

    public CentralNode() {
        getChildren().addAll(sourceEditor);
        setSpacing(10);
        setPadding(new Insets(5, 5, 5, 5));

        // Show the target text input field only if there is data in it
        targetEditor.addTextListener((observable, oldText, newText) -> {
            if (!newText.isEmpty() && !getChildren().contains(targetEditor)) {
                getChildren().add(targetEditor);
            }
        });
    }

    /**
     * Replaces the entire content of the source text input field with the given text.
     *
     * @param text text to set
     */
    public void setSourceText(@NotNull String text) {
       sourceEditor.setText(text);
    }

    /**
     * Replaces the entire content of the target text input field with the given text.
     *
     * @param text text to set
     */
    public void setTargetText(@NotNull String text) {
        targetEditor.setText(text);
    }

    /**
     * @return the index of the current line in the source text input field
     */
    public int getSourceLineIndex() {
        return sourceEditor.getLineIndex();
    }

    /**
     * @return the index of the current line in the target text input field
     */
    public int getTargetLineIndex() {
        return targetEditor.getLineIndex();
    }

    /**
     * @return the index of the current column in the source text input field
     */
    public int getSourceColumnIndex() {
        return sourceEditor.getColumnIndex();
    }

    /**
     * @return the index of the current column in the target text input field
     */
    public int getTargetColumnIndex() {
        return targetEditor.getColumnIndex();
    }

    /**
     * Erases the last change done in the currently focused text input field.
     * Does nothing if none of the input fields are in focus.
     */
    public void undo() {
        if (sourceEditor.hasFocus()) {
            sourceEditor.undo();
        } else if (targetEditor.hasFocus()) {
            targetEditor.undo();
        }
    }

    /**
     * Restores any actions that were previously undone using an undo
     * in the currently focused text input field.
     * Does nothing if none of the input fields are in focus.
     */
    public void redo() {
        if (sourceEditor.hasFocus()) {
            sourceEditor.redo();
        } else if (targetEditor.hasFocus()) {
            targetEditor.redo();
        }
    }

    /**
     * Selects everything in the area in the currently focused text input field.
     * Does nothing if none of the input fields are in focus.
     */
    public void selectAll() {
        if (sourceEditor.hasFocus()) {
            sourceEditor.selectAll();
        } else if (targetEditor.hasFocus()) {
            targetEditor.selectAll();
        }
    }

    /**
     * Transfers the currently selected text in the currently focused text input field
     * to the clipboard, removing the current selection.
     * Does nothing if none of the input fields are in focus.
     */
    public void cut() {
        if (sourceEditor.hasFocus()) {
            sourceEditor.cut();
        } else if (targetEditor.hasFocus()) {
            targetEditor.cut();
        }
    }

    /**
     * Transfers the currently selected text in the currently focused text input field
     * to the clipboard, leaving the current selection.
     * Does nothing if none of the input fields are in focus.
     */
    public void copy() {
        if (sourceEditor.hasFocus()) {
            sourceEditor.copy();
        } else if (targetEditor.hasFocus()) {
            targetEditor.copy();
        }
    }

    /**
     * Inserts the content from the clipboard into this currently focused text input field,
     * replacing the current selection.
     * If there is no selection, the content from the clipboard is inserted
     * at the current caret position of the currently focused text input field.
     * Does nothing if none of the input fields are in focus.
     */
    public void paste() {
        if (sourceEditor.hasFocus()) {
            sourceEditor.paste();
        } else if (targetEditor.hasFocus()) {
            targetEditor.paste();
        }
    }

    /**
     * @return true if undo is available for use in the currently focused
     * text input field, false otherwise
     */
    public boolean isUndoAvailable() {
        if (sourceEditor.hasFocus()) {
            return sourceEditor.isUndoAvailable();
        } else if (targetEditor.hasFocus()) {
            return targetEditor.isUndoAvailable();
        } else {
            return false;
        }
    }

    /**
     * @return true if redo is available for use in the currently focused
     * text input field, false otherwise
     */
    public boolean isRedoAvailable() {
        if (sourceEditor.hasFocus()) {
            return sourceEditor.isRedoAvailable();
        } else if (targetEditor.hasFocus()) {
            return targetEditor.isRedoAvailable();
        } else {
            return false;
        }
    }

    /**
     * Increases the font size of the source and target text input fields by 1 px.
     */
    public void zoomIn() {
        sourceEditor.zoomIn();
        targetEditor.zoomIn();
    }

    /**
     * Decreases the font size of the source and target text input fields by 1 px.
     */
    public void zoomOut() {
        sourceEditor.zoomOut();
        targetEditor.zoomOut();
    }

    /**
     * Requests focus for the specified text input field.
     *
     * @param inFocus text input field to focus on
     */
    public void focusOn(@NotNull CentralNode.FieldInFocus inFocus) {
       if (inFocus == FieldInFocus.SOURCE) {
            sourceEditor.requestFocus();
        } else if (inFocus == FieldInFocus.TARGET) {
            targetEditor.requestFocus();
        }
    }

    /**
     * Sets the {@link TargetFieldPolicy}.
     * <p>
     * The default value is {@link TargetFieldPolicy#AS_NEEDED}.
     *
     * @param policy target field policy to set
     */
    public void setTargetFieldPolicy(@NotNull TargetFieldPolicy policy) {
        if (policy == TargetFieldPolicy.ALWAYS) {
            if (!getChildren().contains(targetEditor)) {
                getChildren().add(targetEditor);
            }
        } else if (policy == TargetFieldPolicy.AS_NEEDED) {
            if (targetEditor.getText().isEmpty()) {
                getChildren().remove(targetEditor);
            }
        }
    }

    /**
     * Sets the {@link CodeEditor.WrappingPolicy} of the text input fields.
     * <p>
     * The default value is {@link CodeEditor.WrappingPolicy#NO_WRAP}.
     *
     * @param policy wrapping policy to set
     */
    public void setWrappingPolicy(@NotNull CodeEditor.WrappingPolicy policy) {
        sourceEditor.setWrappingPolicy(policy);
        targetEditor.setWrappingPolicy(policy);
    }

    /**
     * Sets the {@link CodeEditor.HighlighterPolicy} of the text input fields.
     * <p>
     * The default value is {@link CodeEditor.HighlighterPolicy#HIGHLIGHT}.
     *
     * @param policy highlighter policy to set
     */
    public void setHighlighterPolicy(@NotNull CodeEditor.HighlighterPolicy policy) {
        sourceEditor.setHighlighterPolicy(policy);
        targetEditor.setHighlighterPolicy(policy);
    }

    /**
     * Sets the {@link CodeEditor.LineNumbersPolicy} of the text input fields.
     * <p>
     * The default value is {@link CodeEditor.LineNumbersPolicy#SHOW}.
     *
     * @param policy line numbers policy to set
     */
    public void setLineNumbersPolicy(@NotNull CodeEditor.LineNumbersPolicy policy) {
        sourceEditor.setLineNumbersPolicy(policy);
        targetEditor.setLineNumbersPolicy(policy);
    }
    
    /**
     * Adds a listener which will be notified when the text
     * in the source text input field changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addSourceTextListener(@NotNull ChangeListener<String> listener) {
        sourceEditor.addTextListener(listener);
    }

    /**
     * Adds a listener which will be notified when the text
     * in the target text input field changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addTargetTextListener(@NotNull ChangeListener<String> listener) {
        targetEditor.addTextListener(listener);
    }

    /**
     * Adds a listener which will be notified when the current line index
     * in the source text input field changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addSourceLineListener(@NotNull ChangeListener<Integer> listener) {
        sourceEditor.addLineListener(listener);
    }

    /**
     * Adds a listener which will be notified when the current line index
     * in the target text input field changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addTargetLineListener(@NotNull ChangeListener<Integer> listener) {
        targetEditor.addLineListener(listener);
    }

    /**
     * Adds a listener which will be notified when the current column index
     * in the source text input field changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addSourceColumnListener(@NotNull ChangeListener<Integer> listener) {
        sourceEditor.addColumnListener(listener);
    }

    /**
     * Adds a listener which will be notified when the current column index
     * in the target text input field changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addTargetColumnListener(@NotNull ChangeListener<Integer> listener) {
        targetEditor.addColumnListener(listener);
    }

    /**
     * Adds a listener which will be notified when the source text input field focus changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addSourceFocusListener(@NotNull ChangeListener<Boolean> listener) {
        sourceEditor.addFocusListener(listener);
    }

    /**
     * Adds a listener which will be notified when the target text input field focus changes.
     * If the same listener is added more than once, then it will be notified more than once.
     *
     * @param listener the listener to register
     */
    public void addTargetFocusListener(@NotNull ChangeListener<Boolean> listener) {
        targetEditor.addFocusListener(listener);
    }
}
