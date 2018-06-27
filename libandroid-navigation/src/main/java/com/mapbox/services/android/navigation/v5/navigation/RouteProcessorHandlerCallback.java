package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.Message;

import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.buildSnappedLocation;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkMilestones;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.isUserOffRoute;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.shouldCheckFasterRoute;

class RouteProcessorHandlerCallback implements Handler.Callback {

  private NavigationRouteProcessor routeProcessor;
  private RouteProcessorBackgroundThread.Listener listener;
  private Handler responseHandler;

  RouteProcessorHandlerCallback(NavigationRouteProcessor routeProcessor, Handler responseHandler,
                                RouteProcessorBackgroundThread.Listener listener) {
    this.routeProcessor = routeProcessor;
    this.responseHandler = responseHandler;
    this.listener = listener;
  }

  @Override
  public boolean handleMessage(Message msg) {
    NavigationLocationUpdate update = ((NavigationLocationUpdate) msg.obj);
    handleRequest(update);
    return true;
  }

  /**
   * Takes a new location model and runs all related engine checks against it
   * (off-route, milestones, snapped location, and faster-route).
   * <p>
   * After running through the engines, all data is submitted to {@link NavigationService} via
   * {@link RouteProcessorBackgroundThread.Listener}.
   *
   * @param navigationLocationUpdate hold location, navigation (with options), and distances away from maneuver
   */
  private void handleRequest(final NavigationLocationUpdate navigationLocationUpdate) {

    final MapboxNavigation mapboxNavigation = navigationLocationUpdate.mapboxNavigation();
    boolean snapToRouteEnabled = mapboxNavigation.options().snapToRoute();

    final Location rawLocation = navigationLocationUpdate.location();

    RouteProgress routeProgress = routeProcessor.buildNewRouteProgress(mapboxNavigation, rawLocation);

    final boolean userOffRoute = isUserOffRoute(navigationLocationUpdate, routeProgress, routeProcessor);

    routeProcessor.checkIncreaseIndex(mapboxNavigation);

    RouteProgress previousRouteProgress = routeProcessor.getRouteProgress();
    final List<Milestone> milestones = checkMilestones(previousRouteProgress, routeProgress, mapboxNavigation);

    final Location location = buildSnappedLocation(mapboxNavigation, snapToRouteEnabled,
      rawLocation, routeProgress, userOffRoute);

    boolean fasterRouteEnabled = mapboxNavigation.options().enableFasterRouteDetection();
    final boolean checkFasterRoute = fasterRouteEnabled && !userOffRoute
      && shouldCheckFasterRoute(navigationLocationUpdate, routeProgress);

    final RouteProgress finalRouteProgress = routeProgress;
    routeProcessor.setRouteProgress(finalRouteProgress);

    responseHandler.post(new Runnable() {
      @Override
      public void run() {
        listener.onNewRouteProgress(location, finalRouteProgress);
        listener.onMilestoneTrigger(milestones, finalRouteProgress);
        listener.onUserOffRoute(location, userOffRoute);
        listener.onCheckFasterRoute(location, finalRouteProgress, checkFasterRoute);
      }
    });
  }
}
