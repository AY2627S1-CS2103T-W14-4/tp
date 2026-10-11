# Add Gig Title Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Require, display, and persist a validated title for every newly created gig while continuing to load v1.2 gigs that have no stored title.

**Architecture:** Add a focused immutable `GigTitle` value object and thread it through the existing `Gig` model, `addgig` command path, JSON adapter, and list rendering. Keep backward compatibility isolated in `JsonAdaptedGig`: only absent stored titles receive `Untitled gig`; all normal constructors continue to require an explicit title.

**Tech Stack:** Java 25, JavaFX 17, Jackson 2.7, JUnit 5, Gradle

**Spec:** `docs/superpowers/specs/2026-10-10-add-gig-title-design.md`

## Global Constraints

- Newly created gigs require an explicit title; no title-less production constructor is retained.
- Normalize with leading/trailing whitespace removed; require 1–100 normalized characters.
- Permit internal spaces, common punctuation, and duplicate titles.
- Preserve titles in every `Gig` copy operation, including client relinking and UID replacement.
- Load stored gigs whose `title` property is absent as `Untitled gig`, then write that title on the next save.
- Do not remove any existing legacy client, gig, fee, UID, or payment-link migration behavior.
- Keep the feature CLI-first and single-command; do not add mouse-only input or a multi-step form.
- Store titles as plain text in the existing local, human-editable JSON file; introduce no DBMS, server, network call, account, external software, or new dependency.
- Use only platform-independent Java 25/JavaFX behavior and retain the existing single-JAR packaging.
- Keep the packaged JAR below the 100 MB course limit and add no binary assets.
- Deliver in small, independently tested commits so every increment leaves a working product.

## Review Focus

- A title containing only whitespace must fail with `GigTitle.MESSAGE_CONSTRAINTS`.
- A raw title with surrounding spaces and exactly 100 normalized characters must be accepted and normalized.
- A 101-character normalized title must be rejected in both command parsing and stored JSON.
- Duplicate `t/` prefixes must use the existing clear duplicate-prefix error, not silently choose one.
- Editing a client must preserve both the gig title and payment-obligation link to that gig.
- A 100-character title must remain readable without forcing the window beyond the supported 1280x720-at-150%-scale usability floor.

---

### Task 1: Gig title value object and core model

**Files:**
- Create: `src/main/java/gigabyte/model/gig/GigTitle.java`
- Create: `src/test/java/gigabyte/model/gig/GigTitleTest.java`
- Modify: `src/main/java/gigabyte/model/gig/Gig.java`
- Modify: `src/test/java/gigabyte/model/gig/GigTest.java`
- Modify: all production and test call sites constructing `Gig`

**Interfaces:**
- Produces: `GigTitle(String)`, `GigTitle.isValidTitle(String)`, `GigTitle.MESSAGE_CONSTRAINTS`, `GigTitle.UNTITLED`, `Gig.getTitle()`.
- Produces: `Gig(Client, GigTitle, GigStatus, Deadline, Fee)` and `Gig(UUID, Client, GigTitle, GigStatus, Deadline, Fee)`.

- [ ] **Step 1: Write failing `GigTitleTest` cases** for null, blank, surrounding whitespace, punctuation/internal spaces, 100 characters, and 101 characters; add `GigTest` assertions that constructors require a title and `withClient`/`withNewUid` preserve it.
- [ ] **Step 2: Run `./gradlew.bat test --tests gigabyte.model.gig.GigTitleTest --tests gigabyte.model.gig.GigTest`** and verify failure because `GigTitle` and the new constructor/getter do not exist.
- [ ] **Step 3: Implement `GigTitle` and add the required title field to `Gig`**, including equality, hash code, string output, and copy preservation.
- [ ] **Step 4: Update all existing `Gig` constructor call sites** with explicit descriptive test/sample titles; do not add a title-less overload.
- [ ] **Step 5: Re-run the two focused test classes**, then run `./gradlew.bat test` and verify they pass.
- [ ] **Step 6: Commit** with `feat(gig): add required gig title value`.

### Task 2: Add-gig command and parser

**Files:**
- Modify: `src/main/java/gigabyte/logic/parser/CliSyntax.java`
- Modify: `src/main/java/gigabyte/logic/parser/ParserUtil.java`
- Modify: `src/main/java/gigabyte/logic/parser/AddGigCommandParser.java`
- Modify: `src/main/java/gigabyte/logic/commands/AddGigCommand.java`
- Modify: `src/test/java/gigabyte/logic/parser/ParserUtilTest.java`
- Modify: `src/test/java/gigabyte/logic/parser/AddGigCommandParserTest.java`
- Modify: `src/test/java/gigabyte/logic/commands/AddGigCommandTest.java`

**Interfaces:**
- Consumes: `GigTitle` and the new `Gig` constructor from Task 1.
- Produces: `CliSyntax.PREFIX_TITLE`, `ParserUtil.parseGigTitle(String)`, and `AddGigCommand(Index, GigTitle, GigStatus, Deadline, Fee)`.

