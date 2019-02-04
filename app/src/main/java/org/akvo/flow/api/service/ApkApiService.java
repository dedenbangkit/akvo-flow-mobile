/*
 *  Copyright (C) 2010-2017 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Flow.
 *
 *  Akvo Flow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Akvo Flow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.api.service;

import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.akvo.flow.util.HttpUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ApkApiService {

    private static final String APK_VERSION_SERVICE_PATH =
            "/deviceapprest?action=getLatestVersion&deviceType=androidPhone&appCode=flowapp&androidBuildVersion=";

    @Nullable
    public JSONObject getApkDataObject(String baseUrl) throws IOException, JSONException {
        final String url = baseUrl + APK_VERSION_SERVICE_PATH + Build.VERSION.SDK_INT;
        String response = HttpUtil.httpGet(url);
        if (!TextUtils.isEmpty(response)) {
            return new JSONObject(response);
        }
        return null;
    }
}