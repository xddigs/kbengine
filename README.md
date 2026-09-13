![kbengine_logo](src/main/resources/assets/ui/logo.png)

**kbengine** is a 3D voxel engine/game with switchable orthographic and first-person cameras, built in Java with LWJGL/OpenGL.

## Current Technical Stack

- **Language & Build:** Java **21** + Maven.
- **Rendering & Runtime:** LWJGL **3.3.4** (GLFW, OpenGL, STB, OpenAL) + JOML.
- **Audio & Utilities:** OpenAL, SLF4J/Logback, Gson, OSHI.
- **Unified Input:** Keyboard, mouse, and gamepad via `config.json`.
- **Camera System:** Detached orthographic exploration and an engine-level perspective first-person camera.

## Camera Modes

The game starts in its normal orthographic view. Press `V` to switch to the first-person camera and press it again to return. The first-person view initially adopts the orthographic heading; returning copies the current first-person heading back while preserving the orthographic zoom and pitch.

First-person mode captures the mouse for unrestricted relative look, attaches the view to the player's interpolated eye height, and aims block/entity interactions through the center crosshair. Opening chat, inventory, backpack, or the crafting book temporarily releases the mouse; closing the interface captures it again.

Fog of war is intentionally disabled in first person. The local player model, its animated render, and its shadow-map render are also omitted while that camera is active, preventing the eye camera from seeing the avatar from inside. These behaviors revert immediately when switching back to the orthographic camera.

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
| Switch camera mode | `V` | — | — | Toggles orthographic / first-person view |
| Toggle zoom | `C` | — | — | Zooms camera in/out |
| Fine zoom adjustment | `Alt` + wheel | Scroll Wheel | — | Adjusts zoom offset |
| Rotate camera (keys) | `Arrow keys` | — | — | Continuous orthographic rotation |
| Rotate camera (drag) | `Alt` + `Right Click` + move | RMB Drag | — | Orthographic rotation gesture |
| Camera pan | — | `Middle Click` + drag | — | Temporary orthographic tactical pan |
| First-person look | — | Mouse movement | — | Active while the first-person cursor is captured |
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
- `org.kbeng.Main`

### From Terminal
```bash
mvn clean package
java -jar target/kbengine-1.0-SNAPSHOT.jar
```
