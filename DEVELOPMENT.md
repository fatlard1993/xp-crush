# XP Crush - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Source Map

| File | What is in it |
|---|---|
| `Main.java` | Loading the config, rebuilding prices on start and reload, the lectern hooks |
| `Crush.java` | Which items are caught under what, and turning them into orbs |
| `Worth.java` | The recipe tree, and what a stack is worth |
| `Bottling.java` | Hoppers taking in orbs and filling bottles |
| `Tome.java` | The lectern spell, and the tome it makes |
| `XpCrushConfig.java` | The knobs and the price list |
| `mixin/PistonCrushMixin.java` | After a moving piston block has shoved everything in its way |
| `mixin/FallingBlockCrushMixin.java` | The moment a falling block lands |
| `mixin/LivingEntityXpMixin.java` | The share paid for a death nobody caused |
| `mixin/ExperienceOrbHopperMixin.java` | Offering each orb to the hopper it landed in |
| `mixin/ExperienceOrbAccessor.java` | How many orbs a merged orb stands for |
| `mixin/HopperXpMixin.java` | The number a hopper holds between bottles, saved with it |
| `generate_icon.py` | The mod menu icon, cut from vanilla's own textures |

## Installation

Install server-side alongside its declared dependencies (see `fabric.mod.json`). Vanilla clients need nothing. Version targets live in `gradle.properties` (Minecraft, loader, Fabric API) and `fabric.mod.json` (Java).
