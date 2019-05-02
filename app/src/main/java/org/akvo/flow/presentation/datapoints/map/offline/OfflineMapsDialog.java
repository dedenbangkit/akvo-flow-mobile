/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
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
 */

package org.akvo.flow.presentation.datapoints.map.offline;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.akvo.flow.R;
import org.akvo.flow.app.FlowApp;
import org.akvo.flow.injector.component.ApplicationComponent;
import org.akvo.flow.injector.component.DaggerViewComponent;
import org.akvo.flow.injector.component.ViewComponent;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class OfflineMapsDialog extends DialogFragment implements OfflineMapsView {

    public static final String TAG = "OfflineMapsDialog";

    @Inject
    OfflineMapsPresenter presenter;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    @BindView(R.id.addMapButton)
    Button addMapsButton;

    @BindView(R.id.noMapsTextView)
    TextView noMapsTextView;

    public OfflineMapsDialog() {
    }

    public static OfflineMapsDialog newInstance() {
        return new OfflineMapsDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getActivity().getString(R.string.offline_maps_dialog_title));
        View main = LayoutInflater.from(getContext())
                .inflate(R.layout.offline_maps_dialog, null);
        ButterKnife.bind(this, main);
        builder.setView(main);
        return builder.create();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initializeInjector();

        presenter.setView(this);
        presenter.load(getContext().getApplicationContext());
    }

    private void initializeInjector() {
        ViewComponent viewComponent = DaggerViewComponent.builder()
                .applicationComponent(getApplicationComponent())
                .build();
        viewComponent.inject(this);
    }

    /**
     * Get the Main Application component for dependency injection.
     *
     * @return {@link ApplicationComponent}
     */
    @SuppressWarnings("ConstantConditions")
    private ApplicationComponent getApplicationComponent() {
        return ((FlowApp) getActivity().getApplication()).getApplicationComponent();
    }

    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void displayRegions(OfflineRegion[] offlineRegions) {
        addMapsButton.setVisibility(View.GONE);
        noMapsTextView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayNoOfflineMaps() {
        recyclerView.setVisibility(View.GONE);
        addMapsButton.setVisibility(View.VISIBLE);
        noMapsTextView.setVisibility(View.VISIBLE);
    }
}
