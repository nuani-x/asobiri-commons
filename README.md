# asobiri-commons

The core&lt;-&gt;plugin wire contract for [Asobiri](https://github.com/nuani-x/asobiri), a JoiPlay-style
modular Android game launcher. Defines `Engine` ids, `PluginContract` action/meta-data/extra
constants, `LaunchConfig`/`LaunchIntent`/`LaunchRequest` marshalling, and folder-based engine
detection. No game runtime lives here — this is the frozen wire format both the launcher and
every engine plugin (Ren'Py, mkxp, Godot, Web, ...) depend on.

Consumed as a git submodule by the main Asobiri repo and by
[asobiri-plugin-mkxp](https://github.com/nuani-x/asobiri-plugin-mkxp) (published separately so its
GPL-2.0 corresponding-source obligation doesn't force the rest of Asobiri open).

## The gamepad module

`gamepad/` is the on-screen controller (D-pad, A/B) that plugins draw over their game. It is UI
only and not part of the wire contract; it lives here so the launcher and every plugin share one
copy. A host adds it next to `:commons`:

```kotlin
include(":gamepad")
project(":gamepad").projectDir = file("commons/gamepad")
```

## Build

```
./gradlew test assemble
```

## Contract discipline

- `Engine.id` and every `PluginContract` string constant are ADD-only once shipped — never
  renamed or repurposed.
- New `LaunchConfig` fields are additive with defaults; never a new required extra.
- `PluginContract.API_VERSION` bumps only on a genuine breaking change.
