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

* _{List the sources of reused or adapted ideas, code, documentation, and third-party libraries here, with links to the originals.}_

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
`GigListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart`
class, which captures common behavior among classes that represent visible GUI parts. Selecting a client in
`ClientListPanel` passes that client to `GigListPanel`, which filters the observable gig list by the client's stable
identifier and renders each gig's title, status, deadline, and fee.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Client` objects from the model.
* displays `Gig` objects for the selected client without changing the client filter used by commands.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

<box type="info" seamless>

**Note:** The lifeline for `DeleteClientCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, the lifeline continues till the end of diagram.
</box>


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `GigabyteParser` object, which in turn creates a parser that matches the command (e.g., `DeleteClientCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteClientCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a client).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:
* When called upon to parse a user command, the `GigabyteParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddClientCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddClientCommand`). The `GigabyteParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddClientCommandParser` and `DeleteClientCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.
* `AddGigCommandParser` parses one displayed client index plus `t/`, `s/`, `d/`, and `f/` fields.
  `AddPaymentCommandParser` parses displayed client and per-client gig indexes plus `a/` and `d/` fields. Both reject
  missing or repeated single-valued prefixes before their commands update the model.

### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-W14-4/tp/tree/master/src/main/java/gigabyte/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="450" />


The `Model` component,

* stores all `Client`, `Gig`, and `PaymentObligation` objects. Clients are contained in a `UniqueClientList`; gigs and
  payment obligations are exposed as unmodifiable observable lists.
* stores the `Client` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Client>` that the UI can observe and bind to, so the UI updates when the list changes.
* uses stable UUIDs to preserve links from each gig to its client and from each payment obligation to its gig. Client
  edits replace linked immutable objects while retaining those identifiers and links.
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
* migrates supported older representations when loading, including name-linked gigs, index-linked payment
  obligations, decimal fee strings, and title-less gigs. A title-less gig receives `Untitled gig`, which is written in
  the current format on the next successful save.

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
relinks payment obligations to the replacement gig objects. `Gig#withClient` preserves the gig UUID and title, so
editing contact details does not break payment links or discard gig information. When loading legacy data, storage
resolves older name- and index-based links once and the next successful save writes the stable-ID representation.

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
  * Cons: We must ensure that the implementation of each individual command is correct.

_{more aspects and alternatives to be added}_

### \[Proposed\] Data archiving

_{Explain here how the data archiving feature will be implemented}_


--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

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

| Priority | As a …                                     | I want to …                                                              | So that I can…                                                         |
|----------|--------------------------------------------|--------------------------------------------------------------------------|------------------------------------------------------------------------|
| `* * *`  | new freelancer user                        | see instructions for available commands                                  | learn how to use Gigabyte                                              |
| `* * *`  | user                                       | add a new person                                                         |                                                                        |
| `* * *`  | user                                       | delete a person                                                          | remove entries that I no longer need                                   |
| `* * *`  | user                                       | find a person by name                                                    | locate details of persons without having to go through the entire list |
| `* * *`  | freelancer                                 | record a payment obligation linked to a gig, with an amount and due date | know what payment I expect and when                                    |
| `* * *`  | freelancer                                 | mark a payment obligation as paid                                        | distinguish settled payments from money still owed                      |
| `* * *`  | freelancer                                 | view a client’s details and associated gigs                              | understand my work with that client                                    |
| `* * *`  | freelancer                                 | create a gig linked to a client with a title, status, deadline, and fee  | track each piece of work                                               |
| `* * *`  | freelancer                                 | view upcoming gig deadlines in date order                                | prioritise my work                                                     |
| `* *`    | user                                       | hide private contact details                                             | minimize chance of someone else seeing them by accident                |
| `* *`    | freelancer                                 | filter clients or gigs by relevant criteria                              | focus on the records I need                                            |
| `*`      | user with many persons in the address book | sort persons by name                                                     | locate a person easily                                                 |
| `*`      | freelancer                                 | archive completed gigs                                                   | keep the active view focused on current work                           |


### Use cases

For all use cases below, the **System** is the `Gigabyte` and the **Actor** is the `freelancer`, unless specified otherwise.

**Use case: Create a gig for a displayed client**

**Preconditions:** The client already exists and is visible in the current client list.

**MSS**

1. Freelancer identifies the client's displayed index.
2. Freelancer enters `addgig` with a title, initial status, deadline, and agreed fee.
3. Gigabyte validates the index and all four fields.
4. Gigabyte creates the gig, links it to the client, saves the data, and confirms the new gig details.

    Use case ends.

