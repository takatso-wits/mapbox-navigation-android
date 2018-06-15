package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouteState;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.buildSnappedLocation;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.checkMilestones;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationHelper.shouldCheckFasterRoute;

/**
 * This class extends handler thread to run most of the navigation calculations on a separate
 * background thread.
 */
class NavigationEngine extends HandlerThread implements Handler.Callback {

  private static final String THREAD_NAME = "NavThread";

  private Handler responseHandler;
  private Handler workerHandler;
  private Callback callback;
  private NavigationRouteProcessor routeProcessor;
  private Navigator navigator;

  NavigationEngine(Handler responseHandler, Callback callback) {
    super(THREAD_NAME, Process.THREAD_PRIORITY_BACKGROUND);
    this.responseHandler = responseHandler;
    this.callback = callback;
    routeProcessor = new NavigationRouteProcessor();
  }

  @Override
  public boolean handleMessage(Message msg) {
    NewLocationModel newLocationModel = (NewLocationModel) msg.obj;
    handleRequest(newLocationModel);
    return true;
  }

  void queueTask(int msgIdentifier, NewLocationModel newLocationModel) {
    workerHandler.obtainMessage(msgIdentifier, newLocationModel).sendToTarget();
  }

  void prepareHandler() {
    workerHandler = new Handler(getLooper(), this);
  }

  /**
   * Takes a new location model and runs all related engine checks against it
   * (off-route, milestones, snapped location, and faster-route).
   * <p>
   * After running through the engines, all data is submitted to {@link NavigationService} via
   * {@link NavigationEngine.Callback}.
   *
   * @param newLocationModel hold location, navigation (with options), and distances away from maneuver
   */
  private void handleRequest(final NewLocationModel newLocationModel) {

    final MapboxNavigation mapboxNavigation = newLocationModel.mapboxNavigation();
    if (navigator == null) {
      navigator = mapboxNavigation.getNavigator();
    }
    boolean snapToRouteEnabled = mapboxNavigation.options().snapToRoute();

    final Location rawLocation = newLocationModel.location();

    RouteProgress routeProgress = routeProcessor.buildNewRouteProgress(mapboxNavigation, rawLocation);

    final boolean userOffRoute = isUserOffRoute(rawLocation);

    routeProcessor.checkIncreaseIndex(mapboxNavigation);

    RouteProgress previousRouteProgress = routeProcessor.getRouteProgress();
    final List<Milestone> milestones = checkMilestones(previousRouteProgress, routeProgress, mapboxNavigation);

    final Location location = buildSnappedLocation(mapboxNavigation, snapToRouteEnabled,
      rawLocation, routeProgress, userOffRoute);

    boolean fasterRouteEnabled = mapboxNavigation.options().enableFasterRouteDetection();
    final boolean checkFasterRoute = fasterRouteEnabled && !userOffRoute
      && shouldCheckFasterRoute(newLocationModel, routeProgress);

    final RouteProgress finalRouteProgress = routeProgress;
    routeProcessor.setRouteProgress(finalRouteProgress);

    responseHandler.post(new Runnable() {
      @Override
      public void run() {
        callback.onNewRouteProgress(location, finalRouteProgress);
        callback.onMilestoneTrigger(milestones, finalRouteProgress);
        callback.onUserOffRoute(location, userOffRoute);
        callback.onCheckFasterRoute(location, finalRouteProgress, checkFasterRoute);
      }
    });
  }

  private boolean isUserOffRoute(Location rawLocation) {
    FixLocation fixLocation = buildFixLocationFrom(rawLocation);
    NavigationStatus navigationStatus = navigator.onLocationChanged(fixLocation);
    return navigationStatus.getRouteState() == RouteState.OFFROUTE;
  }

  private FixLocation buildFixLocationFrom(Location rawLocation) {
    float latitude = (float) rawLocation.getLatitude();
    float longitude = (float) rawLocation.getLongitude();
    Long time = rawLocation.getTime();
    Float speed = rawLocation.getSpeed();
    Float bearing = rawLocation.getBearing();
    Float altitude = (float) rawLocation.getAltitude();
    Float horizontalAccuracy = rawLocation.getAccuracy();
    String provider = rawLocation.getProvider();

    return new FixLocation(
      latitude,
      longitude,
      time,
      speed,
      bearing,
      altitude,
      horizontalAccuracy,
      provider
    );
  }


  /**
   * Callbacks for posting back to the Navigation Service once the thread finishes calculations.
   * <p>
   * No matter what, with each new message added to the queue, these callbacks get invoked once
   * finished and within Navigation Service it is determined if the public corresponding listeners
   * need invoking or not; the Navigation event dispatcher class handles those callbacks.
   */
  interface Callback {
    void onNewRouteProgress(Location location, RouteProgress routeProgress);

    void onMilestoneTrigger(List<Milestone> triggeredMilestones, RouteProgress routeProgress);

    void onUserOffRoute(Location location, boolean userOffRoute);

    void onCheckFasterRoute(Location location, RouteProgress routeProgress, boolean checkFasterRoute);
  }
}
