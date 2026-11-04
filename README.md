# Loop Habit Tracker – Personal Fork

A personal fork of [Loop Habit Tracker](https://github.com/iSoron/uhabits), built on its lightweight, offline-first approach to habit tracking.

The goal of this fork is to add a small set of carefully selected features while keeping the app simple, reliable, and close to upstream Loop.

## Project Goals

### 1. No-Goal Habits

Track activities without setting a required frequency or numerical target.

- Available for both Yes/No and Numerical habits.
- Continue recording activity and preserve historical entries.
- Inactive days are not treated as failures.
- No missed-goal penalties are applied to scores or streaks.

No Goal is a goal configuration, not a third habit type. The existing Yes/No and Numerical types remain unchanged.

### 2. Habit Conversion

Allow existing habits to switch between tracking modes:

- Yes/No ↔ Numerical
- Yes/No ↔ No Goal
- Numerical ↔ No Goal

Preserve historical entries and notes wherever possible. If a conversion changes the meaning of past values, explain the conversion and ask for confirmation. Never silently discard or overwrite user data.

### 3. Overall / Group Streaks

Allow multiple habits to contribute to one shared streak.

- A group day counts as completed when at least one habit in the group is completed.
- Calculate the shared streak independently from individual habit streaks.
- Preserve each habit's own history and statistics.
- Keep group streaks separate from categories.

## Development Principles

- Stay close to upstream Loop to simplify future updates and merges.
- Keep the app offline-first and respect its existing data model.
- Isolate custom features and keep calculations testable.
- Protect user data, especially during conversions and database changes.
- Add tests for new calculations and conversion rules.

These are project goals; they do not imply that the features are already implemented.

## Upstream

- Original project: [iSoron/uhabits](https://github.com/iSoron/uhabits)
- Build instructions: [docs/BUILD.md](https://github.com/iSoron/uhabits/blob/dev/docs/BUILD.md)

## License

This fork is based on Loop Habit Tracker and is distributed under the GNU General Public License v3.0 (GPL-3.0). See the repository's license file for the full terms.

Original project copyright: Copyright (C) 2016-2025 Álinson Santos Xavier.
