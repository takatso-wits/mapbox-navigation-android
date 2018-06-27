package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.services.android.navigation.v5.instruction.Instruction;
import com.mapbox.services.android.navigation.v5.milestone.Milestone;
import com.mapbox.services.android.navigation.v5.milestone.StepMilestone;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class RouteProcessorThreadListenerTest {

  @Test
  public void onNewRouteProgress_notificationProviderIsUpdated() {
    NavigationNotificationProvider provider = mock(NavigationNotificationProvider.class);
    RouteProcessorThreadListener listener = buildListener(provider);

    listener.onNewRouteProgress(mock(Location.class), mock(RouteProgress.class));

    verify(provider).updateNavigationNotification(any(RouteProgress.class));
  }

  @Test
  public void onNewRouteProgress_eventDispatcherProgressIsUpdated() {
    NavigationEventDispatcher dispatcher = mock(NavigationEventDispatcher.class);
    RouteProcessorThreadListener listener = buildListener(dispatcher);

    listener.onNewRouteProgress(mock(Location.class), mock(RouteProgress.class));

    verify(dispatcher).onProgressChange(any(Location.class), any(RouteProgress.class));
  }

  @Test
  public void onMilestoneTrigger_eventDispatcherSendsMilestone() {
    List<Milestone> milestones = new ArrayList<>();
    StepMilestone stepMilestone = new StepMilestone.Builder().build();
    milestones.add(stepMilestone);
    NavigationEventDispatcher eventDispatcher = mock(NavigationEventDispatcher.class);
    RouteProcessorThreadListener listener = buildListener(eventDispatcher);
    RouteProgress routeProgress = mock(RouteProgress.class);

    listener.onMilestoneTrigger(milestones, routeProgress);

    verify(eventDispatcher).onMilestoneEvent(routeProgress, "", stepMilestone);
  }

  @Test
  public void onMilestoneTrigger_correctInstructionIsBuilt() {
    final String customInstruction = "Custom instruction!";
    Instruction instruction = buildCustomInstruction(customInstruction);
    List<Milestone> milestones = new ArrayList<>();
    Milestone stepMilestone = new StepMilestone.Builder().setInstruction(instruction).build();
    milestones.add(stepMilestone);
    NavigationEventDispatcher eventDispatcher = mock(NavigationEventDispatcher.class);
    RouteProcessorThreadListener listener = buildListener(eventDispatcher);
    RouteProgress routeProgress = mock(RouteProgress.class);

    listener.onMilestoneTrigger(milestones, routeProgress);

    verify(eventDispatcher).onMilestoneEvent(routeProgress, customInstruction, stepMilestone);
  }

  @Test
  public void onUserOffRouteTrue_eventDispatcherSendsEvent() {
    NavigationEventDispatcher dispatcher = mock(NavigationEventDispatcher.class);
    RouteProcessorThreadListener listener = buildListener(dispatcher);

    listener.onUserOffRoute(mock(Location.class), true);

    verify(dispatcher).onUserOffRoute(any(Location.class));
  }

  @Test
  public void onUserOffRouteFalse_eventDispatcherDoesNotSendEvent() {
    NavigationEventDispatcher dispatcher = mock(NavigationEventDispatcher.class);
    RouteProcessorThreadListener listener = buildListener(dispatcher);

    listener.onUserOffRoute(mock(Location.class), false);

    verifyZeroInteractions(dispatcher);
  }

  @Test
  public void onCheckFasterRouteTrue_eventDispatcherSendsEvent() {
    NavigationRouteProvider provider = mock(NavigationRouteProvider.class);
    RouteProcessorThreadListener listener = buildListener(provider);

    listener.onCheckFasterRoute(mock(Location.class), mock(RouteProgress.class), true);

    verify(provider).checkForFasterRoute(any(Location.class), any(RouteProgress.class));
  }

  @Test
  public void onCheckFasterRouteFalse_eventDispatcherDoesNotSendEvent() {
    NavigationEventDispatcher dispatcher = mock(NavigationEventDispatcher.class);
    RouteProcessorThreadListener listener = buildListener(dispatcher);

    listener.onCheckFasterRoute(mock(Location.class), mock(RouteProgress.class), false);

    verifyZeroInteractions(dispatcher);
  }

  private RouteProcessorThreadListener buildListener(NavigationNotificationProvider provider) {
    NavigationRouteProvider routeProvider = mock(NavigationRouteProvider.class);
    NavigationEventDispatcher eventDispatcher = mock(NavigationEventDispatcher.class);
    return new RouteProcessorThreadListener(eventDispatcher, routeProvider, provider);
  }

  private RouteProcessorThreadListener buildListener(NavigationRouteProvider routeProvider) {
    NavigationNotificationProvider provider = mock(NavigationNotificationProvider.class);
    NavigationEventDispatcher eventDispatcher = mock(NavigationEventDispatcher.class);
    return new RouteProcessorThreadListener(eventDispatcher, routeProvider, provider);
  }

  private RouteProcessorThreadListener buildListener(NavigationEventDispatcher eventDispatcher) {
    NavigationNotificationProvider provider = mock(NavigationNotificationProvider.class);
    NavigationRouteProvider routeProvider = mock(NavigationRouteProvider.class);
    return new RouteProcessorThreadListener(eventDispatcher, routeProvider, provider);
  }

  @NonNull
  private Instruction buildCustomInstruction(final String customInstruction) {
    return new Instruction() {
      @Override
      public String buildInstruction(RouteProgress routeProgress) {
        return customInstruction;
      }
    };
  }
}