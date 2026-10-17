# Add a Title to Every Gig — Design

> Updated on 2026-10-17 to reflect support for the current save format only.

## Purpose

Every gig must have a short descriptive title so freelancers can distinguish similar work for the same client. The title is part of the core gig representation, is entered with `t/` when adding a gig, is shown prominently in the gig list, and survives persistence.

## Domain model

Add an immutable `GigTitle` value object in `gigabyte.model.gig`. It stores the normalized title after removing leading and trailing whitespace. A valid title contains at least one non-whitespace character and is at most 100 characters after normalization. Internal whitespace, punctuation, and duplicate values are allowed.

`Gig` gains a required `GigTitle` field and getter. Both public constructors require it. Equality, hashing, and string rendering include it. Copy operations such as `withClient` and `withNewUid` preserve it unchanged.

## Command and parsing

`addgig` becomes:

```text
addgig CLIENT_INDEX t/TITLE s/STATUS d/DEADLINE f/FEE
```

`CliSyntax` exposes a semantic `PREFIX_TITLE` for the existing `t/` token. `ParserUtil.parseGigTitle(String)` centralizes validation and normalization. `AddGigCommandParser` requires exactly one title prefix, reports duplicate `t/` prefixes through the existing duplicate-prefix mechanism, and constructs `AddGigCommand` with a `GigTitle`.

`AddGigCommand` stores the title, includes it when constructing the `Gig`, and mentions it in usage and success messages.

## Storage validation

`JsonAdaptedGig` reads and writes a required `title` property. A missing, null, non-textual, blank, or overlong title causes loading to fail. No default title is supplied.

Storage accepts only the current format: all collections, client and gig UUIDs, UUID references, titles, and integer-cent amounts are required. Older formats and unknown fields are rejected.

## UI and sample data

`GigListPanel` renders each item with the title first and the existing status, deadline, and fee after it. Long valid titles wrap within the existing list rather than increasing the application's minimum width, preserving usability at the course's supported resolutions and display scales. Sample data includes titled gigs linked to canonical sample clients so the UI demonstrates the feature.

## Course-constraint compliance

- The change evolves the existing brownfield code in small, independently tested increments.
- Gig creation remains a one-shot CLI command optimized for fast typists; the GUI remains visual feedback only.
- `GigTitle` is an OO value object and uses only Java 25/JDK APIs already available to the project.
- Titles remain in the existing local, human-editable JSON file as plain text. No DBMS, remote server, account, network access, external software, or new dependency is introduced.
- The implementation avoids OS-specific behavior and continues to package into the existing single portable JAR without an installer.
- The change adds no binary assets and must keep the packaged deliverable below 100 MB.

## Testing

Tests cover title normalization and boundaries, gig copy/equality behavior, parser missing/blank/duplicate/valid titles, command construction, client edits preserving titles, JSON adapter validation, full storage round trips, rejection of title-less and other older data, UI text, and sample data. The User Guide documents the new command format, validation, supported save format, examples, and command summary.

## Out of scope

Gig descriptions, notes, attachments, and globally unique titles remain out of scope.
