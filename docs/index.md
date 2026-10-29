---
  layout: default.md
  title: ""
---

# Gigabyte

[![CI Status](https://github.com/AY2627S1-CS2103T-W14-4/tp/actions/workflows/gradle.yml/badge.svg)](https://github.com/AY2627S1-CS2103T-W14-4/tp/actions/workflows/gradle.yml)
[![codecov](https://codecov.io/gh/AY2627S1-CS2103T-W14-4/tp/graph/badge.svg?token=S4O2ICMM1L)](https://codecov.io/gh/AY2627S1-CS2103T-W14-4/tp)

![Ui](images/Ui.png)

**Gigabyte is a desktop app for freelancers who manage multiple clients, gigs, and payment deadlines.** It is designed
for users who prefer typing commands, and helps them keep client work and payments organised so that no invoice goes
overdue unnoticed.

* If you are interested in using Gigabyte, head over to the [_Quick Start_ section of the **User Guide**](UserGuide.html#quick-start).
* If you are interested in developing Gigabyte, the [**Developer Guide**](DeveloperGuide.html) is a good place to start.

**Features**

* Add, edit and delete clients, with their contact details and tags.
* Find clients by name, and list all clients again.
* Select a client to see their gigs, and a gig to see its payment obligations, with the mouse or keyboard.
* Add, edit and delete gigs, each with a title, status, deadline and agreed fee.
* Update a gig's status, or filter gigs across all clients by status.
* Find gigs by words in their titles.
* Use `deadlines` to see all active gig deadlines, soonest first; overdue gigs are labelled `OVERDUE`.
* Record, edit and delete payment obligations for a gig, and mark them paid or unpaid.
* Use `outstanding` to see all unpaid payment obligations, earliest due first; overdue ones are labelled `OVERDUE`.
* Use `summary` to see client and gig counts with outstanding, overdue and paid totals.
* Records are saved after every command and restored when the app is reopened.

**Acknowledgements**

* Libraries used: [JavaFX](https://openjfx.io/), [Jackson](https://github.com/FasterXML/jackson), [JUnit5](https://github.com/junit-team/junit5)
* Gigabyte is based on AddressBook-Level3, created by the [SE-EDU initiative](https://se-education.org).
