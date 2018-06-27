package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.route.RouteFetcher;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NavigationRouteProviderTest {

  @Test
  public void onInitialization_routeListenerIsAdded() {
    RouteFetcher routeFetcher = mock(RouteFetcher.class);
    NavigationRouteProviderListener listener = mock(NavigationRouteProviderListener.class);

    new NavigationRouteProvider(routeFetcher, listener);

    verify(routeFetcher).addRouteListener(listener);
  }

  @Test
  public void checkForFasterRoute_routeFetcherFindRouteIsCalled() {
    RouteFetcher routeFetcher = mock(RouteFetcher.class);
    NavigationRouteProvider routeProvider = buildNavigationRouteProvider(routeFetcher);
    Location location = mock(Location.class);
    RouteProgress routeProgress = mock(RouteProgress.class);

    routeProvider.checkForFasterRoute(location, routeProgress);

    verify(routeFetcher).findRouteFromRouteProgress(location, routeProgress);
  }

  @Test
  public void removeRouteEngineListener_routeFetcherListenerIsRemoved() {
    RouteFetcher routeFetcher = mock(RouteFetcher.class);
    NavigationRouteProviderListener listener = mock(NavigationRouteProviderListener.class);
    NavigationRouteProvider routeProvider = new NavigationRouteProvider(routeFetcher, listener);

    routeProvider.removeRouteEngineListener();

    verify(routeFetcher).removeRouteEngineListener(listener);
  }

  private NavigationRouteProvider buildNavigationRouteProvider(RouteFetcher routeFetcher) {
    NavigationRouteProviderListener listener = mock(NavigationRouteProviderListener.class);
    return new NavigationRouteProvider(routeFetcher, listener);
  }
}