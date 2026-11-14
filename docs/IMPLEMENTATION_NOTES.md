# Pause — implementation notes

Written while the app is being built; completed in phase 9. The structure follows §13 of the spec.

## 1. What was implemented per phase

| Phase | Main classes |
|---|---|
| 0 | `PauseApp` (Hilt + `HiltWorkerFactory`), `MainActivity` (edge-to-edge), backup disabled, texts of the existing screens in `values/strings.xml` and `values-pl/strings.xml`, `formatDuration` |
| 1 | `domain/model/*`, `UsageEventAggregator`, `PauseDatabase` + DAOs (schema in `app/schemas`), `SettingsDataStore`, `UsageStatsDataSource`, `InstalledAppsDataSource`, repositories + Hilt modules |
| 2 | Navigation (`Destinations`, `PauseNavHost`, `PauseBottomBar`, `MainViewModel`), Onboarding, Home on real data (`HomeStateMapper`), accessibility service declaration |
| 3 | Limits list (`LimitsStateMapper`), Limit edit, `AppPickerSheet`, `AppIcon` |
| 8 | `Notifier` + `NotificationHelper`, `NotificationChannels`, `OpenTarget` (notification → screen), limit warning in `EnforcementController`, Focus notifications, `WeeklySummaryCalculator`, `SendWeeklySummaryUseCase`, `WeeklySummaryWorker`, `DeleteAllDataUseCase` + `LocalDataWiper`, Settings (`SettingsViewModel`, `SettingsScreen`), `rememberNotificationPermissionRequest` |
| 7 | `FocusConfig`, `StartFocusSessionUseCase`, `FinishExpiredFocusSessionUseCase`, `EndFocusSessionEarlyUseCase`, `FocusSessionEndWorker`, Focus Mode (`FocusViewModel`, `FocusScreen`), multi-select `AppPickerSheet`, remaining time on Home, `ForegroundAppResolver` |
| 6 | `SyncUsageHistoryUseCase`, `UsageSyncWorker`, `WorkManagerScheduler`, `StatisticsRanges`, `StatisticsCalculator`, `GetStatisticsUseCase`, Statistics (`StatsStateMapper`, `StatsViewModel`), `UsageBarChart`, App details (`GetAppDetailsUseCase`, `AppDetailsStateMapper`, `AppDetailsViewModel`, `AppDetailsScreen`) |
| 5 | `StreakCalculator`, `StreakConfig`, `DayTracking`, `CloseFinishedDaysUseCase`, `StreakRepository`, Profile (`ProfileStateMapper`, `ProfileViewModel`, `ProfileScreen`), debug-only `DemoDataSeeder` + `DebugToolsActivity` |
| 4 | `FrictionConfig`, `FrictionPolicy`, `EnforcementController`, `PauseAccessibilityService`, `BlockActivity` + `BlockFlow` (`LimitReachedScreen`, `PauseScreen`, `PurposeScreen`), `BlockViewModel`, `BypassConsequence`, `GrantAccessUseCase` |

## 2. Deviations from the spec and decisions taken

