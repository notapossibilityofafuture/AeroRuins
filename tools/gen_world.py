import json, os, sys
ROOT = sys.argv[1]; KT = sys.argv[2]
BIOME_DIR = os.path.join(ROOT, "data/aeroruins/worldgen/biome")
TAG_DIR = os.path.join(ROOT, "data/minecraft/tags/worldgen/biome")
os.makedirs(BIOME_DIR, exist_ok=True); os.makedirs(os.path.join(TAG_DIR, "has_structure"), exist_ok=True)
with open(os.path.join(ROOT, "pack.mcmeta"), "w", encoding="utf-8") as handle:
    json.dump({"pack": {"pack_format": 94, "description": "AeroRuins custom biomes"}}, handle, indent=2)
    handle.write("\n")
H = lambda s: int(s, 16)
def mob(t, w, lo=4, hi=4): return {"type": "minecraft:" + t, "weight": w, "minCount": lo, "maxCount": hi}
def cr(*p): return [mob(n, w, lo, hi) for (n, w, lo, hi) in p]
MONSTERS = [mob("zombie",100), mob("skeleton",80), mob("creeper",100), mob("spider",100), mob("enderman",10,1,4), mob("witch",5,1,1)]
BAT = [mob("bat",10,8,8)]
def spawners(profile, extra=None):
    extra = extra or {}; s = {}
    if profile == "surface":
        s["monster"] = list(extra.get("monster", []))                                                                    
                                                                                             
        s["creature"] = [mob("sheep",12,2,4), mob("cow",8,2,4), mob("pig",10,2,3), mob("chicken",10,2,4)] + extra.get("creature", [mob("rabbit",12,2,3), mob("bee",10,2,3)]); s["ambient"] = BAT
    elif profile == "gloom":
        s["monster"] = [mob("zombie",120), mob("skeleton",100), mob("creeper",80), mob("spider",100), mob("enderman",30,1,4), mob("witch",10,1,1)] + extra.get("monster", [])
        s["creature"] = extra.get("creature", []); s["ambient"] = BAT
    elif profile == "water":
        s["monster"] = []; s["water_creature"] = [mob("squid",10,1,2)]; s["water_ambient"] = [mob("cod",25,3,6)]
    elif profile == "dome":
        s["creature"] = extra.get("creature", [mob("parrot",20,1,2), mob("rabbit",15,2,3), mob("bee",10,2,3)])
    elif profile == "cave":
        s["monster"] = list(MONSTERS) + extra.get("monster", [])
        if "creature" in extra: s["creature"] = extra["creature"]
        s["ambient"] = BAT; s["underground_water_creature"] = [mob("glow_squid",10,2,4)]
    elif profile == "deep":
        s["monster"] = [mob("zombie",100), mob("skeleton",100), mob("creeper",60), mob("enderman",15,1,4)] + extra.get("monster", []); s["ambient"] = BAT
    elif profile == "ward":
        s["monster"] = [mob("zombie_villager",30,1,2)]
    elif profile == "flesh":
        s["monster"] = [mob("zombie_villager",40,1,2), mob("husk",30,1,2), mob("enderman",10,1,1)]
    elif profile == "backrooms":
        s["monster"] = [mob("enderman",100,1,2), mob("silverfish",50), mob("zombie",40), mob("creeper",20,1,2)]
    elif profile == "mall":
        s["monster"] = [mob("zombie_villager",60,2,4), mob("skeleton",60,2,4), mob("creeper",40,1,2), mob("witch",5,1,1)]
    elif profile == "quarry":
        s["monster"] = list(MONSTERS) + [mob("cave_spider",20)]; s["underground_water_creature"] = [mob("glow_squid",10,2,4)]; s["ambient"] = BAT
    return s
def particle(n, p): return {"probability": p, "options": {"type": "minecraft:" + n}}

B = {}
def add(i, t, d, pr, sky, fog, w, wf, g, f, part=None, prof="surface", extra=None):
    B[i] = dict(t=t, d=d, pr=pr, sky=H(sky), fog=H(fog), w=H(w), wf=H(wf), g=H(g), f=H(f), part=part, prof=prof, extra=extra)

                                                                                                
                                                                                                                          
S = []                                                           
def surf(i, t, d, pr, sky, fog, w, wf, g, f, log, fl, fb, tree, alt, dens, flags="", extra=None, part=None, prof="surface", flora=6):
    add(i, t, d, pr, sky, fog, w, wf, g, f, part, prof, extra)
    S.append((i, log, fl, fb, tree, alt, dens, flags, flora))
W_ = "4f7384"; WF = "1f3744"
      
surf("frostbitten_steppe",0.1,0.2,True,"a9b6c0","c5ced4",W_,WF,"a4ad92","7e957a","SPRUCE_LOG","COLD","SNOWY_PLAINS","REDWOOD","TALL_REDWOOD",1,"cold",{"creature":cr(("rabbit",12,2,3),("fox",8,1,2))})
surf("paper_birch_barrens",0.2,0.25,True,"b4bdc2","cdd3d4","55798a","24404c","a9b08a","a3ae6f","BIRCH_LOG","WHITE","BIRCH_FOREST","BIRCH","TALL_BIRCH",5,"cold")
surf("hoarfrost_taiga",0.1,0.5,True,"a5b2bb","c0cad0",W_,WF,"8f9f88","5f7f68","SPRUCE_LOG","COLD","SNOWY_TAIGA","REDWOOD","TALL_REDWOOD",12,"cold",{"creature":cr(("wolf",8,4,4),("fox",8,2,4))})
surf("silver_birch_vale",0.25,0.5,True,"b0bcc2","cad2d3","55798a","24404c","9aa87e","8fa070","BIRCH_LOG","WHITE","BIRCH_FOREST","BIRCH","TALL_BIRCH",10,"cold")
surf("fogbound_spruce",0.2,0.7,True,"98a6af","b2bec4",W_,WF,"7f9275","506f5c","SPRUCE_LOG","COLD","TAIGA","REDWOOD","TALL_REDWOOD",16,"cold")
surf("grey_fen",0.3,0.9,True,"9aa4a6","b0b9b8","4a6460","223530","7d8c64","63774f","MANGROVE_LOG","COLD","SWAMP","SWAMP","TREE",6,"cold",{"monster":cr(("bogged",40,4,4))})
        