**Extensions**

* 1a. The required client is not visible because the client list is filtered.
  * 1a1. Freelancer runs `list` or a suitable `find` command and resumes at step 1 using the newly displayed index.
* 3a. The client index is invalid.
  * 3a1. Gigabyte reports that the client index is invalid without creating a gig.
* 3b. The title, status, deadline, or fee is invalid.
  * 3b1. Gigabyte explains the relevant constraint without creating a gig.

**Use case: Record a payment obligation**

**Preconditions:** The client and gig already exist.

**MSS**

1. Freelancer identifies the client's displayed index and the gig's position within that client's gig list.
2. Freelancer enters `addpayment` with both indexes, an amount, and a due date.
3. Gigabyte validates the indexes, amount, and due date.
4. Gigabyte records the obligation as unpaid, links it to the gig, saves the data, and confirms the amount and due date.

    Use case ends.

**Extensions**

* 3a. Either index is invalid.
  * 3a1. Gigabyte reports whether the client index or per-client gig index is invalid without recording an obligation.
* 3b. The amount or due date is invalid.
  * 3b1. Gigabyte explains the relevant constraint without recording an obligation.

**Use case: Record and settle a payment obligation**

**Preconditions:** The relevant gig already exists in Gigabyte.

**MSS**

1.  Freelancer requests to view the gig.
2.  Gigabyte shows the gig and its associated payment obligations.
3.  Freelancer requests to record a new payment obligation.
4.  Freelancer supplies the amount and due date.
5.  Gigabyte validates and records the obligation as outstanding.
6.  Freelancer requests to view outstanding payments.
7.  Gigabyte shows the obligation in due date order.
8.  Freelancer selects the obligation and marks it as paid.
9.  Gigabyte records the paid status and confirms the change.

    Use case ends.

**Extensions**

* 4a. The amount or due date is invalid.

    * 4a1. Gigabyte explains the error without creating an obligation.
    Use case resumes at step 4.

* 8a. The selected obligation is already paid.

    * 8a1. Gigabyte informs the freelancer that no change was made.
    Use case ends.

### Non-Functional Requirements

1. Platform and distribution: Gigabyte should run on mainstream desktop operating systems with Java 25 or above installed, and be distributed as a single JAR file that requires no installer.
2. Offline use and persistence: Gigabyte’s core client, gig, and payment workflows should work without a remote server or network connection. Changes should be saved locally and remain available after the application is closed and reopened.
3. Capacity and performance: Gigabyte should support up to 1,000 clients, 5,000 gigs, and 5,000 payment obligations without noticeable sluggishness during typical use. On a supported computer, searches, filters, and deadline or due-date views should display results within 2 seconds.
4. Command-based usability: Users should be able to complete the main workflows by typing commands, without needing a mouse. For regular English text, users with above-average typing speed should be able to complete most tasks faster with commands than with the mouse.
5. Display compatibility: Gigabyte should remain usable at 1280×720 with 150% screen scaling, and work well at 1920×1080 with 100% or 125% scaling.
6. Data integrity and validation: Gigabyte should maintain valid links between gigs and clients, and between payment obligations and gigs. It should reject invalid dates and fees with clear error messages.

### Glossary

* **Client**: A person or organization that engages the freelancer for work.
* **Gig**: A piece of work undertaken for a client, with its own deadline, agreed fee, and status.
* **Mainstream OS**: Windows, Linux, Unix, or macOS
* **Outstanding payment**: A payment obligation that has not been marked as paid.
* **Overdue payment**: An outstanding payment whose due date is before the current date.
* **Private contact detail**: A contact detail that is not meant to be shared with others
* **Payment obligation**: An amount the freelancer expects to receive for a gig, with a due date and payment status.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<box type="info" seamless>

**Preparation:** Build the release candidate with `gradlew shadowJar` (macOS/Linux: `./gradlew shadowJar`) and copy
`build/libs/gigabyte.jar` into an otherwise empty test folder. Unless a test says otherwise, launch it from that folder
with `java -jar gigabyte.jar`. Back up `data/addressbook.json` before any test that changes or corrupts it.
</box>

### Launch, sample data, and empty states

