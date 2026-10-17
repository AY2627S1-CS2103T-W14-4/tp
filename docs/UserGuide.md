---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# Gigabyte User Guide

**Gigabyte** is a desktop application for freelancers who manage multiple clients, projects, and payment deadlines. It is optimized for use through a Command Line Interface (CLI) while retaining the benefits of a Graphical User Interface (GUI).

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download the latest `.jar` file from [here](https://github.com/AY2627S1-CS2103T-W14-4/tp/releases).

1. Copy the file to the folder you want to use as the _home folder_ for Gigabyte.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar gigabyte.jar`.<br>
   A GUI similar to the one below should appear in a few seconds. Note how the app contains some sample data.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   Some example commands you can try:

   * `list` : Lists all clients.

   * `add n/John Doe p/98765432 e/johnd@example.com a/John street, block 123, #01-01` : Adds a client named `John Doe` to the client list.

   * `delete 3` : Deletes the 3rd client shown in the current list.

   * `addgig 1 t/Website redesign s/IN_PROGRESS d/2027-01-31 f/1250.00` : Adds a titled gig for the first displayed client.

   * `setstatus 1 2 s/COMPLETED` : Marks the second gig for the first displayed client as completed.

   * `addpayment 1 1 a/500.00 d/2027-02-15` : Records an unpaid obligation for that client's first gig.

   * `markpaid 1 1 1` : Marks the first displayed payment for the first displayed client's first gig as paid.

   * `clear` : Deletes all clients.

   * `exit` : Exits the app.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

## Features

<box type="info" seamless>

**Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `n/NAME [t/TAG]` can be used as `n/John Doe t/friend` or as `n/John Doe`.

* Items followed by `...` can appear zero or more times.<br>
  For example, `[t/TAG]... ` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `n/NAME p/PHONE_NUMBER`, `p/PHONE_NUMBER n/NAME` is also acceptable.

* Extraneous parameters for commands that take no parameters, such as `help`, `list`, `exit`, and `clear`, are ignored.<br>
  For example, `help 123` is interpreted as `help`.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</box>

### Viewing help: `help`

Shows a message explaining how to access the help page.

![help message](images/helpMessage.png)

Format: `help`


### Adding a client: `add`

Adds a client to the client list.

Format: `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]... `

<box type="tip" seamless>

**Tip:** A client can have any number of tags, including zero.
</box>

Examples:
* `add n/John Doe p/98765432 e/johnd@example.com a/John street, block 123, #01-01`
* `add n/Betsy Crowe t/friend e/betsycrowe@example.com a/Newgate Prison p/1234567 t/criminal`

### Listing all clients: `list`

Shows a list of all clients in the client list.

Format: `list`

### Understanding indexes and filtered results

Gigabyte uses one-based indexes: the first displayed item is `1`, the second is `2`, and so on. Zero, negative
numbers, decimals, text, and indexes larger than the displayed list are invalid.

* A **client index** always refers to the numbered client in the list currently shown on the left. Commands such as
  `addgig`, `addpayment`, `markpaid`, `edit`, and `delete` use this displayed list.
* `find` replaces the displayed client list with its matching results. A later command therefore uses the numbers in
  those results, not the numbers from the earlier full list. Run `list` to restore the full client list.
* A **gig index** is the gig's one-based position among the chosen client's gigs. It is not an index into all gigs in
  Gigabyte. Selecting that client in the UI lets you confirm the order shown in the **Gigs** panel, but clicking the
  client is not required before running `addpayment`.
* The currently implemented payment command does not take a payment index: the two indexes in `addpayment` are the
  client index and that client's gig index, in that order.
* `setstatus` uses the same client and gig index rules as `addpayment`.

Example: after `find Bernice`, `addgig 1 ...` targets the first client in the filtered results. After `list`, the same
client might have a different index.

### Viewing gigs and payment obligations

Select a client in the client list to show only that client's gigs in the **Gigs** panel. Select a gig to show only
its payment obligations in the **Payment obligations** panel beneath the gig list. Payment obligations are numbered
from 1 and ordered by due date, earliest first. Obligations with the same due date retain the order in which they were
recorded. These displayed payment indexes restart from 1 for each selected gig; `markpaid` uses them.

Each payment row shows its amount, due date, and current display state:

* `PAID`: the obligation has been marked as paid.
* `OUTSTANDING`: the obligation is unpaid and is due today or later.
* `OVERDUE`: the obligation is unpaid and its due date is before the current local date.

Paid, outstanding, and overdue rows use distinct colors. `OVERDUE` is calculated for display only and does not change
the saved paid status. If there is no selection or no data for the selection, the panels explain whether to select a
client, select a gig, or that the selected gig has no payment obligations.

### Viewing outstanding payments: `outstanding`

Format: `outstanding`

Lists **unpaid** obligations across all gigs and clients, including clients hidden by `find`. Rows show the client,
gig title, amount, due date, and payment state. Results sort by due date, earliest first; ties keep their recorded order.

* `OVERDUE` means an unpaid obligation is due **before today**, using your computer's local calendar date.
  Obligations due today or later display `OUTSTANDING`. Text and color identify overdue items, and the display
  refreshes at local midnight without changing records.
* Paid obligations are excluded. The empty state is `No outstanding payment obligations.`.
* Results wrap and scroll vertically. Selecting a client and gig restores that gig's payment view.
  Overview row numbers are not `markpaid` payment indexes; select the client and gig to check the per-gig indexes.
* Re-run `outstanding` after an obligation is marked paid to confirm it is excluded. Displaying results does not
  change records or the client list. The command takes no arguments; `outstanding extra` is rejected.

### Filtering gigs by status: `gigs`

Format: `gigs s/STATUS`

Shows matching gigs across **all clients**, even when `find` has narrowed the client list. Each row shows the title,
client name, status, deadline, and agreed fee. Results keep the order in which gigs were recorded and scroll vertically.

* `STATUS` is `NOT_STARTED`, `IN_PROGRESS`, or `COMPLETED`, ignoring case and surrounding whitespace.
* Supply exactly one `s/` argument. Missing, unsupported, repeated, or extra arguments are rejected.
* No matches displays `No gigs with status STATUS.`. Filtering does not modify stored records or the client list.
* Running `gigs` clears client/gig selections and the payment panel. Select a result to view its payments, or select
  a client to return to that client's gigs. `addpayment` and `markpaid` use that client's gig order, not this status
  view.

Examples: `gigs s/IN_PROGRESS`, `gigs s/completed`. Invalid examples: `gigs`, `gigs s/WAITING`,
`gigs s/IN_PROGRESS s/COMPLETED`.

### Viewing active deadlines: `deadlines`

Format: `deadlines`

Shows every gig whose status is not `COMPLETED`, across all clients, in the **Gigs** panel.
Rows contain the gig title, client name, status, deadline, and agreed fee. Results are ordered by
deadline ascending; equal deadlines are ordered deterministically by client name, gig title, and gig UID.

Deadlines before the current local date remain visible and are labelled `OVERDUE`. Completed gigs are
never included in this view. The command does not change the client filter. Selecting a client returns
the panel to that client's normal gig view, and running `deadlines` again restores the overview. The
overview updates automatically when gigs are added, changed, or removed.

If there are no active gigs, the panel displays `There are no active gig deadlines.`

### Creating a gig: `addgig`

Creates a gig linked to an existing client and saves it automatically.

Format: `addgig INDEX t/TITLE s/STATUS d/DEADLINE f/FEE`

* `INDEX` is the positive index shown in the current client list, including filtered `find` results.
* All four fields are required and may appear in any order. Each prefix must appear exactly once.
* `TITLE` is a short description containing 1–100 characters after leading and trailing whitespace is removed.
  Internal spaces and common punctuation are allowed. Titles do not need to be unique.
* `STATUS` is `NOT_STARTED`, `IN_PROGRESS`, or `COMPLETED` (case-insensitive).
* `DEADLINE` is a valid calendar date in `yyyy-MM-dd` format.
* `FEE` is a positive amount with at most two decimal places, without a currency symbol.
* The confirmation shows the linked client, title, status, deadline, and agreed fee. The displayed client list stays
  unchanged, including when it is filtered.
* Gigs, including their titles, are retained after restarting the app. Every saved gig must have a valid title.

Successful examples:

* `addgig 1 t/Website redesign s/NOT_STARTED d/2027-01-31 f/1250.00`
* `find Bernice` followed by
  `addgig 1 t/Product photography s/in_progress d/2027-02-28 f/500.50`
  creates a titled gig for the first matching client.

Unsuccessful examples:

Command | Why it is rejected
--------|-------------------
`addgig 1 t/ s/NOT_STARTED d/2027-01-31 f/1250.00` | The title is blank after surrounding whitespace is removed.
`addgig 1 t/Website redesign s/WAITING d/2027-01-31 f/1250.00` | `WAITING` is not a supported status.
`addgig 1 t/Website redesign s/NOT_STARTED d/2027-02-29 f/1250.00` | 2027 is not a leap year, so the date is invalid.
`addgig 1 t/Website redesign s/NOT_STARTED d/2027-01-31 f/0` | Fees must be greater than zero.
`addgig 1 t/Website redesign t/Second title s/NOT_STARTED d/2027-01-31 f/1250.00` | The single-valued `t/` prefix is repeated.
`addgig 0 t/Website redesign s/NOT_STARTED d/2027-01-31 f/1250.00` | Client indexes start at 1.

### Updating a gig's status: `setstatus`

Changes the status of one existing gig and saves it automatically. The gig keeps its title, client, deadline, agreed
fee, and UID. Existing payment obligations remain linked to it, and the **Gigs** panel refreshes as soon as the
command succeeds.

Format: `setstatus CLIENT_INDEX GIG_INDEX s/STATUS`

* `CLIENT_INDEX` is the positive index in the currently displayed client list, including filtered `find` results.
* `GIG_INDEX` is the positive position in that client's gig list, counted from the top. It uses the same order as the
  **Gigs** panel and `addpayment`.
* `STATUS` is `NOT_STARTED`, `IN_PROGRESS`, or `COMPLETED`; parsing ignores case and surrounding whitespace.
* Supply exactly two indexes and one `s/` field. Missing or unsupported statuses, repeated `s/` fields, extra
  positional arguments, and indexes outside the displayed lists are rejected without changing stored data.
* Requesting the gig's current status is rejected. Statuses can otherwise be changed in either direction.

Examples:

* `setstatus 1 2 s/IN_PROGRESS`
* `setstatus 1 2 s/completed`
* `find Bernice` followed by `setstatus 1 1 s/COMPLETED` updates the first gig belonging to the first matching client.

Unsuccessful examples:

Command | Why it is rejected
--------|-------------------
`setstatus 1 2` | A status is required.
`setstatus 1 2 s/WAITING` | `WAITING` is not a supported status.
`setstatus 1 2 s/IN_PROGRESS s/COMPLETED` | The single-valued `s/` prefix is repeated.
`setstatus 1 2 3 s/COMPLETED` | Exactly two indexes are required.
`setstatus 1 99 s/COMPLETED` | The selected client does not have a gig at position 99.
`setstatus 1 2 s/NOT_STARTED` | Gig 2 is already `NOT_STARTED`.

### Recording a payment obligation: `addpayment`

Records an unpaid payment obligation against an existing gig and saves it automatically.

Format: `addpayment CLIENT_INDEX GIG_INDEX a/AMOUNT d/DUE_DATE`

* `CLIENT_INDEX` is the positive index in the currently displayed client list, including `find` results.
* `GIG_INDEX` is the positive position in that client's gig list, counted from the top starting at 1. Click the
  client to inspect the same order in the **Gigs** panel. It is not an index in the complete list of all clients' gigs.
* Both indexes and both fields are required. The `a/` and `d/` fields may appear in either order, once each.
* `AMOUNT` must be positive with at most two decimal places, without a currency symbol.
* `DUE_DATE` must be a valid calendar date in `yyyy-MM-dd` format.
* Multiple obligations may be recorded for the same gig. New obligations start unpaid.
* Invalid input leaves existing clients, gigs, and obligations unchanged.
* Obligations survive restarting the app. The save file must include a `paymentObligations` array, even when empty.

Successful examples:

* `addpayment 1 2 a/500.00 d/2027-01-31` records an obligation for the second gig of the first displayed client.
* `find Alex` followed by `addpayment 1 1 d/2027-02-15 a/250` records an obligation for the first gig belonging to
  the first matching client. The prefixed fields may appear in either order.

Unsuccessful examples:

Command | Why it is rejected
--------|-------------------
`addpayment 0 1 a/500.00 d/2027-01-31` | Client indexes start at 1.
`addpayment 1 0 a/500.00 d/2027-01-31` | Gig indexes start at 1.
`addpayment 1 99 a/500.00 d/2027-01-31` | The selected client does not have a gig at position 99.
`addpayment 1 1 a/-10 d/2027-01-31` | Amounts must be positive numbers.
`addpayment 1 1 a/500.00 d/31-01-2027` | Dates must use `yyyy-MM-dd`.
`addpayment 1 1 a/500.00 a/600.00 d/2027-01-31` | The single-valued `a/` prefix is repeated.

The confirmation names the client and gig position, amount, and due date. To see the saved obligation, select the
client and gig in the application; the **Payment obligations** panel refreshes immediately.

### Marking a payment as paid: `markpaid`

Marks one outstanding payment obligation as paid and saves it automatically.

Format: `markpaid CLIENT_INDEX GIG_INDEX PAYMENT_INDEX`

* `CLIENT_INDEX` is the positive index in the currently displayed client list, including `find` results.
* `GIG_INDEX` is the positive position in that client's gig list, in the order shown in the **Gigs** panel.
* `PAYMENT_INDEX` is the positive position in the selected gig's payment list, ordered by due date and then by the
  order in which obligations were recorded. The payment panel and command use this same ordering.
* All three indexes are required. Zero, negative, non-numeric, missing, extra, and out-of-range indexes are rejected.
* A payment that is already paid cannot be marked paid again. This leaves the saved data unchanged.
* The selected row changes to `PAID` immediately. Its gig, amount, and due date remain the same, and the paid state
  survives restarting Gigabyte.

Successful examples:

* `markpaid 2 1 3` marks the third displayed payment of the first displayed gig belonging to the second displayed
  client.
* `find Alex` followed by `markpaid 1 1 2` marks the second displayed payment for the first matching client's first
  gig.

Unsuccessful examples:

Command | Why it is rejected
--------|-------------------
`markpaid 0 1 1` | Client indexes start at 1.
`markpaid 1 0 1` | Gig indexes start at 1.
`markpaid 1 1 0` | Payment indexes start at 1.
`markpaid 1 1 99` | The selected gig has no payment at position 99.
`markpaid 1 1` | Exactly three indexes are required.
`markpaid 1 1 1 2` | Extra positional indexes are not accepted.
`markpaid 1 1 1` on an already paid row | A paid obligation cannot be marked paid a second time.

### Editing a client: `edit`

Edits an existing client in the client list.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]... `

* Edits the client at the specified `INDEX`. The index refers to the index number shown in the displayed client list. The index **must be a positive integer** 1, 2, 3, ...
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* When editing tags, all of the client's existing tags are removed; adding tags is not cumulative.
* To remove all of a client's tags, enter `t/` without a tag after it.

Examples:
*  `edit 1 p/91234567 e/johndoe@example.com` Edits the phone number and email address of the 1st client to be `91234567` and `johndoe@example.com` respectively.
*  `edit 2 n/Betsy Crower t/` Edits the name of the 2nd client to be `Betsy Crower` and clears all existing tags.

### Locating clients by name: `find`

Finds clients whose names contain any of the given keywords.

Format: `find KEYWORD [MORE_KEYWORDS]`

* The search is case-insensitive; for example, `hans` matches `Hans`.
* Keyword order does not matter; for example, `Hans Bo` matches `Bo Hans`.
* The search considers only names.
* Only full words match; for example, `Han` does not match `Hans`.
* Clients matching at least one keyword are returned (an `OR` search); for example, `Hans Bo` returns `Hans Gruber` and `Bo Yang`.

Examples:
* `find John` returns `john` and `John Doe`
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)

### Deleting a client: `delete`

Deletes the specified client from the client list.

Format: `delete INDEX`

* Deletes the client at the specified `INDEX`.
* A client with associated gigs cannot be deleted; an error explains this restriction.
* The index refers to the index number shown in the displayed client list.
* The index **must be a positive integer** 1, 2, 3, ...

Examples:
* `list` followed by `delete 3` deletes the 3rd sample client, who has no associated gigs.
* `find David` followed by `delete 1` deletes the 1st client in the results of the `find` command.

### Clearing all entries: `clear`

Clears all clients, their gigs, and payment obligations.

Format: `clear`

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

Gigabyte automatically saves data after every successfully executed command. You do not need to save manually.
Client, gig, and payment-obligation data is restored when you next launch the app from the same home folder.

### Editing the data file

Gigabyte data is saved automatically as a JSON file `[JAR file location]/data/addressbook.json`. Advanced users are welcome to update data directly by editing that data file.

Client records are stored in the `clients` array with stable `uid` values. Gigs refer to clients by `clientUid`, store
their titles as plain text, and are referenced by payment obligations using `gigUid`. Fees and payment amounts are
stored as positive integer cents. The `clients`, `gigs`, and `paymentObligations` arrays are all required; use `[]`
for an empty collection. Each client must include its `uid`, contact fields, and `tags` array. Each gig must include
its `uid`, `clientUid`, `title`, `status`, `deadline`, and `feeCents`. Each payment obligation must include its
`gigUid`, `amountCents`, `dueDate`, and `paid` state.

Only the current save format is supported. Older files using `persons`, missing UUIDs or titles, client-name links,
decimal `fee`/`amount` strings, or `gigIndex` references cannot be loaded. Unknown fields and missing required fields
are rejected; Gigabyte does not upgrade older save files.

<box type="warning" seamless>

**Caution:**
If the data file is invalid or uses an unsupported older format, Gigabyte starts with an empty client list at the next run. The invalid file remains on disk until you run a command (Gigabyte saves after every command). Still, we recommend backing up the file before editing it.<br>
Furthermore, certain edits can cause Gigabyte to behave in unexpected ways (e.g., if a value entered is outside of the acceptable range). Therefore, edit the data file only if you are confident that you can update it correctly.
</box>

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install the app on the other computer and overwrite the data file it creates with a save file in the current format from your previous Gigabyte home folder. Older save formats are not supported.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **If you minimize the Help Window** and then run the `help` command (or use the `Help` menu, or the keyboard shortcut `F1`) again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action     | Format, Examples
-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------
**Add**    | `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]... ` <br> e.g., `add n/James Ho p/22224444 e/jamesho@example.com a/123, Clementi Rd, 1234665 t/friend t/colleague`
**Add gig** | `addgig INDEX t/TITLE s/STATUS d/DEADLINE f/FEE` <br> e.g., `addgig 1 t/Website redesign s/NOT_STARTED d/2027-01-31 f/1250.00`
**Add payment** | `addpayment CLIENT_INDEX GIG_INDEX a/AMOUNT d/DUE_DATE` <br> e.g., `addpayment 1 2 a/500.00 d/2027-01-31`
**Mark payment paid** | `markpaid CLIENT_INDEX GIG_INDEX PAYMENT_INDEX` <br> e.g., `markpaid 2 1 3`
**Set gig status** | `setstatus CLIENT_INDEX GIG_INDEX s/STATUS` <br> e.g., `setstatus 1 2 s/COMPLETED`
**Clear**  | `clear`
**Delete** | `delete INDEX`<br> e.g., `delete 3`
**Edit**   | `edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS] [t/TAG]... `<br> e.g.,`edit 2 n/James Lee e/jameslee@example.com`
**Exit**   | `exit`
**Find**   | `find KEYWORD [MORE_KEYWORDS]`<br> e.g., `find James Jake`
**Filter gigs** | `gigs s/STATUS`<br> e.g., `gigs s/IN_PROGRESS`
**Active deadlines** | `deadlines`
**Outstanding payments** | `outstanding`
**List**   | `list`
**Help**   | `help`