surf("dry_meadow",0.5,0.25,True,"b3bdbf","cdd2cb","4f7280","223f4a","a0a878","7d9055","OAK_LOG","WILD","MEADOW","TREE","BIG_TREE",1)
surf("windswept_heath",0.45,0.3,True,"a9b4b9","c2cac9",W_,WF,"8f9a70","6f8458","OAK_LOG","WILD","WINDSWEPT_HILLS","TREE","REDWOOD",1)
surf("old_oak_forest",0.55,0.5,True,"a9b5b8","c1c9c5",W_,WF,"83975e","5e7a45","OAK_LOG","WILD","FOREST","TREE","BIG_TREE",12)
surf("white_birch_forest",0.5,0.5,True,"b2bcbd","cbd2cc",W_,WF,"8fa068","8a9d58","BIRCH_LOG","WHITE","BIRCH_FOREST","BIRCH","TALL_BIRCH",12)
surf("wet_woodland",0.6,0.8,True,"a0acae","b8c3c0","4a6a70","213d42","788f5a","55723f","OAK_LOG","WILD","FOREST","TREE","BIG_TREE",14)
surf("mossy_hollows",0.55,0.85,True,"94a2a4","afbab6","4a6a64","213a36","6f8a58","4f6f42","DARK_OAK_LOG","WILD","DARK_FOREST","TREE","DARK_OAK",15)
        
surf("yellow_plains",0.8,0.25,False,"bfc4bd","d5d6c8","4f7280","223f4a","b0b073","8f9650","OAK_LOG","DRY","PLAINS","TREE","BIG_TREE",1,"warm")
surf("scrub_savanna",0.9,0.2,False,"c2c1b6","d8d4c2","4f7280","223f4a","b4a96c","8f8c4d","ACACIA_LOG","DRY","SAVANNA","ACACIA","ACACIA",3,"warm")
surf("mixed_forest",0.7,0.5,True,"adb9ba","c6cfc9",W_,WF,"859960","62804a","OAK_LOG","WILD","FOREST","TREE","BIRCH",12)
surf("dark_oak_forest",0.7,0.6,True,"93a09f","aeb9b3","4a6a68","213d3c","6f8656","3e5a35","DARK_OAK_LOG","WILD","DARK_FOREST","DARK_OAK","TREE",16)
surf("overgrown_thicket",0.75,0.8,True,"a2b0aa","bac6bb","4a6a64","213a36","76905a","4c7040","OAK_LOG","WILD","FOREST","TREE","BIG_TREE",18)
surf("damp_jungle_edge",0.85,0.9,True,"a0b0a8","b8c6b8","46685c","1d3a30","6f8f50","427a38","JUNGLE_LOG","WILD","JUNGLE","SMALL_JUNGLE","JUNGLE",8,"warm")
          
surf("dust_flats",1.0,0.1,False,"c8c4b4","dcd6c2","4f7280","223f4a","b8aa70","928a50","ACACIA_LOG","DRY","SAVANNA","ACACIA","ACACIA",1,"warm")
surf("dry_acacia_plain",0.95,0.2,False,"c4c2b2","d8d3c0","4f7280","223f4a","b5a86e","8c8a4c","ACACIA_LOG","DRY","SAVANNA","ACACIA","TREE",4,"warm")
surf("sun_bleached_grove",0.9,0.4,True,"bfc3b8","d3d6c8",W_,WF,"a0a870","7c8f52","OAK_LOG","DRY","FOREST","TREE","BIG_TREE",6,"warm")
surf("olive_woodland",0.9,0.45,True,"b6bfb4","cdd3c4",W_,WF,"8e9e66","6a8250","OAK_LOG","WILD","FOREST","TREE","BIG_TREE",9,"warm")
surf("humid_rainforest",0.95,0.95,True,"98aaa2","b0c0b4","46685c","1d3a30","6b8f4e","3f7035","JUNGLE_LOG","WILD","JUNGLE","JUNGLE","SMALL_JUNGLE",14,"warm")
surf("black_bog",0.8,0.95,True,"8c9896","a2aeaa","3d5450","1a2825","5f7448","3f5a3a","MANGROVE_LOG","WILD","SWAMP","SWAMP","TREE",8,"warm",{"monster":cr(("bogged",50,4,4),("slime",30,2,4))})
        
surf("stone_highlands",0.4,0.4,True,"c6d9e3","e2edf2","648da1","31586b","a4bd9b","7f9d70","SPRUCE_LOG","COLD","WINDSWEPT_FOREST","REDWOOD","TREE",2,"high",{"creature":cr(("goat",8,1,3),("sheep",10,2,3))})
surf("cloud_crest",0.2,0.3,True,"d6e5ed","edf5f8","78a9bb","3e6e82","b7cea8","91ad82","SPRUCE_LOG","COLD","WINDSWEPT_HILLS","REDWOOD","REDWOOD",1,"high,cold",{"creature":cr(("goat",10,1,3))})
                                  
