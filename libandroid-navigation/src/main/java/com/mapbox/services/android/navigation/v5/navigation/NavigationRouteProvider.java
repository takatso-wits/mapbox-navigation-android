package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.route.RouteFetcher;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

class NavigationRouteProvider {

  private final RouteFetcher routeFetcher;
  private final NavigationRouteProviderListener routeListener;

  NavigationRouteProvider(RouteFetcher routeFetcher, NavigationRouteProviderListener routeListener) {
    this.routeFetcher = routeFetcher;
    this.routeListener = routeListener;
    this.routeFetcher.addRouteListener(routeListener);
  }

  void checkForFasterRoute(Location location, RouteProgress routeProgress) {
    routeFetcher.findRouteFromRouteProgress(location, routeProgress);
  }

  void removeRouteEngineListener() {
    routeFetcher.removeRouteEngineListener(routeListener);
  }
}
