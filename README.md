This mod expands vanilla keybinds with combo support, letting you bind any action to a chord or a key sequence instead of a single key.

It is **client-side only** and requires no server-side installation.

## Features

### Chord Combos
- Bind actions to key combinations such as **Shift + E**, **Ctrl + Left Button**, or any other modifier + trigger pair
- Any key or mouse button can serve as either a modifier or the trigger. The last key pressed during recording becomes the trigger, and all previously held keys become modifiers
- No limits! Stack as many modifier keys as you want for as wild a combination as you need

### Sequence Combos
- Bind actions to repeated key presses such as **double-tap Q** or **triple-tap Space**
- A configurable time window determines how quickly the taps must occur
- No limits! Bind to as many taps as you want

### Controls Screen Integration
- Combo recording is embedded directly in the vanilla **Controls > Key Binds** screen, meaning no separate UI
- Press **Escape** to unbind a key

### Configuration
- **Allow Conflicts**: When disabled, pressing a combo will not also trigger bindings whose keys are fully contained in it. For example, binding both **E** and **Shift + E** means pressing **Shift + E** would normally fire both. Disabling this ensures only **Shift + E** fires. Two bindings like **Ctrl + E** and **Shift + E** are not considered conflicting, since pressing one can never accidentally fire the other. They only both trigger if you deliberately hold **Ctrl + Shift + E**. This setting only applies to bindings that share the same trigger key (default: on)
- **Sequence Window**: How quickly you must press the key repeatedly in-game for a sequence to register (default: 400 ms)
- **Sequence Recording Window**: How long the recorder waits between taps when capturing a sequence in the Controls screen (default: 200 ms)
- Settings are saved to `config/combind.json`

## Use Cases
This mod shines in modpacks where keys are scarce and every binding needs to be deliberate, but works just as well in vanilla for extra flexibility.

### A Practical Example
**Double-tap Q (Q Q)** to drop items instead of a single press. No more accidental drops. 

## Compatibility

This mod has no required dependencies beyond Fabric API. It works with:

- **[ModMenu](https://modrinth.com/mod/modmenu)** _(recommended)_ — adds a config button to the mod list as an alternative to manually editing `config/combind.json`
- **[Controlling](https://modrinth.com/mod/controlling)** and **[Searchables](https://modrinth.com/mod/searchables)** — combo names display correctly in the searchable Controls screen provided by Controlling


---


_**Note**: Some niche vanilla interactions are tied to a specific input type and may not work when rebound. For example, a mouse button bound to "Open Inventory" will not close it the way a key would, and a key bound to "Pick Block" will not support block duplication dragging inside containers the way a mouse button would._

_**Note**: If you encounter any compatibility issues, please report them on the [GitHub Issues page](https://github.com/Pemiridosa/combind/issues)._
