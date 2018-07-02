package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.geojson.Point;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.Navigator;
import com.mapbox.services.android.navigation.v5.location.LocationValidator;

import java.util.Date;

class NavigationLocationEngineListener implements LocationEngineListener {

  private final RouteProcessorBackgroundThread thread;
  private final LocationValidator locationValidator;
  private final LocationEngine locationEngine;
  private final Navigator navigator;
  private MapboxNavigation mapboxNavigation;

  NavigationLocationEngineListener(RouteProcessorBackgroundThread thread, MapboxNavigation mapboxNavigation,
                                   LocationEngine locationEngine) {
    this.thread = thread;
    this.mapboxNavigation = mapboxNavigation;
    this.locationEngine = locationEngine;
    this.navigator = mapboxNavigation.retrieveNavigator();
    locationValidator = initializeLocationValidator(mapboxNavigation.options());
  }

  @Override
  @SuppressWarnings("MissingPermission")
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    navigator.updateLocation(buildFixLocationFrom(location));
    if (isValidLocationUpdate(location)) {
      queueLocationUpdate(location);
    }
  }

  boolean isValidLocationUpdate(Location location) {
    return location != null && locationValidator.isValidUpdate(location);
  }

  /**
   * Queues a new task created from a location update to be sent
   * to {@link RouteProcessorBackgroundThread} for processing.
   *
   * @param location to be processed
   */
  void queueLocationUpdate(Location location) {
    thread.queueUpdate(NavigationLocationUpdate.create(location, mapboxNavigation));
  }

  /**
   * Creates a new location validator used to filter incoming
   * location updates from the location engine.
   */
  private LocationValidator initializeLocationValidator(MapboxNavigationOptions options) {
    int accuracyAcceptableThreshold = options.locationAcceptableAccuracyInMetersThreshold();
    int accuracyPercentThreshold = options.locationAccuracyPercentThreshold();
    int timeInMillisThreshold = options.locationUpdateTimeInMillisThreshold();
    int velocityInMetersPerSecondThreshold = options.locationVelocityInMetersPerSecondThreshold();
    return new LocationValidator(accuracyAcceptableThreshold, accuracyPercentThreshold,
      timeInMillisThreshold, velocityInMetersPerSecondThreshold);
  }

  private FixLocation buildFixLocationFrom(Location rawLocation) {
    Point rawPoint = Point.fromLngLat(rawLocation.getLongitude(), rawLocation.getLatitude());
    Date time = new Date(rawLocation.getTime());
    Float speed = rawLocation.getSpeed();
    Float bearing = rawLocation.getBearing();
    Float altitude = (float) rawLocation.getAltitude();
    Float horizontalAccuracy = rawLocation.getAccuracy();
    String provider = rawLocation.getProvider();

    return new FixLocation(
      rawPoint,
      time,
      speed,
      bearing,
      altitude,
      horizontalAccuracy,
      provider
    );
  }
}
