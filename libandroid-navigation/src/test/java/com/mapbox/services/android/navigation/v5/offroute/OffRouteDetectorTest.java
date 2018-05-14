package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.util.List;
import java.util.Locale;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OffRouteDetectorTest extends BaseTest {

  @Test
  public void sanity() throws Exception {
    OffRouteDetector offRouteDetector = new OffRouteDetector();

    assertNotNull(offRouteDetector);
  }

  @Test
  public void invalidOffRoute_onFirstLocationUpdate() throws Exception {
    Location mockLocation = mock(Location.class);
    RouteProgress mockProgress = mock(RouteProgress.class);
    when(mockProgress.distanceRemaining()).thenReturn(1000d);
    MapboxNavigationOptions options = buildDefaultNavigationOptions();
    OffRouteDetector offRouteDetector = buildOffRouteDetector();

    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(mockLocation, mockProgress, options);

    assertFalse(isUserOffRoute);
  }

  @Test
  public void validOffRoute_onMinimumDistanceBeforeReroutingPassed() throws Exception {
    OffRouteDetector offRouteDetector = buildOffRouteDetector();
    Location mockLocation = mock(Location.class);
    RouteProgress mockProgress = mock(RouteProgress.class);
    Location mapboxOffice = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    when(mockProgress.distanceRemaining()).thenReturn(1000d);
    MapboxNavigationOptions options = buildDefaultNavigationOptions();
    offRouteDetector.isUserOffRoute(mockLocation, mockProgress, options);
    Point target = buildPointAwayFromLocation(mapboxOffice, options.minimumDistanceBeforeRerouting() + 1);
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Location locationOverMinimumDistance = buildDefaultLocationUpdate(target.longitude(), target.latitude());

    boolean validOffRoute = offRouteDetector.isUserOffRoute(locationOverMinimumDistance, routeProgress, options);

    assertTrue(validOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertTrueWhenTooFarFromStep() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();
    OffRouteDetector offRouteDetector = buildOffRouteDetector();
    MapboxNavigationOptions options = buildDefaultNavigationOptions();
    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 100, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());

    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options);
    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(secondUpdate, routeProgress, options);

    assertTrue(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenOnStep() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();
    OffRouteDetector offRouteDetector = buildOffRouteDetector();
    MapboxNavigationOptions options = buildDefaultNavigationOptions();
    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 10, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());

    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options);
    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(secondUpdate, routeProgress, options);

    assertFalse(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenWithinRadiusAndStepLocationHasBadAccuracy() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Point stepManeuverPoint = routeProgress.directionsRoute().legs().get(0).steps().get(0).maneuver().location();
    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    Point offRoutePoint = buildPointAwayFromPoint(stepManeuverPoint, 250, 90);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());
    when(secondUpdate.getAccuracy()).thenReturn(300f);
    OffRouteDetector offRouteDetector = buildOffRouteDetector();
    MapboxNavigationOptions options = buildDefaultNavigationOptions();

    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options);
    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(secondUpdate, routeProgress, options);

    assertFalse(isUserOffRoute);
  }

  @Test
  public void isUserOffRoute_AssertFalseWhenOffRouteButCloseToUpcomingStep() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    Point upcomingStepManeuverPoint = routeProgress.currentLegProgress().upComingStep().maneuver().location();
    Location firstUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    OffRouteCallback mockCallback = mock(OffRouteCallback.class);
    OffRouteDetector offRouteDetector = buildOffRouteDetector(mockCallback);
    MapboxNavigationOptions options = buildDefaultNavigationOptions();

    offRouteDetector.isUserOffRoute(firstUpdate, routeProgress, options);
    Point offRoutePoint = buildPointAwayFromPoint(upcomingStepManeuverPoint, 30, 180);
    Location secondUpdate = buildDefaultLocationUpdate(offRoutePoint.longitude(), offRoutePoint.latitude());
    boolean isUserOffRoute = offRouteDetector.isUserOffRoute(secondUpdate, routeProgress, options);

    assertFalse(isUserOffRoute);
    verify(mockCallback, times(1)).onShouldIncreaseIndex();
  }

  @Test
  public void isUserOffRoute_AssertTrueWhenOnRouteButMovingAwayFromManeuver() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();
    Location firstLocationUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    OffRouteDetector offRouteDetector = buildOffRouteDetector();
    MapboxNavigationOptions options = buildDefaultNavigationOptions();

    offRouteDetector.isUserOffRoute(firstLocationUpdate, routeProgress, options);
    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location secondLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    offRouteDetector.isUserOffRoute(secondLocationUpdate, routeProgress, options);
    Point secondLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location thirdLocationUpdate = buildDefaultLocationUpdate(
      secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    offRouteDetector.isUserOffRoute(thirdLocationUpdate, routeProgress, options);
    Point thirdLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fourthLocationUpdate = buildDefaultLocationUpdate(
      thirdLastPointInCurrentStep.longitude(), thirdLastPointInCurrentStep.latitude()
    );
    offRouteDetector.isUserOffRoute(fourthLocationUpdate, routeProgress, options);
    Point fourthLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location fifthLocationUpdate = buildDefaultLocationUpdate(
      fourthLastPointInCurrentStep.longitude(), fourthLastPointInCurrentStep.latitude()
    );
    offRouteDetector.isUserOffRoute(fifthLocationUpdate, routeProgress, options);
    Point fifthLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location sixthLocationUpdate = buildDefaultLocationUpdate(
      fifthLastPointInCurrentStep.longitude(), fifthLastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteFifthTry = offRouteDetector.isUserOffRoute(sixthLocationUpdate, routeProgress, options);

    assertTrue(isUserOffRouteFifthTry);
  }

  @Test
  public void isUserOffRoute_AssertFalseTwoUpdatesAwayFromManeuverThenOneTowards() throws Exception {
    RouteProgress routeProgress = buildDefaultTestRouteProgress();
    LegStep currentStep = routeProgress.currentLegProgress().currentStep();
    LineString lineString = LineString.fromPolyline(currentStep.geometry(), Constants.PRECISION_6);
    List<Point> coordinates = lineString.coordinates();
    Location firstLocationUpdate = buildDefaultLocationUpdate(-77.0339782574523, 38.89993519985637);
    OffRouteDetector offRouteDetector = buildOffRouteDetector();
    MapboxNavigationOptions options = buildDefaultNavigationOptions();

    offRouteDetector.isUserOffRoute(firstLocationUpdate, routeProgress, options);
    Point lastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location secondLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    offRouteDetector.isUserOffRoute(secondLocationUpdate, routeProgress, options);
    Point secondLastPointInCurrentStep = coordinates.remove(coordinates.size() - 1);
    Location thirdLocationUpdate = buildDefaultLocationUpdate(
      secondLastPointInCurrentStep.longitude(), secondLastPointInCurrentStep.latitude()
    );
    offRouteDetector.isUserOffRoute(thirdLocationUpdate, routeProgress, options);
    Location fourthLocationUpdate = buildDefaultLocationUpdate(
      lastPointInCurrentStep.longitude(), lastPointInCurrentStep.latitude()
    );
    boolean isUserOffRouteThirdTry = offRouteDetector.isUserOffRoute(fourthLocationUpdate, routeProgress, options);

    assertFalse(isUserOffRouteThirdTry);
  }

  @Test
  public void isUserOffRoute_assertTrueWhenRouteDistanceRemainingIsZero() {
    OffRouteDetector offRouteDetector = buildOffRouteDetector();
    MapboxNavigationOptions options = buildDefaultNavigationOptions();
    Location location = mock(Location.class);
    RouteProgress routeProgress = mock(RouteProgress.class);
    when(routeProgress.distanceRemaining()).thenReturn(0d);

    boolean isOffRoute = offRouteDetector.isUserOffRoute(location, routeProgress, options);

    assertTrue(isOffRoute);
  }

  private OffRouteDetector buildOffRouteDetector() {
    return new OffRouteDetector();
  }

  private OffRouteDetector buildOffRouteDetector(OffRouteCallback callback) {
    OffRouteDetector detector = new OffRouteDetector();
    detector.setOffRouteCallback(callback);
    return detector;
  }

  private MapboxNavigationOptions buildDefaultNavigationOptions() {
    return MapboxNavigationOptions.builder().build();
  }
}
