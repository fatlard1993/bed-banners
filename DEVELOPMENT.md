# Bed Banners - Development Guide

For what the mod is and how it plays, see [README.md](README.md).

## Installation

Install server-side, with Pandorical; connecting clients need only Pandorical. Seeing a finished
bed properly needs Pandorical; without it a client sees a banner lying flat on the mattress instead
of the pattern on the blanket. Making one needs Pandorical on the client too, because a vanilla loom
screen crashes on a bed in its banner slot the moment a pattern is picked, and the guard for that
lives in Pandorical's client half. Version targets live in `gradle.properties` (Minecraft,
loader, Fabric API) and `fabric.mod.json` (Java).

## Key Files

| File | Responsibility |
|------|---------------|
| `Main.java` | Entry point; taking the blanket off a bed being broken, showing blankets to players as they arrive, and re-posing blankets as they load |
| `BedBanners.java` | What counts as a beddable bed, and which banner stands in for it |
| `BedBlanket.java` | The pattern, lying on the blanket |
| `mixin/LoomBannerSlotMixin.java` | Letting a bed into the loom's banner slot |
| `mixin/BlockPlacedMixin.java` | Laying the blanket when a patterned bed is placed |
