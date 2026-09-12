![kbengine_logo](src/main/resources/assets/ui/logo.png)

**kbengine** es un motor/juego isométrico 3D voxel en Java con LWJGL/OpenGL.  
Actualmente el proyecto está enfocado en gameplay sandbox con simulación del mundo, combate, inventario/crafteo y render isométrico moderno (sombras, clima, partículas y postproceso), con controles configurables por archivo.

## Base técnica actual

- **Lenguaje y build:** Java **21** + Maven.
- **Render y runtime:** LWJGL **3.3.4** (GLFW, OpenGL, STB, OpenAL) + JOML.
- **Audio y utilidades:** OpenAL, SLF4J/Logback, Gson, OSHI.
- **Entrada unificada:** teclado, ratón y gamepad vía `config.json`.
- **Arquitectura principal:** servicios de mundo (`World`, `ChunkManager`, `FluidSimulation`, `TimeService`, `WeatherService`, `ViewService`), entidades (`Player`, NPC, enemigos, animales), UI y pipeline gráfico (`GameRenderer`, sombras y post-FX).

## Dirección actual del proyecto

La dirección del proyecto está claramente orientada a:

1. **Consolidar la base sandbox** (mundo procedural por chunks, ciclo día/noche, estaciones, lluvia, cultivo y economía NPC).
2. **Pulir sistemas core de jugabilidad** (combate/interacción, inventarios, crafteo, navegación y feedback visual).
3. **Fortalecer calidad técnica del render** (fog-of-war, visibilidad interior/subterránea, sombras y estabilidad de frame).
4. **Mejorar robustez general** (menos errores en runtime, mejor mantenimiento del código y mayor configurabilidad de controles).

## Controles (default, expandido)

> Los bindings se pueden cambiar en `src/main/resources/config.json`.

| Acción | Teclado | Ratón | Gamepad | Notas |
|---|---|---|---|---|
| Moverse | `W A S D` | — | `D-Pad` o `Stick Izquierdo` | Movimiento relativo a cámara |
| Saltar | `Space` | — | `A` | En tierra |
| Nadar arriba | `Space` | — | `A` | En agua |
| Nadar abajo | `Left Ctrl` | — | `RB` | En agua |
| Agacharse (sneak) | `Left Ctrl` | — | `RB` | También se usa para descenso en agua |
| Ataque / picar bloque | — | `Click Izquierdo` | `X` | Acción primaria |
| Interactuar / colocar | — | `Click Derecho` | `B` | Acción secundaria |
| Pathfinding hacia celda | — | `Click Derecho` (mantener) | — | Click sobre terreno para fijar destino |
| Foco a NPC objetivo | — | `Mouse Button 4` | — | Alterna lock del NPC más cercano |
| Zoom (toggle) | `C` | — | — | Acerca/aleja cámara |
| Ajuste fino de zoom | `Alt` + rueda | Rueda | — | Cambia offset de zoom |
| Rotar cámara (teclas) | `Flechas` | — | — | Rotación continua |
| Rotar cámara (drag) | `Alt` + `Click Derecho` + mover | Drag RMB | — | Gesto de rotación |
| Pan de cámara | — | `Click Medio` + arrastrar | — | Pan táctico temporal |
| Abrir/cerrar chat consola | `Enter` | — | `Start` | Ejecuta comando al cerrar |
| Abrir/cerrar inventario | `E` | — | `Y` | Si mochila abierta, la cierra |
| Selección UI / inventario | `Ctrl` (modificador) | `Click Izquierdo` | — | `Ctrl + click` hace quick-move de stacks |
| Menú contextual UI | — | `Click Derecho` | — | Divide/usa stacks según panel |
| Abrir/cerrar libro crafteo | `Tab` | — | `Back` | Requiere libro de crafteo |
| Página anterior del libro | `Left Arrow` | — | `LB` | Navegación de libros |
| Página siguiente del libro | `Right Arrow` | — | `RB` | Navegación de libros |
| Soltar item | `Q` | — | — | `Ctrl + Q` suelta stack |
| Toggle escudo | `F` | — | — | Si hay escudo equipado |
| Smart Shift (modificador) | `Left Shift` | — | — | Atajos contextuales (ej. tala inteligente) |
| Toggle HUD | `F1` | — | — | Muestra/oculta HUD |
| Toggle debug | `F3` | — | — | Info de depuración |
| Cambiar idioma | `F5` | — | — | Cicla idioma activo |
| Mostrar idioma actual | `F6` | — | — | Toast informativo |
| Toggle fullscreen | `F11` | — | — | Pantalla completa |
| Música on/off | `M` | — | — | Alterna música/ambiente |
| Salida forzada | `Shift + Escape` | — | — | Cierre inmediato del juego |

## Cómo ejecutar

### Requisitos
- JDK **21**
- Maven **3.9+**

### Desde IDE
Ejecuta la clase principal:
- `org.kbeng.Game`

### Desde terminal
```bash
mvn clean package
java -jar target/kbengine-1.0-SNAPSHOT.jar
```

## Estructura rápida

- `src/main/java/org/kbeng/wrld` → mundo, chunks, fluidos, game loop
- `src/main/java/org/kbeng/graphics` → render, shaders, cámaras, sombras, postprocesado
- `src/main/java/org/kbeng/entity` → player, NPCs, enemigos, animales, items en mundo
- `src/main/java/org/kbeng/service` → reglas/sistemas (tiempo, clima, vista, audio, etc.)
- `src/main/java/org/kbeng/input` → capa de input y mapping de acciones lógicas
- `src/main/resources/shaders` → shaders GLSL

## Licencia

Este repositorio incluye archivo `LICENSE`; revisa ese archivo para términos de uso y distribución.
