---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# Gigabyte Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* Gigabyte is based on the [AddressBook-Level3](https://github.com/se-edu/addressbook-level3) project created by the
  [SE-EDU initiative](https://se-education.org). Its architecture, much of its `Main`, `Commons`, `Storage`, and `UI`
  code, its test infrastructure, and the structure and some text and diagrams of this Developer Guide and the User
  Guide were adapted from that project.
* Libraries used: [JavaFX](https://openjfx.io/) (GUI), [Jackson](https://github.com/FasterXML/jackson) (JSON
  serialization), and [JUnit 5](https://junit.org/junit5/) (testing).
* Tools used: [Gradle](https://gradle.org/) with the [Shadow plugin](https://gradleup.com/shadow/) (build and JAR
  packaging), [PlantUML](https://plantuml.com/) (UML diagrams), and [MarkBind](https://markbind.org/) (documentation
  website).

### Notable uses of AI

* **Lim Kerjun (`patientotter`)** used **OpenAI Codex and ChatGPT**
  extensively to generate and refine source code for gig and payment
  commands, generate and extend automated tests, and draft documentation
  and UML diagrams. These tools also assisted with preliminary pull
  request reviews, debugging implementation and build issues, and
  developing manual and automated test scenarios.
* **Clement Neo (`approxneo`)** used **OpenAI Codex with Computer Use**
  to generate and refine source code for storage and data-file loading,
  and navigation commands. They were also used to generate and extend
  automated tests, and draft documentation.
* **Axel Heng (`axelheng`)** used **OpenAI Codex and ChatGPT** to assist
  with source-code development, debugging, test generation, and documentation
  refinement. The generated suggestions were reviewed, adapted, and validated
  before being included.
* **Tang Ming Hong (`minghong-dev`)** used **Claude Code** to generate and refine
  source code, generate and extend automated tests, plan features, and draft and
  restructure documentation. **ChatGPT** was used to validate and cross-check
  Claude Code's plans and code changes. The generated output was reviewed,
  adapted, and tested before being included.

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" width="280" />

The ***Architecture Diagram*** given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/MainApp.java)) is in charge of the app launch and shut down.
* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The *Sequence Diagram* below shows how the components interact with each other for the scenario where the user issues the command `delete 1`.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentManagers.puml" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `ClientListPanel`,
`GigListPanel`, `PaymentObligationListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit
from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.
Selecting a client in `ClientListPanel` passes that client to `GigListPanel`, which filters the observable gig list
by the client's stable identifier and renders each gig's title, status, deadline, and fee. Selecting a gig then passes
it to `PaymentObligationListPanel`, which shows that gig's due-date-ordered payments and their paid, outstanding, or
overdue display states. `MainWindow` also switches these panels into the cross-client `gigs`, `findgig`,
`deadlines`, and `outstanding` overview modes based on fields carried by `CommandResult` (see
[Cross-client gig and payment views](#cross-client-gig-and-payment-views)).

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Client`, `Gig`, and `PaymentObligation`
  objects from the model.
* displays gigs and payments for the current selection without changing the client filter used by commands.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.
The command resolves the index against the currently displayed client list and passes the selected `Client` to
the model. Invalid indexes and clients with associated gigs are rejected; only a successful deletion creates a
`CommandResult`. The diagram omits storage interactions and assumes the save succeeds on the successful path.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

<box type="info" seamless>

**Note:** The lifeline for `DeleteClientCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, the lifeline continues till the end of diagram.
</box>

The following sequence diagram shows how the `findgig` command parses title keywords,
creates a predicate, and applies the search result to the gig list displayed in the UI.

<puml src="diagrams/tracing/FindGigSequenceDiagram.puml" alt="Interactions for the `findgig` Command" />


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `GigabyteParser` object, which in turn creates a parser that matches the command (e.g., `DeleteClientCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteClientCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a client).<br>
   The command may make several calls to the `Model`; interactions within the model are omitted from this diagram.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:
* When called upon to parse a user command, the `GigabyteParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddClientCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddClientCommand`). The `GigabyteParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddClientCommandParser` and `DeleteClientCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.
* `AddGigCommandParser` parses one displayed client index plus `t/`, `s/`, `d/`, and `f/` fields.
  `AddPaymentCommandParser` parses displayed client and per-client gig indexes plus `a/` and `d/` fields. Both reject
  missing or repeated single-valued prefixes before their commands update the model.
* `EditGigCommandParser` accepts a displayed client index, a per-client gig index, and at least one of `t/`, `d/`,
  or `f/`. It rejects `s/`, so status changes always go through `setstatus`. `DeleteGigCommandParser` accepts the
  two indexes only. `FindGigCommandParser` accepts title keywords and rejects an empty query.
  `SetGigStatusCommandParser` parses two indexes and one `s/` field. `EditPaymentCommandParser` parses three indexes
  and at least one of `a/` or `d/`; `DeletePaymentCommandParser`, `MarkPaymentPaidCommandParser`, and
  `MarkPaymentUnpaidCommandParser` parse three indexes only. `FilterGigsCommandParser` parses one `s/` field.
* Some prefixes are shared by different commands (e.g., `t/` is a tag for `add` but a title for `addgig`, and `a/` is
  an address for `add` but an amount for `addpayment`). Each `XYZCommandParser` passes only the prefixes it accepts
  to `ArgumentTokenizer`, so the meaning of a prefix depends on the command being parsed.
* `summary`, `list`, `clear`, `help`, and `exit` have no parser class: `GigabyteParser` creates these commands
  directly and ignores any arguments. `deadlines` and `outstanding` are also created directly, but `GigabyteParser`
  rejects any arguments supplied to them. Overview commands return `CommandResult` view instructions that `MainWindow` applies
  without changing the current client filter.

### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="450" />


The `Model` component,

* stores all `Client`, `Gig`, and `PaymentObligation` objects. Clients are contained in a `UniqueClientList`; gigs and
  payment obligations are exposed as unmodifiable observable lists.
* stores the `Client` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Client>` that the UI can observe and bind to, so the UI updates when the list changes.
* uses stable UUIDs to preserve links from each gig to its client and from each payment obligation to its gig. Client
  edits replace linked immutable objects while retaining those identifiers and links. Payment edits replace the
  selected immutable obligation while preserving its canonical gig and paid state. Payment-status changes replace
  the exact immutable obligation while retaining its gig, amount, and due date.
* replaces gigs through `Model#setGig` when details or status change. `GigabyteData#setGig` relinks dependent
  payments to the replacement object. `EditGigCommand` preserves the gig UID, client, status, and payments; a
  deadline edit refreshes and can reorder the deadline overview. `FindGigCommand` searches all gig titles without
  changing the filtered client list.
* deletes gigs by stable UID through `Model#deleteGig` and `GigabyteData#removeGig`. Any paid or unpaid obligation
  raises `GigHasPaymentsException` before mutation. Removing one observable-list entry preserves other references;
  the gig panel clears a removed selection before its filtered/sorted views process the removal.
* deletes payments through `Model#deletePaymentObligation`, delegated to `GigabyteData`. Removal uses object
  identity to delete exactly one stored instance because separate obligations can have identical fields.
  `DeletePaymentCommand` resolves indexes using `GigOrdering` and `PaymentObligationOrdering`, as `markpaid` does.
  The observable removal refreshes the selected payment list or outstanding overview and its empty state.
  `LogicManager` saves the resulting data through the existing storage path after successful execution.
* defines the order in which a client's gigs and a gig's payment obligations are numbered, through the
  `GigOrdering` and `PaymentObligationOrdering` utility classes. Commands and the UI both use these classes, so a
  displayed index always refers to the same object in both places.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)


<box type="info" seamless>

**Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `GigabyteData`, and each `Client` references tags from that list. This lets `GigabyteData` maintain one `Tag` object per unique tag instead of each `Client` holding its own `Tag` objects.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="550" />

The `Storage` component,
* can save both Gigabyte data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonGigabyteDataStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)
* serializes clients, gigs, and payment obligations through `JsonAdaptedClient`, `JsonAdaptedGig`, and
  `JsonAdaptedPaymentObligation`. Gigs store `clientUid`; payment obligations store `gigUid`; monetary values are
  stored as integer cents.
* reads only the current save format through `JsonUtil.readJsonFileStrict`, rejecting unknown fields and
  fractional values for integer fields. `JsonAdaptedGig` and `JsonAdaptedPaymentObligation` read `feeCents`,
  `amountCents` and `paid` through `StrictJsonDeserializers`, which accept only a JSON integer or a JSON boolean
  and reject quoted values such as `"5025"` or `"true"` instead of converting them. The adapters require all three
  top-level arrays and each record's required fields; IDs and titles are never generated during loading. Older
  formats fail with `DataLoadingException`. User preferences continue to use the separate, permissive
  `JsonUtil.readJsonFile` path.
* migrates `data/addressbook.json` to `data/gigabyte.json` during `MainApp.init`, before loading data, if the current
  file does not exist. `JsonGigabyteDataStorage.migrateLegacyDataFile` moves the file without changing its contents
  or replacing an existing destination. An `IOException` during migration stops startup.

The save-file fields are:

| Object | Required fields |
| --- | --- |
| Root | `clients`, `gigs`, `paymentObligations` (arrays, including when empty) |
| Client | `uid`, `name`, `phone`, `email`, `address`, `tags` |
| Gig | `uid`, `clientUid`, `title`, `status`, `deadline`, `feeCents` |
| Payment obligation | `gigUid`, `amountCents`, `dueDate`, `paid` |

UUIDs must be valid, references must resolve to saved records, titles must be valid text, and monetary values must
be positive JSON integers of cents. `paid` must be JSON `true` or `false`. Null collections and null records are
rejected. There is no automatic conversion of quoted numbers or booleans, `persons`, name- or index-based
references, decimal amount fields, or missing IDs and titles.

#### Data file format

`MainApp` uses `data/gigabyte.json` for application data and `preferences.json` for GUI preferences. Both paths
are relative to the process's working directory. `JsonSerializableGigabyteData` stores three top-level arrays:
`clients`, `gigs`, and `paymentObligations`. The following is a complete example with one record of each type:

```json
{
  "clients": [
    {
      "uid": "11111111-1111-4111-8111-111111111111",
      "name": "Jamie Tan",
      "phone": "81234567",
      "email": "jamie@example.com",
      "address": "10 Clementi Road",
      "tags": []
    }
  ],
  "gigs": [
    {
      "uid": "22222222-2222-4222-8222-222222222222",
      "clientUid": "11111111-1111-4111-8111-111111111111",
      "title": "Website redesign",
      "status": "IN_PROGRESS",
      "deadline": "2027-06-30",
      "feeCents": 125050
    }
  ],
  "paymentObligations": [
    {
      "gigUid": "22222222-2222-4222-8222-222222222222",
      "amountCents": 50000,
      "dueDate": "2027-07-15",
      "paid": false
    }
  ]
}
```

