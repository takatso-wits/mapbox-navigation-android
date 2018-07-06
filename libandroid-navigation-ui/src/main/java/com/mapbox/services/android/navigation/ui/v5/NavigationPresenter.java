package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;
import android.support.design.widget.BottomSheetBehavior;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;

import static com.mapbox.services.android.navigation.ui.v5.CameraState.NOT_TRACKING;
import static com.mapbox.services.android.navigation.ui.v5.CameraState.OVERVIEW;
import static com.mapbox.services.android.navigation.ui.v5.CameraState.TRACKING;

class NavigationPresenter {

  private NavigationContract.View view;
  private boolean resumeState;
  @CameraState.Type
  private int currentCameraState = TRACKING;

  NavigationPresenter(NavigationContract.View view) {
    this.view = view;
  }

  void updateResumeState(boolean resumeState) {
    this.resumeState = resumeState;
  }

  void onRecenterClick() {
    setTracking();
  }

  void onMapScroll() {
    if (!view.isSummaryBottomSheetHidden()) {
      setNotTracking();
    }
  }

  void onRouteUpdate(DirectionsRoute directionsRoute) {
    view.drawRoute(directionsRoute);
    view.updateWaynameVisibility(true);
    if (!resumeState) {
      view.startCamera(directionsRoute);
    }
  }

  void onDestinationUpdate(Point point) {
    view.addMarker(point);
  }

  void onShouldRecordScreenshot() {
    view.takeScreenshot();
  }

  void onNavigationLocationUpdate(Location location) {
    if (resumeState && !view.isRecenterButtonVisible()) {
      view.resumeCamera(location);
      resumeState = false;
    }
    view.updateNavigationMap(location);
  }

  void onInstructionListVisibilityChanged(boolean visible) {
    if (visible) {
      view.hideRecenterBtn();
    } else {
      if (view.isSummaryBottomSheetHidden()) {
        view.showRecenterBtn();
      }
    }
  }

  void onRouteOverviewClick() {
    setOverview();
  }

  int getCameraState() {
    return currentCameraState;
  }

  void onMapReady() {
    if (resumeState) {
      engageState(currentCameraState);
    }
  }

  void engageState(@CameraState.Type int cameraState) {
    switch (cameraState) {
      case TRACKING:
        setTracking();
        break;
      case NOT_TRACKING:
        setNotTracking();
        break;
      case OVERVIEW:
        setOverview();
        break;
      default:
        break;
    }
  }

  void restoreState(@CameraState.Type int cameraState) {
    this.currentCameraState = cameraState;
  }

  private void setOverview() {
    currentCameraState = CameraState.OVERVIEW;
    view.updateWaynameVisibility(false);
    view.updateCameraRouteOverview();
    view.showRecenterBtn();
  }

  private void setTracking() {
    currentCameraState = TRACKING;
    view.setSummaryBehaviorHideable(false);
    view.setSummaryBehaviorState(BottomSheetBehavior.STATE_EXPANDED);
    view.updateWaynameVisibility(true);
    view.resetCameraPosition();
    view.hideRecenterBtn();
  }

  private void setNotTracking() {
    currentCameraState = CameraState.NOT_TRACKING;
    view.setSummaryBehaviorHideable(true);
    view.setSummaryBehaviorState(BottomSheetBehavior.STATE_HIDDEN);
    view.updateCameraTrackingEnabled(false);
    view.updateWaynameVisibility(false);
    view.showRecenterBtn();
  }
}
