# Loop Habit Tracker — arfx fork

This repository is a personal fork of [Loop Habit Tracker](https://github.com/iSoron/uhabits).
For the original app description, screenshots, installation information, contribution
guidelines and upstream documentation, see the
[original Loop Habit Tracker README](https://github.com/iSoron/uhabits#readme).

This fork keeps the upstream project as its foundation and adds the features listed
below. The completed Android feature work is integrated into `dev`. Android test builds use a dedicated debug application ID and can be installed alongside the official Loop app.

## Fork features

### Habit tracking

- **No-Goal Habits:** Track habits without a numerical or frequency goal. Their
  list indicator is a colored dot instead of a progress ring.
- **Habit type conversion:** Convert between yes/no and numerical tracking while
  preserving historical entries. Positive numerical values become completed checks
  when converting to yes/no; their former value and unit are retained in the entry
  note. Zero and negative values become unchecked entries. Skipped and unknown
  entries retain their states.
- **Numeric tap-to-increment:** Tap the displayed value of a numerical habit to
  increase it by one. Long-press the value to open the existing manual number-entry
  dialog. This keeps quick daily updates simple while retaining precise editing.

### Custom habit colors

- **Custom colors:** Choose a custom color for a habit using the color picker,
  including colors beyond the built-in palette.
- **Color selection shortcut:** The custom-color action is represented by a
  rainbow button in the color palette.
- **Color persistence and export:** Custom color values are stored with the habit
  and their hexadecimal color value is included in CSV export.

### Group Habits

- **Groups on the habit list:** Group ordinary habits, then expand or collapse the
  group to show its members. Nested groups are not supported.
- **Configurable completion rules:**
  - **OR:** at least one member succeeds on that date.
  - **AND:** every member succeeds on that date.
  - **N aus M (AT_LEAST):** a configurable minimum number of members must succeed.
- **Independent group goal:** A group can have its own enabled/disabled goal and
  frequency, independently of the goals configured for its members.
- **Group insights:** Group-level progress, scores, streaks, calendar/history and
  contributing member colors are derived from the selected completion rule.
- **Editing:** The completion rule and threshold can be changed after group creation.

### Data and compatibility

- **CSV export:** Exports ordinary habits; group metadata is kept separately rather
  than represented as ordinary habit rows. Custom habit colors are exported as
  hexadecimal values.
- **Database backups:** Group definitions, members and completion-rule settings are
  stored in the fork's backup metadata.
- **Upstream compatibility:** The exported database is structured so that the
  official Loop 2.3.1 importer can still import ordinary habits. The official app
  does not understand this fork's group metadata; restoring a backup through the
  official app is therefore not a way to preserve groups. Use this fork to restore
  group data.

## Development and testing

The `dev` branch contains the no-goal, habit-conversion, group-habit,
custom-color and numeric tap-to-increment work described above. It is a fork development
branch, not an upstream Loop release.

APK builds are started manually through GitHub Actions. See
[`docs/BUILD.md`](docs/BUILD.md) for Android Studio, command-line and GitHub Actions
build instructions. The workflow runs the core JVM unit tests, compiles the Android instrumentation
tests, and then builds the debug APK; generated artifacts are retained for 14 days.

The fork release uses application ID `org.arfx.habittracker`; debug/test builds use
`org.arfx.habittracker.testing`. Both are separate from the official Loop app
(`org.isoron.uhabits`) and keep their own app data and database.

### Package identity — release decision

- **Debug/testing:** `org.arfx.habittracker.testing` (automatically adds `.testing`).
- **Release:** `org.arfx.habittracker` (the fork's own application identity).
- **Upstream:** The official Loop app remains `org.isoron.uhabits`; the fork does not
  attempt to replace or impersonate that installation.
- **Namespace:** The Kotlin namespace remains `org.isoron.uhabits` and is independent
  of the application ID. Widget actions and habit content URIs intentionally use the
  fork identifier `org.arfx.habittracker`; the FileProvider authority follows the
  selected variant through `${applicationId}`.
- **Signing:** Releases are signed with a dedicated arfx keystore. Keep the keystore
  and passwords private and backed up. Every future release must use the same key
  to update an existing fork installation.

For the first release, create the signing keystore and configure the repository
secrets documented in [`docs/BUILD.md`](docs/BUILD.md). Then push a version tag such
as `v2.3.2`; the Android Release workflow tests, builds and publishes the signed APK.

## License

This fork retains the upstream project's license. See
[`LICENSE.txt`](LICENSE.txt) and the [upstream repository](https://github.com/iSoron/uhabits).
