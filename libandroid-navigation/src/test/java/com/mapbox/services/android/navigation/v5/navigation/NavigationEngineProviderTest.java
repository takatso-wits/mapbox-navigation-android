package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.offroute.OffRoute;
import com.mapbox.services.android.navigation.v5.route.FasterRoute;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class NavigationEngineProviderTest {

  @Test
  public void onInitialization_defaultEnginesAreCreated() {
    NavigationEngineProvider provider = new NavigationEngineProvider();

    assertNotNull(provider.retrieveCameraEngine());
  }

  @Test
  public void updateOffRouteEngine() {
    NavigationEngineProvider provider = new NavigationEngineProvider();
    OffRoute originalOffRouteEngine = provider.retrieveOffRouteEngine();
    OffRoute newOffRouteEngine = mock(OffRoute.class);

    provider.updateOffRouteEngine(newOffRouteEngine);

    assertNotSame(originalOffRouteEngine, newOffRouteEngine);
  }

  @Test
  public void retrieveFasterRouteEngine() {
    NavigationEngineProvider provider = new NavigationEngineProvider();
    FasterRoute newFasterRouteEngine = mock(FasterRoute.class);

    provider.updateFasterRouteEngine(newFasterRouteEngine);

    assertEquals(newFasterRouteEngine, provider.retrieveFasterRouteEngine());
  }

  @Test
  public void clearEngines() {
    NavigationEngineProvider provider = new NavigationEngineProvider();

    provider.clearEngines();

    assertNull(provider.retrieveCameraEngine());
  }
}