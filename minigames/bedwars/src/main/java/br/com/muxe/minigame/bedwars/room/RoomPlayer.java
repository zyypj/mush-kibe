package br.com.muxe.minigame.bedwars.room;

import java.util.UUID;

public final class RoomPlayer {

    private final UUID id;
    private int beds;
    private int bedsLost;
    private int armorTier;
    private int pickaxeTier;
    private int axeTier;
    private boolean shears;
    private int invisibleBeds;
    private int endStones;
    private boolean usedExplosives;
    private int bedbugKills;
    private int emeraldsThisLife;
    private int bestEmeralds;
    private double fireballDamage;
    private int explosiveBlocks;
    private int clayBought;
    private int blocksBought;
    private int bridgeEggs;
    private int diamonds;
    private double bowDamage;
    private boolean milkCleansed;

    public RoomPlayer(UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }

    public int beds() {
        return beds;
    }

    void addBed() {
        beds++;
    }

    public int bedsLost() {
        return bedsLost;
    }

    void loseBed() {
        bedsLost = 1;
    }

    public int invisibleBeds() {
        return invisibleBeds;
    }

    public void addInvisibleBed() {
        invisibleBeds++;
    }

    public int endStones() {
        return endStones;
    }

    public void addEndStone() {
        endStones++;
    }

    public boolean usedExplosives() {
        return usedExplosives;
    }

    public void useExplosive() {
        usedExplosives = true;
    }

    public int bedbugKills() {
        return bedbugKills;
    }

    public void addBedbugKill() {
        bedbugKills++;
    }

    public int bestEmeralds() {
        return bestEmeralds;
    }

    public void addEmeralds(int amount) {
        emeraldsThisLife += amount;
        bestEmeralds = Math.max(bestEmeralds, emeraldsThisLife);
    }

    public double fireballDamage() {
        return fireballDamage;
    }

    public void addFireballDamage(double amount) {
        fireballDamage += Math.max(0, amount);
    }

    public int explosiveBlocks() {
        return explosiveBlocks;
    }

    public void addExplosiveBlocks(int amount) {
        explosiveBlocks += amount;
    }

    public int clayBought() {
        return clayBought;
    }

    public void addClay(int amount) {
        clayBought += amount;
    }

    public int blocksBought() {
        return blocksBought;
    }

    public void addBlocks(int amount) {
        blocksBought += Math.max(0, amount);
    }

    public int bridgeEggs() {
        return bridgeEggs;
    }

    public void addBridgeEgg() {
        bridgeEggs++;
    }

    public int diamonds() {
        return diamonds;
    }

    public void addDiamonds(int amount) {
        diamonds += Math.max(0, amount);
    }

    public double bowDamage() {
        return bowDamage;
    }

    public void addBowDamage(double amount) {
        bowDamage += Math.max(0, amount);
    }

    public boolean milkCleansed() {
        return milkCleansed;
    }

    public void cleanseWithMilk() {
        milkCleansed = true;
    }

    public int armorTier() {
        return armorTier;
    }

    public void armorTier(int tier) {
        this.armorTier = Math.max(armorTier, tier);
    }

    public int pickaxeTier() {
        return pickaxeTier;
    }

    public void pickaxeTier(int tier) {
        this.pickaxeTier = tier;
    }

    public int axeTier() {
        return axeTier;
    }

    public void axeTier(int tier) {
        this.axeTier = tier;
    }

    public boolean hasShears() {
        return shears;
    }

    public void giveShears() {
        this.shears = true;
    }

    void died() {
        emeraldsThisLife = 0;
        pickaxeTier = Math.max(pickaxeTier > 0 ? 1 : 0, pickaxeTier - 1);
        axeTier = Math.max(axeTier > 0 ? 1 : 0, axeTier - 1);
    }
}
