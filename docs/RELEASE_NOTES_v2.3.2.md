# Habit Tracker v2.3.2

First feature release of the arfx fork, based on Loop Habit Tracker 2.3.2.

## What's included

### Habit tracking
- Added **No-Goal Habits** for tracking without a numerical or frequency target.
- Added conversion between yes/no and numerical habits while preserving historical entries.
- Added **numeric tap-to-increment**: tap a numerical value to add one; long-press to open manual number entry.

### Custom colors
- Added a custom habit color picker with a rainbow palette shortcut.
- Persist custom colors and include their hexadecimal values in CSV exports.

### Group Habits
- Added collapsible groups for ordinary habits.
- Added configurable **OR**, **AND**, and **N aus M** completion rules.
- Added independent group goals and group-level insights.
- Persist group definitions and rule settings in fork backups.

### Data and compatibility
- Kept ordinary-habit CSV export compatible with the upstream format.
- Preserved the ability to import ordinary habits into the official Loop 2.3.1 app. Fork-specific group metadata is only understood by this fork.

## Installation

Download `uhabits-android-release.apk` below and install it on Android 9 (API 28) or newer.

This fork uses the separate application ID `org.arfx.habittracker`, so it can coexist with the official Loop app. Its data and database are separate.

## Important notes

- This is an independent community fork, not an official Loop Habit Tracker release.
- Back up your data before installing or upgrading.
- Group Habits and other fork-specific metadata are not understood by the official upstream app.
- Future updates of this fork must be signed with the same arfx release key used for this release. Keep that keystore and its passwords safe.
