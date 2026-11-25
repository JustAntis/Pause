# Screen UI tests — design

Date: 2026-11-25

## Goal

Add instrumented Compose UI tests for every screen that has none: Onboarding, Home, Limits,
App details, Focus, Stats, Profile, Settings and Limit reached. Pause, Purpose and Limit edit
already have tests (`BreakScreensTest`, `LimitEditScreenTest`); those stay unchanged.

## Decisions

- **Level:** stateless screens only. Each test renders `XScreen(state, onIntent)` with a
  hand-built state. ViewModels, Hilt and navigation are not involved; ViewModel logic is covered
  by the JVM unit tests.
- **Depth:** the main states and the main clicks, 2–5 tests per screen, 36 tests in total.
- **Structure:** one test class per screen, following `BreakScreensTest`. Nodes are found by
  their English text, as in the existing tests.

## Files

All in `app/src/androidTest/java/com/justantis/pause/presentation/`:

- `testing/ScreenTestHelpers.kt` (new)
  - `class IntentRecorder<T>`: `val intents: MutableList<T>`, `operator fun invoke(intent: T)`
    (so it can be passed as `onIntent`), `fun last(): T`.
  - `fun ComposeContentTestRule.setScreen(content: @Composable () -> Unit)`: sets content
    wrapped in `PauseTheme`.
- `OnboardingScreenTest.kt`, `HomeScreenTest.kt`, `LimitsScreenTest.kt`,
  `AppDetailsScreenTest.kt`, `FocusScreenTest.kt`, `StatsScreenTest.kt`, `ProfileScreenTest.kt`,
  `SettingsScreenTest.kt`, `LimitReachedScreenTest.kt` (new).

No new dependencies: `ui-test-junit4`, `androidx.junit` and `ui-test-manifest` are already set up.

## Test pattern

- Build the state with `isLoading = false` and only the fields the test needs.
- Check content with `onNodeWithText` / `onNodeWithContentDescription` and `assertIsDisplayed`,
  `assertDoesNotExist`, `assertIsEnabled`, `assertIsNotEnabled`.
- For clicks, call `performClick()`, then `assertEquals(expectedIntent, recorder.last())`.
- If the state must change during a test (a dialog opening, a step changing), keep it in
  `mutableStateOf` and change it from the test, as `BreakScreensTest` does.
- Content below the first screenful is reached with `performScrollTo()` before it is checked or
  clicked.
- Expected texts are copied exactly from `res/values/strings.xml`, with plurals and format
  arguments filled in.

## Test cases

### Onboarding (5)
1. Welcome step: the button reads "Next" and is enabled; typing a name sends `NameChanged`.
2. Permissions step, nothing granted: "Next" is disabled; the two grant buttons send
   `GrantUsageAccess` and `GrantAccessibility`.
3. Permissions step, both granted: the granted label is shown twice and "Next" is enabled.
4. Goals step: the button shows the finish text; the steppers send the Decrease and Increase
   intents for both goals.
5. A goal at its minimum disables its decrease button.

### Home (5)
1. While loading, the greeting is not shown.
2. Filled state: the greeting with the name, the streak and "used of limit" text for an app with
   a limit are shown.
3. Each permission warning appears only when that permission is missing; its "open settings"
   button sends `FixUsageAccess` or `FixAccessibility`.
4. Tapping an app row sends `AppClicked(packageName)`; with no apps, the "no usage yet" text is
   shown.
5. The start button sends `StartSessionClicked`; with `focusMinutesLeft` set it shows the running
   text with the remaining time.

### Limits (4)
1. With no limits (and not loading), the empty card is shown.
2. A card shows the right status line for Remaining, UsedUp (with attempts), NotActiveToday,
   and with active-days text when the days are not every day.
3. Tapping a card sends `LimitClicked(packageName)`.
4. "Add limit" sends `AddLimitClicked`.

### App details (4)
1. The stat tiles show today's time, opens and bypasses.
2. Without a limit the set-limit button is shown; with a limit, the edit-limit button and the
   limit text are shown; tapping it sends `LimitClicked`.
3. The pause-in-focus switch sends `PausedInFocusChanged(true)`.
4. Back sends `BackClicked`.

### Focus (5)
1. Idle, no paused apps: the start button is disabled and the no-apps text is shown.
2. Idle, with apps: choosing a duration sends `DurationSelected(minutes)`; start sends
   `StartClicked`.
3. Running: the clean or with-breaks status is shown; "End session" sends `EndClicked`.
4. End dialog visible: confirm sends `EndConfirmed`; keep going sends `EndDismissed`.
5. Finished: the result text is shown; "Done" sends `FinishedDismissed`.

### Stats (4)
1. Choosing a period sends `PeriodSelected(period)`.
2. Day period shows the "today" label; Week shows the "daily average" label.
3. The change line shows the up, down and no-change text depending on `changePercent`.
4. With no apps, the no-apps text is shown; tapping an app sends `AppClicked(packageName)`.

### Profile (4)
1. Current streak, record and freezes are shown, with the next-freeze text when
   `daysUntilNextFreeze` is set and the "full" text when it is null.
2. The settings button sends `SettingsClicked`; a goal row sends `EditGoal(type)`.
3. Goal dialog visible: it shows the value; the steppers send `GoalDecreased` / `GoalIncreased`;
   Save sends `GoalConfirmed`; Cancel sends `GoalDismissed`.
4. A calendar cell has its day-status content description.

### Settings (3)
1. The two switches send `LimitWarningToggled` and `WeeklySummaryToggled`; the
   notifications-off text is shown when `notificationsAllowed` is false.
2. The name row shows the not-set text and sends `EditNameClicked`; with the name dialog open,
   Save sends `NameConfirmed`.
3. "Delete all data" sends `DeleteClicked`; with the delete dialog open, confirm sends
   `DeleteConfirmed` and Cancel sends `DeleteDismissed`.

### Limit reached (2)
1. LIMIT reason shows the used-of-limit text, the attempts, the streak and the freezes; FOCUS
   reason shows the focus-in-progress text.
2. Close sends `CloseClicked`; "need access" sends `NeedAccessClicked` and is disabled while
   loading.

## Running

- `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest` on the API 37 emulator only —
  never on the author's phone.
- Done when all 45 instrumented tests pass (9 existing + 36 new) and
  `./gradlew assembleDebug testDebugUnitTest lintDebug` stays green.
- After the run, switch the accessibility service off and on again in the emulator (see
  `docs/IMPLEMENTATION_NOTES.md`).

## Out of scope

- ViewModel-backed or Hilt/navigation tests.
- Screenshot tests.
- Changes to the existing tests or to production code. If a screen cannot be tested without a
  production change (for example a missing content description), stop and ask first.
