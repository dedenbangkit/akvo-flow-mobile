package org.akvo.flow.ui.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public abstract class Feature {
    protected static final int POINT_SIZE = 40;// Default marker size (px).
    protected static final int POINT_SIZE_SELECTED = 50;// Selected marker size (px).

    protected static final int POINT_COLOR = 0xEE736357;
    protected static final int POINT_COLOR_ACTIVE = 0xFFE27C00;
    protected static final int POINT_COLOR_SELECTED = 0xFF00A79D;
    protected static final int POINT_COLOR_FILL = 0x55FFFFFF;

    protected static final int STROKE_COLOR = 0xEE736357;
    protected static final int STROKE_COLOR_SELECTED = 0xFF736357;

    protected boolean mSelected;
    protected Marker mSelectedMarker;

    protected GoogleMap mMap;
    protected List<LatLng> mPoints;
    protected List<Marker> mMarkers;

    private static final BitmapDescriptor MARKER_UNSELECTED, MARKER_ACTIVE, MARKER_SELECTED;

    static {
        MARKER_UNSELECTED = getMarkerBitmapDescriptor(PointStatus.UNSELECTED);
        MARKER_ACTIVE = getMarkerBitmapDescriptor(PointStatus.ACTIVE);
        MARKER_SELECTED = getMarkerBitmapDescriptor(PointStatus.SELECTED);
    }

    private enum PointStatus {
        UNSELECTED, // Unselected Feature. Normal mode.
        SELECTED, // Currently selected marker.
        ACTIVE // Selected Feature, but marker is not selected (just 'active').
    }

    public Feature(GoogleMap map) {
        mMap = map;
        mPoints = new ArrayList<>();
        mMarkers = new ArrayList<>();
    }

    public abstract String getTitle();
    public abstract String geoGeometryType();

    public boolean contains(Marker marker) {
        return mMarkers.contains(marker);
    }

    public List<LatLng> getPoints() {
        return mPoints;
    }

    public void addPoint(LatLng point) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(point)
                .title(String.format("lat/lng: %.5f, %.5f", point.latitude, point.longitude))
                .anchor(0.5f, 0.5f)
                .draggable(true)
                .icon(MARKER_UNSELECTED));

        // Insert new point just after the currently selected marker (if any)
        if (mSelectedMarker != null) {
            int index =  mMarkers.indexOf(mSelectedMarker) + 1;
            mMarkers.add(index, marker);
            mPoints.add(index, point);
        } else {
            mMarkers.add(marker);
            mPoints.add(point);
        }

        mSelectedMarker = marker;
        invalidate();
    }

    /**
     * Delete selected point
     */
    public void removePoint() {
        if (mSelectedMarker == null) {
            return;
        }

        int index =  mMarkers.indexOf(mSelectedMarker);
        mSelectedMarker.remove();
        mPoints.remove(index);
        mMarkers.remove(index);
    }

    public void delete() {
        for (Marker marker : mMarkers) {
            marker.remove();
        }
        mMarkers.clear();
        mPoints.clear();
    }

    public void onDrag(Marker marker) {
        int index =  mMarkers.indexOf(marker);
        if (index == -1) {
            return;
        }

        mPoints.remove(index);
        mPoints.add(index, marker.getPosition());
        invalidate();
    }

    public void setSelected(boolean selected, Marker marker) {
        mSelected = selected;
        mSelectedMarker = selected ? marker: null;
        invalidate();
    }

    protected void invalidate() {
        // Recompute icons, depending on selection status
        for (Marker marker : mMarkers) {
            if (mSelected && marker.equals(mSelectedMarker)) {
                marker.setIcon(MARKER_SELECTED);
                marker.showInfoWindow();
            } else if (mSelected) {
                marker.setIcon(MARKER_ACTIVE);
            } else {
                marker.setIcon(MARKER_UNSELECTED);
            }
        }
    }

    public void load(List<LatLng> points) {
        for (LatLng point : points) {
            addPoint(point);
        }
    }

    protected static BitmapDescriptor getMarkerBitmapDescriptor(PointStatus status) {
        int size, color;
        switch (status) {
            case SELECTED:
                size = POINT_SIZE_SELECTED;
                color = POINT_COLOR_SELECTED;
                break;
            case ACTIVE:
                size = POINT_SIZE;
                color = POINT_COLOR_ACTIVE;
                break;
            case UNSELECTED:
            default:
                size = POINT_SIZE;
                color = POINT_COLOR;
        }

        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint solid = new Paint();
        solid.setColor(color);
        solid.setAntiAlias(true);
        Paint fill = new Paint();
        fill.setAntiAlias(true);
        fill.setColor(POINT_COLOR_FILL);

        final float center = size / 2f;
        canvas.drawCircle(center, center, center, solid);// Outer circle
        canvas.drawCircle(center, center, center * 0.9f, fill);// Fill circle
        canvas.drawCircle(center, center, center * 0.25f, solid);// Inner circle

        return BitmapDescriptorFactory.fromBitmap(bmp);
    }

}
