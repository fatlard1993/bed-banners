# Bed Banners

A Fabric mod that lets a bed go in the loom and come out with a pattern on the blanket.

## What This Mod Does

Put a bed in the loom's banner slot, a dye beside it, a pattern if you have one, and take out a bed
wearing that pattern. Every pattern the loom offers, layered as deep as a banner can be layered.

## How It Works, And Why That Matters

**Beds have no block entity on this version.** They were converted to ordinary model-rendered
blocks, which means a placed bed has nowhere to keep anything and no renderer of its own to draw
with. A pattern cannot be part of the bed.

So it is drawn over the bed instead: an **item display holding a banner**, in its flat inventory
transform, laid on the mattress. A banner already knows how to render an arbitrary stack of pattern
layers, and an item display will render any stack you hand it. The banner wears the bed's own
colour, so what you see is the bed's colour with the pattern on it.

That entity is also the only record. Nothing else stores which bed is patterned - the thing sitting
on it is the fact, which is why there is no second copy to disagree with it.

Two consequences worth knowing:

- **The bed item in your hand stays plain.** It carries the pattern, and lays it back down when
  placed, but the item model does not show it.
- **The blanket is an entity.** `/kill @e[type=item_display]` takes it off, and the bed underneath
  is an ordinary bed.

## The Loom

The crafting half is one method, because vanilla's loom never actually cared that it was working on
a banner. Its result is built by copying whatever is in that slot and appending a layer to the
stack's `banner_patterns` component, and the pattern list it offers is read off the same component.
Both are generic already. Only the slot said no, so only the slot was changed.

Dyed beds only. A straw bed has no colour for a banner to stand in for.

## Installation

Install server-side. Vanilla clients need nothing at all: the blanket is a vanilla entity holding a
vanilla banner, so everybody sees it. Version targets live in `gradle.properties` (Minecraft,
loader, Fabric API) and `fabric.mod.json` (Java).

## Key Files

| File | Responsibility |
|------|---------------|
| `Main.java` | Entry point; taking the blanket off a bed being broken |
| `BedBanners.java` | What counts as a beddable bed, and which banner stands in for it |
| `BedBlanket.java` | The pattern, lying on the blanket |
| `mixin/LoomBannerSlotMixin.java` | Letting a bed into the loom's banner slot |
| `mixin/BlockPlacedMixin.java` | Laying the blanket when a patterned bed is placed |

## License

MIT, see [LICENSE](LICENSE).
