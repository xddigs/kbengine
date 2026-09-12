![kbengine_logo](src/main/resources/assets/ui/logo.png)

**kbengine** is a 3D isometric voxel engine/game built in Java with LWJGL/OpenGL.  
Currently, the project focuses on sandbox gameplay featuring world simulation, combat, inventory/crafting, and modern isometric rendering (shadows, weather, particles, and post-processing), with file-configurable controls.

## Current Technical Stack

- **Language & Build:** Java **21** + Maven.
- **Rendering & Runtime:** LWJGL **3.3.4** (GLFW, OpenGL, STB, OpenAL) + JOML.
- **Audio & Utilities:** OpenAL, SLF4J/Logback, Gson, OSHI.
- **Unified Input:** Keyboard, mouse, and gamepad via `config.json`.
- **Core Architecture:** World services (`World`, `ChunkManager`, `FluidSimulation`, `TimeService`, `WeatherService`, `ViewService`), entities (`Player`, NPCs, enemies, animals), UI, and graphics pipeline (`GameRenderer`, shadows, and post-FX).

## Current Project Roadmap

The development focus is clearly set on:
1. **Consolidating the sandbox base** (chunk-based procedural generation, day/night cycle, seasons, rain, farming, and NPC economy).
2. **Polishing core gameplay systems** (combat/interaction, inventories, crafting, navigation, and visual feedback).
3. **Enhancing rendering quality** (fog-of-war, interior/underground visibility, shadows, and frame stability).
4. **Improving overall robustness** (fewer runtime errors, better code maintainability, and broader control configurability).

## Controls (Default & Expanded)

> Keybindings can be remapped in `src/main/resources/config.json`.

| Action | Keyboard | Mouse | Gamepad | Notes |
|---|---|---|---|---|
| Movement | `W A S D` | — | `D-Pad` or `Left Stick` | Camera-relative movement |
| Jump | `Space` | — | `A` | On land |
| Swim up | `Space` | — | `A` | In water |
| Swim down | `Left Ctrl` | — | `RB` | In water |
| Sneak / Crouch | `Left Ctrl` | — | `RB` | Also used to descend in water |
| Attack / Mine block | — | `Left Click` | `X` | Primary action |
| Interact / Place block | — | `Right Click` | `B` | Secondary action |
| Pathfinding to tile | — | `Right Click` (hold) | — | Click on terrain to set target destination |
| Lock-on target NPC | — | `Mouse Button 4` | — | Toggles target lock on the nearest NPC |
| Toggle zoom | `C` | — | — | Zooms camera in/out |
| Fine zoom adjustment | `Alt` + wheel | Scroll Wheel | — | Adjusts zoom offset |
| Rotate camera (keys) | `Arrow keys` | — | — | Continuous rotation |
| Rotate camera (drag) | `Alt` + `Right Click` + move | RMB Drag | — | Rotation gesture |
| Camera pan | — | `Middle Click` + drag | — | Temporary tactical pan |
| Open/close chat console | `Enter` | — | `Start` | Executes command on close |
| Open/close inventory | `E` | — | `Y` | Closes backpack if open |
| UI / Inventory selection | `Ctrl` (modifier) | `Left Click` | — | `Ctrl + Click` quick-moves stacks |
| UI context menu | — | `Right Click` | — | Splits/uses stacks depending on panel |
| Open/close crafting book | `Tab` | — | `Back` | Requires crafting book in inventory |
| Previous book page | `Left Arrow` | — | `LB` | Book navigation |
| Next book page | `Right Arrow` | — | `RB` | Book navigation |
| Drop item | `Q` | — | — | `Ctrl + Q` drops full stack |
| Toggle shield | `F` | — | — | If shield is equipped |
| Smart Shift (modifier) | `Left Shift` | — | — | Contextual shortcuts (e.g., smart woodcutting) |
| Toggle HUD | `F1` | — | — | Shows/hides HUD |
| Toggle debug | `F3` | — | — | Debug information overlay |
| Switch language | `F5` | — | — | Cycles through active languages |
| Show current language | `F6` | — | — | Informational toast |
| Toggle fullscreen | `F11` | — | — | Fullscreen mode |
| Mute/Unmute audio | `M` | — | — | Toggles music/ambient sounds |
| Force quit | `Shift + Escape` | — | — | Immediate game shutdown |

## How to Run

### Prerequisites
- JDK **21**
- Maven **3.9+**

### From IDE
Run the main class:
- `org.kbeng.Game`

### From Terminal
```bash
mvn clean package
java -jar target/kbengine-1.0-SNAPSHOT.jar