| # | Topic | Decision | Why |
|---|---|---|---|
| D1 | Domain imports | Use cases use `javax.inject.Inject`. | Chosen by the author; avoids a hand-written provider for every use case. `javax.inject` is a plain annotation library without Android. |
| D2 | `ACTIVITY_STOPPED` | Not mapped to BACKGROUND; only `ACTIVITY_PAUSED` is. | STOPPED of the previous activity often arrives after the next activity of the same app has resumed and would end the session early. |
| D3 | One foreground app | The aggregator assumes one app in front at a time; in split screen the last resumed app gets the time. | Total screen time is never counted twice. |
| D4 | Unmatched BACKGROUND | A BACKGROUND event without a FOREGROUND event adds no time; sessions crossing the window start are found through a 3-hour lookback. | After a reboot the event history starts mid-day; counting from midnight produced "Settings 20h 59m" on the author's phone. |
| D5 | Unlocks without a lock screen | See "Unlock counting" below. | The author's phone has no lock screen and never reports an unlock. |
| D6 | `daily_summary.closed` | Extra column next to `tracked`. | Distinguishes a synced-but-open day from a closed untracked day. |
| D7 | App icons | Not in the domain model; the UI loads them by package name (`AppIcon`). | `Drawable` is an Android type. |
| D8 | Home app list | Only launchable apps used for at least one minute; the launcher and system packages still count toward total screen time. Apps without a limit get a neutral bar relative to the longest one. | Accepted by the author. |
| D9 | Goal steppers | Screen time 30 min – 12 h in 15 min steps; unlocks 5 – 200 in steps of 5. | Not specified; accepted by the author. |
| D10 | Dates | Formatted in the language of the app texts (`app_locale_tag`), not the device locale. | On a Russian device the date was Russian while texts fell back to English. |
| D11 | Limit day text | A limit active on more than half of the week is described by the days left out ("Except Sat"). | "Mon, Tue, Wed, Thu, Fri, Sun only" is hard to read; accepted by the author. |
| D12 | Events from Pause itself | Not evaluated (Pause is never blocked), but they tell the controller that the previously tracked app is no longer in front; the same applies to the home launcher. | When the break screen or Pause is in front, a pending timer for the app behind it must not fire. |
| D13 | Ignored windows | Keyboards (enabled input methods) and non-launchable packages (system UI, dialogs) do not change the app in front. | Opening the keyboard or the notification shade inside a limited app must not stop its timer. |
| D14 | Leaving the break screen | Leaving it (home, recents, screen off) resets the flow to the first screen and restarts the pause next time, instead of finishing the activity. | Finishing it would show the limited app again after the screen is turned back on. |
| D15 | `BlockViewModel` | Depends on `FrictionPolicy` directly, besides use cases. | It is a pure domain rule without data access. |
| D16 | Streak on the break screen | Streak, freezes and the consequence banner use the real rule (`BypassConsequence`) but get 0 streak / 0 freezes until phase 5 provides the streak. | Streak calculator is phase 5. |
| D17 | "5 minutes left" notification | Not yet posted by the controller. | Notifications are phase 8. |
| D18 | Bypass on an untracked day | A day with a limit bypass is FROZEN or BROKEN even if it later closes as untracked. | The freeze is used / the streak drops "immediately" (§4.2); changing that at day close would make the result depend on when it is computed. |
| D19 | "A limit applied that day" | Approximated by the current, enabled limits that were created by the end of that day and are active on its weekday. The accessibility-service state at the moment of closing applies to all days closed in that run. | Past limits and past service state are not stored. Days are normally closed on the next app start or by the 15-minute worker (phase 6), so the approximation is close. |
| D20 | Re-syncing old days | `syncDay` never replaces a stored day with an empty result. | The system deletes raw events after a few days; re-syncing would otherwise wipe history. |
| D21 | Onboarding date | Stored when onboarding finishes; installs that finished onboarding before this existed get today. | Needed for "untracked before onboarding". |
| D22 | Settings | The settings icon on Profile opens a placeholder until phase 8. | Settings is phase 8. |
| D23 | Backfill | The first sync imports past days going back from the day before yesterday until a day has no system events, at most 30 days; a stored flag makes it run once. An empty result is never stored. | "As many past days as the system still has events for" (§5.1) without querying empty days every 15 minutes. |
| D24 | Statistics averages | Week and Month: "Daily average", "unlocks per day" and the comparison chip use completed days with data only; today is used only when it is the only day with data. Today's bar stays on the chart. Day shows today. The chip is hidden when the previous period has no data. | Decided by the author: an unfinished today and days without data (before installation, future days) would pull the average down. |
| D25 | Statistics apps | Launchable apps used at least one minute in the period, top 10. Default period when the screen opens: Week. | Same filter as Home (D8); Week is the most informative first view. |
| D26 | Charts | Not scrollable or zoomable; the whole period is fitted to the width. | Vico scrolls by default, which hid most bars of Day and Month. |
| D28 | Finishing a Focus session | At the planned end, by whichever comes first: a one-time WorkManager job, the next app start, or the open Focus screen; the end time is always the planned end. Not by the enforcement controller's timer. | §6.1 allows the controller one pending timer, used for the app in front; the three triggers already cover a killed process. |
| D29 | Default paused apps | When no Focus apps are set, the idle screen preselects all apps with a limit; starting saves the selection. Turning off the last Focus app in App details brings the default back. | §7.7 "defaults to all apps that have a limit". |
| D30 | Focus notifications | The ongoing countdown notification and "session finished" are phase 8. | §8 lists them with notifications. |
| D31 | Break-screen minutes left | "Focus session in progress · N min left" rounds up (13:10 left → "14 min"). | Never shows "0 min" while time remains. |
| D32 | Service start with an app open | When the accessibility service starts (app update, crash, turned on), the app already in front is taken from the last 30 minutes of usage events (`ForegroundAppResolver`). | No window event arrives for an app that is already open; found on the device after a process restart. |
| D33 | "5 min left" warning | Shown once per app per day; the "already warned" memory lives in the controller and is lost when the process dies, so after a restart the same day it can appear once more. When less than 5 minutes are left on opening the app, it is shown at once with the real minutes left. | Keeps the single-timer design; persisting it was not worth a table. |
| D34 | Notification permission | Asked when a notification switch is turned on while notifications are off, and on every Focus session start while they are off; Android itself stops showing the dialog after the user declines twice. | §8: in context, not in onboarding. |
| D35 | Weekly summary | Periodic work every 7 days, first on the next Monday at 09:00 local time; scheduled only while the switch is on (also re-checked on app start). Last week = previous Monday–Sunday, average over days with data; no notification when last week has no data. | §8. |
| D36 | Delete all data | Cancels all work and notifications, clears Room and DataStore, returns to onboarding. Past days are not imported again from the system's event log after a wipe (the "backfilled" flag is set again). | A wipe should start history fresh from that day. |
| D27 | App details "limit reached" | A day is highlighted when the current limit applied on that weekday and was reached. | Past limits are not stored (as D19). |

