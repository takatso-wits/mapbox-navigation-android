package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

class NavigationLocationProvider {

  private final NavigationLocationEngineListener listener;
  private final RouteUtils routeUtils = new RouteUtils();
  private LocationEngine locationEngine;

  NavigationLocationProvider(LocationEngine locationEngine, NavigationLocationEngineListener listener) {
    this.locationEngine = locationEngine;
    this.listener = listener;
    locationEngine.addLocationEngineListener(listener);
  }

  void updateLocationEngine(LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
    locationEngine.addLocationEngineListener(listener);
  }

  @SuppressWarnings("MissingPermission")
  void forceLocationUpdate(DirectionsRoute route) {
    Location location = locationEngine.getLastLocation();
    if (!listener.isValidLocationUpdate(location)) {
      location = routeUtils.createFirstLocationFromRoute(route);
    }
    listener.queueLocationUpdate(location);
  }

  void removeLocationEngineListener() {
    locationEngine.removeLocationEngineListener(listener);
  }
}
