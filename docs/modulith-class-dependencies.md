# Cross-Module Class Dependency Graph

Generated from Spring Modulith and ArchUnit metadata.

```mermaid
flowchart LR
  subgraph Analytics["Analytics"]
    biz_thonbecker_personal_analytics_platform_service_PostHogEventListener["PostHogEventListener<br/>analytics.platform.service"]
  end
  subgraph Booking["Booking"]
    biz_thonbecker_personal_booking_api_BookingAvailabilityViewedEvent["BookingAvailabilityViewedEvent<br/>booking.api"]
    biz_thonbecker_personal_booking_api_BookingCancellationRequestedEvent["BookingCancellationRequestedEvent<br/>booking.api"]
    biz_thonbecker_personal_booking_api_BookingCancelledEvent["BookingCancelledEvent<br/>booking.api"]
    biz_thonbecker_personal_booking_api_BookingCreatedEvent["BookingCreatedEvent<br/>booking.api"]
    biz_thonbecker_personal_booking_api_BookingStartedEvent["BookingStartedEvent<br/>booking.api"]
    biz_thonbecker_personal_booking_api_BookingSubmittedEvent["BookingSubmittedEvent<br/>booking.api"]
  end
  subgraph Calendar_Integration["Calendar Integration"]
    biz_thonbecker_personal_calendar_platform_CalendarEventListener["CalendarEventListener<br/>calendar.platform"]
    biz_thonbecker_personal_calendar_platform_CalendarSyncScheduler["CalendarSyncScheduler<br/>calendar.platform"]
    biz_thonbecker_personal_calendar_platform_IcsGenerator["IcsGenerator<br/>calendar.platform"]
    biz_thonbecker_personal_calendar_platform_NextcloudCalDavService["NextcloudCalDavService<br/>calendar.platform"]
  end
  subgraph Foosball["Foosball"]
    biz_thonbecker_personal_foosball_api_GameRecordedEvent["GameRecordedEvent<br/>foosball.api"]
    biz_thonbecker_personal_foosball_api_PlayerCreatedEvent["PlayerCreatedEvent<br/>foosball.api"]
  end
  subgraph Landscape_Planning["Landscape Planning"]
    biz_thonbecker_personal_landscape_api_LandscapeAnalysisCompletedEvent["LandscapeAnalysisCompletedEvent<br/>landscape.api"]
    biz_thonbecker_personal_landscape_api_LandscapePlanSavedEvent["LandscapePlanSavedEvent<br/>landscape.api"]
    biz_thonbecker_personal_landscape_api_LandscapePlantAddedEvent["LandscapePlantAddedEvent<br/>landscape.api"]
    biz_thonbecker_personal_landscape_api_LandscapePreviewGeneratedEvent["LandscapePreviewGeneratedEvent<br/>landscape.api"]
  end
  subgraph Notification_Services["Notification Services"]
    biz_thonbecker_personal_notification_platform_CalendarService["CalendarService<br/>notification.platform"]
    biz_thonbecker_personal_notification_platform_EmailNotificationService["EmailNotificationService<br/>notification.platform"]
    biz_thonbecker_personal_notification_api_EventLoggingListener["EventLoggingListener<br/>notification.api"]
    biz_thonbecker_personal_notification_api_NotificationEventListener["NotificationEventListener<br/>notification.api"]
  end
  subgraph Skateboard_Trick_Analyzer["Skateboard Trick Analyzer"]
    biz_thonbecker_personal_skatetricks_api_SkatetricksAnalysisCompletedEvent["SkatetricksAnalysisCompletedEvent<br/>skatetricks.api"]
    biz_thonbecker_personal_skatetricks_api_SkatetricksAnalysisStartedEvent["SkatetricksAnalysisStartedEvent<br/>skatetricks.api"]
    biz_thonbecker_personal_skatetricks_api_SkatetricksAttemptVerifiedEvent["SkatetricksAttemptVerifiedEvent<br/>skatetricks.api"]
  end
  subgraph Trivia_Quiz["Trivia Quiz"]
    biz_thonbecker_personal_trivia_api_PlayerJoinedQuizEvent["PlayerJoinedQuizEvent<br/>trivia.api"]
    biz_thonbecker_personal_trivia_api_QuizCompletedEvent["QuizCompletedEvent<br/>trivia.api"]
    biz_thonbecker_personal_trivia_api_QuizStartedEvent["QuizStartedEvent<br/>trivia.api"]
    biz_thonbecker_personal_trivia_api_TriviaAnswerSubmittedEvent["TriviaAnswerSubmittedEvent<br/>trivia.api"]
  end
  subgraph User_Management["User Management"]
    biz_thonbecker_personal_user_api_LoginCompletedEvent["LoginCompletedEvent<br/>user.api"]
    biz_thonbecker_personal_user_api_LoginFailedEvent["LoginFailedEvent<br/>user.api"]
    biz_thonbecker_personal_user_api_LoginRequestedEvent["LoginRequestedEvent<br/>user.api"]
    biz_thonbecker_personal_user_api_UserLoginEvent["UserLoginEvent<br/>user.api"]
    biz_thonbecker_personal_user_api_UserLoginLinkRequestedEvent["UserLoginLinkRequestedEvent<br/>user.api"]
    biz_thonbecker_personal_user_api_UserProfileUpdatedEvent["UserProfileUpdatedEvent<br/>user.api"]
    biz_thonbecker_personal_user_api_UserRegisteredEvent["UserRegisteredEvent<br/>user.api"]
  end
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_booking_api_BookingAvailabilityViewedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_booking_api_BookingCancelledEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_booking_api_BookingCreatedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_booking_api_BookingStartedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_booking_api_BookingSubmittedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_foosball_api_GameRecordedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_foosball_api_PlayerCreatedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_landscape_api_LandscapeAnalysisCompletedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_landscape_api_LandscapePlanSavedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_landscape_api_LandscapePlantAddedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_landscape_api_LandscapePreviewGeneratedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_skatetricks_api_SkatetricksAnalysisCompletedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_skatetricks_api_SkatetricksAnalysisStartedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_skatetricks_api_SkatetricksAttemptVerifiedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_trivia_api_PlayerJoinedQuizEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_trivia_api_QuizCompletedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_trivia_api_QuizStartedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_trivia_api_TriviaAnswerSubmittedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_LoginCompletedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_LoginFailedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_LoginRequestedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_UserLoginEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_UserProfileUpdatedEvent
  biz_thonbecker_personal_analytics_platform_service_PostHogEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_UserRegisteredEvent
  biz_thonbecker_personal_calendar_platform_CalendarEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_booking_api_BookingCancelledEvent
  biz_thonbecker_personal_calendar_platform_CalendarEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_booking_api_BookingCreatedEvent
  biz_thonbecker_personal_calendar_platform_CalendarSyncScheduler -->|"DEFAULT"| biz_thonbecker_personal_booking_api_BookingCancellationRequestedEvent
  biz_thonbecker_personal_calendar_platform_IcsGenerator -->|"DEFAULT"| biz_thonbecker_personal_booking_api_BookingCreatedEvent
  biz_thonbecker_personal_calendar_platform_NextcloudCalDavService -->|"DEFAULT"| biz_thonbecker_personal_booking_api_BookingCreatedEvent
  biz_thonbecker_personal_notification_platform_CalendarService -->|"DEFAULT"| biz_thonbecker_personal_booking_api_BookingCreatedEvent
  biz_thonbecker_personal_notification_platform_EmailNotificationService -->|"DEFAULT"| biz_thonbecker_personal_booking_api_BookingCancelledEvent
  biz_thonbecker_personal_notification_platform_EmailNotificationService -->|"DEFAULT"| biz_thonbecker_personal_booking_api_BookingCreatedEvent
  biz_thonbecker_personal_notification_api_NotificationEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_booking_api_BookingCancelledEvent
  biz_thonbecker_personal_notification_api_NotificationEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_booking_api_BookingCreatedEvent
  biz_thonbecker_personal_notification_api_EventLoggingListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_foosball_api_GameRecordedEvent
  biz_thonbecker_personal_notification_api_EventLoggingListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_foosball_api_PlayerCreatedEvent
  biz_thonbecker_personal_notification_api_EventLoggingListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_trivia_api_PlayerJoinedQuizEvent
  biz_thonbecker_personal_notification_api_EventLoggingListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_trivia_api_QuizCompletedEvent
  biz_thonbecker_personal_notification_api_EventLoggingListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_trivia_api_QuizStartedEvent
  biz_thonbecker_personal_notification_platform_EmailNotificationService -->|"DEFAULT"| biz_thonbecker_personal_user_api_UserLoginLinkRequestedEvent
  biz_thonbecker_personal_notification_api_EventLoggingListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_UserLoginEvent
  biz_thonbecker_personal_notification_api_EventLoggingListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_UserProfileUpdatedEvent
  biz_thonbecker_personal_notification_api_EventLoggingListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_UserRegisteredEvent
  biz_thonbecker_personal_notification_api_NotificationEventListener -->|"DEFAULT, EVENT_LISTENER"| biz_thonbecker_personal_user_api_UserLoginLinkRequestedEvent
```