surf("frozen_estate",-0.2,0.5,True,"d8dde2","e8ecef","6a8a9a","2a4050","b8c4c4","8a9a9a","SPRUCE_LOG","COLD","SNOWY_PLAINS","REDWOOD","REDWOOD",2,"gloom,bare,cold",{"monster":cr(("stray",60,2,4))},("white_ash",0.01),"gloom")
surf("ashen_cathedral",0.4,0.2,True,"5a5e66","7a7e86","4a5a60","1e2a30","5a6050","4a5040","DARK_OAK_LOG","COLD","DARK_FOREST","TREE","TREE",3,"gloom,bare",{"monster":cr(("skeleton",60,2,4))},("ash",0.008),"gloom")
surf("gloomwood",0.5,0.6,True,"3a4048","566068","3a5058","1a2a30","4a5a3a","3a4a2e","DARK_OAK_LOG","COLD","DARK_FOREST","DARK_OAK","TREE",14,"gloom",{"monster":cr(("spider",40,2,4))},None,"gloom")
surf("grey_marsh",0.6,0.9,True,"6a7470","8a948c","4a5a50","24302a","6a7a50","55663f","MANGROVE_LOG","COLD","SWAMP","SWAMP","TREE",5,"gloom",{"monster":cr(("bogged",50,4,4),("slime",30,2,4))},None,"gloom")
surf("dead_orchard",0.5,0.3,False,"807a70","a09a8e","5a6a70","2a3438","8a8450","6a6a40","OAK_LOG","DRY","PLAINS","TREE","TREE",4,"gloom,bare",None,None,"gloom")
surf("soot_fields",0.6,0.1,False,"3a3a3c","5a5a5c","40484c","1c2024","5a5a48","484838","ACACIA_LOG","DRY","PLAINS","TREE","TREE",3,"gloom,bare",None,("ash",0.01),"gloom")
surf("silent_boulevard",0.5,0.3,True,"a0a4a8","c4c8cc","70808a","303a40","7a8470","6a7860","BIRCH_LOG","WHITE","PLAINS","BIRCH","BIRCH",3,"gloom,bare",None,None,"gloom")
surf("rust_meadow",0.7,0.2,False,"8a6a58","b0907a","6a7a78","2e3a38","9a8a50","7e7440","ACACIA_LOG","DRY","SAVANNA","ACACIA","TREE",2,"gloom",None,None,"gloom")
surf("hollow_mist",0.4,0.8,True,"aab4b8","d0d8da","6a8890","2a4048","8a9a88","6a8070","SPRUCE_LOG","COLD","TAIGA","REDWOOD","TALL_REDWOOD",12,"gloom",None,("white_ash",0.006),"gloom")
surf("requiem_heath",0.4,0.4,True,"575a66","777a84","5a5a68","24223a","6f7068","5a5a58","DARK_OAK_LOG","COLD","PLAINS","TREE","TREE",3,"gloom,bare",None,("ash",0.004),"gloom")
surf("snowy_apocalypse",0.15,0.35,True,"dcecff","f4f8ff","9fc7d8","50788e","d6e8d0","b0c8b0","SPRUCE_LOG","COLD","SNOWY_PLAINS","REDWOOD","TALL_REDWOOD",3,"cold",{"monster":cr(("stray",50,2,4))},("snowflake",0.004),"gloom")
surf("foggy_woods",0.25,0.7,True,"c6d6d8","e4eff0","7d9a8f","49665e","8eae78","6d8e62","SPRUCE_LOG","COLD","TAIGA","REDWOOD","TALL_REDWOOD",20,"cold",None,("white_ash",0.008),"gloom")
surf("dreamcore_meadow",0.75,0.35,True,"d9ecff","f5fbff","75a9bc","345d6d","a6d68d","82b86b","OAK_LOG","WILD","MEADOW","TREE","BIG_TREE",3,"",{"creature":cr(("rabbit",12,2,3),("bee",10,2,3))},None,"surface",24)
surf("liminal_parkland",0.6,0.25,True,"e0ebee","f8fbf8","83a7a5","476866","a8c58e","7fa56e","BIRCH_LOG","WHITE","BIRCH_FOREST","BIRCH","TREE",5,"",{"creature":cr(("rabbit",12,2,3))},None,"surface",18)
surf("sunlit_woods",0.8,0.45,True,"d8f0ff","f8ffff","6fa5ae","345c68","9bd080","76ad63","OAK_LOG","WILD","FOREST","TREE","BIRCH",10,"",{"creature":cr(("bee",12,2,3))},None,"surface",14)

                                                                          
                 
surf("forgotten_garden",0.7,0.6,True,"a8b8b0","c0cec4","4a7068","213f3a","7a9a58","56793f","OAK_LOG","WILD","FLOWER_FOREST","TREE","BIG_TREE",7,"",{"creature":cr(("rabbit",12,2,3),("bee",10,2,3))},None,"surface",18)
surf("ivy_courtyard",0.6,0.5,True,"a0b0b0","bccac4","46686a","1f3a3c","6f8f55","4c6f3f","DARK_OAK_LOG","WILD","FOREST","TREE","DARK_OAK",9,"",None,None,"surface",14)
surf("wild_orchard",0.7,0.5,True,"b0bcb4","c8d2c4","4a7068","213f3a","8aa060","6a8c45","OAK_LOG","WILD","FOREST","TREE","BIG_TREE",10,"",{"creature":cr(("bee",10,2,3),("rabbit",8,2,3))},None,"surface",16)
                  
surf("pale_flower_fields",0.6,0.4,True,"b8c2c2","d2d8d2",W_,WF,"9aac78","7a9558","OAK_LOG","WHITE","FLOWER_FOREST","TREE","BIRCH",1,"",{"creature":cr(("bee",20,2,3),("rabbit",8,2,3))},None,"surface",48)
surf("poppy_meadow",0.65,0.35,True,"b4bec0","cdd3cc",W_,WF,"98a672","76904f","OAK_LOG","REDS","MEADOW","TREE","TREE",1,"",{"creature":cr(("bee",20,2,3),("sheep",10,2,3))},None,"surface",46)
surf("dandelion_plain",0.7,0.3,True,"bec4be","d6d9cc",W_,WF,"a8ae72","889650","OAK_LOG","YELLOWS","PLAINS","TREE","BIG_TREE",1,"warm",{"creature":cr(("bee",15,2,3),("horse",6,2,3))},None,"surface",46)
surf("cornflower_haze",0.55,0.45,True,"aebcc4","c8d2d6","4f7384","1f3744","8fa380","6f8a62","BIRCH_LOG","BLUES","FLOWER_FOREST","BIRCH","TREE",1,"",{"creature":cr(("bee",20,2,3),("rabbit",8,2,3))},None,"surface",44)
               
surf("quiet_suburbia",0.6,0.35,True,"b8c2c6","d0d8da",W_,WF,"8fa070","6f8650","OAK_LOG","WILD","PLAINS","TREE","BIRCH",3,"",{"creature":cr(("rabbit",10,2,3))},None,"surface",8)
surf("cul_de_sac",0.6,0.35,True,"bcc4c4","d4dad8",W_,WF,"94a674","74905a","BIRCH_LOG","WHITE","PLAINS","BIRCH","TREE",2,"",{"creature":cr(("rabbit",10,2,3))},None,"surface",8)
assert len(S) == 50, len(S)

                                  
add("still_lake",0.6,0.6,True,"b0bbbd","c8d0cf","4a7080","213f4a","7f9360","5f7d48",prof="water")
add("deep_lake",0.5,0.6,True,"a4b0b4","bcc6c8","3a5a70","182f40","7f9360","5f7d48",prof="water")
add("marsh_water",0.7,0.8,True,"9aa6a2","b0bab4","4a6458","213028","6f8a58","4f6f42",prof="water")
add("cold_water",0.4,0.5,True,"b0bcc4","ccd4d8","5a7f90","264452","9aa88a","7a9078",prof="water")
add("fluorescent_suburb",0.5,0.3,False,"c9d3d8","dfe6e6","6a8890","2a4048","8a9a70","6f8458",prof="gloom")
                                                                      
add("sunlit_plaza",0.8,0.5,False,"5cc8ff","e6f8ff","35e0f5","1aa0c0","8ce85a","38b6ff",prof="dome")
add("crystal_atrium",0.8,0.5,False,"80d8ff","f0ffff","60f0ff","20b0e0","70e8c0","60b0ff",("end_rod",0.002),prof="dome")
add("aqua_conservatory",0.85,0.7,False,"40e0e0","d0fff8","20e8d8","08a098","50e890","20c8b0",prof="dome")
add("pearl_arboretum",0.7,0.4,False,"e8e0ff","faf4ff","80d8f8","4090c0","b0f0a0","a0c8ff",("cherry_leaves",0.002),prof="dome")
                                        
