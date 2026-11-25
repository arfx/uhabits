# Loop Habit Tracker — arfx fork

This repository is a personal fork of [Loop Habit Tracker](https://github.com/iSoron/uhabits).
For the original app description, screenshots, installation information, contribution
guidelines and upstream documentation, see the
[original Loop Habit Tracker README](https://github.com/iSoron/uhabits#readme).

This fork keeps the upstream project as its foundation and adds the features listed
below. The active development branch is
[`feature/habit-conversion-integration`](https://github.com/arfx/uhabits/tree/feature/habit-conversion-integration).
Changes are tested in Android APK builds before any integration into `dev`.

## Fork features

### Habit tracking

- **No-Goal Habits:** Track habits without a numerical or frequency goal. Their
  list indicator is a colored dot instead of a progress ring.
- **Habit type conversion:** Convert between yes/no and numerical tracking while
  preserving historical entries. Positive numerical values become completed checks
  when converting to yes/no; their former value and unit are retained in the entry
  note. Zero and negative values become unchecked entries. Skipped and unknown
  entries retain their states.

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
  than represented as ordinary habit rows.
- **Database backups:** Group definitions, members and completion-rule settings are
  stored in the fork's backup metadata.
- **Upstream compatibility:** The exported database is structured so that the
  official Loop 2.3.1 importer can still import ordinary habits. The official app
  does not understand this fork's group metadata; restoring a backup through the
  official app is therefore not a way to preserve groups. Use this fork to restore
  group data.

## Development status

The features above are implemented on the active feature branch and have been
partially verified with Android testing. They should not be considered an upstream
Loop release. APK builds are started manually through GitHub Actions; no build is
triggered automatically by documentation or branch changes.

Before integrating the feature branch into `dev`, the changes will receive further
testing and an explicit review.

## License

This fork retains the upstream project's license. See
[LICENSE](LICENSE) and the [upstream repository](https://github.com/iSoron/uhabits).
