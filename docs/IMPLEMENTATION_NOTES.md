# Pause — implementation notes

Written while the app is being built; completed in phase 9. The structure follows §13 of the spec.

## 1. What was implemented per phase

| Phase | Main classes |
|---|---|
| 0 | `PauseApp` (Hilt + `HiltWorkerFactory`), `MainActivity` (edge-to-edge), backup disabled, texts of the existing screens in `values/strings.xml` and `values-pl/strings.xml`, `formatDuration` |
| 1 | `domain/model/*`, `UsageEventAggregator`, `PauseDatabase` + DAOs (schema in `app/schemas`), `SettingsDataStore`, `UsageStatsDataSource`, `InstalledAppsDataSource`, repositories + Hilt modules |
| 2 | Navigation (`Destinations`, `PauseNavHost`, `PauseBottomBar`, `MainViewModel`), Onboarding, Home on real data (`HomeStateMapper`), accessibility service declaration |
| 3 | Limits list (`LimitsStateMapper`), Limit edit, `AppPickerSheet`, `AppIcon` |
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
- Starting `BlockActivity` from the accessibility service was allowed on API 36 (`BAL_ALLOW_TOKEN` in the system log), so the `TYPE_ACCESSIBILITY_OVERLAY` fallback was not needed. **Not verified on API 37** (targetSdk); no API 37 device or emulator was available. Manual check: on an API 37 device, set a 5-minute limit, use the app for 5 minutes and confirm the break screen appears; `adb logcat | grep BlockActivity` must show `result code=0`.

## 5. Known limitations

- A day is computed from events starting 3 hours before midnight; an app opened earlier than that and still in front without any events is not counted.
- "Today" on Home and Limits is recomputed when the screen is resumed, not continuously while it is visible.