1. **Fresh launch with sample data**
   1. Ensure the test folder has no `data` directory or `preferences.json`, then launch the JAR.<br>
      Expected: Six sample clients appear. The Gigs panel says `Select a client to view their gigs.`
   1. Select `Alex Yeoh`.<br>
      Expected: The Gigs panel shows `Portfolio website redesign` and `Product launch photography`, including each
      gig's status, deadline, and fee.
   1. Select `Charlotte Oliveiro`.<br>
      Expected: The Gigs panel says `This client has no gigs.`

1. **Empty client list**
   1. Input: `clear`<br>
      Expected: The client list becomes empty, all gigs disappear, and the result display says
      `Address book has been cleared!`.

1. **Window preferences**
   1. Resize and move the window, close it, then relaunch the JAR.<br>
      Expected: The most recent window size and position are restored.

### Adding gigs

Start this section from a fresh test folder so the sample clients and gigs are present.

1. **Valid gig**
   1. Input: `list`<br>
      Expected: All six sample clients are displayed.
   1. Input: `addgig 1 t/Conference landing page s/IN_PROGRESS d/2027-06-30 f/1800.50`<br>
      Expected: The result names `Alex Yeoh` and repeats the normalized title, status, deadline, and fee.
   1. Select `Alex Yeoh`.<br>
      Expected: `Conference landing page` appears after Alex's two sample gigs with status `IN_PROGRESS`, deadline
      `2027-06-30`, and fee `$1800.50`.

1. **Title validation**
   1. Input: `addgig 1 t/    s/NOT_STARTED d/2027-06-30 f/100`<br>
      Expected: No gig is added and the result explains that a title needs a non-whitespace character.
   1. Input: `addgig 1 t/` followed by a 101-character title, then
      ` s/NOT_STARTED d/2027-06-30 f/100`.<br>
      Expected: No gig is added and the result explains the 100-character maximum.
   1. Input: `addgig 1 t/  Short title  s/NOT_STARTED d/2027-06-30 f/100`<br>
      Expected: The gig is added with the displayed title `Short title`; surrounding whitespace is removed.

1. **Invalid status, date, and fee**
   1. Input: `addgig 1 t/Invalid status s/WAITING d/2027-06-30 f/100`<br>
      Expected: No gig is added; the result lists `NOT_STARTED`, `IN_PROGRESS`, and `COMPLETED` as valid statuses.
   1. Input: `addgig 1 t/Invalid date s/NOT_STARTED d/2027-02-29 f/100`<br>
      Expected: No gig is added; the result requires a valid `yyyy-MM-dd` calendar date.
   1. Try the same command with each fee: `f/0`, `f/-10`, `f/abc`, and `f/10.999`.<br>
      Expected: No gig is added in every case; the result requires a positive number with at most two decimal places.

1. **Repeated prefix**
   1. Input: `addgig 1 t/First t/Second s/NOT_STARTED d/2027-06-30 f/100`<br>
      Expected: No gig is added; the result reports multiple values for `t/`.

1. **Invalid client indexes**
   1. Repeat a valid `addgig` command using client indexes `0`, `-1`, `abc`, and `999`.<br>
      Expected: No gig is added. Zero, negative, and non-numeric values produce the command-format error; `999`
      reports that the client index is invalid.

### Help and client commands

1. **Help**
   1. Input: `help`<br>
      Expected: The Help window opens and shows the link to the User Guide.

1. **Add a client**
   1. Input: `add n/Jamie Tan p/81234567 e/jamie@example.com a/10 Clementi Road t/new`<br>
      Expected: Jamie is added, appears in the displayed client list, and the result shows the supplied details.

1. **Edit a client**
   1. Input: `find Jamie`, followed by `edit 1 p/87654321 t/priority`.<br>
      Expected: The complete client list is displayed again. Jamie's phone changes to `87654321`, and `priority`
      replaces the existing tags.

1. **Find and list clients**
   1. Input: `find Jamie`<br>
      Expected: Only clients whose names contain the full word `Jamie` are displayed.
   1. Input: `list`<br>
      Expected: The complete client list is displayed again and the result says `Listed all clients.`

1. **Exit**
   1. Input: `exit`<br>
      Expected: Gigabyte closes. Relaunching it from the same folder restores the client changes made above.

### Adding payment obligations

Start this section from a fresh test folder. Alex's sample gigs make `CLIENT_INDEX 1` and `GIG_INDEX 1` valid.

1. **Valid payment obligation**
   1. Input: `addpayment 1 1 a/500.00 d/2027-07-15`<br>
      Expected: The result says an unpaid obligation was recorded for `Alex Yeoh`, gig 1, with amount `500.00` and
      due date `2027-07-15`.

