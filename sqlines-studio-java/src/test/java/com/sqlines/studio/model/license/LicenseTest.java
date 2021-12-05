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

package com.sqlines.studio.model.license;

import com.sqlines.studio.model.CoreProcess;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class LicenseTest {
    private License license;
    private CoreProcess coreProcess;

    @Before
    public void setUp() {
        coreProcess = mock(CoreProcess.class);


        String path = getClass().getResource("/license.txt").getPath();
        path = path.substring(0, path.lastIndexOf("/"));
        System.setProperty("model.app-dir", path);

        license = new License(coreProcess);
    }

    @Test
    public void isActiveShouldReturnFalseWhenActiveIsNotActive() {
        assertThat(license.isActive(), equalTo(false));
    }
}