This mod expands vanilla keybinds with combo support, letting you bind any action to a chord or a key sequence instead of a single key.

It is **client-side only** and requires no server-side installation.

## Features

### Chord Combos
- Bind actions to key combinations such as **Shift + E**, **Ctrl + Mouse Left**, or any other modifier + trigger pair
- Any key or mouse button can serve as either a modifier or the trigger

### Sequence Combos
- Bind actions to repeated key presses such as **double-tap Q** or **triple-tap Space**
- A configurable time window determines how quickly the taps must occur

### Controls Screen Integration
- Combo recording is embedded directly in the vanilla **Controls > Key Binds** screen — no separate UI
- A live preview displays the combo being recorded as you press keys
- Press **Escape** to unbind a key

### Configuration
- **Allow Conflicts**: When enabled, multiple bindings sharing the same combo all fire simultaneously (default: on)
- **Sequence Window**: How quickly you must press the key repeatedly in-game for a sequence to register (default: 400 ms)
- **Sequence Recording Window**: How long the recorder waits between taps when capturing a sequence in the Controls screen (default: 600 ms)
- Settings are saved to `.minecraft/config/combind.json`

## Compatibility

This mod has no required dependencies beyond Fabric API. It works with:

- **[ModMenu](https://modrinth.com/mod/modmenu)** _(recommended)_ — adds a config button to the mod list
- **[Controlling](https://modrinth.com/mod/controlling)** and **[Searchables](https://modrinth.com/mod/searchables)** — combo names display correctly in the searchable Controls screen provided by Controlling


---


_**Note**: If you encounter any compatibility issues, please report them on the [GitHub Issues page](https://github.com/Pemiridosa/combind/issues)._