1. **Invalid amount and date**
   1. Repeat the command with each amount: `a/0`, `a/-10`, `a/abc`, and `a/10.999`.<br>
      Expected: No obligation is recorded in every case; the result requires a positive number with at most two
      decimal places.
   1. Input: `addpayment 1 1 a/500 d/15-07-2027`<br>
      Expected: No obligation is recorded; the result requires a valid `yyyy-MM-dd` calendar date.

1. **Repeated prefix**
   1. Input: `addpayment 1 1 a/500 a/600 d/2027-07-15`<br>
      Expected: No obligation is recorded; the result reports multiple values for `a/`.

1. **Invalid client and gig indexes**
   1. Repeat a valid command with each client index: `0`, `-1`, `abc`, and `999`.<br>
      Expected: No obligation is recorded. The first three produce the command-format error; `999` reports an invalid
      client index.
   1. Repeat a valid command with each gig index: `0`, `-1`, `abc`, and `999`.<br>
      Expected: No obligation is recorded. The first three produce the command-format error; `999` reports that the
      gig index is invalid for this client.

### Commands after filtering clients

1. **Adding a gig after `find`**
   1. Input: `find Bernice`<br>
      Expected: Only `Bernice Yu` is displayed and is numbered 1.
   1. Input: `addgig 1 t/Filtered client gig s/NOT_STARTED d/2027-08-01 f/250`<br>
      Expected: The gig is created for Bernice, not for the client who was number 1 in the full list. The filtered
      client list remains visible.
   1. Select Bernice.<br>
      Expected: The new gig appears with Bernice's existing `Brand identity refresh` gig.

1. **Adding a payment after `find`**
   1. Input: `find Bernice`, followed by `addpayment 1 1 a/125 d/2027-08-15`.<br>
      Expected: The result confirms an unpaid obligation for Bernice's first gig.
   1. Input: `list`<br>
      Expected: The full six-client list is restored; subsequent client indexes use this full list.

### Deleting a client with gigs

1. Input: `list`, followed by `delete 1`.<br>
   Expected: Alex is not deleted and the result says `Client cannot be deleted while it has associated gigs`.
1. Input: `find Charlotte`, followed by `delete 1`.<br>
   Expected: Charlotte, who has no gigs in the sample data, is deleted and the result shows her details.

### Persistence and data-file recovery

1. **Restart after adding data**
   1. In a fresh test folder, input
      `addgig 1 t/Persistence check s/NOT_STARTED d/2027-09-01 f/900`, then
      `addpayment 1 3 a/450 d/2027-09-15`, then `exit`.<br>
      Expected: Both commands succeed before the app exits.
   1. Relaunch the same JAR from the same folder and select Alex.<br>
      Expected: `Persistence check` is still Alex's third gig with the same status, deadline, and fee.
   1. Close the app and open `data/addressbook.json` in a text editor.<br>
      Expected: The `paymentObligations` array contains an unpaid obligation with `"amountCents" : 45000`, linked by
      `gigUid` to the persisted gig.

1. **Missing data file**
   1. Close the app and rename `data/addressbook.json` to `addressbook.backup.json`, then relaunch.<br>
      Expected: Gigabyte starts with the six sample clients and their sample gigs. The backup file is unchanged.

1. **Corrupted data file**
   1. Close the app, back up `data/addressbook.json`, replace its contents with `{not valid json`, and relaunch.<br>
      Expected: Gigabyte starts with an empty client list because the existing file could not be read.
   1. Input: `list`.<br>
      Expected: The command succeeds with the empty list and overwrites the corrupted file with valid empty data.

### Complete end-to-end workflow

1. Start from a fresh test folder and input `find Alex`.<br>
   Expected: Alex is the only displayed client and is numbered 1.
1. Input: `addgig 1 t/End-to-end website s/IN_PROGRESS d/2027-10-01 f/2000`.<br>
   Expected: A titled gig is created for Alex while the filtered list remains visible.
1. Select Alex and note that the new gig is third, then input `addpayment 1 3 a/1000 d/2027-10-15`.<br>
   Expected: An unpaid obligation is recorded for Alex's third gig.
1. Input `exit`, relaunch from the same folder, input `find Alex`, and select Alex.<br>
   Expected: `End-to-end website` is still present with its status, deadline, and fee unchanged.
1. Close the app and inspect `data/addressbook.json`.<br>
   Expected: The gig has a stable `uid` and Alex's `clientUid`; the payment obligation refers to that gig's `uid`
   using `gigUid`.
