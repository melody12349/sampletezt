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

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;

import org.jetbrains.annotations.NotNull;

/**
 * Horizontal bar containing 2 text areas:
 * <li>File path area
 * <li>Line&Column number area
 */
class StatusBar extends HBox {
    private final Text filePath = new Text();
    private final Text lineColumnNumber = new Text();
    private final ToolBar rightToolBar = new ToolBar();

    private int currLineNumber = 1;
    private int currColumnNumber = 1;

    public StatusBar() {
        // Stop the tab key and the arrow keys from navigating through the controls
        addEventFilter(KeyEvent.ANY, keyEvent -> {
            if (keyEvent.getCode() == KeyCode.TAB
                    || keyEvent.getCode() == KeyCode.RIGHT
                    || keyEvent.getCode() == KeyCode.LEFT
                    || keyEvent.getCode() == KeyCode.UP
                    || keyEvent.getCode() == KeyCode.DOWN) {
                keyEvent.consume();
            }
        });

        filePath.setText("Source: Editor");
        lineColumnNumber.setText("Line: 1, Column: 1");

        rightToolBar.setId("statusBar");
        rightToolBar.getItems().add(lineColumnNumber);

        ToolBar leftToolBar = new ToolBar();
        leftToolBar.setId("statusBar");
        leftToolBar.getItems().add(filePath);

        getChildren().addAll(leftToolBar, rightToolBar);
        setHgrow(leftToolBar, Priority.ALWAYS);
        setHgrow(rightToolBar, Priority.NEVER);
    }

    /**
     * Defines the state of the line&column number area.
     *
     * @param show shows line&column number area if true, hides otherwise
     */
    public void showLineColumnNumberArea(boolean show) {
        ObservableList<Node> children = getChildren();
        if (show && !children.contains(rightToolBar)) {
            children.add(rightToolBar);
        } else if (!show){
            children.remove(rightToolBar);
        }
    }

    /**
     * Defines file path that is to be displayed.
     * If the path is empty, "Source: Editor" is shown.
     *
     * @param path file path to display
     */
    public void setFilePath(@NotNull String path) {
        if (!path.isEmpty()) {
            filePath.setText("Source: " + path);
        } else {
            filePath.setText("Source: Editor");
        }
    }

    /**
     * Defines line number that is to be displayed.
     *
     * @param lineNumber line number to display
     */
    public void setLineNumber(int lineNumber) {
        String text = "Line: " + lineNumber + ", Column: " + currColumnNumber;
        lineColumnNumber.setText(text);
        currLineNumber = lineNumber;
    }

    /**
     * Defines column number that is to be displayed.
     *
     * @param columnNumber column number to display
     */
    public void setColumnNumber(int columnNumber) {
        String text = "Line: " + currLineNumber + ", Column: " + columnNumber;
        lineColumnNumber.setText(text);
        currColumnNumber = columnNumber;
    }
}
