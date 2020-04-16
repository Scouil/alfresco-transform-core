/*
 * #%L
 * Alfresco Transform Core
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * -
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * -
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * -
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * -
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.transformer.executors;

import java.util.HashMap;
import java.util.Map;

/**
 * CommandExecutor implementation for running ImageMagick transformations. It runs the
 * transformation logic as a separate Shell process.
 */
public class ImageMagickCommandExecutor extends AbstractCommandExecutor
{
    private final String ROOT;
    private final String DYN;
    private final String EXE;

    public ImageMagickCommandExecutor(String exe, String dyn, String root)
    {
        if (exe == null || exe.isEmpty())
        {
            throw new IllegalArgumentException("ImageMagickCommandExecutor EXE variable cannot be null or empty");
        }
        if (dyn == null || dyn.isEmpty())
        {
            throw new IllegalArgumentException("ImageMagickCommandExecutor DYN variable cannot be null or empty");
        }
        if (root == null || root.isEmpty())
        {
            throw new IllegalArgumentException("ImageMagickCommandExecutor ROOT variable cannot be null or empty");
        }
        this.EXE = exe;
        this.DYN = dyn;
        this.ROOT = root;

        super.transformCommand = createTransformCommand();
        super.checkCommand = createCheckCommand();
    }

    public static final String LICENCE = "This transformer uses ImageMagick from ImageMagick Studio LLC. See the license at http://www.imagemagick.org/script/license.php or in /ImageMagick-license.txt";

    @Override
    protected RuntimeExec createTransformCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*",
            new String[]{EXE, "${source}", "SPLIT:${options}", "-strip", "-quiet", "${target}"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        Map<String, String> processProperties = new HashMap<>();
        processProperties.put("MAGICK_HOME", ROOT);
        processProperties.put("DYLD_FALLBACK_LIBRARY_PATH", DYN);
        processProperties.put("LD_LIBRARY_PATH", DYN);
        runtimeExec.setProcessProperties(processProperties);

        Map<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("options", null);
        runtimeExec.setDefaultProperties(defaultProperties);

        runtimeExec.setErrorCodes(
            "1,2,255,400,405,410,415,420,425,430,435,440,450,455,460,465,470,475,480,485,490,495,499,700,705,710,715,720,725,730,735,740,750,755,760,765,770,775,780,785,790,795,799");

        return runtimeExec;
    }

    @Override
    protected RuntimeExec createCheckCommand()
    {
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[]{EXE, "-version"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);
        return runtimeExec;
    }
}
