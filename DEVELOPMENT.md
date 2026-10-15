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

## How it works

**Beds have no block entity on this version.** They are ordinary model-rendered blocks, so a placed
bed has nowhere to keep a pattern and no renderer of its own to draw one. The pattern is drawn over
the bed instead.

- **Storage.** The record of which bed has which pattern is an item display holding a banner of the
  bed's colour, laid on its back on the mattress and tagged `bed_banners_blanket`. Nothing else
  stores the pattern, so there is no second copy to disagree with the bed. Breaking the bed reads the
  layers back off that banner (`BedBlanket.pickUp`).
- **Drawing.** On a Pandorical client the layers are sent as a decal through `BannerDecalApi`:
  vanilla's banner pattern sprites drawn flat over the bed's own blanket texture with no base colour,
  from the pillow's edge to the foot, at the blanket's pixel size. The banner in the item display
  carries `BannerDecalApi.HIDDEN_ITEM_KEY`, so a Pandorical client leaves it undrawn and a vanilla
  client draws it in place of the decal.
- **The loom.** Vanilla's loom builds its result by copying whatever is in the banner slot and
  appending a layer to its `banner_patterns` component, and reads its pattern list off the same
  component. Both are generic already, so only the slot (`LoomMenu$3.mayPlace`) is changed. The
  loom screen's preview casts the slot's item to a banner to learn the base colour; Pandorical's
  client half (`LoomScreenBedMixin`) hands it a banner of the bed's colour instead. This mod ships
  no client code.
