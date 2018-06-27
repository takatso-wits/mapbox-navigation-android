package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;
import com.mapbox.services.android.navigation.v5.route.RouteFetcher;

import timber.log.Timber;

/**
 * Internal usage only, use navigation by initializing a new instance of {@link MapboxNavigation}
 * and customizing the navigation experience through that class.
 * <p>
 * This class is first created and started when {@link MapboxNavigation#startNavigation(DirectionsRoute)}
 * get's called and runs in the background until either the navigation sessions ends implicitly or
 * the hosting activity gets destroyed. Location updates are also tracked and handled inside this
 * service. Thread creation gets created in this service and maintains the thread until the service
 * gets destroyed.
 * </p>
 */
public class NavigationService extends Service {

  private final IBinder localBinder = new LocalBinder();

  private RouteProcessorBackgroundThread thread;
  private NavigationLocationProvider locationProvider;
  private NavigationRouteProvider routeProvider;
  private NavigationNotificationProvider notificationProvider;

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return localBinder;
  }

  /**
   * Only should be called once since we want the service to continue running until the navigation
   * session ends.
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    NavigationTelemetry.getInstance().initializeLifecycleMonitor(getApplication());
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    stopForeground(true);
    super.onDestroy();
  }

  /**
   * This gets called when {@link MapboxNavigation#startNavigation(DirectionsRoute)} is called and
   * setups variables among other things on the Navigation Service side.
   */
  void startNavigation(MapboxNavigation mapboxNavigation) {
    initialize(mapboxNavigation);
    startForegroundNotification(notificationProvider.retrieveNotification());
    locationProvider.forceLocationUpdate(mapboxNavigation.getRoute());
  }

  /**
   * Removes the location / route listeners and  quits the thread.
   */
  void endNavigation() {
    locationProvider.removeLocationEngineListener();
    routeProvider.removeRouteEngineListener();
    notificationProvider.unregisterNotificationReceiver(getApplication());
    thread.quit();
  }

  /**
   * Called with {@link MapboxNavigation#setLocationEngine(LocationEngine)}.
   * Updates this service with the new {@link LocationEngine}.
   *
   * @param locationEngine to update the provider
   */
  void updateLocationEngine(LocationEngine locationEngine) {
    locationProvider.updateLocationEngine(locationEngine);
  }

  private void initialize(MapboxNavigation mapboxNavigation) {
    NavigationEventDispatcher dispatcher = mapboxNavigation.getEventDispatcher();
    String accessToken = mapboxNavigation.obtainAccessToken();
    initializeRouteProvider(dispatcher, accessToken, mapboxNavigation.retrieveEngineProvider());
    initializeNotificationProvider(mapboxNavigation);

    initializeRouteProcessorThread(dispatcher, routeProvider, notificationProvider);
    initializeLocationProvider(mapboxNavigation);
  }

  private void initializeRouteProvider(NavigationEventDispatcher dispatcher, String accessToken,
                                       NavigationEngineProvider engineProvider) {
    RouteFetcher routeFetcher = new RouteFetcher(getApplication(), accessToken);
    FasterRoute fasterRouteEngine = engineProvider.retrieveFasterRouteEngine();
    NavigationRouteProviderListener listener = new NavigationRouteProviderListener(dispatcher, fasterRouteEngine);
    routeProvider = new NavigationRouteProvider(routeFetcher, listener);
  }

  private void initializeNotificationProvider(MapboxNavigation mapboxNavigation) {
    notificationProvider = new NavigationNotificationProvider(getApplication(), mapboxNavigation);
  }

  private void initializeRouteProcessorThread(NavigationEventDispatcher dispatcher,
                                              NavigationRouteProvider routeProvider,
                                              NavigationNotificationProvider notificationProvider) {
    RouteProcessorThreadListener listener = new RouteProcessorThreadListener(
      dispatcher, routeProvider, notificationProvider
    );
    thread = new RouteProcessorBackgroundThread(new Handler(), listener);
  }

  private void initializeLocationProvider(MapboxNavigation mapboxNavigation) {
    LocationEngine locationEngine = mapboxNavigation.getLocationEngine();
    NavigationLocationEngineListener listener = new NavigationLocationEngineListener(
      thread, mapboxNavigation, locationEngine
    );
    locationProvider = new NavigationLocationProvider(locationEngine, listener);
  }

  private void startForegroundNotification(NavigationNotification navigationNotification) {
    Notification notification = navigationNotification.getNotification();
    int notificationId = navigationNotification.getNotificationId();
    notification.flags = Notification.FLAG_FOREGROUND_SERVICE;
    startForeground(notificationId, notification);
  }

  class LocalBinder extends Binder {
    NavigationService getService() {
      Timber.d("Local binder called.");
      return NavigationService.this;
    }
  }
}
