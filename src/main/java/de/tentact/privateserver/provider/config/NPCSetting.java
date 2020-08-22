package de.tentact.privateserver.provider.config;
/*  Created in the IntelliJ IDEA.
    Copyright(c) 2020
    Created by 0utplay | Aldin Sijamhodzic
    Datum: 12.08.2020
    Uhrzeit: 17:28
*/

public class NPCSetting {

    private final boolean shouldImitatePlayer;
    private final boolean shouldLookAtPlayer;
    private final String skinValue;
    private NPCLocation npcLocation;

    /**
     * Settings of the spawned NPC
     * @param shouldImitatePlayer if the npc shouldImitatePlayer
     * @param shouldLookAtPlayer if the npc shouldLookAtPlayer
     * @param skinValue
     * @param npcLocation the npcLocation to spawn the NPC at
     */
    public NPCSetting(boolean shouldImitatePlayer, boolean shouldLookAtPlayer, String skinValue, NPCLocation npcLocation) {
        this.shouldImitatePlayer = shouldImitatePlayer;
        this.shouldLookAtPlayer = shouldLookAtPlayer;
        this.skinValue = skinValue;
        this.npcLocation = npcLocation;
    }

    public boolean isImitatePlayer() {
        return this.shouldImitatePlayer;
    }

    public boolean isLookAtPlayer() {
        return this.shouldLookAtPlayer;
    }

    public String getSkinValue() {
        return this.skinValue;
    }

    public NPCLocation getNPCLocation() {
        return this.npcLocation;
    }

    public NPCSetting setNPCLocation(NPCLocation npcLocation) {
        this.npcLocation = npcLocation;
        return this;
    }
}
