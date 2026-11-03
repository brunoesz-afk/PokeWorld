# World Navigation and Portal System Implementation Plan

This plan outlines the restructuring of the world map system to follow the hierarchical design provided (City as the hub with connected regions).

## Proposed Changes

### [Component] Map Connectivity Design
The world will follow a compass-based navigation system centered around the **CITY**:
- **North**: CITY ↔ MOUNTAIN ↔ SKY TEMPLE
- **South**: CITY ↔ MINE ↔ DESERT ↔ VOLCANO
- **West**: CITY ↔ FOREST
- **East**: CITY ↔ RIO ↔ OCEAN
- **Special**: CITY Center ↔ DUNGEON, CITY Bottom-Right ↔ ARENA

---

### [Component] TileMap Logic

#### [MODIFY] [TileMap.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/TileMap.java)
Update map generators to place portals at consistent compass points:
- **TOP**: `(0, cols/2)`
- **BOTTOM**: `(rows-1, cols/2)`
- **LEFT**: `(rows/2, 0)`
- **RIGHT**: `(rows/2, cols-1)`

Specific Generator Updates:
- `generateCityMap`: Portals at Top, Bottom, Left, Right, Center, and Bottom-Right.
- `generateForestMap`: Portal at Right (to City).
- `generateRioMap`: Portals at Left (to City) and Right (to Ocean).
- `generateOceanMap`: Portal at Left (to Rio).
- `generateMountainMap`: Portals at Bottom (to City) and Top (to Sky Temple).
- `generateMineMap`: Portals at Top (to City) and Bottom (to Desert).
- `generateDesertMap`: Portals at Top (to Mine) and Bottom (to Volcano).
- `generateVolcanoMap`: Portal at Top (to Desert).
- `generateSkyTempleMap`: Portal at Bottom (to Mountain).

---

### [Component] World Management Logic

#### [MODIFY] [WorldManager.java](file:///C:/Users/Bruno/Desktop/AndroidStudioProjects/PokeWorld/app/src/main/java/com/example/pokeworld/WorldManager.java)
- **`loadMap`**: Add a `fromPortalSide` parameter to correctly position the player when entering a new map (e.g., if entering from the North, spawn at the Top portal).
- **`handlePortal`**: Implement logic to detect which specific portal was touched based on tile coordinates and trigger the correct map transition.
- **Enemy/NPC Spawns**: Add spawn definitions for the new maps (Mountain, Desert, etc.) so they aren't empty.

## Verification Plan

### Manual Verification
1. Start in **CITY**.
2. Walk West to **FOREST** and check if spawn is on the right side.
3. Walk East from **FOREST** back to **CITY**.
4. Test North/South routes to **MOUNTAIN** and **MINE**.
5. Verify special portals like the **DUNGEON** in the center of the city.
6. Check that all new regions (Desert, Volcano, Ocean) are reachable.
