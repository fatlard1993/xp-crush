# XP Crush

A Minecraft Fabric mod. Crush a dropped item under a piston, an anvil or a fall of sand and get experience back, worth what the item took to make. And a few more ways for experience to move: mobs that die on their own pay a share, hoppers fill bottles, lecterns bind experience into books.

## What This Mod Does

An item on the floor is worth what went into it, and vanilla has no way to get any of that back. It burns, it despawns, it falls in lava, and the diamonds it was made of are gone. Here, an item that gets **crushed** turns into experience orbs instead, and the amount is worked out from what it took to make.

Three things crush:

- **A piston.** An item driven into a block by a moving piston block, the way a mob would be squashed.
- **An anvil.** Or a falling stalactite: anything that lands hard enough to hurt a mob. It has to have fallen more than one block, as it does to hurt.
- **A falling column.** Sand, gravel, concrete powder: a plain falling block landing on an item as part of a column of at least two. One block on its own just pops the item out of the hole it filled, so one block on its own does nothing here either.

The item is destroyed, breaks with its own particles and sound, and pays out where it lay.

**Harder pays more.** An anvil dropped from higher, a taller column of sand, a longer line of blocks behind a piston head: each block of fall beyond the second and each extra block in the stack or the chain adds to what the item pays, up to a ceiling of four times. An anvil from ten up pays nearly double; a piston shoving a full line of twelve pays the ceiling. Pistons closing on one item from several sides add their lines together, and two heads meeting from opposite sides count for more again: being caught between two things driving is not the same as being driven into a wall. A ring of four heads with a block each, pressing on both axes, reaches the ceiling.

## What An Item Is Worth

Worked out from the recipe tree rather than written down per item, because the tree already knows. A crafted thing is worth its ingredients plus a little for the crafting, divided by how many the recipe makes. Where several recipes make it, the cheapest one sets the price, since that is the one a player would have used. Smelting, stonecutting, smithing and brewing all count as recipes.

The bottom of the tree is a price list of raw materials in the config: a diamond, an ender pearl, a nether star. Anything raw that is not on the list is worth a lump of cobblestone, which is very little.

On top of the item's own price:

- **Rarity** multiplies it. The item's own rarity, before an enchantment bumps it.
- **Every enchantment level** adds a flat amount, on tools and on books alike.
- **A worn tool** pays for what is left of it. A pickaxe on its last swing was mostly spent already.
- **A shulker box or a bundle** pays for what is inside, crushed along with it.

So a stack of cobblestone pays about three points, a diamond pickaxe about six, and a netherite sword with a full set of enchantments pays like the evening it cost.

## Experience From Nobody's Kill

Vanilla pays experience only for a death a player had a recent hand in. The fall, the lava, the iron golem and the trapdoor all pay nothing. Here they pay a share, three quarters by default, on the same terms vanilla applies otherwise: the mob has to be one that drops, and the game rule has to be on. Deaths vanilla already pays for are left to vanilla, so nothing is paid twice.

## Bottling

A hopper with an empty glass bottle in it, or in the container it feeds, takes in the experience orbs that land in it. It holds what it has taken as a number, and every time that reaches a bottle's worth, seven points by default, an empty bottle is filled and put back where the empty came from.

A hopper with no empty bottle anywhere leaves the orbs alone: a hopper that swallowed experience it could not keep would be a hole in the floor. A locked hopper leaves them alone too.

The held number is saved with the hopper, so a farm does not lose a part-filled bottle to a restart.

## Tomes

Write `/xp 500` on the first line of a book and quill and set it on a lectern. If you have the five hundred points and the binding's share on top, the lectern takes them and hands you a **Tome of Experience**, an enchanted book to look at, holding five hundred. Use the tome and they come back out, for you or whoever you gave it to. The share spent on the binding is gone: it is what keeps a tome from being a free bank.

`/xp all` binds as much as you have once the binding has taken its share, which is the shortcut for emptying yourself into a book before something dangerous.

**The share climbs with the amount,** the way the ladder does: five percent plus five more per thousand points, capped at half. Five hundred points pay a 7.5% binding, two thousand pay 15%, a hoard of nine thousand pays the cap. Binding everything into one book is not the cheap way to carry it.

