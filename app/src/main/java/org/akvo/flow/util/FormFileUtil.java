/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.akvo.flow.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class FormFileUtil {

    private static final String DIR_FORMS = "forms";

    @Inject
    public FormFileUtil() {
    }

    @NonNull
    public File getFormsFolder(Context context) {
        String path = FileUtil.getInternalFolderPath(context, DIR_FORMS);
        return FileUtil.createDir(path);
    }

    @NonNull
    public File findFormFile(Context context, String formFileName) {
        File file = getAppInternalFormFileIfExists(context, formFileName);
        if (file == null) {
            file = getAppExternalFormFileIfExists(context, formFileName);
        }
        if (file == null) {
            file = getPublicFormFile(formFileName);
        }
        return file;
    }

    @NonNull
    public List<File> findAllPossibleFormFolders(Context context) {
        List<File> folders = new ArrayList<>(3);
        File folder = getAppInternalFormsDir(context);
        if (folder.exists()) {
            folders.add(folder);
        }
        File folder2 = getAppExternalFormsDir(context);
        if (folder2 != null && folder2.exists()) {
            folders.add(folder2);
        }
        File folder3 = getPublicFormsDir();
        if (folder3.exists()) {
            folders.add(folder3);
        }
        return folders;
    }

    @NonNull
    private File getPublicFormFile(String formFileName) {
        return new File(getPublicFolderPath(), formFileName);
    }

    @Nullable
    private File getAppInternalFormFileIfExists(Context context, String formFileName) {
        File folder = getAppInternalFormsDir(context);
        if (folder.exists()) {
            File file = new File(getFormsFolder(context), formFileName);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    @NonNull
    private File getAppInternalFormsDir(Context context) {
        String path = FileUtil.getInternalFolderPath(context, DIR_FORMS);
        return new File(path);
    }

    @Nullable
    private File getAppExternalFormFileIfExists(Context context, String formFileName) {
        File folder = getAppExternalFormsDir(context);
        if (folder != null && folder.exists()) {
            File file = new File(folder, formFileName);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    @Nullable
    private File getAppExternalFormsDir(Context context) {
        String path = FileUtil.getAppExternalFolderPath(context, DIR_FORMS);
        File folder = null;
        if (path != null) {
            folder = new File(path);
        }
        return folder;
    }

    @NonNull
    private File getPublicFormsDir() {
        return new File(FileUtil.getPublicFolderPath(DIR_FORMS));
    }

    @NonNull
    private File getPublicFolderPath() {
        String publicFolderPath = FileUtil.getPublicFolderPath(DIR_FORMS);
        return FileUtil.createDir(publicFolderPath);
    }

}
