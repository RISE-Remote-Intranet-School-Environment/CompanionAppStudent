# Objectives and evaluation

## Objective

Build a student-facing “companion” application using this template. Your solution must use:
- Kotlin Compose Multiplatform for the client UI (Android, iOS, Desktop/JVM, Web/Wasm).
- Ktor for the backend server provided in this repository.

Purpose
The app should help students with day-to-day school-related matters. Example features (pick a realistic subset):
- Schedule and calendar: courses, exams, reminders, notifications.
- Coursework: assignments, deadlines, submissions checklist, grade tracking.
- Information hub: announcements, news feed, FAQs, campus map/contacts.
- Productivity: simple notes/to-dos, bookmarks/links, offline-first for key data.
- Communication: read-only messages/alerts from the school or a mock feed.

Constraints
- Keep the baseline stack: Kotlin Compose Multiplatform + Ktor. You may refactor, rewrite, or delete example code, but do not replace the core technologies.
- Favor official JetBrains/Google libraries. Third-party dependencies beyond these must be justified and pre-approved (during meeting).
- Aim for accessibility (scalable text, contrast), basic security (no secrets in client; validate on server), and performance (smooth UI at 60fps on modest devices).

Deliverable outcome
- A functional prototype demonstrating a coherent student companion experience across supported platforms.

## Evaluation

Group project with mandatory check-ins every two weeks. Meetings are for scope tracking, blockers, and feedback.

Milestones
- First meeting: present a realistic scope proposal for the deadline. Define target platforms for the demo, user stories, and a mini roadmap.
- Iterations: show incremental progress at each meeting (UI walkthroughs, server endpoints, tests, or usability improvements).
- Final session: live demo of the agreed scope running on the declared platforms.

Grading rubric (indicative weights)
- Group criteria (70%)
  - Attendance and preparation for meetings (10%)
  - Collaboration and organization: task board, commits, PRs, reviews (10%)
  - Product scope and delivery: implements the agreed features by the deadline (20%)
  - Cross-platform readiness: runs on declared targets; graceful fallbacks if a platform is dropped with justification (10%)
  - Code quality: architecture, readability, tests where relevant, stable builds (15%)
  - UX and creativity: thoughtful flows, polish, accessibility basics (5%)
- Individual criteria (30%)
  - Demonstrated contribution and ownership: commits, issues, documentation, initiative (25%)
  - Professionalism: communication, reliability, responsiveness to feedback (5%)

Definition of Done (for each feature)
- Implemented and integrated on the declared platforms.
- Basic tests or manual test notes, and a short demo scenario.
- No critical bugs or crashes; acceptable performance.
- Documented in README or a SHORT_SCOPE.md (feature description, how to use, known limitations).

Submission and demo
- Prepare a demo focused on user value, not only code.
- Include a short written scope summary and platform matrix in the README.
- Provide instructions to run the app and server; ensure the final commit builds.

Policies
- Use of AI is allowed but must be clearly disclosed in the repo (WHAT was generated, WHERE it’s used, and any manual edits).
- Respect academic integrity. Cite assets and sources. Avoid sensitive data; use mock data.
- Late or missed milestones must be discussed in advance; scope may be reduced rather than slipping the deadline.

## A few notes

Usage of AI is authorized but should be fully disclosed.

To limit the risk of supply chain attack, code dependencies (libraries) are limited to official Google/jetBrains ones.

# Technical aspect

## Project architecture

This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM), Server.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

## Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

## Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

## Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

## Build and Run Web Application

The Ktor server is configured to host an existing wasm distribution of the Compose app.
Before running the server, run this:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:wasmJsBrowserDistribution
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:wasmJsBrowserDistribution
  ```

## Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).