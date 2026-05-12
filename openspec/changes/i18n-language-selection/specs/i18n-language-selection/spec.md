# i18n-language-selection Specification

## Purpose

Define per-app language selection, runtime locale application, and resource-backed UI strings for the app.

## Requirements

### Requirement: Language Selection UI

The system MUST provide a Settings UI control that lets the user select a language from a predefined list and a system-default option.

#### Scenario: User selects a specific language
- GIVEN the user is on the Settings screen
- WHEN the user selects a specific language from the list
- THEN the selection is stored as the app’s language preference

#### Scenario: User selects system default
- GIVEN the user previously selected a specific language
- WHEN the user selects the system-default option
- THEN the app’s language preference is cleared to use the system locale

### Requirement: Language Preference Persistence

The system MUST persist the app’s language preference across app restarts.

#### Scenario: Preference persists after restart
- GIVEN the user has selected a specific language
- WHEN the app is closed and reopened
- THEN the app uses the previously selected language

#### Scenario: No preference falls back to system
- GIVEN no app language preference is stored
- WHEN the app starts
- THEN the app uses the system locale

### Requirement: Runtime Locale Application

The system MUST apply the app’s language preference at runtime so UI strings are localized accordingly.

#### Scenario: Locale applied on launch
- GIVEN a language preference is stored
- WHEN the app starts
- THEN localized UI strings render in the selected language

#### Scenario: Locale changes during use
- GIVEN the app is running and the user changes the language
- WHEN the new preference is saved
- THEN the UI updates to the newly selected language

### Requirement: Resource-backed UI Strings

The system MUST use Android string resources for all user-facing text on main screens, with localized resource files for the initial language set.

#### Scenario: All visible strings use resources
- GIVEN the user navigates primary screens (including Settings)
- WHEN text is rendered
- THEN each user-facing string is sourced from Android string resources

#### Scenario: Missing translation fallback
- GIVEN a localized resource is missing for a string
- WHEN the string is rendered in a non-default language
- THEN the system falls back to the default language resource
