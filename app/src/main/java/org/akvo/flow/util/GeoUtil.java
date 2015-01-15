/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.util;

import android.location.Location;

import com.google.android.maps.GeoPoint;

import java.text.DecimalFormat;

/**
 * simple utility class for handling Locations
 * 
 * @author Christopher Fagiani
 */
public class GeoUtil {
    /**
     * converts lat/lon values into a GeoPoint
     * 
     * @param lat
     * @param lon
     * @return
     */
    public static GeoPoint convertToPoint(String lat, String lon) {
        Double latitude = Double.parseDouble(lat) * 1E6;
        Double longitude = Double.parseDouble(lon) * 1E6;
        return new GeoPoint(latitude.intValue(), longitude.intValue());
    }

    /**
     * converts lat/lon values into a GeoPoint
     * 
     * @param lat
     * @param lon
     * @return
     */
    public static GeoPoint convertToPoint(Double lat, Double lon) {
        Double latitude = lat * 1E6;
        Double longitude = lon * 1E6;
        return new GeoPoint(latitude.intValue(), longitude.intValue());
    }

    /**
     * converts a Location object to a GeoPoint
     * 
     * @param loc
     * @return
     */
    public static GeoPoint convertToPoint(Location loc) {
        Double latitude = loc.getLatitude() * 1E6;
        Double longitude = loc.getLongitude() * 1E6;
        return new GeoPoint(latitude.intValue(), longitude.intValue());
    }

    /**
     * decodes a lat/lon pair from a single integer
     * 
     * @param val
     * @return
     */
    public static String decodeLocation(int val) {
        return "" + ((double) val / (double) 1E6);
    }

    /**
     * computes as-the-crow-flies distance between 2 points
     * 
     * @param point1
     * @param point2
     * @return
     */
    public static double computeDistance(GeoPoint point1, GeoPoint point2) {
        return Math.sqrt(Math.pow(point1.getLatitudeE6()
                - point2.getLatitudeE6(), 2d)
                + Math.pow(point1.getLongitudeE6() - point2.getLongitudeE6(),
                        2d));
    }

    public static String getDisplayLength(double distance) {
        // default: km
        DecimalFormat df = new DecimalFormat("#.#");
        String unit = "km";
        Double factor = 0.001; // convert from meters to km

        // for distances smaller than 1 km, use meters as unit
        if (distance < 1000.0) {
            factor = 1.0;
            unit = "m";
            //df = new DecimalFormat("#"); // only whole meters
        }
        double dist = distance * factor;
        return df.format(dist) + " " + unit;
    }

    public static String getDisplayArea(double area) {
        // default: square km
        DecimalFormat df = new DecimalFormat("#.#");
        String unit = "km²";
        Double factor = 0.000001; // convert from m² to km²

        // for distances smaller than 1 km², use m² as unit
        if (area < 1000000.0) {
            factor = 1.0;
            unit = "m²";
            //df = new DecimalFormat("#"); // only whole meters
        }
        double dist = area * factor;
        return df.format(dist) + " " + unit;
    }

}