`feeCents` and `amountCents` are positive integer counts of cents; the example represents a fee of `1250.50` and a
payment of `500.00`. `Fee` uses `BigDecimal` in memory and converts to and from `BigInteger` cents for storage.
Dates use `yyyy-MM-dd`. The stored `paid` boolean records whether an obligation is settled; overdue status is
derived from the due date and the current local date rather than stored separately.

Loading proceeds in dependency order: clients, then gigs, then payment obligations. The adapters resolve
`clientUid` and `gigUid` to the canonical objects already loaded. Duplicate client records or client/gig UUIDs,
unresolved parent references, and invalid field values detected during conversion cause the load to fail instead
of returning partially loaded data.

#### Backward compatibility and filename migration

There is no schema-version field. The v1.4 loader accepts only the current required fields described above.
It does not convert a v1.3 JSON shape: `persons`, absent collections or UUIDs, `clientName`, `gigIndex`, decimal
`fee`/`amount`, and missing titles fail strict loading. `JsonGigabyteDataStorage#migrateLegacyDataFile` only moves
`data/addressbook.json` to `data/gigabyte.json` when the destination is absent. It does not transform its contents.
When both files exist, the current filename takes precedence and neither file is moved.

This distinction matters during upgrade: a moved v1.3 file remains unsupported. Preserve a backup before launch;
if loading fails, the app starts with an empty model and the next successful command overwrites the moved file.
The manual compatibility test below checks both the filename move with current-format data and the rejection of a
representative v1.3 file.

#### Save lifecycle and failure behavior

`LogicManager#execute` performs these steps in order:

1. Parse the input into a command.
1. Execute the command against the model.
1. Save the complete current `GigabyteData` through `StorageManager` and `JsonGigabyteDataStorage`.
1. Return the `CommandResult` to the UI.

Every command that completes step 2 reaches the save step, including commands such as `list` that do not change
stored records. A parsing or command-execution exception skips the save step. Missing parent directories and the
data file are created when saving.

If saving throws `AccessDeniedException`, `LogicManager` reports insufficient write permissions through a
`CommandException`. Other `IOException`s produce a `CommandException` containing the underlying error. The
command's successful result is not returned to the UI, but any changes already made to the in-memory model remain:
there is no rollback. After the write problem is resolved, the next successful command and save persist the current
model, including those earlier unsaved changes.

`FileUtil#writeToFile` writes directly to the destination file. There is no temporary-file replacement or automatic
backup, so a failed write does not guarantee that the previous file contents remain intact.

At startup, a missing data file causes `MainApp` to use sample data. A malformed JSON file or a validation failure
reported as `DataLoadingException` is logged and causes the app to start with empty data. Loading itself does not
overwrite that file, but the next successful save, even after `list`, replaces it with the current model. Preserve
the original file before running commands when investigating a load failure.

GUI preferences are saved separately during initialization and shutdown. If `preferences.json` is missing, malformed,
has `"guiSettings": null`, or contains a GUI-setting value of the wrong type, `MainApp` starts with default window
settings and writes those defaults to `preferences.json`. Valid saved window size and position are restored. Failures
to save preferences are logged. Shutdown does not provide an additional application-data save, so it does not retry
a failed command save.

### Common classes

Classes used by multiple components are in the `gigabyte.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Stable client, gig, and payment links

Each `Client` and `Gig` has an immutable UUID. A `Gig` holds the canonical `Client` object from `GigabyteData`, while a
`PaymentObligation` holds the canonical `Gig` object. The JSON format persists these relationships as `clientUid` and
`gigUid`, avoiding ambiguous links when clients or gigs have equal display fields.

When a client is edited, `GigabyteData#setClient` rebuilds that client's immutable gigs with `Gig#withClient` and then
relinks payment obligations to the replacement gig objects. Only affected rows are replaced in the observable lists,
preserving unrelated stored objects and selections. `Gig#withClient` preserves the gig UUID and title, so
editing contact details does not break payment links or discard gig information. During loading, storage resolves
`clientUid` and `gigUid` against the saved UUIDs and rejects missing or invalid references.

`EditPaymentCommand` resolves the displayed client, per-client gig, and due-date-ordered payment indexes before it
creates a replacement with `PaymentObligation#withDetails`. `GigabyteData#setPaymentObligation` searches for the
selected stored object by identity before falling back to value equality, so editing one of two equal-valued payments
changes only the indexed occurrence. The replacement keeps the stored gig object and paid state. Replacing the item
in the observable payment list immediately updates and reorders the selected-gig and outstanding payment views.

`MarkPaymentPaidCommand` and `MarkPaymentUnpaidCommand` resolve the displayed client, per-client gig, and
due-date-ordered payment indexes before changing any data. They reject a request that already matches the stored paid
state, then call `Model#setPaymentPaidState`. `GigabyteData` replaces the selected object by identity with
`PaymentObligation#withPaid`, which preserves its gig, amount, and due date and keeps equal-valued obligations
independent. The observable-list replacement refreshes selected-gig and outstanding views immediately, while normal
storage saving persists the corrected state.

`EditGigCommand` resolves both indexes before constructing a replacement gig with the same UID. `Model#setGig`
relinks dependent payments to that replacement, so changing a title, deadline, or fee does not detach payments.
`DeleteGigCommand` also resolves the normal per-client index first, but `Model#deleteGig` refuses removal while any
paid or unpaid payment refers to the gig UID. The user must explicitly delete those payments, then retry the gig
deletion. `DeleteClientCommand` similarly refuses to remove a client while gigs refer to its UID. `clear` is the
explicit operation that removes the complete graph of clients, gigs, and payments at once. These policies preserve
referential integrity and avoid silent dependent-record deletion.

`FindGigCommand` searches all titles with `GigTitleContainsKeywordsPredicate`; `MainWindow` displays the matching
gigs without changing the filtered client list. Overview row positions are not command gig indexes. Commands use
`GigOrdering.forClient` for a client's recorded gig order and `PaymentObligationOrdering.forGig` for due-date-ordered
payments, including when another view is open.

#### Design considerations:

**Aspect: How gigs and payment obligations refer to their owners:**

* **Alternative 1 (current choice):** Store a stable UUID for each client and gig, and save references as `clientUid`
  and `gigUid`.
  * Pros: Links stay correct when two clients or gigs have identical display fields, and when a client's name or a
    gig's title is edited.
  * Cons: The save file is harder to edit by hand, and older save files without IDs cannot be loaded.

* **Alternative 2:** Refer to the owner by its name (for clients) or its position in a list (for gigs).
  * Pros: Simpler save format that is easier to read.
  * Cons: Renaming a client, reordering gigs, or having duplicate titles can silently move payments to the wrong gig.

### Per-client gig and payment indexes

Gig and payment commands identify their target with nested indexes instead of one global index. For example,
`markpaid 2 1 3` means "the 3rd payment of the 1st gig of the 2nd displayed client". Commands resolve these indexes
in the following order:

1. The client index is resolved against `Model#getFilteredClientList()`, so it follows the client list currently on
   screen (including `find` results), just like AB3's `delete` and `edit` commands.
1. The gig index is resolved against `GigOrdering#forClient(model.getGigList(), client)`, which keeps the client's gigs
   in the order they appear in the stored gig list. (`setstatus` uses an equivalent filter on the stored gig list.)
1. The payment index (for `editpayment`, `markpaid`, `markunpaid`, and `deletepayment`) is resolved against
   `PaymentObligationOrdering#forGig(model.getPaymentObligationList(), gig)`, which sorts the gig's payments by due date
   and then by their position in the stored list.

All commands that take a gig index (`addpayment`, `editgig`, `setstatus`, `deletegig`, `editpayment`, `markpaid`,
`markunpaid`, and `deletepayment`) resolve it this way. If any index is out of range, the command throws a
`CommandException` before changing the model, so a command never changes some records and then fails.

`GigListPanel` and `PaymentObligationListPanel` use the same `GigOrdering` and `PaymentObligationOrdering` logic when
displaying a selected client's gigs and a selected gig's payments. This keeps the numbers shown in those panels equal
to the numbers that commands expect. The cross-client views (`gigs`, `findgig`, `deadlines`, and `outstanding`) are
filtered or sorted differently, so their row numbers are only for display. Commands still use the per-client indexes,
even while one of these views is visible.

#### Design considerations:

**Aspect: How to identify a gig or payment obligation in a command:**

* **Alternative 1 (current choice):** Use nested indexes (client, then gig, then payment).
  * Pros: Indexes are short and stay stable while the user works within one client. They also match the
    client → gigs → payments layout of the GUI.
  * Cons: Commands need two or three indexes, and the row numbers in cross-client views cannot be used directly.

* **Alternative 2:** Use one global index for each gig and payment obligation.
  * Pros: Commands are shorter, and every view could be used to pick a target.
  * Cons: Global indexes change whenever a gig or payment for any client is added or reordered, making them easy to
    mistype for users who manage many clients.

### Editing a gig

`editgig CLIENT_INDEX GIG_INDEX [t/TITLE] [d/DEADLINE] [f/FEE]` corrects a gig's details without changing its
identity, client, status, or payment obligations.

1. `EditGigCommandParser` checks that exactly two indexes and at least one of `t/`, `d/`, and `f/` are given, rejects
   `s/` and repeated prefixes, and parses each supplied field into a `GigTitle`, `Deadline`, or `Fee`. Missing fields
   are passed to `EditGigCommand` as empty `Optional`s.
1. `EditGigCommand#execute` resolves both indexes as described in
   [Per-client gig and payment indexes](#per-client-gig-and-payment-indexes).
1. It creates the replacement with `Gig#withDetails`, which copies the target's UUID, client, and status and uses the
   target's existing value for every field that was not supplied. If the replacement equals the target, the command
   fails with `The gig already has the specified details.`
1. It calls `Model#setGig(target, replacement)`. `GigabyteData#setGig` finds the stored gig by UUID, checks that the
   replacement keeps the same UUID and client, replaces the gig in the observable gig list, and relinks the gig's
   payment obligations to the replacement object.

Payment amounts are stored separately from the gig's agreed fee, so changing the fee does not change any recorded
payment. The gig views observe the gig list, so the selected client's gigs, status results, title search results, and
the deadline overview update immediately. `setstatus` uses the same `Model#setGig` path, with `Gig#withStatus` creating
the replacement.

`GigListPanel` captures the selected gig before its source list changes and restores its selection by UUID after
filtering and sorting finish. Intermediate row changes do not switch the payment view. A deleted gig or one that
leaves the active filter clears its selection and payments. `ClientListPanel` treats replacement details with the
same client UUID as the same selection, preserving the current gig view during contact edits.

#### Design considerations:

**Aspect: Whether `editgig` should also change the gig's status:**

* **Alternative 1 (current choice):** Reject `s/` in `editgig` and keep `setstatus` as the only way to change status.
  * Pros: Each command has one clear purpose, and `setstatus` can report a status-specific error when the status is
    already set.
  * Cons: Users need two commands to change a gig's details and its status.

* **Alternative 2:** Allow `editgig` to change every gig field, including status.
  * Pros: One command can update everything at once.
  * Cons: Two commands would then change status, with different validation and error messages.

### Deleting linked records safely

Clients, gigs, and payment obligations form a chain of links: each gig refers to its client, and each payment
obligation refers to its gig. Gigabyte never deletes a record that another record still refers to:

* `delete` fails with `Client cannot be deleted while it has associated gigs` while any gig refers to the client's
  UID.
* `deletegig` fails while any paid or unpaid payment obligation refers to the gig's UID. `GigabyteData#removeGig`
  checks this before changing anything and throws a `GigHasPaymentsException`, which `DeleteGigCommand` reports as a
  `CommandException` telling the user to delete the payments first with `deletepayment`.
* `deletepayment` has no dependent records, so it succeeds for any valid index. `GigabyteData` removes the exact
  stored object, so deleting one of two identical payments removes only the indexed one.
* `clear` is the only command that removes linked records together. It replaces all data with an empty
  `GigabyteData`.

#### Design considerations:

**Aspect: What happens to linked records when a record is deleted:**

* **Alternative 1 (current choice):** Block the deletion until the user has deleted the linked records.
  * Pros: Records, including payments that are still owed, are never deleted by accident. This matters because
    there is no undo.
  * Cons: Deleting a gig that has several payments takes several commands.

* **Alternative 2:** Cascade the deletion, removing a gig's payments (or a client's gigs and payments) with it.
  * Pros: One command removes everything that belongs to the record.
  * Cons: Payment records could be lost without the user noticing, and the data cannot be recovered.

