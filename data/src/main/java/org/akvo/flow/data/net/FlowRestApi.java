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

package org.akvo.flow.data.net;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.akvo.flow.data.entity.ApiLocaleResult;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;

@Singleton
public class FlowRestApi {

    public static final String HMAC_SHA_1_ALGORITHM = "HmacSHA1";
    public static final String CHARSET_UTF8 = "UTF-8";

    private final String androidId;
    private final String imei;
    private final String phoneNumber;
    private final RestServiceFactory serviceFactory;

    @Inject
    public FlowRestApi(DeviceHelper deviceHelper, RestServiceFactory serviceFactory) {
        this.androidId = deviceHelper.getAndroidId();
        this.imei = deviceHelper.getImei();
        this.phoneNumber = deviceHelper.getPhoneNumber();
        this.serviceFactory = serviceFactory;
    }

    public Observable<ApiLocaleResult> loadNewDataPoints(@NonNull String baseUrl,
            @NonNull String apiKey, long surveyGroup, @NonNull String timestamp) {
        String lastUpdated = !TextUtils.isEmpty(timestamp) ? timestamp : "0";
        return serviceFactory.createRetrofitService(baseUrl, DataPointSyncService.class, apiKey)
                .loadNewDataPoints(androidId, imei, lastUpdated, phoneNumber, surveyGroup + "");
    }

    //TODO: move these to constants
    interface Path {

        String SURVEYED_LOCALE = "/surveyedlocale";
    }

    interface Param {

        String SURVEY_GROUP = "surveyGroupId";
        String PHONE_NUMBER = "phoneNumber";
        String IMEI = "imei";
        String TIMESTAMP = "ts";
        String LAST_UPDATED = "lastUpdateTime";
        String HMAC = "h";
        String VERSION = "ver";
        String ANDROID_ID = "androidId";
    }
}
