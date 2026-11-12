import json, os, sys
ROOT = sys.argv[1]
OUT = os.path.join(ROOT, "data/aeroruins/loot_table/chests")
os.makedirs(OUT, exist_ok=True)
for f in os.listdir(OUT): os.remove(os.path.join(OUT, f))

def cnt(a, b=None):
    b = a if b is None else b
    return {"function": "minecraft:set_count", "count": ({"type": "minecraft:uniform", "min": a, "max": b} if a != b else float(a))}

def ench(d, add=False):
    return {"function": "minecraft:set_enchantments", "enchantments": {"minecraft:" + k: v for k, v in d.items()}, "add": add}

def item(name, w, count=None, e=None):
    fn = []
    if count: fn.append(cnt(*count) if isinstance(count, tuple) else cnt(count))
    if e: fn.append(ench(e))
    ent = {"type": "minecraft:item", "name": "minecraft:" + name, "weight": w}
    if fn: ent["functions"] = fn
    return ent

def book(w, e):
                                                                                                
    return item("enchanted_book", w, None, e)

def pool(rolls, entries, bonus=None):
    p = {"rolls": ({"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]} if isinstance(rolls, tuple) else float(rolls)), "entries": entries}
    return p

def table(*pools):
    return {"type": "minecraft:chest", "pools": list(pools)}

                                                            
BOOKS_TOP = [
    book(10, {"mending": 1}), book(8, {"unbreaking": 3}), book(8, {"efficiency": 5}), book(6, {"fortune": 3}),
    book(6, {"silk_touch": 1}), book(8, {"sharpness": 5}), book(8, {"protection": 4}), book(6, {"looting": 3}),
    book(6, {"feather_falling": 4}), book(5, {"thorns": 3}), book(4, {"swift_sneak": 3}), book(5, {"depth_strider": 3}),
    book(4, {"frost_walker": 2}), book(5, {"respiration": 3}), book(5, {"aqua_affinity": 1}), book(5, {"power": 5}),
    book(4, {"infinity": 1}), book(4, {"riptide": 3}), book(4, {"loyalty": 3}), book(3, {"channeling": 1}),
    book(4, {"quick_charge": 3}), book(4, {"piercing": 4}), book(4, {"density": 5}), book(4, {"breach": 4}),
    book(3, {"wind_burst": 3}), book(5, {"fire_protection": 4}), book(5, {"blast_protection": 4}), book(5, {"projectile_protection": 4}),
    book(5, {"sweeping_edge": 3}), book(4, {"soul_speed": 3}), book(4, {"multishot": 1}), book(5, {"smite": 5}), book(5, {"bane_of_arthropods": 5}),
]

def gear_sword(w):  return item("diamond_sword", w, None, {"sharpness": 4, "unbreaking": 3, "looting": 3, "sweeping_edge": 3})
def gear_pick(w):   return item("diamond_pickaxe", w, None, {"efficiency": 5, "unbreaking": 3, "fortune": 3})
def gear_axe(w):    return item("diamond_axe", w, None, {"efficiency": 5, "unbreaking": 3, "sharpness": 4})
def gear_shovel(w): return item("diamond_shovel", w, None, {"efficiency": 5, "unbreaking": 3, "silk_touch": 1})
def gear_helm(w):   return item("diamond_helmet", w, None, {"protection": 4, "unbreaking": 3, "respiration": 3, "aqua_affinity": 1})
def gear_chest(w):  return item("diamond_chestplate", w, None, {"protection": 4, "unbreaking": 3, "mending": 1})
def gear_legs(w):   return item("diamond_leggings", w, None, {"protection": 4, "unbreaking": 3, "swift_sneak": 2})
def gear_boots(w):  return item("diamond_boots", w, None, {"protection": 4, "unbreaking": 3, "feather_falling": 4, "depth_strider": 2})
def gear_bow(w):    return item("bow", w, None, {"power": 5, "unbreaking": 3, "infinity": 1})
def gear_xbow(w):   return item("crossbow", w, None, {"quick_charge": 3, "unbreaking": 3, "piercing": 3})
def gear_trident(w):return item("trident", w, None, {"loyalty": 3, "unbreaking": 3, "impaling": 5})
def gear_elytra(w): return item("elytra", w, None, {"unbreaking": 3, "mending": 1})
def gear_shield(w): return item("shield", w, None, {"unbreaking": 3, "mending": 1})
def gear_rod(w):    return item("fishing_rod", w, None, {"luck_of_the_sea": 3, "lure": 3, "unbreaking": 3})
def gear_mace(w):   return item("mace", w, None, {"density": 4, "unbreaking": 3, "wind_burst": 2})
def gear_hoe(w):    return item("netherite_hoe", w, None, {"efficiency": 5, "unbreaking": 3, "fortune": 3})
def gear_nsword(w): return item("netherite_sword", w, None, {"sharpness": 5, "unbreaking": 3, "looting": 3, "fire_aspect": 2})

GEAR_ALL = [gear_sword(8), gear_pick(8), gear_axe(6), gear_shovel(5), gear_helm(6), gear_chest(6), gear_legs(6), gear_boots(6),
            gear_bow(6), gear_xbow(4), gear_trident(3), gear_shield(5), gear_rod(3), gear_elytra(2), gear_mace(2), gear_nsword(1)]

RARE = [
    item("totem_of_undying", 6), item("enchanted_golden_apple", 5, (1, 2)), item("netherite_ingot", 3, (1, 2)),
    item("netherite_scrap", 6, (2, 4)), item("diamond_block", 3), item("emerald_block", 4, (1, 2)),
    item("ender_chest", 2), item("heart_of_the_sea", 2), item("nautilus_shell", 3), item("experience_bottle", 10, (12, 32)),
    item("name_tag", 5), item("saddle", 4), item("diamond_horse_armor", 3), item("beacon", 1), item("shulker_shell", 2, (1, 2)),
    item("golden_carrot", 6, (16, 32)), item("diamond", 10, (4, 9)), item("ancient_debris", 4, (2, 4)),
]

def treasure(extra, rolls=(2, 3)):
    return pool(rolls, BOOKS_TOP + GEAR_ALL + RARE + extra)

def mk(name, supplies, flavor, treasure_rolls=(2, 3), supply_rolls=(2, 4), extra_treasure=None):
    pools = [treasure(flavor + (extra_treasure or []), treasure_rolls),
             pool(supply_rolls, supplies)]
    with open(os.path.join(OUT, name + ".json"), "w") as fh:
        json.dump(table(*pools), fh, indent=2)

                        
FOOD = [item("golden_carrot", 8, (6, 16)), item("cooked_beef", 10, (8, 16)), item("bread", 8, (6, 12)), item("honey_bottle", 4, (2, 4)), item("glow_berries", 6, (8, 16))]
ORE = [item("iron_ingot", 12, (6, 14)), item("gold_ingot", 10, (4, 10)), item("diamond", 6, (2, 5)), item("lapis_lazuli", 10, (8, 20)), item("redstone", 10, (8, 20)), item("emerald", 8, (3, 8)), item("copper_ingot", 10, (8, 20))]
MISC = [item("ender_pearl", 6, (2, 5)), item("arrow", 8, (16, 40)), item("spectral_arrow", 3, (8, 16)), item("obsidian", 5, (4, 10)), item("slime_ball", 4, (2, 6)), item("experience_bottle", 6, (6, 16))]

mk("tower",     ORE + FOOD + MISC + [item("spyglass", 4), item("amethyst_shard", 8, (8, 20)), item("quartz_block", 8, (8, 24))],
   [item("recovery_compass", 4), item("lodestone", 3), item("clock", 3), item("echo_shard", 3, (1, 3)), item("bundle", 4), item("brewing_stand", 3)])
mk("apartment", ORE + FOOD + MISC + [item("white_bed", 4), item("cake", 4), item("book", 6, (4, 10)), item("bookshelf", 4, (4, 8))],
   [item("enchanting_table", 4), item("cartography_table", 2), item("smithing_table", 2), item("bundle", 4), item("music_disc_cat", 3), item("music_disc_13", 3)])
mk("frame",     ORE + FOOD + MISC + [item("moss_block", 8, (8, 20)), item("vine", 6, (8, 20)), item("bone_meal", 10, (8, 24))],
   [item("lantern", 3, (2, 6)), item("mud_bricks", 2, (8, 16)), item("tuff", 2, (8, 16)), item("goat_horn", 3), item("honeycomb_block", 3, (2, 6))])
mk("salon",     ORE + FOOD + MISC + [item("note_block", 6, (2, 6)), item("jukebox", 4), item("paper", 8, (8, 20)), item("book", 6, (4, 10))],
   [item("music_disc_pigstep", 3), item("music_disc_otherside", 3), item("music_disc_relic", 3), item("music_disc_5", 2), item("bell", 3), item("painting", 5, (1, 3))],
   treasure_rolls=(2, 4))
mk("dome",      ORE + FOOD + MISC + [item("sea_lantern", 6, (4, 10)), item("prismarine_crystals", 6, (4, 10)), item("glow_ink_sac", 5, (4, 10))],
   [item("heart_of_the_sea", 5), item("conduit", 3), item("nautilus_shell", 5, (1, 2)), item("trident", 4), item("turtle_helmet", 4, None, {"respiration": 3, "unbreaking": 3, "mending": 1}), item("sniffer_egg", 2)],
   treasure_rolls=(2, 4))
mk("backrooms", FOOD + MISC + [item("paper", 8, (8, 20)), item("string", 8, (8, 20)), item("ender_eye", 6, (2, 5)), item("lead", 5, (2, 4)), item("name_tag", 6, (1, 3))],
   [item("enchanted_golden_apple", 6, (1, 3)), item("ender_chest", 4), item("echo_shard", 5, (2, 4)), item("disc_fragment_5", 4, (1, 3)), item("totem_of_undying", 8), item("recovery_compass", 5), item("music_disc_creator_music_box", 3)])
mk("mall",      FOOD + ORE + [item("emerald", 14, (6, 16)), item("cookie", 10, (8, 24)), item("cake", 5), item("map", 6), item("clock", 6), item("compass", 6), item("bundle", 6), item("leather_boots", 3)],
   [item("villager_spawn_egg", 2), item("emerald_block", 8, (2, 5)), item("name_tag", 8, (1, 3)), item("saddle", 6), item("diamond_horse_armor", 5), item("golden_apple", 8, (2, 5)), item("trial_key", 4, (1, 2))],
   treasure_rolls=(2, 4))
mk("quarry",    ORE + FOOD + MISC + [item("tnt", 4, (4, 8)), item("cobbled_deepslate", 6, (16, 32)), item("raw_iron_block", 4, (1, 3)), item("raw_gold_block", 3, (1, 2))],
   [gear_pick(12), gear_shovel(8), gear_axe(8), item("diamond", 12, (6, 12)), item("netherite_scrap", 8, (3, 6)), item("ancient_debris", 8, (3, 6)), item("raw_copper_block", 4, (2, 4)), item("minecart", 2)])
mk("cave",      ORE + FOOD + MISC + [item("glow_berries", 8, (8, 16)), item("moss_block", 6, (8, 16)), item("raw_gold", 6, (4, 10)), item("amethyst_shard", 8, (6, 16))],
   [item("spore_blossom", 4, (1, 3)), item("glow_ink_sac", 4, (4, 8)), item("echo_shard", 3, (1, 2)), item("lodestone", 2), item("diamond", 8, (3, 7)), item("emerald", 8, (4, 10))])
mk("amethyst",  ORE + FOOD + MISC + [item("amethyst_shard", 14, (12, 32)), item("calcite", 6, (8, 20)), item("tinted_glass", 5, (4, 10)), item("spyglass", 5)],
   [item("amethyst_block", 10, (4, 12)), item("amethyst_cluster", 8, (2, 5)), item("spyglass", 8), item("diamond", 8, (4, 8)), item("echo_shard", 4, (1, 3)), item("end_crystal", 1)],
   treasure_rolls=(2, 4))
mk("deep",      ORE + FOOD + MISC + [item("deepslate_diamond_ore", 2), item("raw_iron_block", 6, (2, 4)), item("raw_gold_block", 5, (1, 3)), item("coal_block", 6, (3, 8)), item("magma_block", 4, (4, 10))],
   [item("netherite_ingot", 10, (1, 3)), item("netherite_scrap", 12, (3, 6)), item("ancient_debris", 12, (3, 7)), item("totem_of_undying", 10), item("enchanted_golden_apple", 8, (2, 3)), item("echo_shard", 8, (2, 5)),
    item("netherite_chestplate", 3, None, {"protection": 4, "unbreaking": 3, "mending": 1}), item("netherite_pickaxe", 4, None, {"efficiency": 5, "unbreaking": 3, "fortune": 3, "mending": 1}),
    item("netherite_sword", 4, None, {"sharpness": 5, "unbreaking": 3, "looting": 3, "mending": 1}), item("beacon", 3), item("dragon_head", 1), item("nether_star", 1), item("elytra", 4, None, {"unbreaking": 3, "mending": 1})],
   treasure_rolls=(3, 5), supply_rolls=(3, 5))
print("ok", sorted(os.listdir(OUT)))
