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

package com.sqlines.studio.view;

import org.jetbrains.annotations.NotNull;

import javafx.geometry.Insets;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.net.URL;

/**
 * Provides a message for informing the user about an occurred error.
 */
public class ErrorWindow extends Window {

    /**
     * Creates a new {@link ErrorWindow} with the specified window title and message.
     *
     * @param title window title to set
     * @param message error message to show
     *
     * @throws IllegalStateException if error icon was not found in application resources
     */
    public ErrorWindow(@NotNull String title, @NotNull String message) {
        setRoot(makeCentralNode(message));
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setTitle(title);
        sizeToScene();
        setResizable(false);
    }
    
    private @NotNull BorderPane makeCentralNode(@NotNull String message) {
        URL iconUrl = getClass().getResource("/icons/error.png");
        if (iconUrl == null) {
            String errorMsg = "File not found in application resources: icons/error.png";
            throw new IllegalStateException(errorMsg);
        }

        ImageView icon = new ImageView(new Image(iconUrl.toExternalForm()));
        icon.setFitWidth(60);
        icon.setFitHeight(60);

        Button okButton = new Button("Ok");
        okButton.setOnAction( event -> close() );

        HBox imageLayout = new HBox(icon);
        imageLayout.setPadding(new Insets(15, 15, 15, 10));

        VBox textLayout = new VBox(new Text(message));
        textLayout.setSpacing(15);
        textLayout.setPadding(new Insets(30, 15, 15, 0));

        HBox buttonLayout = new HBox(okButton);
        buttonLayout.setPadding(new Insets(0, 0, 0, 6));

        BorderPane mainLayout = new BorderPane();
        mainLayout.setId("errorWindow");
        mainLayout.setLeft(imageLayout);
        mainLayout.setCenter(textLayout);
        mainLayout.setBottom(new ToolBar(buttonLayout));

        return mainLayout;
    }
}
