package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NavigationLocationEngineListenerTest {

  @Test
  public void onConnected_engineRequestsUpdates() {
    LocationEngine locationEngine = mock(LocationEngine.class);
    NavigationLocationEngineListener listener = buildListener(locationEngine);

    listener.onConnected();

    verify(locationEngine).requestLocationUpdates();
  }

  @Test
  public void queueLocationUpdate_threadReceivesUpdate() {
    RouteProcessorBackgroundThread thread = mock(RouteProcessorBackgroundThread.class);
    NavigationLocationEngineListener listener = buildListener(thread);

    listener.queueLocationUpdate(mock(Location.class));

    verify(thread).queueUpdate(any(NavigationLocationUpdate.class));
  }

  private NavigationLocationEngineListener buildListener(RouteProcessorBackgroundThread thread) {
    MapboxNavigation mapboxNavigation = mock(MapboxNavigation.class);
    when(mapboxNavigation.options()).thenReturn(MapboxNavigationOptions.builder().build());
    return new NavigationLocationEngineListener(thread, mapboxNavigation, mock(LocationEngine.class));
  }

  private NavigationLocationEngineListener buildListener(LocationEngine locationEngine) {
    MapboxNavigation mapboxNavigation = mock(MapboxNavigation.class);
    when(mapboxNavigation.options()).thenReturn(MapboxNavigationOptions.builder().build());
    return new NavigationLocationEngineListener(mock(RouteProcessorBackgroundThread.class),
      mapboxNavigation, locationEngine);
  }
}