add("levelzero_lobby",0.7,0.0,False,"c9d3c5","c9d3c5","9fb8b0","5e7a72","7fa85a","6a9450",("white_ash",0.004),prof="backrooms")
add("damp_annex",0.7,0.0,False,"9fb8b0","9fb8b0","6f9890","2e4a42","5f8a50","4a7a40",("white_ash",0.003),prof="backrooms")
add("greenroom_garden",0.8,0.0,False,"b8e0b0","b8e0b0","8fd8c0","3e7a6a","60d040","40b030",("spore_blossom_air",0.003),prof="backrooms")
add("flicker_wing",0.7,0.0,False,"d8d0a0","d8d0a0","b0a880","5a5430","a0a850","888840",("ash",0.003),prof="backrooms")
add("pedestrian_underpass",0.6,0.0,False,"c3ccd0","c3ccd0","7a9098","2e444c","8a9a80","708466",("white_ash",0.003),prof="backrooms")
add("flooded_subway",0.6,0.0,False,"d0bcd4","d8c2dc","7a8a98","2e3a48","8a9080","708066",("white_ash",0.004),prof="backrooms")
add("atrium_concourse",0.7,0.3,False,"6fd3c8","7fe0d0","3fd0c0","0f6f66","3fbf8f","2fa37a",prof="mall")
add("fountain_court",0.7,0.3,False,"7fc8f0","a0e0f8","40c8f8","1080c0","50d0a0","30b080",prof="mall")
add("deadmall_east",0.5,0.0,False,"2a3030","2a3030","507070","1a3030","5a7a50","486a40",("ash",0.003),prof="mall")
add("skylight_arcade",0.8,0.3,False,"f0d0e8","f8e8f4","70d8e8","2a90a8","a0e0a0","80d090",prof="mall")
add("chalk_quarry",0.6,0.0,False,"cfd6cf","cfd6cf","6fb0b8","2a5a62","8fb870","78a060",prof="quarry")
add("flooded_pit",0.6,0.0,False,"1a3a4a","1a3a4a","2e8f9a","0e4a58","5fa050","4a8a45",prof="quarry")
add("daisy_cavern",0.7,0.0,False,"1f4a2a","1f4a2a","3fd0b0","0f6a58","8fe060","6ac048",("spore_blossom_air",0.003),prof="quarry")
                                                       
add("mossy_hollow",0.8,0.5,False,"16301f","16301f","3fd0c0","0f6f66","7ce04a","4fcb3c",("spore_blossom_air",0.004),prof="cave",extra={"creature":cr(("axolotl",10,4,4))})
add("amethyst_vault",0.6,0.0,False,"2a2060","2a2060","8a7cff","3a2f8a","8cc8ff","6fa8ff",("end_rod",0.003),prof="cave")
add("spore_cavern",0.7,0.5,False,"3a1f4a","3a1f4a","b07cff","5a2f8a","b0c8a0","90b080",("warped_spore",0.004),prof="cave",extra={"creature":cr(("mooshroom",8,4,8))})
add("glowworm_grotto",0.8,0.6,False,"0a2a3a","0a2a3a","30c8ff","105088","40d0b0","30b090",("end_rod",0.003),prof="cave")
add("dripstone_gallery",0.7,0.0,False,"3a2a20","3a2a20","5fa8c8","2a5a78","a09060","806a48",prof="cave")
add("frozen_cavern",0.1,0.0,False,"80b0e0","80b0e0","60c8ff","2060a0","c0e0f0","a0c8e8",("snowflake",0.004),prof="cave",extra={"monster":cr(("stray",60,2,4))})
add("sandstone_gallery",1.0,0.0,False,"5a4a30","5a4a30","50c0d0","1a6a80","d0c070","b0a050",prof="cave",extra={"monster":cr(("husk",50,2,4))})
add("bone_catacomb",0.5,0.0,False,"201c18","201c18","5a7a80","1e3a40","6a7a50","586a40",("ash",0.004),prof="cave",extra={"monster":cr(("skeleton",120,4,4),("stray",20,2,4))})
add("root_tangle",0.8,0.6,False,"2a2010","2a2010","40b0a0","106058","6a9a40","508030",prof="cave")
add("verdigris_vault",0.7,0.0,False,"103830","103830","30d0b0","0a7a68","50c0a0","40a888",prof="cave")
add("tuff_labyrinth",0.6,0.0,False,"303030","303030","5a9ab0","2a566a","808878","687060",prof="cave")
add("clay_warren",0.8,0.4,False,"3a2820","3a2820","6a98a0","30505a","a08a60","887048",prof="cave")
add("cenote_pool",0.9,0.6,False,"083040","083040","20e0e0","08909a","50d0a0","38b080",("end_rod",0.002),prof="cave",extra={"creature":cr(("axolotl",20,4,4))})
add("spiderweb_hollow",0.5,0.0,False,"181818","181818","4a6a78","1a2e38","4a5a40","3a4a30",prof="cave",extra={"monster":cr(("cave_spider",120,3,5),("spider",80,3,4))})
add("terracotta_gallery",1.1,0.0,False,"4a2a18","4a2a18","50c0b8","1a7068","c09050","a07840",prof="cave")
add("deepdark_abyss",0.4,0.0,False,"041418","041418","104050","041c24","284a50","1e3a40",prof="deep")
add("mantle_core",2.0,0.0,False,"300800","501000","c06030","601800","7a5a30","604820",("ash",0.006),prof="deep",extra={"monster":cr(("wither_skeleton",30,1,3),("blaze",10,1,2),("magma_cube",40,2,4))})
add("obsidian_rift",0.5,0.0,False,"120a28","1a0a30","8a50ff","301080","6a4aa0","583a88",("portal",0.01),prof="deep",extra={"monster":cr(("enderman",60,1,3))})
add("deepslate_halls",0.4,0.0,False,"101018","101018","406a88","18304a","58606a","485058",prof="deep")

                                                                              
                       
add("sterile_wards",0.6,0.0,False,"d4e0e0","d8e4e4","8ab0b8","3a5a62","9aa88a","7a9068",prof="ward")
add("quarantine_wing",0.6,0.0,False,"b8d0c0","c0d8c8","7aa8a0","34584e","8aa080","6a8860",("white_ash",0.003),prof="ward")
add("bloodied_wards",0.6,0.0,False,"8a6a6a","a07878","7a8088","38404a","7a7a6a","625f52",("ash",0.004),prof="ward")
                          
