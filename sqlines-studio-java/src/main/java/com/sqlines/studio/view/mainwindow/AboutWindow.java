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

import com.sqlines.studio.view.Window;

import org.jetbrains.annotations.NotNull;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.net.URL;

/**
 * Provides a window with information about the application.
 */
class AboutWindow extends Window {

    /**
     * Creates a new {@link AboutWindow}.
     *
     * @throws IllegalStateException if SQLines Studio logo icon was not found in application resources
     */
    public AboutWindow() {
        setRoot(makeCentralNode());
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setTitle("About SQLines Studio");
        setHeight(240);
        setWidth(230);
        setResizable(false);
    }

    private @NotNull VBox makeCentralNode() {
        URL iconUrl = getClass().getResource("/icons/logo.png");
        if (iconUrl == null) {
            String errorMsg = "File not found in application resources: icons/logo.png";
            throw new IllegalStateException(errorMsg);
        }

        ImageView logoImg = new ImageView(new Image(iconUrl.toExternalForm()));
        logoImg.setFitHeight(115);
        logoImg.setFitWidth(110);

        Text appInfo = new Text("SQLines Studio\n  Version: 3.0");
        Text copyrightInfo = new Text("© 2021 SQLines\nAll rights reserved");

        VBox infoLayout = new VBox(appInfo, copyrightInfo);
        infoLayout.setAlignment(Pos.CENTER);
        infoLayout.setSpacing(5);

        VBox copyrightLayout = new VBox(copyrightInfo);
        copyrightLayout.setAlignment(Pos.CENTER);

        VBox mainLayout = new VBox(logoImg, infoLayout, copyrightLayout);
        mainLayout.setId("aboutWindow");
        mainLayout.setSpacing(10);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(50, 0, 60, 0));

        return mainLayout;
    }
}