- [ ] **Step 1: Write failing parser and command tests** for valid normalized titles, missing `t/`, blank `t/`, duplicate `t/`, punctuation, 101 characters, title equality/toString, success feedback, and the resulting `Gig` title.
- [ ] **Step 2: Run the three focused test classes** and verify failures are specifically caused by the missing title-aware interfaces and behavior.
- [ ] **Step 3: Add `PREFIX_TITLE`, `parseGigTitle`, and title-aware parsing/command construction**, updating usage and success text to include the title.
- [ ] **Step 4: Re-run the focused tests**, then run `./gradlew.bat test` and verify they pass.
- [ ] **Step 5: Commit** with `feat(command): require title when adding gig`.

### Task 3: JSON persistence and backward compatibility

**Files:**
- Modify: `src/main/java/gigabyte/storage/JsonAdaptedGig.java`
- Modify: `src/test/java/gigabyte/storage/JsonAdaptedGigTest.java`
- Modify: `src/test/java/gigabyte/storage/JsonGigabyteDataStorageTest.java`
- Create: `src/test/data/JsonGigabyteDataStorageTest/legacyGigsWithoutTitlesGigabyteData.json`
- Modify: `src/test/java/gigabyte/model/GigabyteDataTest.java`

**Interfaces:**
- Consumes: `GigTitle.UNTITLED`, validation, getter, and title-aware `Gig` constructors.
- Produces: JSON `title` serialization and absent-title migration to `Untitled gig`.

- [ ] **Step 1: Write failing adapter tests** proving valid titles round-trip, invalid present titles are rejected, and an absent title maps to `GigTitle.UNTITLED`.
- [ ] **Step 2: Write failing storage tests and fixture** proving old title-less JSON loads, the next save contains `"title" : "Untitled gig"`, and multiple titled gigs survive round trips.
- [ ] **Step 3: Extend the existing client-edit/payment-link model test** to assert the gig title is unchanged after relinking.
- [ ] **Step 4: Run the focused storage/model tests** and verify the expected title-related failures.
- [ ] **Step 5: Add nullable JSON input plus required JSON output in `JsonAdaptedGig`**, validating present titles and using the fallback only when the property is absent.
- [ ] **Step 6: Re-run the focused tests**, then run `./gradlew.bat test` and verify they pass.
- [ ] **Step 7: Commit** with `feat(storage): persist and migrate gig titles`.

### Task 4: UI, sample data, and User Guide

**Files:**
- Modify: `src/main/java/gigabyte/ui/GigListPanel.java`
- Modify: `src/test/java/gigabyte/ui/GigListPanelTest.java`
- Modify: `src/main/java/gigabyte/model/util/SampleDataUtil.java`
- Modify: `src/test/java/gigabyte/model/util/SampleDataUtilTest.java`
- Modify: `docs/UserGuide.md`

**Interfaces:**
- Consumes: `Gig.getTitle()` and the title-aware `Gig` constructor.

- [ ] **Step 1: Write a failing JavaFX list-cell test** that renders an item and asserts its text starts with the title and contains status, deadline, and fee.
- [ ] **Step 2: Write a failing sample-data test** asserting sample data contains titled gigs linked to its canonical clients.
- [ ] **Step 3: Run the focused UI and sample-data tests** and verify the expected failures.
- [ ] **Step 4: Render the title first in `GigListPanel`, allow long title text to wrap within the list cell, and add representative titled sample gigs** without changing client filtering, empty states, or the window's minimum width.
- [ ] **Step 5: Update `docs/UserGuide.md`** with `t/TITLE`, validation rules, examples, confirmation fields, `Untitled gig` migration behavior, and the command summary.
- [ ] **Step 6: Re-run the focused tests**, then run `./gradlew.bat test` and verify they pass.
- [ ] **Step 7: Commit** with `docs(ui): show and document gig titles`.

### Task 5: End-to-end verification

**Files:**
- Review all files changed in Tasks 1–4.

**Interfaces:**
- Consumes: the complete feature from Tasks 1–4.

- [ ] **Step 1: Search for every `new Gig(` call** and verify each supplies an intentional title; search every `withClient`/`withNewUid` path and verify preservation.
- [ ] **Step 2: Run `./gradlew.bat clean test`** and verify the complete test suite passes with no failures.
- [ ] **Step 3: Run `./gradlew.bat checkstyleMain checkstyleTest`** and verify both checks pass.
- [ ] **Step 4: Run `./gradlew.bat shadowJar`** and verify `build/libs/gigabyte.jar` is produced, remains below 100 MB, and no new runtime dependency was added.
- [ ] **Step 5: Inspect the generated test and checkstyle reports** for hidden failures or warnings.
- [ ] **Step 6: Review the final diff against every acceptance criterion and course constraint**: CLI-first, local human-editable JSON, no DBMS/server/network/new dependency, OO Java 25, platform-independent behavior, usable long-title rendering, and retained legacy storage support.
- [ ] **Step 7: Commit any verification-only corrections** with a narrowly scoped conventional commit message.