add("fleshy_downtown",0.8,0.0,False,"4a1a1a","6a2a28","8a4a4a","4a2020","6a3030","582525",("crimson_spore",0.006),prof="flesh")
add("sinew_district",0.8,0.0,False,"3a1a28","5a2a3a","7a4a5a","3a2030","5a3038","4a2830",("crimson_spore",0.005),prof="flesh")
add("rotted_arcade",0.8,0.0,False,"2a1a14","4a2a1c","6a4a3a","3a2820","584028","483420",("ash",0.006),prof="flesh")
                           
add("torn_veil",0.5,0.2,False,"7a6a72","98868c","6a6a78","2e2e3e","7a7a60","625f48",("ash",0.008),prof="gloom")

                                          
add("bioluminescent_gallery",0.8,0.6,False,"0a2a30","0a2a30","30d0c0","106058","5ad0a0","40b088",("end_rod",0.003),prof="cave")
add("quartz_cavern",0.6,0.0,False,"a0a0a8","a0a0a8","80a8c0","385870","c0c0c0","a0a0a0",prof="cave")
add("basalt_tubes",0.9,0.0,False,"1a1a20","1a1a20","507078","203840","505058","404048",("ash",0.003),prof="cave")
add("mushroom_grove",0.8,0.7,False,"2a1a3a","2a1a3a","9070c0","402a70","a89078","907060",("warped_spore",0.004),prof="cave",extra={"creature":cr(("mooshroom",8,4,8))})
add("glacial_fissure",0.0,0.0,False,"90c0e8","90c0e8","70d0ff","2870b0","d0e8f8","b0d0e8",("snowflake",0.005),prof="cave",extra={"monster":cr(("stray",60,2,4))})
add("timber_galleries",0.6,0.0,False,"2a2218","2a2218","5a7080","28404a","7a6a48","605238",prof="cave",extra={"monster":cr(("cave_spider",80,3,5),("zombie",40,2,4))})
add("honeycomb_hollow",0.8,0.5,False,"4a3a10","4a3a10","90b8a0","386050","a8a050","888038",prof="cave",extra={"creature":cr(("bee",20,2,3))})
add("drowned_crypt",0.7,0.0,False,"102028","102028","3a8090","124050","50705a","3e5848",("white_ash",0.003),prof="cave",extra={"monster":cr(("drowned",80,2,4),("skeleton",60,2,4))})
add("ruby_hollow",0.7,0.0,False,"3a0f12","3a0f12","a04050","501820","7a3a38","603028",prof="cave")
add("fossil_cavern",0.9,0.0,False,"4a4030","4a4030","60a0a8","28585e","b0a070","908058",prof="cave",extra={"monster":cr(("husk",50,2,4),("skeleton",60,2,4))})
add("salt_caverns",0.7,0.0,False,"b0b0b8","b0b0b8","80b0c8","3a6078","c8c8c8","a8a8a8",prof="cave")
add("ember_cavity",1.5,0.0,False,"3a1008","501a0c","d07030","702808","7a4a28","603820",("ash",0.005),prof="cave",extra={"monster":cr(("magma_cube",30,2,3))})
add("bamboo_grotto",0.8,0.7,False,"14301a","14301a","40c088","106040","6aa850","4a8a38",("spore_blossom_air",0.003),prof="cave",extra={"creature":cr(("panda",6,1,2))})
add("nostalgia_valley",0.9,0.7,False,"d8f4ff","f8ffff","75c8b0","2e806c","9ee878","70c85a",("spore_blossom_air",0.006),prof="cave",extra={"creature":cr(("rabbit",12,2,3),("bee",10,2,3))})
add("cloud_gardens",0.95,0.7,False,"e8f7ff","ffffff","a0e8d0","4a9f82","b8f090","82c86c",("end_rod",0.004),prof="cave",extra={"creature":cr(("bee",16,2,3),("rabbit",8,2,3))})
add("sunken_ruins",0.6,0.2,False,"526878","71828c","6faaa0","294f50","759a68","52764e",("ash",0.003),prof="cave",extra={"monster":cr(("zombie",70,2,4),("skeleton",50,2,4))})
add("crystal_river",0.85,0.8,False,"8edcff","d8ffff","30e0e0","087c8a","78d890","42b878",("end_rod",0.005),prof="cave",extra={"creature":cr(("axolotl",18,4,4))})
add("lavender_dream",0.8,0.5,False,"c8b8ff","f4e8ff","b080e0","553080","d8a8e8","a878c8",("spore_blossom_air",0.005),prof="cave",extra={"creature":cr(("allay",8,1,2))})
add("echoing_library",0.45,0.0,False,"24263a","3e4054","706a94","2d2946","6a7088","4e5268",("ash",0.003),prof="cave",extra={"monster":cr(("skeleton",80,2,4),("silverfish",40,1,2))})
add("mushroom_rain",0.75,0.7,False,"36234a","5c3a6a","9a70d0","4a2d70","a88a78","806050",("warped_spore",0.006),prof="cave",extra={"creature":cr(("mooshroom",14,4,8))})
add("ancient_observatory",0.65,0.2,False,"39424a","65747a","8ab8b8","365c60","a0ba8a","70886a",("end_rod",0.004),prof="cave",extra={"monster":cr(("enderman",12,1,2),("skeleton",50,2,4))})
add("sunken_grove",0.8,0.7,False,"203c38","38584c","4aa890","1d604c","72b85c","4d8c46",("spore_blossom_air",0.004),prof="cave",extra={"creature":cr(("axolotl",10,4,4))})
add("rainbow_geode",0.7,0.3,False,"302060","503c90","b080ff","5a38a0","b080e8","9060c8",("end_rod",0.005),prof="cave")
add("moonlit_marsh",0.5,0.9,False,"18283a","30465a","4a8890","1e4a58","66885c","4c6848",("glow_lichen",0.004),prof="cave")
add("obsidian_cathedral",0.2,0.0,False,"100814","1c1028","6840a0","2a1648","54406c","40304f",("soul_fire_flame",0.003),prof="deep")
add("copper_rain",0.6,0.2,False,"28382e","486050","50c8a0","287060","8ca86c","648450",("end_rod",0.003),prof="cave")
add("forgotten_station",0.4,0.0,False,"202830","384550","5a8490","2b4650","69785e","505e48",("ash",0.003),prof="cave",extra={"monster":cr(("zombie",80,2,4),("cave_spider",25,2,4))})
add("pink_sanctum",0.8,0.2,False,"552d54","805878","e090c8","6a2e68","d09ab0","b878a0",("end_rod",0.003),prof="cave")
add("teal_corridors",0.7,0.3,False,"143b42","28626a","38d8d0","187f82","70c8a0","4ca878",("end_rod",0.003),prof="cave")
add("golden_chasm",1.0,0.0,False,"49351c","70522c","e0b040","8a6428","c8a060","a08048",prof="cave")
add("blue_ice_palace",0.0,0.0,False,"5688c0","86b8e0","60d8ff","2868b0","b0d8f0","90b8d8",("snowflake",0.004),prof="cave",extra={"monster":cr(("stray",50,2,4))})
add("red_lichen_rift",0.9,0.1,False,"431b18","733020","d05a50","733020","a06048","784038",("warped_spore",0.003),prof="cave")
add("quiet_quarry",0.5,0.0,False,"30383c","4c5558","668e98","304a50","82906c","687454",prof="cave")
add("mossy_aqueduct",0.7,0.8,False,"1b3d3d","345c58","38bfc0","185e68","6eae68","4c884e",("end_rod",0.003),prof="cave",extra={"creature":cr(("axolotl",16,4,4))})
add("sapphire_depths",0.4,0.0,False,"101e4c","223b78","4878e8","202e80","587ab0","405c98",("end_rod",0.003),prof="deep")
add("emerald_bower",0.8,0.7,False,"163b28","2d6840","60d8a0","287050","78c870","4c9848",("spore_blossom_air",0.004),prof="cave",extra={"creature":cr(("bee",12,2,3))})
add("lavender_lake",0.7,0.7,False,"34274c","604d78","8c98e0","484e90","9c8ac0","7868a0",("end_rod",0.003),prof="cave",extra={"creature":cr(("axolotl",12,4,4))})
add("hollow_mountain",0.5,0.2,False,"34383c","586066","748c98","384c58","888c78","68745e",prof="cave")
add("granite_ravine",0.7,0.1,False,"422c28","70463c","b07868","633a38","a08060","806048",prof="cave")
add("diorite_skyway",0.7,0.1,False,"646a74","8c949c","c0d0e0","68788c","b0b0a8","909098",("end_rod",0.003),prof="cave")
add("andesite_warrens",0.5,0.2,False,"3c4448","687478","7898a0","3e5a62","849080","68705f",prof="cave")
add("mudslide_caves",0.8,0.8,False,"332a24","5c493c","769c88","3a554a","8b8358","6a6848",prof="cave")
add("dripstone_forest",0.7,0.4,False,"30281e","554331","7c9a9e","405460","9c8058","786044",prof="cave")
add("echo_lake",0.4,0.5,False,"121a28","28364a","3a7898","183a5a","536878","3e5068",("glow_lichen",0.004),prof="deep")
add("sculk_garden",0.2,0.0,False,"07171c","102a34","1c6370","0a3038","315c62","24484e",prof="deep")
add("warden_throne",0.1,0.0,False,"02080c","071419","124452","061e28","203b42","193038",prof="deep",extra={"monster":cr(("warden",5,1,1))})
add("soul_sands",0.2,0.0,False,"171724","302d43","605078","28283e","514c5c","3d3a48",("soul_fire_flame",0.004),prof="deep")
add("warped_canopy",0.6,0.6,False,"0b3030","145555","39c8b0","17685f","5cc891","3e9a72",("warped_spore",0.005),prof="deep",extra={"creature":cr(("enderman",15,1,2))})
add("crimson_roots",0.9,0.3,False,"3a1018","6a1e2c","c04c58","6a1f2e","8e4a42","6c3435",("crimson_spore",0.005),prof="deep")
add("netherite_seam",0.3,0.0,False,"101016","25222a","686070","25252c","57545c","45434c",prof="deep")
add("ashfall_basin",0.4,0.0,False,"201c20","3a3438","686068","2f2c32","696054","504a45",("ash",0.006),prof="deep")
add("chorus_grotto",0.5,0.0,False,"1e173a","382d60","9878d8","4e3a8a","8c7caa","685c88",("end_rod",0.005),prof="deep",extra={"monster":cr(("enderman",50,1,3))})
add("end_void",0.2,0.0,False,"080812","121225","4b3b76","18183c","4b4260","37334e",prof="deep")
add("starfall_cavern",0.4,0.0,False,"090b20","171b3d","6680f0","26306e","5c68a0","464e7c",("end_rod",0.008),prof="deep")
add("glass_sea",0.8,0.8,False,"143d5c","266b82","6bd8ef","24698a","6ac8b0","48a88c",("end_rod",0.003),prof="cave",extra={"creature":cr(("glow_squid",15,2,4))})
add("kelp_catacomb",0.6,0.9,False,"102b35","1b4e56","329eaa","164d5d","4d9d70","327b5b",prof="cave",extra={"creature":cr(("drowned",40,2,4))})
add("coral_vault",0.8,0.9,False,"25434a","3c7070","54d8cc","247b80","75c88d","4f9e72",("end_rod",0.003),prof="cave",extra={"creature":cr(("tropical_fish",18,3,6))})
add("flooded_mines",0.5,0.8,False,"162830","2c5054","3d9aa0","1b535e","62876a","49644e",prof="cave",extra={"monster":cr(("drowned",60,2,4),("cave_spider",30,2,4))})
add("paper_halls",0.7,0.0,False,"b9c9ce","d8e4e6","91c9d0","467b86","b4d69b","8eb878",("end_rod",0.003),prof="cave")
add("checkerboard_void",0.3,0.0,False,"20232a","383c48","687e94","303a4e","687a68","4f5c50",prof="cave")
add("empty_hotel",0.5,0.0,False,"48404a","70626c","8c8790","46404b","7d766c","625b58",("end_rod",0.002),prof="cave")
add("faded_arcade",0.7,0.2,False,"492d54","754b78","d070c8","6f3a78","a080a0","806080",("end_rod",0.006),prof="cave")
add("fluorescent_tunnel",0.6,0.0,False,"89989a","c4d2d0","8ad8d0","4d8884","9eb78c","778b6d",("end_rod",0.005),prof="cave")
add("abandoned_greenhouse",0.8,0.8,False,"2e593d","4b8660","5bbba5","28705b","84c86c","5c9e50",("spore_blossom_air",0.005),prof="cave",extra={"creature":cr(("bee",15,2,3))})
add("overgrown_mall",0.7,0.7,False,"263b31","42644a","4b9c82","235d4f","78b06a","528449",("spore_blossom_air",0.004),prof="cave")
add("clockwork_cave",0.5,0.1,False,"383c3c","59615c","71b8a0","3b6b60","a0a06c","78784e",("end_rod",0.003),prof="cave")
add("library_of_moss",0.6,0.7,False,"243b32","3f5b4b","5da080","2d604c","79ad68","55854d",("spore_blossom_air",0.004),prof="cave")
add("candle_cathedral",0.4,0.0,False,"292325","483b3c","9a6e68","4d3336","806d5d","63574c",("soul_fire_flame",0.002),prof="cave")
add("white_noise_caves",0.0,0.0,False,"9caec0","d3e0e8","a8c8d8","5e7d90","c0d0c0","a8b8a8",("snowflake",0.004),prof="cave")
add("green_noise_caves",0.7,0.8,False,"173b29","2c6542","45b88a","237057","72bb68","4e914c",("spore_blossom_air",0.004),prof="cave")
add("mirror_grotto",0.2,0.0,False,"121a2d","293754","5d8bb0","2f4d70","687c88","4b5d70",("end_rod",0.004),prof="deep")
add("hanging_gardens",0.8,0.8,False,"244c35","408062","62c89a","2e795e","8ccf78","5fa95b",("spore_blossom_air",0.006),prof="cave",extra={"creature":cr(("bee",16,2,3))})
add("fairy_ring",0.8,0.7,False,"35284c","634a78","a883dc","543b82","9c9c78","787052",("warped_spore",0.006),prof="cave",extra={"creature":cr(("mooshroom",15,4,8))})
add("sunken_temple",0.7,0.6,False,"263746","476576","5db5b8","2d6672","779c72","557a59",("end_rod",0.003),prof="cave")
add("cracked_sanctuary",0.5,0.2,False,"2e2f30","4e5050","71888a","364a4e","757c62","5b624d",("ash",0.003),prof="cave")
add("lost_cistern",0.7,0.8,False,"1d3740","35616a","4fbdb4","276f73","689b6b","4c7957",("end_rod",0.003),prof="cave",extra={"creature":cr(("axolotl",14,4,4))})
assert len(B) == 174, len(B)


                                                                                                             
def mixc(a, b, t):
    ra, ga, ba = (a >> 16) & 255, (a >> 8) & 255, a & 255
    rb, gb, bb = (b >> 16) & 255, (b >> 8) & 255, b & 255
    return (int(ra + (rb - ra) * t) << 16) | (int(ga + (gb - ga) * t) << 8) | int(ba + (bb - ba) * t)

