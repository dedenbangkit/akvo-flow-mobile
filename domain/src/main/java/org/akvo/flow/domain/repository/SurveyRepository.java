/*
 * Copyright (C) 2017-2019 Stichting Akvo (Akvo Foundation)
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

package org.akvo.flow.domain.repository;

import android.support.annotation.NonNull;

import org.akvo.flow.domain.entity.DataPoint;
import org.akvo.flow.domain.entity.FormInstanceMetadata;
import org.akvo.flow.domain.entity.InstanceIdUuid;
import org.akvo.flow.domain.entity.Survey;
import org.akvo.flow.domain.entity.User;

import java.util.List;
import java.util.Set;

import io.reactivex.Flowable;
import io.reactivex.Observable;

public interface SurveyRepository {

    Observable<List<Survey>> getSurveys();

    Observable<List<DataPoint>> getDataPoints(Long surveyGroupId, Double latitude,
            Double longitude, Integer orderBy);

    Flowable<Integer> downloadDataPoints(long surveyGroupId);

    Observable<Boolean> deleteSurvey(long surveyToDeleteId);

    Observable<List<User>> getUsers();

    Observable<Boolean> editUser(User user);

    Observable<Boolean> deleteUser(User user);

    Observable<Long> createUser(String userName);

    Observable<User> getUser(Long userId);

    Observable<Boolean> clearResponses();

    Observable<Boolean> clearAllData();

    Observable<Boolean> unSyncedTransmissionsExist();

    Observable<List<String>> getAllTransmissionFileNames();

    Observable<Set<String>> processTransmissions(String deviceId, @NonNull String surveyId);

    Observable<Set<String>> processTransmissions(String deviceId);

    Observable<List<InstanceIdUuid>> getSubmittedInstances();

    Observable<Boolean> setInstanceStatusToRequested(long id);

    Observable<List<Long>> getPendingSurveyInstances();

    Observable<FormInstanceMetadata> getFormInstanceData(Long instanceId, String deviceId);

    Observable<Boolean> createTransmissions(Long instanceId, String formId, Set<String> fileNames);

    Observable<List<String>> getFormIds(String surveyId);

    Observable<List<String>> getFormIds();
}