### Cross-client gig and payment views

By default, the Gigs panel shows the selected client's gigs, and the Payment obligations panel shows the selected
gig's payments. Four commands switch these panels into views that cover all clients, without changing the client
filter used by other commands:

| Command | `CommandResult` field set | Panel method called by `MainWindow` |
| --- | --- | --- |
| `gigs s/STATUS` | `gigStatusFilter` | `GigListPanel#showGigsWithStatus` |
| `findgig KEYWORD...` | `gigTitleSearch` | `GigListPanel#showGigsMatchingTitle` |
| `deadlines` | `showDeadlineOverview` | `GigListPanel#showDeadlineOverview` |
| `outstanding` | `showOutstandingPayments` | `PaymentObligationListPanel#showOutstandingPayments` |

These commands do not change the `Model`. At most, they count the matching records for the feedback message. Each
returns a `CommandResult` that describes the view to show. After `Logic#execute` returns, `MainWindow#executeCommand`
checks these fields, clears the client (and, where needed, gig) selection, and calls the matching panel method.

Each panel wraps the model's observable list in a `FilteredList` and a `SortedList`. A view only replaces the
predicate and comparator. For example, `deadlines` keeps non-completed gigs and sorts them by deadline, while `findgig`
uses a `GigTitleContainsKeywordsPredicate` and keeps the stored order. Because the panels still observe the model's
lists, later edits, status changes, payment updates, and deletions appear in the current view immediately. Selecting
a client calls `GigListPanel#showGigsFor`, which leaves the cross-client view and shows that client's gigs again.

#### Design considerations:

**Aspect: Where the state of the current view is kept:**

* **Alternative 1 (current choice):** The command returns view instructions in `CommandResult`, and the `UI` keeps
  the view state.
  * Pros: `Model` stays free of display-only state, and switching views does not change the client filter that
    commands use for indexes.
  * Cons: `CommandResult` gains a field for each kind of view, and the view state cannot be unit tested through
    `Model` alone.

* **Alternative 2:** Store the current gig and payment filters in `Model`, like AB3's filtered client list.
  * Pros: Consistent with how `find` and `list` work.
  * Cons: Commands would have to know which view is visible to interpret indexes, which makes nested indexes
    ambiguous.

### Work and payment summary

`summary` shows a read-only overview of all stored data. `SummaryCommand#execute`:

1. counts all clients through `Model#getGigabyteData()`, so the client filter set by `find` does not affect the count,
1. counts gigs for each `GigStatus`, starting every status at zero so that statuses with no gigs are still shown, and
1. adds up payment amounts in cents as `BigInteger` values. Unpaid payments are counted as **Outstanding**, unpaid
   payments with a due date before today are counted as **Of which overdue**, and paid payments are counted as
   **Marked paid**.

The command reads "today" from a `java.time.Clock` that defaults to the system clock. Tests pass a fixed clock so
that results which depend on overdue dates stay deterministic.

### \[Proposed\] Undo/redo feature

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedGigabyteData`. It extends `GigabyteData` with an undo/redo history, stored internally as an `gigabyteDataStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedGigabyteData#commit()` -- Saves the current Gigabyte data state in its history.
* `VersionedGigabyteData#undo()` -- Restores the previous Gigabyte data state from its history.
* `VersionedGigabyteData#redo()` -- Restores a previously undone Gigabyte data state from its history.

These operations are exposed in the `Model` interface as `Model#commitGigabyteData()`, `Model#undoGigabyteData()` and `Model#redoGigabyteData()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The user launches the application for the first time. The `VersionedGigabyteData` will be initialized with the initial Gigabyte data state, and the `currentStatePointer` pointing to that single Gigabyte data state.

<puml src="diagrams/UndoRedoState0.puml" alt="UndoRedoState0" />

Step 2. The user executes `delete 5` command to delete the 5th client in the Gigabyte data. The `delete` command calls `Model#commitGigabyteData()`, causing the modified state of the Gigabyte data after the `delete 5` command executes to be saved in the `gigabyteDataStateList`, and the `currentStatePointer` is shifted to the newly inserted Gigabyte data state.

<puml src="diagrams/UndoRedoState1.puml" alt="UndoRedoState1" />

Step 3. The user executes `add n/David …​` to add a new client. The `add` command also calls `Model#commitGigabyteData()`, causing another modified Gigabyte data state to be saved into the `gigabyteDataStateList`.

<puml src="diagrams/UndoRedoState2.puml" alt="UndoRedoState2" />

<box type="info" seamless>

**Note:** If a command fails its execution, it will not call `Model#commitGigabyteData()`, so the Gigabyte data state will not be saved into the `gigabyteDataStateList`.
</box>

Step 4. The user now decides that adding the client was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoGigabyteData()`, which will shift the `currentStatePointer` once to the left, pointing it to the previous Gigabyte data state, and restores the Gigabyte data to that state.

<puml src="diagrams/UndoRedoState3.puml" alt="UndoRedoState3" />


<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index 0, pointing to the initial GigabyteData state, then there are no previous GigabyteData states to restore. The `undo` command uses `Model#canUndoGigabyteData()` to check if this is the case. If so, it will return an error to the user rather
than attempting to perform the undo.
</box>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

<puml src="diagrams/UndoSequenceDiagram-Logic.puml" alt="UndoSequenceDiagram-Logic" />

<box type="info" seamless>

**Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</box>

Similarly, how an undo operation goes through the `Model` component is shown below:

<puml src="diagrams/UndoSequenceDiagram-Model.puml" alt="UndoSequenceDiagram-Model" />

The `redo` command does the opposite — it calls `Model#redoGigabyteData()`, which shifts the `currentStatePointer` once to the right, pointing to the previously undone state, and restores the Gigabyte data to that state.

<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index `gigabyteDataStateList.size() - 1`, pointing to the latest Gigabyte data state, then there are no undone GigabyteData states to restore. The `redo` command uses `Model#canRedoGigabyteData()` to check if this is the case. If so, it will return an error to the user rather than attempting to perform the redo.
</box>

Step 5. The user then decides to execute the command `list`. Commands that do not modify the Gigabyte data, such as `list`, will usually not call `Model#commitGigabyteData()`, `Model#undoGigabyteData()` or `Model#redoGigabyteData()`. Thus, the `gigabyteDataStateList` remains unchanged.

<puml src="diagrams/UndoRedoState4.puml" alt="UndoRedoState4" />

Step 6. The user executes `clear`, which calls `Model#commitGigabyteData()`. Since the `currentStatePointer` is not pointing at the end of the `gigabyteDataStateList`, all Gigabyte data states after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add n/David …` command. This is the behavior that most modern desktop applications follow.

<puml src="diagrams/UndoRedoState5.puml" alt="UndoRedoState5" />

The following activity diagram summarizes what happens when a user executes a new command:

<puml src="diagrams/CommitActivityDiagram.puml" width="250" />

#### Design considerations:

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire Gigabyte data.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the client being deleted).
  * Cons: We must ensure that the implementation of each individual command is correct. This is harder in Gigabyte
    than in AB3 because one command can change linked records, e.g., editing a client also replaces that client's
    gigs and relinks their payment obligations.

### \[Proposed\] Data archiving

#### Proposed Implementation

The proposed archiving mechanism would let freelancers move completed work out of their active data, while keeping
it in a separate file for reference. It would be facilitated by an `ArchiveCommand`, a new `Storage` method,
`Storage#saveArchive(ReadOnlyGigabyteData archive, Path archiveFilePath)`, and a new `Model` method,
`Model#removeArchivedGigs(ReadOnlyGigabyteData archive)`.

Commands only receive the `Model`, so `ArchiveCommand` cannot save files itself. Instead, `LogicManager`, which
already holds both the `Model` and the `Storage`, would coordinate the two saves, in the same way that it saves the
active data after every command today.

Given below is an example usage scenario and how the archiving mechanism would behave.

Step 1. The user executes `archive`. `ArchiveCommand#execute` collects every gig whose status is `COMPLETED` and all
of whose payment obligations are marked paid. Completed gigs that still have unpaid obligations are left in the active
data, so money that is still owed stays visible in `outstanding`.

Step 2. `ArchiveCommand` builds a new `GigabyteData` containing the selected gigs, their payment obligations, and the
clients they belong to. Because gigs and payments are linked by UUID, the archive can be saved in the existing save
format. The command does not change the `Model`. It returns the archive to `LogicManager` in its `CommandResult`.

Step 3. `LogicManager` saves the archive to a new file whose name includes the date and time of the command, down to
the second, e.g., `data/archive/gigabyte-2027-01-31T14-05-09.json`. Hyphens are used instead of colons so that the name
is valid on Windows. `Storage#saveArchive` only creates new files: it opens the file with
`StandardOpenOption.CREATE_NEW`, so it never replaces an existing archive, even when `archive` is run several times on
the same day. If a file with that name already exists, or the save fails for any other reason, `LogicManager` throws a
`CommandException`. The `Model` and the active data file have not been changed, so nothing needs to be rolled back,
and the user can simply run `archive` again.

Step 4. Only after the archive is saved, `LogicManager` calls `Model#removeArchivedGigs`, which removes the archived
gigs (matched by UUID) and their payment obligations from the active data. Clients are kept, even when all their gigs
were archived, so that new gigs can still be added for them.

Step 5. `LogicManager` saves the active data as it does after every command. If this save fails, the error is
reported as for any other failed save. The active data file on disk then still contains the archived records, so they
exist in both files until the next successful save, but no record is lost.

#### Design considerations:

**Aspect: Which component coordinates the two saves:**

* **Alternative 1 (proposed choice):** `LogicManager` saves the archive returned by the command, then updates the
  `Model`.
  * Pros: Commands keep depending only on `Model`. Saving the archive before changing the `Model` means a failed
    archive save never needs a rollback.
  * Cons: `LogicManager` gains logic that is specific to one command, and the two files are not updated atomically.