def dream(b, k):
    t = b["t"]
    if t < 0.35:   sky, grass, fol = 0xbfdcff, 0x8cc0a0, 0x6aa07a
    elif t < 0.75: sky, grass, fol = 0xcfc4ff, 0x91bd59, 0x77ab2f
    else:          sky, grass, fol = 0xffdcc8, 0xb4c450, 0x8fb030
    b["sky"] = mixc(b["sky"], sky, k)
    b["fog"] = mixc(b["fog"], 0xf4ecff, k * 0.9)
    b["g"] = mixc(b["g"], grass, k * 0.8)
    b["f"] = mixc(b["f"], fol, k * 0.8)
    b["w"] = mixc(b["w"], 0x4aa6e8, k * 0.7)
    b["wf"] = mixc(b["wf"], 0x2a6ac8, k * 0.5)

def pastel_lift(b):
    if b["t"] < 0.35:
        sky, fog, grass, fol = 0xc8dcff, 0xdce9f2, 0x9bc7a2, 0x78a987
    elif b["t"] < 0.75:
        sky, fog, grass, fol = 0xd6cfff, 0xe9e0f2, 0xb2c98a, 0x8eaa6c
    else:
        sky, fog, grass, fol = 0xffd9cf, 0xf5e4dc, 0xc5c887, 0xa2ac68
    strength = 0.10 if b["prof"] in ("gloom", "deep", "flesh", "backrooms") else 0.16
    b["sky"] = mixc(b["sky"], sky, strength)
    b["fog"] = mixc(b["fog"], fog, strength * 0.75)
    b["g"] = mixc(b["g"], grass, strength)
    b["f"] = mixc(b["f"], fol, strength)
    b["w"] = mixc(b["w"], 0x63b9df, strength * 0.65)
    b["wf"] = mixc(b["wf"], 0x377cae, strength * 0.55)

