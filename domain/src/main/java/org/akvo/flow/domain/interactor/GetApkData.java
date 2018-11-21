/*
 * Copyright (C) 2016-2018 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo FLOW.
 *
 * Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 * the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 * either version 3 of the License or any later version.
 *
 * Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License included below for more details.
 *
 * The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 *
 */

package org.akvo.flow.domain.interactor;

import android.os.Build;

import org.akvo.flow.domain.executor.PostExecutionThread;
import org.akvo.flow.domain.executor.ThreadExecutor;
import org.akvo.flow.domain.repository.ApkRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.observers.DisposableObserver;

public class GetApkData extends UseCase {

    private final ApkRepository apkRepository;

    @Inject
    public GetApkData(ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread,
            ApkRepository apkRepository) {
        super(threadExecutor, postExecutionThread);
        this.apkRepository = apkRepository;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void execute(DisposableObserver<T> observer, Map<String, Object> parameters) {
        addDisposable(((Observable<T>) buildUseCaseObservable(parameters)).subscribeWith(observer));
    }

    @Override
    protected <T> Observable buildUseCaseObservable(Map<String, T> parameters) {
        return apkRepository.loadApkData(Build.VERSION.SDK_INT + "");
    }
}