* **Alternative 2:** Give `ArchiveCommand` access to `Storage` so that it saves the archive itself.
  * Pros: All archiving logic is kept in one class.
  * Cons: Breaks the current architecture, in which commands interact only with the `Model`, and makes the command
    harder to test with a model stub.

**Aspect: Where archived records are kept:**

* **Alternative 1 (proposed choice):** Save archived records in a separate JSON file.
  * Pros: The active data file stays small, and the existing loading and validation code can read an archive file.
  * Cons: Archived records cannot be searched from within the app without a separate "view archive" feature.

* **Alternative 2:** Keep archived records in the active data file with an `archived` flag.
  * Pros: Archived records can be restored, searched, and included in `summary` easily.
  * Cons: Every view and command would have to filter out archived records, and the save format would change.


--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [Setting up and building](SettingUp.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

* is a freelancer who needs to manage multiple clients, gigs, deadlines, fees, and expected payments
* prefers desktop apps over other types of applications
* can type fast
* prefers typing to mouse interactions
* is reasonably comfortable using CLI apps

**Value proposition**: Track client work and expected payments quickly through a command-driven desktop application.


### User stories

Priorities: High (must have) - `* * *`, Medium (nice to have) - `* *`, Low (unlikely to have) - `*`

| Priority | As a …​                      | I want to …​                                                               | So that I can…​                                                  |
|----------|------------------------------|----------------------------------------------------------------------------|------------------------------------------------------------------|
| `* * *`  | new user                     | see instructions for available commands                                    | learn how to use Gigabyte                                        |
| `* * *`  | freelancer                   | add a new client with their contact details                                | contact them about my work                                       |
| `* * *`  | freelancer                   | edit a client's contact details                                            | keep their information up to date                                |
| `* * *`  | freelancer                   | delete a client who has no gigs                                            | remove clients that I no longer work with                        |
| `* * *`  | freelancer                   | find clients by name                                                       | locate a client without having to go through the entire list    |
| `* * *`  | freelancer                   | view a client's details and associated gigs                                | understand my work with that client                              |
| `* * *`  | freelancer                   | create a gig linked to a client with a title, status, deadline, and fee    | track each piece of work                                         |
| `* * *`  | freelancer                   | correct a gig's title, deadline, or agreed fee                             | keep my records accurate when plans change                       |
| `* * *`  | freelancer                   | update a gig's status                                                      | see how far each piece of work has progressed                    |
| `* * *`  | freelancer                   | delete a gig after its payment obligations are removed                     | remove cancelled work without leaving payments without a gig     |
| `* * *`  | freelancer                   | find gigs by title                                                         | locate a gig without remembering which client it belongs to     |
| `* * *`  | freelancer                   | view active gig deadlines, including overdue gigs, in chronological order  | prioritise my work                                               |
| `* * *`  | freelancer                   | record a payment obligation linked to a gig, with an amount and due date   | know what payment I expect and when                              |
| `* * *`  | freelancer                   | correct a payment obligation's amount or due date                          | keep revised invoice details accurate                            |
| `* * *`  | freelancer                   | mark a payment obligation as paid                                          | distinguish settled payments from money still owed               |
| `* * *`  | freelancer                   | correct a payment that I accidentally marked as paid                       | keep my outstanding-payment list accurate                        |
| `* * *`  | freelancer                   | delete an incorrectly recorded payment obligation                          | keep duplicate or cancelled invoices out of my records           |
| `* * *`  | freelancer                   | view all unpaid payment obligations in due-date order                      | follow up on payments that are due soon or overdue               |
| `* *`    | freelancer                   | filter gigs by status across all clients                                   | focus on work at a particular stage                              |
| `* *`    | freelancer                   | see a summary of my gigs and payment totals                                | get a quick overview of my workload and the money owed to me     |
| `* *`    | new user                     | clear all data                                                             | start afresh after trying out the sample data                    |
| `* *`    | user                         | hide private contact details                                               | minimize chance of someone else seeing them by accident          |
| `* *`    | careless user                | undo my last command                                                       | recover quickly from a mistaken edit or deletion                 |
| `*`      | freelancer with many clients | sort clients by name                                                       | locate a client easily                                           |
| `*`      | freelancer                   | archive completed gigs                                                     | keep the active view focused on current work                     |

<box type="info" seamless>

**Note:** The last four user stories (hiding private contact details, undoing a command, sorting clients, and
archiving completed gigs) are not implemented in the current release. They are planned features for a future
release.
</box>

### Use cases

(For all use cases below, the **System** is `Gigabyte` and the **Actor** is the `freelancer`, unless specified
otherwise)

**Use case: Create and progress a gig for a client**

**MSS**

1.  Freelancer requests to list clients.
2.  Gigabyte shows a list of clients.
3.  Freelancer requests to add a gig with a title, status, deadline, and agreed fee for a specific client in the list.
4.  Gigabyte creates the gig, links it to the client, and shows the new gig's details.
5.  Freelancer requests to update the status of that gig.
6.  Gigabyte updates the gig's status while keeping its client, title, deadline, fee, and payment obligations.

    Use case ends.

**Extensions**

* 1a. Freelancer requests to find clients by name instead.

    * 1a1. Gigabyte shows only the matching clients, numbered from 1.

      Use case resumes at step 3.

* 2a. The list is empty.

  Use case ends.

* 3a. The given client index is invalid.

    * 3a1. Gigabyte shows an error message.

      Use case resumes at step 2.

* 3b. A field is missing, repeated, or invalid.

    * 3b1. Gigabyte shows an error message explaining the relevant constraint.

      Use case resumes at step 2.

* 5a. The given client index, gig index, or status is invalid.

    * 5a1. Gigabyte shows an error message.

      Use case resumes at step 5.

* 5b. The gig already has the requested status.

    * 5b1. Gigabyte informs the freelancer that no change was made.

      Use case ends.

**Use case: Edit a gig's details**

**Preconditions:** The client has at least one gig.

**MSS**

1.  Freelancer requests to view the gigs of a specific client.
2.  Gigabyte shows the client's gigs.
3.  Freelancer requests to change the title, deadline, and/or agreed fee of a specific gig in the list.
4.  Gigabyte updates the gig while keeping its client, status, and payment obligations, and shows the updated details.

    Use case ends.

**Extensions**

* 3a. The given client index or gig index is invalid.

    * 3a1. Gigabyte shows an error message.

      Use case resumes at step 2.

* 3b. No field is given, a field is repeated or invalid, or a status is given.

    * 3b1. Gigabyte shows an error message.

      Use case resumes at step 2.

* 3c. The given details are the same as the gig's current details.

    * 3c1. Gigabyte informs the freelancer that no change was made.

      Use case ends.

<box type="info" seamless>

**Note:** Editing a payment obligation (`editpayment`) is a variation of this use case. In steps 1 and 2, the
freelancer views a gig's payment obligations, which are ordered by due date. In step 3, the freelancer changes the
amount and/or due date of a specific obligation in that list. In step 4, Gigabyte keeps the obligation's gig and paid
state, and moves the obligation to its new position if its due date changed. The extensions are the same, except that
giving a status does not apply.
</box>

**Use case: Delete a gig**

**Preconditions:** The client has at least one gig.

**MSS**

1.  Freelancer requests to view the gigs of a specific client.
2.  Gigabyte shows the client's gigs.
3.  Freelancer requests to delete a specific gig in the list.
4.  Gigabyte deletes the gig and removes it from every open gig view.

    Use case ends.

**Extensions**

* 3a. The given client index or gig index is invalid.

    * 3a1. Gigabyte shows an error message.

      Use case resumes at step 2.

* 3b. The gig still has payment obligations, paid or unpaid.

    * 3b1. Gigabyte shows an error message telling the freelancer to delete those payment obligations first, and
      does not delete the gig.

    * 3b2. Freelancer deletes the gig's payment obligations (see use case _Delete a payment obligation_).

      Use case resumes at step 3.

**Use case: Find gigs by title**

**MSS**

1.  Freelancer requests to find gigs whose titles contain one or more keywords.
2.  Gigabyte shows the matching gigs of all clients in their stored order, without changing the client list.
3.  Freelancer selects a gig to view its payment obligations.

    Use case ends.

**Extensions**

* 1a. No keyword is given.

    * 1a1. Gigabyte shows an error message.

      Use case ends.

* 2a. No gig title matches.

    * 2a1. Gigabyte shows that no gigs match the keywords.

      Use case ends.

**Use case: Record and settle a payment obligation**

**Preconditions:** The client has at least one gig.

**MSS**

1.  Freelancer requests to view the payment obligations of a specific gig of a client.
2.  Gigabyte shows the gig's payment obligations, ordered by due date.
3.  Freelancer requests to record a payment obligation for the gig, with an amount and a due date.
4.  Gigabyte records the obligation as unpaid and shows it in the gig's list in due-date order.
5.  Freelancer requests to view all outstanding payment obligations.
6.  Gigabyte shows the unpaid obligations of all clients in due-date order, marking overdue ones.
7.  Freelancer requests to mark a specific obligation as paid, identifying it by its position in the gig's payment list.
8.  Gigabyte marks the obligation as paid and shows its updated state.

    Use case ends.

**Extensions**

* 3a. The given client index or gig index is invalid.

    * 3a1. Gigabyte shows an error message.

      Use case resumes at step 2.

* 3b. The amount or due date is missing, repeated, or invalid.

    * 3b1. Gigabyte shows an error message explaining the relevant constraint.

      Use case resumes at step 2.

* 6a. There are no unpaid obligations.

    * 6a1. Gigabyte shows that there are no outstanding payment obligations.

      Use case ends.

* 7a. The given client, gig, or payment index is invalid.

    * 7a1. Gigabyte shows an error message.

      Use case resumes at step 7.

* 7b. The selected obligation is already marked as paid.

    * 7b1. Gigabyte informs the freelancer that no change was made.

      Use case ends.

**Use case: Correct a payment marked as paid**

**Preconditions:** The payment obligation is marked as paid.

**MSS**

1.  Freelancer requests to view the payment obligations of a specific gig of a client.
2.  Gigabyte shows the gig's payment obligations, ordered by due date.
3.  Freelancer requests to mark a specific obligation in the list as unpaid.
4.  Gigabyte marks the obligation as unpaid while keeping its gig, amount, and due date, and includes it in the
    outstanding payment obligations again.

    Use case ends.

**Extensions**

* 3a. The given client, gig, or payment index is invalid.

    * 3a1. Gigabyte shows an error message.

      Use case resumes at step 2.

* 3b. The selected obligation is already unpaid.

    * 3b1. Gigabyte informs the freelancer that no change was made.

      Use case ends.

**Use case: Delete a payment obligation**

**Preconditions:** The gig has at least one payment obligation.

**MSS**

1.  Freelancer requests to view the payment obligations of a specific gig of a client.
2.  Gigabyte shows the gig's payment obligations, ordered by due date.
3.  Freelancer requests to delete a specific obligation in the list.
4.  Gigabyte deletes the obligation, whether it is paid or unpaid, while keeping the gig and its other obligations,
    and shows the deleted obligation's details.

    Use case ends.

**Extensions**

* 3a. The given client, gig, or payment index is invalid.

    * 3a1. Gigabyte shows an error message.

      Use case resumes at step 2.

**Use case: Review active deadlines**

**MSS**

1.  Freelancer requests to view active deadlines.
2.  Gigabyte shows every gig that is not completed, across all clients, ordered by deadline and marking overdue ones.
3.  Freelancer selects a client to return to that client's gigs.

    Use case ends.

**Extensions**

* 2a. There are no gigs that are not completed.

    * 2a1. Gigabyte shows that there are no active gig deadlines.

      Use case ends.

<box type="info" seamless>

**Note:** Filtering gigs by status (`gigs`) is a variation of this use case. In step 1, the freelancer gives a status
instead, and in step 2, Gigabyte shows the gigs of all clients that have that status, in their stored order.
</box>

**Use case: Delete a client**

**MSS**

1.  Freelancer requests to list clients.
2.  Gigabyte shows a list of clients.
3.  Freelancer requests to delete a specific client in the list.
4.  Gigabyte deletes the client.

    Use case ends.

**Extensions**

* 2a. The list is empty.

  Use case ends.

* 3a. The given index is invalid.

    * 3a1. Gigabyte shows an error message.

      Use case resumes at step 2.

* 3b. The client still has gigs.

    * 3b1. Gigabyte shows an error message and does not delete the client.

      Use case ends.

### Non-Functional Requirements

1.  Should work on any _mainstream OS_ as long as it has Java `25` or above installed.
2.  Should be distributed as a single JAR file that does not need an installer.
3.  Should work without a network connection, saving all data locally so that it is still available after the app
    is closed and reopened.
4.  Should store data locally in a human-editable text file, without using a database management system (DBMS).
5.  Should be able to hold up to 1000 clients, 1000 gigs, and 1000 payment obligations without noticeable sluggishness
    in performance for typical usage.
6.  Should show the results of searches, filters, and overview commands within 2 seconds on a typical modern computer.
7.  A user with above average typing speed for regular English text (i.e. not code, not system admin commands) should
    be able to accomplish most of the tasks faster using commands than using the mouse.
8.  Should work well for screen resolutions of 1920×1080 and higher at screen scales of 100% and 125%, and remain
    usable for resolutions of 1280×720 and higher at a screen scale of 150%.
9.  Should never store a gig that refers to a missing client, or a payment obligation that refers to a missing gig.
10. Should store and total monetary amounts exactly to the cent, without rounding errors.

### Glossary

* **Active gig**: A gig whose status is not `COMPLETED`.
* **Agreed fee**: The amount a client has agreed to pay for a gig. It is stored separately from the gig's payment
  obligations.
* **Client**: A person or organization that engages the freelancer for work.
* **Client index**: The position of a client in the client list currently on screen, which may be filtered by `find`.
* **Gig**: A piece of work undertaken for a client, with its own title, status, deadline, and agreed fee.
* **Gig index**: The position of a gig in its client's gig list, counted from 1.
* **Mainstream OS**: Windows, Linux, or macOS
* **Outstanding payment**: A payment obligation that has not been marked as paid.
* **Overdue**: Describes an active gig whose deadline, or an outstanding payment whose due date, is before the current
  date.
* **Payment index**: The position of a payment obligation in its gig's payment list, which is ordered by due date.
* **Payment obligation**: An amount the freelancer expects to receive for a gig, with a due date and payment status.
* **Private contact detail**: A contact detail that is not meant to be shared with others
* **UID**: A unique identifier that Gigabyte gives each client and gig, used to link records in the data file.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.

</box>

<box type="info" seamless>

**Preparation:** Build the release candidate with `gradlew shadowJar` (macOS/Linux: `./gradlew shadowJar`) and copy
`build/libs/gigabyte.jar` into an otherwise empty test folder. Unless a test says otherwise, launch it from that folder
with `java -jar gigabyte.jar`. Back up `data/gigabyte.json` before any test that changes or corrupts it.
</box>

Every command that takes indexes (`edit`, `delete`, `addgig`, `editgig`, `setstatus`, `deletegig`, `addpayment`,
`editpayment`, `markpaid`, `markunpaid`, and `deletepayment`) checks them in the same way: a missing or extra index,
or extra words before the first prefix, produces the command-format error with the command's usage; zero, negative,
and non-numeric indexes produce `Index must be a positive integer.`; and an index larger than the displayed list
produces an "invalid index" error. This is tested once, in [Adding payment obligations](#adding-payment-obligations),
and is not repeated for each command.

### Launch, sample data, and empty states

1. **Fresh launch with sample data**
   1. Ensure the test folder has no `data` directory or `preferences.json`, then launch the JAR.<br>
      Expected: Six sample clients appear. The Gigs panel says `Select a client to view their gigs.`, and the
      Payment obligations panel says `Select a gig to view its payment obligations.`
   1. Select `Alex Yeoh`.<br>
      Expected: The Gigs panel shows `Portfolio website redesign` and `Product launch photography`, including each
      gig's status, deadline, and fee. The Payment obligations panel still says
      `Select a gig to view its payment obligations.`
   1. Select `Portfolio website redesign`.<br>
      Expected: The panel shows the sample `$1,250.00` obligation due `2027-01-15` as `PAID`.
   1. Select `Bernice Yu`, then select `Quarterly newsletter template`.<br>
      Expected: The Payment obligations panel says `This gig has no payment obligations.`
   1. Select `Charlotte Oliveiro`.<br>
      Expected: The Gigs panel says `This client has no gigs.`, the previous gig selection is cleared, and the
      Payment obligations panel says `Select a gig to view its payment obligations.`

1. **Empty client list**
   1. Test case: `clear`<br>
      Expected: The client list becomes empty, all gigs disappear, and the result display says
      `Cleared all clients, gigs, and payment obligations.`

1. **Window preferences**
   1. Resize and move the window, close it, then relaunch the JAR.<br>
      Expected: The most recent window size and position are restored.

### Adding gigs

Start this section from a fresh test folder so the sample clients and gigs are present.

1. **Valid gig**
   1. Test case: `list`<br>
      Expected: All six sample clients are displayed.
   1. Test case: `addgig 1 t/Conference landing page s/IN_PROGRESS d/2027-06-30 f/1800.50`<br>
      Expected: The result names `Alex Yeoh` and repeats the normalized title, status, deadline, and fee.
   1. Select `Alex Yeoh`.<br>
      Expected: `Conference landing page` appears after Alex's two sample gigs with status `IN_PROGRESS`, deadline
      `2027-06-30`, and fee `$1,800.50`.

1. **Title validation**
   1. Test case: `addgig 1 t/    s/NOT_STARTED d/2027-06-30 f/100`<br>
      Expected: No gig is added and the result explains that a title needs a non-whitespace character.
   1. Test case: `addgig 1 t/` followed by a 101-character title, then
      ` s/NOT_STARTED d/2027-06-30 f/100`.<br>
      Expected: No gig is added and the result explains the 100-character maximum.
   1. Test case: `addgig 1 t/  Short title  s/NOT_STARTED d/2027-06-30 f/100`<br>
      Expected: The gig is added with the displayed title `Short title`; surrounding whitespace is removed.

1. **Invalid status, date, and fee**
   1. Test case: `addgig 1 t/Invalid status s/WAITING d/2027-06-30 f/100`<br>
      Expected: No gig is added; the result lists `NOT_STARTED`, `IN_PROGRESS`, and `COMPLETED` as valid statuses.
   1. Test case: `addgig 1 t/Invalid date s/NOT_STARTED d/2027-02-29 f/100`<br>
      Expected: No gig is added; the result requires a valid `yyyy-MM-dd` calendar date.
   1. Try the same command with `f/0`, then with `f/10.999`.<br>
      Expected: No gig is added in either case; the result requires a positive number with at most two decimal places.

1. **Repeated prefix**
   1. Test case: `addgig 1 t/First t/Second s/NOT_STARTED d/2027-06-30 f/100`<br>
      Expected: No gig is added; the result reports multiple values for `t/`.

### Correcting and finding gigs

Start each numbered test from a fresh test folder. Client and gig indexes use the currently displayed client list
and the selected client's normal recorded gig order, even while an overview is open.

1. **Edit details and reorder the deadline view**
   1. Test case: `deadlines`, select Alex's second gig, then
      `editgig 1 2 t/Revised photography d/2026-10-01 f/1350`.<br>
      Expected: Alex's second gig retains its UID, `NOT_STARTED` status, and `$1200.00` payment. The deadline
      overview refreshes without another `deadlines` command; the revised gig moves to the first row and stays
      selected, with its payment still visible.
   1. Test case: `find Alex`, then `editgig 1 2 d/2027-12-01`.<br>
      Expected: The same gig's deadline changes again while the filtered client list remains Alex only.
      Select Alex and his second gig: its payment remains linked and unchanged.
   1. Restart from the same folder.<br>
      Expected: The revised title, deadline, fee, status, and linked payment survive.

1. **Reject invalid and no-op gig edits**
   1. Test case: `editgig 1 2`, `editgig 1 2 s/COMPLETED`, `editgig 1 2 t/First t/Second`,
      `editgig 1 2 d/2027-02-29`, `editgig 1 2 f/0`, `editgig 0 2 f/100`, and `editgig 1 99 f/100`.<br>
      Expected: Each command fails; no gig or payment changes. The repeated prefix, invalid date and fee, and
      out-of-range indexes have distinct errors, and the error for `s/COMPLETED` says to use `setstatus`.
   1. Test case: `editgig 1 2 f/1200` on fresh sample data.<br>
      Expected: The no-op is rejected and the saved file is unchanged.

1. **Find gigs across a filtered client list**
   1. Test case: `find Bernice`, then `findgig website photography`.<br>
      Expected: Alex's two gigs appear in recorded order despite Bernice being the only displayed client.
      Select a result: only that gig's payments appear. The client list remains filtered to Bernice.
   1. Test case: `findgig no-such-title`, then `findgig`.<br>
      Expected: The first command shows `No gigs match the supplied title keywords.`; the second is rejected
      without changing stored records. Input `list` to restore all clients.
   1. Test case: `addgig 1 t/Invoice, overdue s/NOT_STARTED d/2027-06-30 f/100`, then `findgig invoice`, then
      `findgig voice`.<br>
      Expected: `findgig invoice` shows the new gig because the trailing comma is ignored. `findgig voice` does not
      show it because `voice` is only part of a word.

### Updating gig statuses and reviewing gigs

Start each test in this section from a fresh test folder.

1. **Update a gig status**
   1. Test case: `setstatus 1 2 s/COMPLETED`<br>
      Expected: The result confirms that Alex's second gig is now `COMPLETED`. Selecting Alex shows
      `Product launch photography` with the new status while its title, deadline, and fee are unchanged.
   1. Test case: Enter the same command again.<br>
      Expected: The command is rejected because the gig is already `COMPLETED`, and no data changes.

1. **Invalid status and repeated prefix**
   1. Test case: `setstatus 1 2 s/WAITING`<br>
      Expected: No gig changes; the result lists `NOT_STARTED`, `IN_PROGRESS`, and `COMPLETED` as valid statuses.
   1. Test case: `setstatus 1 2 s/IN_PROGRESS s/COMPLETED`<br>
      Expected: No gig changes; the result reports multiple values for `s/`.

1. **Active deadline overview**
   1. Test case: `addgig 1 t/Overdue follow-up s/IN_PROGRESS d/2000-01-01 f/100`<br>
      Expected: The overdue test gig is added for Alex.
   1. Test case: `deadlines`<br>
      Expected: Every non-completed gig appears across all clients, ordered by deadline. `Overdue follow-up` is
      first and labelled `OVERDUE`; `Brand identity refresh` is absent because it is completed.
   1. Test case: `deadlines unexpected`<br>
      Expected: An invalid command format message with the `deadlines` usage is shown. The deadline overview stays
      visible and no data changes.
   1. Select `Bernice Yu`.<br>
      Expected: The overview closes and Bernice's normal gig list is restored.

1. **Filter gigs by status**
   1. Test case: `gigs s/IN_PROGRESS`<br>
      Expected: The Gigs panel shows `Portfolio website redesign` and `Quarterly newsletter template`, including
      their client names, and leaves the client list unchanged.
   1. Test case: `gigs s/WAITING`<br>
      Expected: The command is rejected, the previous results remain visible, and no data changes.
   1. Test case: `gigs s/IN_PROGRESS s/COMPLETED`<br>
      Expected: The repeated `s/` prefix is rejected and the previous results remain visible.

### Help and client commands

1. **Help**
   1. Test case: `help`<br>
      Expected: The Help window opens and shows the link to the User Guide.

1. **Add a client**
   1. Test case: `add n/Jamie Tan p/81234567 e/jamie@example.com a/10 Clementi Road t/new`<br>
      Expected: Jamie is added, appears in the displayed client list, and the result shows the supplied details.

1. **Edit a client**
   1. Test case: `find Jamie`, followed by `edit 1 p/87654321 t/priority`.<br>
      Expected: The client list remains filtered to Jamie. Jamie's phone changes to `87654321`, and `priority`
      replaces the existing tags.

1. **Find and list clients**
   1. Test case: `find Jamie`<br>
      Expected: Only clients whose names contain the full word `Jamie` are displayed.
   1. Test case: `list`<br>
      Expected: The complete client list is displayed again and the result says `Listed all clients.`

1. **Exit**
   1. Test case: `exit`<br>
      Expected: Gigabyte closes. Relaunching it from the same folder restores the client changes made above.

### Adding payment obligations

Start this section from a fresh test folder. Alex's sample gigs make `CLIENT_INDEX 1` and `GIG_INDEX 1` valid.

1. **Valid payment obligation**
   1. Select `Alex Yeoh`, then select `Portfolio website redesign`.<br>
      Expected: The panel shows the sample `$1,250.00` obligation due `2027-01-15` as `PAID`.
   1. Test case: `addpayment 1 1 a/500.00 d/2099-07-15`<br>
      Expected: The result says an unpaid obligation was recorded for `Alex Yeoh`, gig 1, with amount `$500.00` and
      due date `2099-07-15`. Without reselecting the gig, the panel immediately shows
      `2. Amount: $500.00`, `Due: 2099-07-15`, and `OUTSTANDING` after the earlier sample payment.

1. **Invalid amount and date**
   1. Repeat `addpayment 1 1 a/AMOUNT d/2099-07-15`, replacing `AMOUNT` with `0`, then with `10.999`.<br>
      Expected: No obligation is recorded in either case; the result requires a positive number with at most two
      decimal places.
   1. Test case: `addpayment 1 1 a/500 d/15-07-2099`<br>
      Expected: No obligation is recorded; the result requires a valid `yyyy-MM-dd` calendar date.

1. **Repeated prefix**
   1. Test case: `addpayment 1 1 a/500 a/600 d/2099-07-15`<br>
      Expected: No obligation is recorded; the result reports multiple values for `a/`.

1. **Invalid client and gig indexes** (representative of all commands that take indexes)
   1. Repeat `addpayment CLIENT_INDEX 1 a/500 d/2099-07-15` with `CLIENT_INDEX` set to `0`, `-1`, `abc`, and `999`.<br>
      Expected: No obligation is recorded. The first three report `Index must be a positive integer.`; `999` reports
      an invalid client index.
   1. Repeat `addpayment 1 GIG_INDEX a/500 d/2099-07-15` with `GIG_INDEX` set to `0`, `-1`, `abc`, and `999`.<br>
      Expected: No obligation is recorded. The first three report `Index must be a positive integer.`; `999` reports
      that the gig index is invalid for this client.
   1. Test case: `addpayment 1 a/500 d/2099-07-15`, `addpayment 1 1 2 a/500 d/2099-07-15`, and
      `addpayment 1 1 extra a/500 d/2099-07-15`.<br>
      Expected: No obligation is recorded. Each reports `Invalid command format!` followed by the `addpayment` usage.

### Correcting payment status

Start each test in this section from a fresh test folder unless the test says otherwise.

1. **Mark a paid future payment as unpaid and refresh `outstanding`**
   1. Test case: `addpayment 1 1 a/600 d/2099-12-31`, followed by `markpaid 1 1 2`.<br>
      Expected: The `$600.00` obligation is payment 2 and displays `PAID`.
   1. Test case: `outstanding`.<br>
      Expected: The paid `$600.00` obligation is absent.
   1. Without leaving the overview, input: `markunpaid 1 1 2`.<br>
      Expected: The `$600.00` obligation immediately appears as `OUTSTANDING`, without rerunning `outstanding`.

1. **Mark a paid past-due payment as unpaid and reject a repeated attempt**
   1. Test case: `addpayment 1 1 a/100 d/2000-01-01`, followed by `markpaid 1 1 1` and
      `markunpaid 1 1 1`.<br>
      Expected: Payment 1 keeps amount `$100.00` and due date `2000-01-01`, changes to unpaid, and displays `OVERDUE`.
   1. Test case: `markunpaid 1 1 1` again.<br>
      Expected: The command is rejected because the obligation is already unpaid, and no payment changes.

1. **Correct one of two equal-valued payments**
   1. Test case: Enter these commands in order:<br>
      `addpayment 1 1 a/500 d/2099-11-30`<br>
      `addpayment 1 1 a/500 d/2099-11-30`<br>
      `markpaid 1 1 2`<br>
      `markpaid 1 1 3`<br>
      `markunpaid 1 1 3`<br>
      Expected: Only payment 3 changes back to unpaid. Payment 2 remains paid, and both retain their original gig,
      amount, and due date.

1. **Use filtered client indexes**
   1. Test case: `find Bernice`, followed by `markpaid 1 1 1` and `markunpaid 1 1 1`.<br>
      Expected: Bernice's first gig's payment changes back to unpaid. No payment belonging to a hidden client changes.

1. **Persist the corrected state**
   1. Test case: `markunpaid 1 1 1`, exit, and relaunch Gigabyte from the same folder.<br>
      Expected: Alex's sample payment remains unpaid, retains its gig, amount, and due date, and appears in
      `outstanding` with the display state derived from the relaunch date.

### Deleting payment obligations

Use a fresh test folder for each numbered test below. Deletion is permanent and has no confirmation or undo.

1. **Paid, future unpaid, overdue, final payment, and restart**
   1. Test case: `addpayment 1 1 a/500 d/2099-01-31`, then `addpayment 1 1 a/100 d/2000-01-31`.<br>
      Select Alex Yeoh and Portfolio website redesign. Expected order: overdue `$100.00` (1), sample paid
      `$1,250.00` (2), future unpaid `$500.00` (3).
   1. Test case: `deletepayment 1 1 2`.<br>
      Expected: Only the paid row disappears immediately. Feedback names Alex Yeoh, gig 1 (Portfolio website
      redesign), payment 2, amount `$1,250.00`, due date `2027-01-15`, and previous state `PAID`.
      The remaining rows become 1 and 2 in the same order. The client and gig remain selected and unchanged.
   1. Test case: `outstanding`, then `deletepayment 1 1 1`.<br>
      Expected: The overdue `$100.00` row disappears without rerunning `outstanding`. Feedback includes its amount,
      due date `2000-01-31`, and previous state `UNPAID`. Other clients' obligations remain visible.
   1. Test case: `deletepayment 1 1 1`.<br>
      Expected: The future `$500.00` obligation disappears from the open overview. Feedback includes due date
      `2099-01-31` and previous state `UNPAID`.
   1. Select Alex and Portfolio website redesign again.<br>
      Expected: `This gig has no payment obligations.` The gig retains its title, status, deadline, and fee.
   1. Test case: `exit`, then relaunch from the same folder.<br>
      Expected: All three deleted obligations remain absent, Alex and both his gigs remain, and other obligations
      retain their data and order. Selecting the same gig still shows the empty state.

1. **Two identical obligations**
   1. Test case: `addpayment 1 1 a/500 d/2099-01-31` twice. Select Alex and his first gig.<br>
      Expected: Two identical unpaid rows follow the sample paid row, at payment indexes 2 and 3.
   1. Test case: `deletepayment 1 1 3`.<br>
      Expected: Exactly one duplicate disappears; the sample paid row and one `$500.00` row remain in order.
   1. Test case: `deletepayment 1 1 2`.<br>
      Expected: The other duplicate disappears independently, leaving only the sample paid row.

1. **Filtered client indexes**
   1. Test case: `find Bernice`, then `addpayment 1 2 a/125 d/2099-08-15`.<br>
      Select Bernice and Quarterly newsletter template, which now has one obligation.
   1. Test case: `deletepayment 1 2 1`.<br>
      Expected: Bernice's payment disappears immediately and her gig shows the empty state. Feedback names Bernice.
      The filtered client list remains unchanged; Alex's payments and Bernice's other gig are unaffected.

1. **Invalid indexes and syntax leave data unchanged**
   1. Select Alex and his first gig. Copy `data/gigabyte.json` for comparison.
   1. Try `deletepayment 1 1 1.5` and `deletepayment 1 1 2147483648`.<br>
      Expected: Both are rejected with `Index must be a positive integer.` The payment list, parents, and saved file
      remain unchanged.
   1. Try `deletepayment`, `deletepayment 1 1`, and `deletepayment 1 1 1 2`.<br>
      Expected: Each reports `Invalid command format!` with the required syntax. The saved file still matches the
      copy.

1. **Outstanding overview becomes empty**
   1. In the disposable test folder, input `clear`, then
      `add n/Test Client p/91234567 e/test@example.com a/Test address`, then
      `addgig 1 t/Test gig s/NOT_STARTED d/2099-12-31 f/100`, then
      `addpayment 1 1 a/50 d/2099-01-31`.
   1. Test case: `outstanding`, then `deletepayment 1 1 1`.<br>
      Expected: The overview immediately shows `No outstanding payment obligations.` without another command.
      Selecting Test Client and Test gig shows `This gig has no payment obligations.`.

### Editing payment obligations

Start this section from a fresh test folder. Select `Alex Yeoh`, then select `Portfolio website redesign` so the
sample `$1,250.00` paid obligation is payment 1.

1. **Edit amount, due date, and both fields**
   1. Test case: `editpayment 1 1 1 a/1300`<br>
      Expected: Payment 1 immediately shows `$1,300.00`, keeps due date `2027-01-15`, and remains `PAID`.
   1. Test case: `addpayment 1 1 a/500 d/2099-07-15`<br>
      Expected: An unpaid `$500.00` obligation is added as payment 2.
   1. Test case: `editpayment 1 1 1 d/2099-08-01`<br>
      Expected: The same paid payment keeps amount `$1,300.00`, moves from payment 1 to payment 2 after the earlier
      `$500.00` obligation, and remains `PAID`.
   1. Test case: `editpayment 1 1 2 a/1350 d/2020-08-01`<br>
      Expected: The paid payment changes both fields atomically, moves back to payment 1, and keeps its paid state and
      gig.

1. **Edit one of two identical obligations**
   1. Test case: Enter these commands:<br>
      `addpayment 1 1 a/500 d/2099-09-01`<br>
      `addpayment 1 1 a/500 d/2099-09-01`<br>
      Expected: Two separate `$500.00` rows appear next to each other in recorded order.
   1. Test case: `editpayment 1 1 PAYMENT_INDEX a/550`, replacing `PAYMENT_INDEX` with the later identical row's displayed
      index.<br>
      Expected: Only the selected row changes to `$550.00`; the other remains `$500.00`.

1. **Refresh the outstanding overview and overdue state**
   1. Select Alex's first gig and note the current per-gig index of the unpaid `$500.00` obligation.
   1. Test case: `outstanding`.<br>
      Expected: The unpaid obligations appear in due-date order.
   1. Test case: `editpayment 1 1 PAYMENT_INDEX d/2000-01-01`, using the `$500.00` obligation's noted index.<br>
      Expected: Without rerunning `outstanding`, the edited row moves earlier and displays `OVERDUE`.

1. **Reject invalid and no-op edits**
   1. Try `editpayment 1 1 1` (no editable field), `editpayment 1 1 1 a/500 a/600`, and `editpayment 1 1 1 a/0`.<br>
      Expected: Every command is rejected and all obligations remain unchanged.
   1. Test case: Enter an edit that supplies the selected payment's existing amount and due date.<br>
      Expected: Gigabyte reports that the payment already has those details and leaves it unchanged.

1. **Persist edited details**
   1. Exit and relaunch Gigabyte after a successful edit.<br>
      Expected: The edited amount and due date, original gig association, and original paid state are restored.

### Viewing payment obligations

Start each test in this section from a fresh test folder unless the test says otherwise.

1. **Filtering, ordering, displayed indexes, and visual states**
   1. Test case: Enter these commands in order:<br>
      `addpayment 1 1 a/300 d/2099-03-01`<br>
      `addpayment 1 1 a/200 d/2099-02-01`<br>
      `addpayment 1 1 a/250 d/2099-02-01`<br>
      `addpayment 1 1 a/100 d/2020-01-01`<br>
      `addpayment 1 2 a/900 d/2099-01-01`<br>
      Expected: All five commands succeed.
   1. Select `Alex Yeoh`, then select `Portfolio website redesign`.<br>
      Expected: Exactly five payments appear. Their displayed indexes restart from 1 and their amounts are ordered
      `$100.00`, `$1,250.00`, `$200.00`, `$250.00`, `$300.00`. The `$1,250.00` sample row is `PAID`; the two
      obligations due on `2099-02-01` retain the order in which they were recorded. The `$100.00` row is `OVERDUE`
      and visually distinct from the three new `OUTSTANDING` rows.
   1. Select `Product launch photography`.<br>
      Expected: The sample `$1,200.00` obligation due `2027-03-01` and the new `$900.00` obligation due `2099-01-01`
      appear in that order; none of the first gig's payments remain visible.

1. **Mark a payment paid and reject a repeated attempt**
   1. Test case: Enter these commands:<br>
      `addpayment 1 1 a/700 d/2099-04-01`<br>
      `addpayment 1 1 a/710 d/2099-05-01`<br>
      `addpayment 1 1 a/720 d/2020-05-01`<br>
      Expected: All three obligations are recorded as unpaid.
   1. Select `Alex Yeoh`, then select `Portfolio website redesign`.<br>
      Expected: The `$720.00` row is payment 1, the sample `$1,250.00` paid row is payment 2, and the `$700.00` and
      `$710.00` rows are payments 3 and 4 respectively.
   1. Test case: `markpaid 1 1 3`<br>
      Expected: The `$700.00` row shows `PAID`. Its muted paid styling is visibly different from the blue outstanding
      `$710.00` row and the red overdue `$720.00` row. Its stored due date remains unchanged.
   1. Test case: `markpaid 1 1 3` again.<br>
      Expected: The command is rejected because the obligation is already paid, and all payment data remains unchanged.

1. **Outstanding-payment overview**
   1. Test case: `addpayment 1 1 a/100 d/2000-01-01`, followed by
      `addpayment 1 1 a/500 d/2099-01-01`.<br>
      Expected: Both unpaid obligations are recorded for Alex's first gig.
   1. Test case: `markpaid 1 1 3`<br>
      Expected: The new `$500.00` obligation is marked paid; payment 1 is the new overdue row and payment 2 is the
      earlier sample paid row.
   1. Test case: `find Charlotte`, followed by `outstanding`.<br>
      Expected: Outstanding obligations across all clients appear despite the filtered client list. The overdue
      `$100.00` row is first and labelled `OVERDUE`; the sample `$1,800.00` and `$1,200.00` unpaid rows follow in
      due-date order. The sample `$1,250.00` and new `$500.00` paid rows are absent.
   1. Test case: `outstanding extra`.<br>
      Expected: The command is rejected, the existing overview remains visible, and no data changes.

1. **Field-identical gigs remain separate**
   1. Test case: Enter the following command twice:<br>
      `addgig 1 t/Identical gig s/NOT_STARTED d/2099-06-01 f/100`<br>
      Expected: Two separate, otherwise identical gigs are added for Alex as gigs 3 and 4.
   1. Test case: `addpayment 1 3 a/111 d/2099-06-15`, followed by
      `addpayment 1 4 a/222 d/2099-06-15`.<br>
      Expected: Both commands succeed.
   1. Select `Alex Yeoh`, then select the first `Identical gig` (gig 3).<br>
      Expected: Only the `$111.00` payment appears.
   1. Select the second `Identical gig` (gig 4).<br>
      Expected: Only the `$222.00` payment appears, confirming that field-identical gigs keep separate payments.

1. **Keyboard navigation across lists**
   1. With the command box focused, press Tab, then Down and Up.<br>
      Expected: The first client is selected on Tab; the selected client then changes and the gig list follows the
      selection without moving keyboard focus.
   1. Press Right, Down, Right, Down, then Left twice.<br>
      Expected: Focus moves through gigs and payment obligations, selects a row in each, then returns to clients.
   1. Press Right twice to reach payment obligations, then Tab, type `list`, and press Tab again.<br>
      Expected: Focus moves to the command box, retains the text, then moves to the client list.

1. **Usability at the supported scaled viewport**
   1. Set the display resolution to 1280×720 and display scaling to 150%, then launch the JAR.<br>
      Expected: The window fits within the usable screen area. Clients remain on the left; the Gigs and Payment
      obligations panels are stacked on the right. The command box, result display, and status bar remain reachable.
   1. Select Alex, select a gig, and resize the vertical divider between the Gigs and Payment obligations panels.<br>
      Expected: Both panels remain usable, and payment rows can be reached by scrolling when necessary.

### Viewing the summary

Start this section from a fresh test folder. The overdue figures below assume the test runs before `2026-12-15`.
From that date onwards, the sample `$1,800.00` obligation is also overdue, so add `1 ($1,800.00)` to each overdue
figure.

1. **Summary of the sample data**
   1. Test case: `summary`<br>
      Expected: The result shows `Clients: 6` and `Gigs: 4 total`, with `NOT_STARTED: 1`, `IN_PROGRESS: 2`, and
      `COMPLETED: 1`. It shows `Payments: 3 total`, with `Outstanding: 2 ($3,000.00)`,
      `Of which overdue: 0 ($0.00)`, and `Marked paid: 1 ($1,250.00)`.

1. **Summary ignores the client filter**
   1. Test case: `find Alex`, then `addpayment 1 1 a/100.50 d/2000-01-01`, then `summary`.<br>
      Expected: The result still shows `Clients: 6`. It now shows `Payments: 4 total`,
      `Outstanding: 3 ($3,100.50)`, and `Of which overdue: 1 ($100.50)`. The filtered client list and the current panels are unchanged.

### Commands after filtering clients

Start this section from a fresh test folder and perform the tests in order.

1. **Adding a gig after `find`**
   1. Test case: `find Bernice`<br>
      Expected: Only `Bernice Yu` is displayed and is numbered 1.
   1. Test case: `addgig 1 t/Filtered client gig s/NOT_STARTED d/2027-08-01 f/250`<br>
      Expected: The gig is created for Bernice, not for the client who was number 1 in the full list. The filtered
      client list remains visible.
   1. Select Bernice.<br>
      Expected: The new gig appears after Bernice's two sample gigs.

1. **Updating a gig after `find`**
   1. Test case: `find Bernice`, followed by `setstatus 1 2 s/COMPLETED`.<br>
      Expected: `Quarterly newsletter template`, Bernice's second gig, changes from `IN_PROGRESS` to `COMPLETED`.
      No gig belonging to a client hidden by the filter changes.

1. **Returning to the full list**
   1. Test case: `list`<br>
      Expected: The full six-client list is restored; subsequent client indexes use this full list.

Adding and settling a payment after `find` is covered by the
[Complete end-to-end workflow](#complete-end-to-end-workflow).

### Deleting gigs safely

Use a disposable fresh sample-data folder for each test. Deletion is permanent and has no undo.

1. Test case: `deletegig 1 1` (paid payment) and `deletegig 1 2` (unpaid payment).<br>
   Expected: Both are rejected with instructions to remove payments using `deletepayment`. Records and the saved
   file remain unchanged. Settling an obligation does not unblock deletion. Input `deletepayment 1 1 1`, then
   `deletegig 1 1`: Alex's first gig can now be deleted, while his other gig and its payment remain unchanged.
1. Select Bernice and Quarterly newsletter template, then input `deletegig 2 2`.<br>
   Expected: Only that gig disappears; Bernice and Brand identity refresh remain. Gig selection clears and the payment
   panel prompts for a gig. Restart: the removed gig remains absent. Repeat from fresh data with `gigs s/IN_PROGRESS`,
   `deadlines`, or `findgig newsletter` open before deletion: the active view refreshes without another command.
1. Add the same gig twice using `addgig 1 t/Duplicate s/NOT_STARTED d/2099-01-31 f/100`, then input
   `addpayment 1 3 a/50 d/2099-02-28`, followed by `deletegig 1 4`.<br>
   Expected: Only the fourth gig disappears. The third gig and its payment remain linked; `deletegig 1 3` is rejected.
1. Test case: `find Bernice`, then `deletegig 1 2`.<br>
   Expected: Bernice's second gig disappears and the client filter remains. Try `deletegig`, `deletegig 1 1 2`,
   `deletegig 0 1`, `deletegig 1 abc`, `deletegig 2 1`, and `deletegig 1 999`: all fail without changing records or disk.
1. Test case: `add n/Deletion Test p/91234567 e/deletion@example.com a/Test address`, then `find Deletion`, then
   `addgig 1 t/Temporary s/COMPLETED d/2099-01-31 f/100`, `deletegig 1 1`, and `delete 1`.<br>
   Expected: The completed gig is removed and its client can then be deleted; other clients remain unchanged.

### Deleting a client with gigs

1. Test case: `list`, followed by `delete 1`.<br>
   Expected: Alex is not deleted and the result says `Client cannot be deleted while it has associated gigs`.
1. Test case: `find Charlotte`, followed by `delete 1`.<br>
   Expected: Charlotte, who has no gigs in the sample data, is deleted and the result shows her details.

### Persistence and data-file recovery

1. **Restart after each mutation type**
   1. Use a separate copy of the fresh sample folder for each command below. Run the command, close with `exit`,
      relaunch the same JAR from that folder, and inspect the named record or run `summary`.<br>
      Inputs and expected persisted results:

      | Input on fresh sample data | Expected after restart |
      | --- | --- |
      | `add n/Jamie Tan p/81234567 e/jamie@example.com a/Test road` | Jamie exists. |
      | `edit 1 p/91234567` | Alex's phone is `91234567`. |
      | `delete 3` | Charlotte is absent; other clients and gigs remain. |
      | `addgig 1 t/Retest s/NOT_STARTED d/2099-01-01 f/100` | Alex has a third gig named Retest. |
      | `editgig 1 2 f/1300` | Alex's second gig has a `$1,300.00` fee and its payment remains. |
      | `setstatus 1 2 s/COMPLETED` | Alex's second gig is completed. |
      | `deletegig 2 2` | Bernice's second gig is absent; her first gig remains. |
      | `addpayment 1 2 a/80 d/2099-02-01` | Alex's second gig has its sample payment and the new `$80.00` payment. |
      | `editpayment 1 2 1 a/1300` | Alex's second gig's sample payment is `$1,300.00`, still unpaid. |
      | `markpaid 1 2 1` | Alex's second gig's sample payment is paid. |
      | `markunpaid 1 1 1` | Alex's first gig's sample payment is unpaid. |
      | `deletepayment 1 2 1` | Alex's second gig has no payments. |
      | `clear` | No clients, gigs, or payments remain. |

   1. After each relaunch, inspect `data/gigabyte.json`.<br>
      Expected: It contains the corresponding current-format records and valid client/gig links; a deleted record
      does not return. Read-only `find`, `findgig`, `gigs`, `deadlines`, `outstanding`, `summary`, `help`, and `list`
      do not mutate records.

1. **Restart after adding data**
   1. In a fresh test folder, input
      `addgig 1 t/Persistence check s/NOT_STARTED d/2027-09-01 f/900`, then
      `addpayment 1 3 a/450 d/2027-09-15`, `setstatus 1 3 s/IN_PROGRESS`, `markpaid 1 3 1`, then `exit`.<br>
      Expected: All four data-changing commands succeed before the app exits.
   1. Relaunch the same JAR from the same folder, select Alex, then select `Persistence check`.<br>
      Expected: `Persistence check` is still Alex's third gig with status `IN_PROGRESS`, deadline `2027-09-01`, and
      fee `$900.00`. Its Payment obligations panel shows `1. Amount: $450.00`, due `2027-09-15`, as `PAID`.

1. **Previous save-file name**
   1. In a test folder, close the app and rename `data/gigabyte.json` to `data/addressbook.json`, then relaunch.<br>
      Expected: All saved data is loaded, `data/gigabyte.json` exists with the same contents, and
      `data/addressbook.json` no longer exists. Subsequent commands save to `data/gigabyte.json`.
   1. Close the app, place a different valid save at `data/addressbook.json` while keeping `data/gigabyte.json`,
      then relaunch.<br>
      Expected: Gigabyte loads `data/gigabyte.json` and leaves both files unchanged.

1. **v1.3 format compatibility boundary**
   1. In a disposable test folder, close the JAR and place a valid v1.3 JSON save using the older `persons`
      collection at `data/addressbook.json`; ensure `data/gigabyte.json` is absent. Keep an untouched backup.
      Relaunch the JAR.<br>
      Expected for the current release candidate: the file is moved to `data/gigabyte.json` without conversion,
      loading fails, and the app starts with an empty client list. The moved file is unchanged until a successful
      command saves data. This is a release compatibility gap if v1.3 data loading is required.
   1. Close the window without entering a command and compare the moved file with the backup.<br>
      Expected: They are byte-for-byte identical. A separate test with current-format JSON in
      `data/addressbook.json` loads successfully after the filename move.

1. **Missing data file**
   1. In a test folder with no `data/addressbook.json`, close the app and rename `data/gigabyte.json` to
      `gigabyte.backup.json`, then relaunch.<br>
      Expected: Gigabyte starts with the six sample clients and their sample gigs. The backup file is unchanged.

1. **Corrupted data file**
   1. Close the app, back up `data/gigabyte.json`, replace its contents with `{not valid json`, and relaunch.<br>
      Expected: Gigabyte starts with an empty client list because the existing file could not be read.
   1. Test case: `list`.<br>
      Expected: The command succeeds with the empty list and overwrites the corrupted file with valid empty data.

### Complete end-to-end workflow

1. Start from a fresh test folder and input `find Alex`.<br>
   Expected: Alex is the only displayed client and is numbered 1.
1. Test case: `addgig 1 t/End-to-end website s/IN_PROGRESS d/2027-10-01 f/2000`.<br>
   Expected: A titled gig is created for Alex while the filtered list remains visible.
1. Select Alex, select `End-to-end website`, note that it is the third gig, then input
   `addpayment 1 3 a/1000 d/2027-10-15`.<br>
   Expected: An unpaid obligation is recorded for Alex's third gig and immediately appears in its Payment
   obligations panel.
1. Test case: `setstatus 1 3 s/COMPLETED`<br>
   Expected: `End-to-end website` changes to `COMPLETED` without changing its title, deadline, fee, or payment.
1. Test case: `editgig 1 3 t/Delivered website d/2027-09-30 f/2100`, then `findgig Delivered`.<br>
   Expected: The edited gig is found under its new title, retains its payment, and has the revised deadline and fee.
   The client list remains filtered to Alex.
1. Test case: `editpayment 1 3 1 a/1050 d/2027-10-20`.<br>
   Expected: The payment amount and due date change while it remains unpaid and linked to the edited gig.
1. Test case: `markpaid 1 3 1`<br>
   Expected: The `$1,050.00` obligation immediately changes to `PAID`.
1. Test case: `markunpaid 1 3 1`, then `outstanding`.<br>
   Expected: The corrected `$1,050.00` payment appears in the unpaid overview.
1. Test case: `markpaid 1 3 1` again.<br>
   Expected: The payment leaves the open outstanding overview immediately.
1. Test case: `gigs s/COMPLETED`<br>
   Expected: Completed gigs across all clients are shown, including `Delivered website`.
1. Test case: `deadlines`<br>
   Expected: Active gigs are shown in deadline order; `Delivered website` is absent because it is completed.
1. Test case: `outstanding`<br>
   Expected: Unpaid obligations across all clients are shown in due-date order; the paid `$1,050.00` obligation is
   absent.
1. Test case: `exit`, relaunch from the same folder, input `find Alex`, and select Alex.<br>
   Expected: `Delivered website` is still present with status `COMPLETED`, deadline `2027-09-30`, and fee `$2,100.00`.
1. Select `Delivered website`.<br>
   Expected: Its Payment obligations panel shows `1. Amount: $1,050.00`, due `2027-10-20`, as `PAID`. No payment from
   another gig is shown.
1. Test case: `deletegig 1 3`.<br>
   Expected: Deletion is blocked by the paid payment. Input `deletepayment 1 3 1`, then `deletegig 1 3`.<br>
   Expected: Both deletions succeed; Alex's other gigs and their payments remain.
1. Exit and relaunch from the same folder, then input `find Alex` and select Alex.<br>
   Expected: `Delivered website` and its payment remain absent, confirming both deletions persisted.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Effort**

**Difficulty level**

AB3 manages a single entity type (`Person`) in one list. Gigabyte manages three linked entity types: `Client`,
`Gig`, and `PaymentObligation`. Each gig belongs to a client and each payment obligation belongs to a gig, so most
features had to consider how a change to one record affects the records linked to it.

**Challenges faced**

* **Keeping links correct with immutable objects.** Core domain entities such as `Client`, `Gig`, and
  `PaymentObligation` are immutable, following AB3's entity design. Editing a client or a gig therefore creates a new
  object, and every linked gig or payment obligation must be relinked to the replacement.
  Stable UUIDs, and the relinking logic in `GigabyteData`, were added so that records with identical display fields
  are never confused (see [Stable client, gig, and payment links](#stable-client-gig-and-payment-links)).
* **Nested indexes.** Gig and payment commands take two or three indexes. The order used by commands and the order
  shown in the GUI had to be kept identical, while the cross-client views use a different order (see
  [Per-client gig and payment indexes](#per-client-gig-and-payment-indexes)).
* **Several views of the same data.** The `gigs`, `findgig`, `deadlines`, and `outstanding` views had to update
  immediately after edits, without changing the client filter that commands depend on (see
  [Cross-client gig and payment views](#cross-client-gig-and-payment-views)).
* **Safe deletion of linked records.** Deleting a client or gig is blocked while other records still refer to it,
  and deleting one of two identical payments must remove only the indexed one (see
  [Deleting linked records safely](#deleting-linked-records-safely)).
* **Strict and safe storage.** The save format stores links by UUID and money as integer cents. Loading rejects
  unknown fields, missing fields, and broken references, and the old `addressbook.json` file name is migrated safely.
* **Exact money handling.** Fees and payment amounts use `BigDecimal` and integer cents, so totals in `summary` are
  exact to the cent.

**Reuse**

The architecture, the `Main` and `Commons` code, the JSON storage framework, the GUI framework, and the test setup were
reused from AB3. AB3's `Person` was adapted into `Client`. The `Gig` and `PaymentObligation` entities, their commands,
parsers, storage adapters, and GUI panels, and the cross-client views were written by the team.

**Achievements**

Gigabyte extends AB3 with gig tracking (`addgig`, `editgig`, `setstatus`, `deletegig`, `gigs`, `findgig`, and
`deadlines`), payment tracking (`addpayment`, `editpayment`, `markpaid`, `markunpaid`, `deletepayment`, and
`outstanding`), and a work and payment `summary`. The GUI was extended from one list to three linked panels for
clients, gigs, and payment obligations.