for (i, *_r) in S:
    dream(B[i], 0.42 if B[i]["prof"] == "gloom" else 0.75)
for i in ("still_lake", "deep_lake", "marsh_water", "cold_water"):
    dream(B[i], 0.65)
for b in B.values():
    pastel_lift(b)

def bj(b):
    eff = {"sky_color": b["sky"], "fog_color": b["fog"], "water_color": b["w"], "water_fog_color": b["wf"], "grass_color": b["g"], "foliage_color": b["f"]}
    if b["part"]: eff["particle"] = particle(*b["part"])
    out = {"has_precipitation": b["pr"], "temperature": b["t"], "downfall": b["d"], "effects": eff, "carvers": [], "features": [], "spawners": spawners(b["prof"], b["extra"]), "spawn_costs": {}}
    if b["prof"] == "surface": out["creature_spawn_probability"] = 0.45                                                    
    elif b["prof"] == "gloom": out["creature_spawn_probability"] = 0.15
    return out
for f in os.listdir(BIOME_DIR): os.remove(os.path.join(BIOME_DIR, f))
for i, b in B.items(): json.dump(bj(b), open(os.path.join(BIOME_DIR, i + ".json"), "w"), indent=2)
ids = ["aeroruins:" + i for i in B]
def tag(path, vals): json.dump({"replace": False, "values": vals}, open(os.path.join(TAG_DIR, path), "w"), indent=2)
sf = ["aeroruins:" + s[0] for s in S] + ["aeroruins:" + i for i, b in B.items() if b["prof"] == "water"]
cv = ["aeroruins:" + i for i, b in B.items() if b["prof"] in ("cave", "deep") and i not in ("mantle_core", "obsidian_rift")]
tag("is_overworld.json", ids); tag("is_jungle.json", [i for i in sf if "jungle" in i or "rainforest" in i])
for n in ["jungle_temple", "pillager_outpost", "ruined_portal_jungle", "ruined_portal_standard", "village_plains", "woodland_mansion"]: tag("has_structure/%s.json" % n, sf)
for n in ["mineshaft", "stronghold", "trial_chambers"]: tag("has_structure/%s.json" % n, sf + cv)
for n in ["ocean_ruin_warm", "shipwreck"]: tag("has_structure/%s.json" % n, ["aeroruins:" + i for i, b in B.items() if b["prof"] == "water"])
os.makedirs(os.path.join(ROOT, "tools"), exist_ok=True)
with open(os.path.join(ROOT, "tools", "biome_ids.json"), "w", encoding="utf-8") as handle:
    json.dump(sorted(B), handle, indent=2)

                                                                     