### Debug tools (debug builds only)

`app/src/debug/` contains `DemoDataSeeder`, `DebugToolsActivity`, a manifest that adds a launcher shortcut "Seed demo history" (long-press the app icon) and debug-only strings. Also: `adb shell am start -n com.justantis.pause/.debug.DebugToolsActivity`. It writes 35 closed past days (8 untracked, 9 clean, 1 frozen, 4 clean, 1 broken, 12 clean) with per-app usage for apps installed on the phone and a few break-screen attempts; result: streak 12, record 13, 1 of 2 freezes, next in 2 days. Today is never changed. Verified: the release APK contains neither the classes nor the shortcut. "Delete all data" (phase 8) removes the demo data.

### Unlock counting

`UsageEventAggregator.countUnlocks` holds the whole rule:

- If the events (the day plus the 3-hour lookback) contain any lock-screen event (`KEYGUARD_HIDDEN` or `KEYGUARD_SHOWN`), the device has a lock screen and only `KEYGUARD_HIDDEN` counts as an unlock.
- Otherwise the device has no lock screen and every `SCREEN_INTERACTIVE` (screen turned on) counts as an unlock.

**Overcounting risk of the fallback:** on a device without a lock screen, a screen that wakes up for a notification (or a raise-to-wake gesture) and turns off again without the user doing anything is still counted. The number is then closer to "pickups" than to unlocks. Devices with a lock screen are not affected.

## 3. Assumptions A1–A8 and where they are encoded

_Completed in phase 9._

## 4. What could not be verified

_Completed in phase 9._ Verified so far on a Samsung Galaxy S21 FE (SM-G990B2, Android API 36, no lock screen):

- Phase 4 end to end: a 5-minute limit on Chrome showed the break screen 5 minutes after Chrome was opened; the pause (10 s), purpose and "Open for 1 min" returned to Chrome; when the minute expired the break screen came back with 2 attempts and a 30 s hint; "Close" went to the home screen.
- Phase 7: a 15-minute Focus session on Chrome survived `kill -9` of the app process (the remaining time was 13:33 after 1 min 27 s); the system restarted the process and the accessibility service; opening Chrome showed the Focus break screen with a 30 s pause; after the bypass Focus Mode showed "With breaks"; ending early with confirmation stored CANCELLED. This test also found two defects, both fixed with regression tests: an extra unseen attempt recorded when a change arrived while the break screen was being shown, and no enforcement for an app already in front when the service starts.
- Phase 8: Settings shows all sections; turning on the weekly summary scheduled `WeeklySummaryWorker` (WorkManager correctly refused a forced early run: "executed before schedule"); the summary notification text was "Daily screen time: 2h 34m (−6% vs previous). Streak: 12 days." (posted via the debug button); a Focus session finished by itself as CLEAN exactly at its planned end and posted "Focus session finished".
- Starting `BlockActivity` from the accessibility service was allowed on API 36 (`BAL_ALLOW_TOKEN` in the system log), so the `TYPE_ACCESSIBILITY_OVERLAY` fallback was not needed. **Not verified on API 37** (targetSdk); no API 37 device or emulator was available. Manual check: on an API 37 device, set a 5-minute limit, use the app for 5 minutes and confirm the break screen appears; `adb logcat | grep BlockActivity` must show `result code=0`.

## 5. Known limitations

- A day is computed from events starting 3 hours before midnight; an app opened earlier than that and still in front without any events is not counted.
- "Today" on Home and Limits is recomputed when the screen is resumed, not continuously while it is visible.
