package com.sqlines.studio.model.filehandler;

import com.sqlines.studio.model.tabsdata.ObservableTabsData;
import org.junit.Before;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FileHandlerTest {
    private FileHandler fileHandler;

    @Before
    public void init() {
        fileHandler = new FileHandler();
        ObservableTabsData tabsData = mock(ObservableTabsData.class);


    }
}