def flags(f): return set(x for x in f.split(",") if x)
rows = []
for (i, log, fl, fb, tree, alt, dens, fg, flora) in S:
    fs = flags(fg)
    rows.append('    %s("%s", Material.%s, %s, Biome.%s, TreeType.%s, TreeType.%s, %d%s%s%s%s%s%s)' % (
        i.upper(), i, log, fl, fb, tree, alt, dens,
        ", cold = true" if "cold" in fs else "", ", warm = true" if "warm" in fs else "",
        ", gloom = true" if "gloom" in fs else "", ", bare = true" if "bare" in fs else "", ", high = true" if "high" in fs else "",
        (", flora = %d" % flora) if flora != 6 else ""))
names = [s[0].upper() for s in S]
grid_names = names[:24]; high = names[24:26]; gloom = names[26:41]; garden = names[41:44]; flowers = names[44:48]; hood = names[48:50]
grid = ""
k = 0
for t in range(4):
    row = []
    for h in range(3):
        row.append("arrayOf(%s, %s)" % (grid_names[k], grid_names[k + 1])); k += 2
    grid += "            arrayOf(" + ", ".join(row) + "),\n"
kt = '''package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.TreeType
import org.bukkit.block.Biome
import org.bukkit.util.noise.SimplexOctaveGenerator
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

private val WILD = listOf(Material.DANDELION, Material.POPPY, Material.OXEYE_DAISY, Material.AZURE_BLUET)
private val WHITE = listOf(Material.OXEYE_DAISY, Material.AZURE_BLUET, Material.LILY_OF_THE_VALLEY)
private val DRY = listOf(Material.DANDELION, Material.DANDELION, Material.POPPY)
private val COLD = listOf(Material.AZURE_BLUET, Material.OXEYE_DAISY)
private val REDS = listOf(Material.POPPY, Material.POPPY, Material.RED_TULIP)
private val BLUES = listOf(Material.CORNFLOWER, Material.AZURE_BLUET, Material.ALLIUM)
private val YELLOWS = listOf(Material.DANDELION, Material.DANDELION, Material.OXEYE_DAISY)

enum class Climate(
    val id: String,
    val log: Material,
    val flowers: List<Material>,
    val fallback: Biome,
    val tree: TreeType,
    val alt: TreeType,
    val density: Int,
    val cold: Boolean = false,
    val warm: Boolean = false,
    val gloom: Boolean = false,
    val bare: Boolean = false,
    val high: Boolean = false,
    val flora: Int = 6
) {
''' + ",\n".join(rows) + ''';

    companion object {
        val GRID: Array<Array<Array<Climate>>> = arrayOf(
''' + grid.rstrip(",\n") + '''
        )
        val GLOOM: Array<Climate> = arrayOf(''' + ", ".join(gloom) + ''')
        val GARDEN: Array<Climate> = arrayOf(''' + ", ".join(garden) + ''')
        val FLOWERS: Array<Climate> = arrayOf(''' + ", ".join(flowers) + ''')
        val HOOD: Array<Climate> = arrayOf(''' + ", ".join(hood) + ''')
        val HIGH_WARM = ''' + high[0] + '''
        val HIGH_COLD = ''' + high[1] + '''
    }
}

object ClimateMap {
    private class Noises(seed: Long) {
        val t = SimplexOctaveGenerator(Random(seed xor 0x1F2E3D4C5B6AL), 2).also { it.setScale(0.0012) }
        val h = SimplexOctaveGenerator(Random(seed xor 0x2A3B4C5D6E7FL), 2).also { it.setScale(0.0012) }
        val v = SimplexOctaveGenerator(Random(seed xor 0x3C4D5E6F7081L), 2).also { it.setScale(0.0022) }
        val g = SimplexOctaveGenerator(Random(seed xor 0x4D5E6F708192L), 2).also { it.setScale(0.0009) }
    }

    private val noises = ConcurrentHashMap<Long, Noises>()

    private class Memo { var seed = 0L; var x = Int.MIN_VALUE; var z = Int.MIN_VALUE; var value: Climate = Climate.OLD_OAK_FOREST }
    private val memo = ThreadLocal.withInitial { Memo() }

    fun at(seed: Long, x: Int, z: Int): Climate {
        val m = memo.get()
        if (m.seed == seed && m.x == x && m.z == z) return m.value
        val value = compute(seed, x, z)
        m.seed = seed; m.x = x; m.z = z; m.value = value
        return value
    }

    private fun compute(seed: Long, x: Int, z: Int): Climate {
        val n = noises.computeIfAbsent(seed) { Noises(it) }
        val fx = x.toDouble()
        val fz = z.toDouble()
        val tv = n.t.noise(fx, fz, 0.5, 0.5, true)
        val hv = n.h.noise(fx, fz, 0.5, 0.5, true)
        val vv = n.v.noise(fx, fz, 0.5, 0.5, true)
        val gv = n.g.noise(fx, fz, 0.5, 0.5, true)

        if (Terrain.heightAt(seed, x, z) >= HIGH_Y) return if (tv > 0.0) Climate.HIGH_WARM else Climate.HIGH_COLD

        val tb = when {
            tv < -0.18 -> 0
            tv < 0.0 -> 1
            tv < 0.18 -> 2
            else -> 3
        }
        val hb = when {
            hv < -0.12 -> 0
            hv < 0.12 -> 1
            else -> 2
        }
        when (Realms.at(seed, x, z)) {
            Realm.GARDEN -> return Climate.GARDEN[(hb + (if (vv < 0.0) 0 else 1)) % Climate.GARDEN.size]
            Realm.FLOWERS -> return Climate.FLOWERS[tb % Climate.FLOWERS.size]
            Realm.NEIGHBORHOOD -> return Climate.HOOD[if (vv < 0.0) 0 else 1]
            else -> Unit
        }
        if (gv > 0.28) return Climate.GLOOM[(tb * 3 + hb) % Climate.GLOOM.size]
        return Climate.GRID[tb][hb][if (vv < 0.0) 0 else 1]
    }

    fun useVanilla(seed: Long, x: Int, z: Int): Boolean {
        if (Settings.vanillaMixChance <= 0) return false
        val regionX = Math.floorDiv(x, 256)
        val regionZ = Math.floorDiv(z, 256)
        return Math.floorMod(mix(seed, regionX, regionZ, 777), 100) < Settings.vanillaMixChance
    }
}
'''
open(KT, "w").write(kt)
print("biomes:", len(B), "surface:", len(S))