**Small numbers are turned away.** Anything under a hundred points, by default, leaves the book a book with a line saying that binding counts points and not levels, and what a hundred is worth from nothing. Somebody who writes `/xp 4` is nearly always thinking in levels, and a tome of four points would teach them nothing except that it did not work.

**Points, never levels.** The ladder gets steeper as it climbs, so a level is worth forty points near the bottom and over a hundred near the top. A tome denominated in levels could be bound cheap and cashed dear, and the ladder's whole point is that the high rungs are the expensive ones. Five hundred points is five hundred points wherever it is spent.

If you do not have enough, the book stays a book and gains a line spelling out the sum and saying how many you could have bound, and the lectern takes it like any other book. Fix the number and try again; only the first line is read, and a second try replaces the note rather than stacking one. If the spelling is wrong, nothing happens at all.

## Details Worth Knowing

- **Only crushing pays.** Items that burn, blow up or land on a cactus are destroyed by vanilla as they always were, and pay nothing.
- **Fractions are paid by chance** rather than dropped, so sixty-four things worth a twentieth each still average about three points.
- **Merged orbs pay in full.** A hopper taking an orb that is several orbs riding together gets all of them.
- **The tree is read** once when the server starts and again after `/reload`, since that is when recipes change.
- **A price on the list is final.** It is used even where a recipe could make the item for less, which is how a gold ingot stays worth a gold ingot when nine nuggets make one.
- **A recipe loop costs nothing extra.** An item on one is priced by the cheapest way into it, so nuggets from an ingot from nuggets bottom out on whichever side has a price of its own. Anything made only from things made from each other, with no raw material under it anywhere, is worth the raw price.
- **Binding empties and refills.** Your experience is set to zero and what is left after the binding is given back through the game's own rule, so the level you land on is the one those points buy and not an approximation of it.

## Configuration

`config/xp-crush.properties`, written out in full on first run with the price list in it.

| Key | Default | |
|---|---|---|
| `pistons` | true | Pistons crush |
| `falling_blocks` | true | Anvils and stalactites crush |
| `falling_columns` | true | Columns of plain falling blocks crush |
| `column_min` | 2 | How many falling blocks make a column |
| `xp_per_block_fallen` | 0.1 | Added to the multiplier per block of fall beyond two |
| `xp_per_block_stacked` | 0.25 | Added per extra block in a column or behind a piston head |
| `xp_per_squeeze` | 1.0 | Added per axis pressed from both sides at once |
| `force_cap` | 4.0 | The most the multiplier can reach |
| `unattended_kills` | true | Mobs that die without a player pay a share |
| `unattended_kill_fraction` | 0.75 | That share |
| `hopper_bottles` | true | Hoppers fill empty bottles from orbs |
| `xp_per_bottle` | 7 | Points to fill one bottle |
| `tomes` | true | Lecterns bind experience into tomes |
| `tome_binding_share` | 0.05 | Base share of the bound points spent on the binding, on top |
| `tome_binding_share_per_thousand` | 0.05 | Added to the share per thousand points bound |
| `tome_binding_share_max` | 0.5 | The most the share can reach |
| `tome_minimum_points` | 100 | Below this the book is turned away with a note |
| `xp_per_raw_item` | 0.05 | A raw material not on the price list |
| `xp_per_craft_step` | 0.05 | Added per recipe on the way to an item |
| `xp_per_enchantment_level` | 1.0 | Added per level of every enchantment |
| `rarity_uncommon` / `rarity_rare` / `rarity_epic` | 2 / 4 / 8 | Multipliers by rarity |
| `worth.<item id>` | see file | A raw material's price, in experience points each |

## Pandorical

Not needed. Everything here happens on the server and shows up on a vanilla client as items vanishing and orbs appearing, bottles filling, and a book that glows.

With Pandorical on the server, ops get XP Crush's page in its mods menu: the switches for pistons, anvils and stalactites, falling columns, unattended kills, hopper bottles and tomes, and the column size, the unattended share (as a percent) and the experience a bottle takes. Prices and scaling stay in the file.

## Development

Installing and the map of the source are in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
