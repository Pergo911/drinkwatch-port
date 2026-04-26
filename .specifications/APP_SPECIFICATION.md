# Overview

This file is a high-level requirement specification of the app.

# Concepts

## Sessions

The app defines "Sessions". A session represents a single "occasion" or a "party". It contains metadata, the objects and events for given occasion. The app cannot be used without entering a session first. If no session is stored in persistent storage, it can be imported from a JSON file, or an empty one can be created.

Metadata: The only metadata for now is the name of the session.

Objects: "Players", drinks, glasses, and their properties. More on them later.

Events: Events record what happens throughout the session.

## Players

A participant of a party is referred to as a "Player" in the app.

Properties: name, phone number, note

## Drinks

Drinks available for serving.

Properties: name, type (shot, long drink, non-alcoholic)

## Glasses

The glass a drink is served in, has to be "returned" in order for the player who took it to be served again.

Properties: group (A-Z), glass number

## Events

A session's event history that gets populated as the night progresses. All events are timestamped.

Event types:

| Type               | Function                                                                                    |
| ------------------ | ------------------------------------------------------------------------------------------- |
| "order"            | Serves a single player a single drink. Optionally records the glass the drink is served in. |
| "return"           | Returns a glass recorded in a previous order.                                               |
| "timeout"          | Marks the start and duration of a timeout for a player.                                     |
| "disable" (player) | Marks the time a player leaves the party. Greyes out the player in the UI.                  |
| "disable" (drink)  | Marks the time the inventory runs out of a drink. Greyes out the drink from the picker UI.  |

## Timeouts

Any player can be sent on TO. A player cannot order until their timeout expires. A player's timeout can be cancelled by recording a timeout event with a duration of 0.

## Active drinks

Any time a player orders an alcoholic beverage the player's active drink counter is incremented. A timeout of any duration resets the active drink counter.

## Queue

Allows the user to record orders in batches. When adding an order, the default behaviour is to add them to a "queue" first. The batch send can be triggered with a floating action button visible when there is an active queue.

# App Structure

## Main screen

Default screen shown on app launch, bottom of the stack. Tabbed layout with a top bar. Orders, timeouts and glasses are managed from here. Tabs are managed through a bottom navigation bar. Automatically opens the "Session" screen if no session is loaded.

### App bar

Top bar with a title (the session name) and hamburger menu. Hamburger menu items: "Session", "Settings", "About".

### Tab 1: Order

Default tab. Displays the list of players.

The player card displays a players name and active drink count if not under a timeout, else the time left. Two actions buttons are available: "Add Drink", "Timeout". Tapping the card anywhere else opens the player's detail screen.

"Add drink": Opens "Order" full-screen dialog.
"Timeout": Opens simple duration picker dialog.

### Tab 2: Glasses

Displays the list of glasses currently taken.

Card displays glass group and number (e.g. "B10"), the drink and the player. Tapping the card returns the glass after a confirmation dialog.

## Session

The session manager screen. Tabbed layout (secondary tabs).

### Tab 1: Overview

If no session is loaded: An empty screen with two options in the middle: "Import From File", "Create New".

Otherwise: Allows editing the session name, exporting and importing to file, and starting a new session.

### Tab 2 - 4: Players, Drinks, Glasses

These tabs are unavailable if no session is loaded.

Allows adding, deleting and modifying the details of items. Each of these functions are achieved through full-screen dialogs.

Glasses work slightly differently: glass groups are toggled A-Z. If a group is toggled on, all glass numbers (any integer) will be available.

## Full-screen dialog: Order

Two-step process:

- Step 1: Select a drink. The list is grouped by drink type. Primary action says "Next".
- Step 2: Select a glass. Pick a group, enter a number. This step is optional, and the user can unselect the currently selected glass. Primary action says "Confirm".

The confirm button is split, the default option queues the order, the secondary option records the order immediately.

## Player details screen

- Name, phone number, notes
- Active drinks, current timeout, number of currently unreturned glasses
- Total drinks, total time spent on timeout
- (Expandable table, collapsed by default) list of drinks
- (Expandable table, collapsed by default) list of timeouts

## Settings

- Dark theme / light theme / system theme
- Active drink highlight (int): The active drink counter will be highlighted red for players with active drinks greater or equal to this number.
- Default timeout duration

## About

- App version
