package br.com.muxe.minigame.bedwars.listener;

import br.com.muxe.core.bukkit.api.MuxeCore;
import br.com.muxe.core.bukkit.cosmetic.animation.Vec;
import br.com.muxe.core.bukkit.nms.Fireballs;
import br.com.muxe.core.bukkit.text.ActionBar;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.api.listener.GameHandlers;
import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.runtime.MatchRuntime;
import br.com.muxe.minigame.api.runtime.Playing;
import br.com.muxe.minigame.bedwars.game.Activity;
import br.com.muxe.minigame.bedwars.game.ChestDeposits;
import br.com.muxe.minigame.bedwars.game.Cooldowns;
import br.com.muxe.minigame.bedwars.custom.CustomRules;
import br.com.muxe.minigame.bedwars.game.ExplosionKnockback.Source;
import br.com.muxe.minigame.bedwars.game.ExplosionKnockback;
import br.com.muxe.minigame.bedwars.game.Foliage;
import br.com.muxe.minigame.bedwars.game.Slingshot;
import br.com.muxe.minigame.bedwars.item.ItemNames;
import br.com.muxe.minigame.bedwars.preferences.PlayerPreferences;
import br.com.muxe.minigame.bedwars.room.BedWarsMatch;
import br.com.muxe.minigame.bedwars.room.ForgePickup;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings;
import br.com.muxe.minigame.bedwars.settings.MatchSounds;
import br.com.muxe.minigame.bedwars.tnt.TntTimers;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class BedWarsHandlers implements GameHandlers<BedWarsMatch> {

    private static final long SLINGSHOT_BAR_TICKS = 2L;

    private final Plugin plugin;
    private final MuxeCore core;
    private final BedWarsSettings settings;
    private final Localized<BedWarsSettings> localized;
    private final MessageCatalog messages;
    private final Bedbugs bedbugs;
    private final PlayerPreferences preferences;
    private final TntTimers tntTimers;
    private final Activity activity;
    private final Slingshot slingshot;
    private final Consumer<Player> openMenu;
    private final BiConsumer<Player, String> playAgain;
    private final Map<UUID, UUID> explosives = new HashMap<>();

    public BedWarsHandlers(Plugin plugin, MuxeCore core, Localized<BedWarsSettings> localized, MessageCatalog messages, Bedbugs bedbugs, PlayerPreferences preferences,
                           TntTimers tntTimers, Activity activity, Slingshot slingshot, Consumer<Player> openMenu, BiConsumer<Player, String> playAgain) {
        this.plugin = plugin;
        this.core = core;
        this.settings = localized.standard();
        this.localized = localized;
        this.messages = messages;
        this.bedbugs = bedbugs;
        this.preferences = preferences;
        this.tntTimers = tntTimers;
        this.activity = activity;
        this.slingshot = slingshot;
        this.openMenu = openMenu;
        this.playAgain = playAgain;
    }

    @Override
    public void openMenu(Player player) {
        openMenu.accept(player);
    }

    @Override
    public void playAgain(Player player, String mode) {
        playAgain.accept(player, mode);
    }

    @Override
    public void interact(Playing<BedWarsMatch> playing, PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BedWarsMatch state = playing.state();
        ItemStack item = event.getItem();
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                state.breakBed(player, event.getClickedBlock());
            }
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == Material.ENDER_CHEST && !(player.isSneaking() && item != null)) {
            event.setCancelled(true);
            player.openInventory(state.enderChest(player));
            MatchSounds.play(settings.sounds().enderChest(), player);
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && item != null && ChestDeposits.isChest(event.getClickedBlock().getType())
            && preferences.of(player.getUniqueId()).chestDeposit().deposits()) {
            event.setCancelled(true);
            deposit(player, state, event.getClickedBlock(), item);
            return;
        }
        if (item == null) {
            return;
        }
        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (rightClick && item.getType() == Material.FIREBALL) {
            event.setCancelled(true);
            consumeOne(player);
            Fireball fireball = player.launchProjectile(Fireball.class);
            Fireballs.accelerate(fireball, player.getLocation().getDirection(), ExplosionKnockback.FIREBALL_ACCELERATION);
            fireball.setIsIncendiary(false);
            fireball.setYield(ExplosionKnockback.FIREBALL_YIELD);
            explosives.put(fireball.getUniqueId(), player.getUniqueId());
            state.room().player(player.getUniqueId()).useExplosive();
        } else if (rightClick && item.getType() == Material.MAGMA_CREAM) {
            event.setCancelled(true);
            sling(player, state);
        }
    }

    @Override
    public void place(Playing<BedWarsMatch> playing, BlockPlaceEvent event) {
        Player player = event.getPlayer();
        BedWarsMatch state = playing.state();
        Block block = event.getBlockPlaced();
        if (!state.mayPlace(block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(messages.get(player, "cannot-place"));
            return;
        }
        if (block.getType() == Material.CHEST) {
            event.setCancelled(true);
            placeTower(player, state, block);
            return;
        }
        if (block.getType() == Material.TNT) {
            block.setType(Material.AIR, false);
            TNTPrimed tnt = block.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5), TNTPrimed.class);
            tnt.setFuseTicks(ExplosionKnockback.TNT_FUSE_TICKS);
            tnt.setYield(ExplosionKnockback.TNT_YIELD);
            tntTimers.track(tnt);
            explosives.put(tnt.getUniqueId(), player.getUniqueId());
            state.room().player(player.getUniqueId()).useExplosive();
            return;
        }
        if (block.getType() == Material.ENDER_STONE) {
            state.endStonePlaced(player, block);
        }
    }

    private void placeTower(Player player, BedWarsMatch state, Block anchor) {
        Optional<Duration> wait = state.towerCooldown(player.getUniqueId());
        if (wait.isPresent()) {
            player.sendMessage(messages.get(player, "tower-cooldown", "time", Cooldowns.seconds(wait.get(), messages.get(player, "decimal-separator"))));
            return;
        }
        ItemStack hand = player.getItemInHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }
        state.buildTower(player, anchor);
    }

    @Override
    public void breakBlock(Playing<BedWarsMatch> playing, BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.BED_BLOCK || block.getType() == Material.DRAGON_EGG && playing.state().bedOwner(block).isPresent()) {
            event.setCancelled(true);
            playing.state().breakBed(event.getPlayer(), block);
            return;
        }
        if (Foliage.isFoliage(block.getType()) && !playing.runtime().match().wasPlaced(block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
            clearFoliage(block);
        }
    }

    @Override
    public boolean explodes(Block block) {
        return block.getType() != Material.GLASS && block.getType() != Material.STAINED_GLASS;
    }

    @Override
    public void exploded(Optional<MatchRuntime<BedWarsMatch>> runtime, EntityExplodeEvent event) {
        UUID exploded = event.getEntity().getUniqueId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> explosives.remove(exploded), 1L);
        runtime.ifPresent(found -> Optional.ofNullable(explosives.get(exploded))
            .filter(owner -> found.match().player(owner).isPresent())
            .ifPresent(owner -> found.state().room().player(owner).addExplosiveBlocks(event.blockList().size())));
    }

    @Override
    public Optional<UUID> attacker(Entity damager) {
        Optional<Bedbugs.Bug> bug = bedbugs.bug(damager);
        if (bug.isPresent()) {
            return Optional.ofNullable(plugin.getServer().getPlayer(bug.get().owner())).map(Player::getUniqueId);
        }
        if (damager instanceof TNTPrimed || damager instanceof Fireball) {
            UUID owner = explosives.get(damager.getUniqueId());
            return owner == null ? Optional.empty() : Optional.ofNullable(plugin.getServer().getPlayer(owner)).map(Player::getUniqueId);
        }
        return Optional.empty();
    }

    @Override
    public boolean selfDamage(Playing<BedWarsMatch> victim, Entity damager, EntityDamageEvent event) {
        if (damager instanceof Fireball || damager instanceof TNTPrimed) {
            launch((Player) event.getEntity(), damager, victim.state().rules().fireballFight());
            return true;
        }
        return false;
    }

    @Override
    public void damaged(Playing<BedWarsMatch> victim, Optional<Participant> attacker, EntityDamageByEntityEvent event) {
        UUID victimId = victim.participant().id();
        attacker.filter(other -> !other.id().equals(victimId) && event.getDamager() instanceof Fireball)
            .ifPresent(other -> victim.state().room().player(other.id()).addFireballDamage(event.getFinalDamage()));
        bedbugs.bug(event.getDamager()).ifPresent(bug -> victim.state().bedbugHit(victimId, bug.team()));
        if (event.getDamager() instanceof Fireball && victim.state().rules().fireballFight() && event.getEntity() instanceof Player hit) {
            event.setDamage(event.getDamage() * CustomRules.FIGHT_DAMAGE);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Vector velocity = hit.getVelocity();
                if (hit.isOnline() && velocity.getY() > 0) {
                    hit.setVelocity(velocity.setY(velocity.getY() * CustomRules.FIGHT_LIFT));
                }
            });
        }
    }

    @Override
    public void pickup(Playing<BedWarsMatch> playing, PlayerPickupItemEvent event) {
        Item item = event.getItem();
        if (!item.hasMetadata(ForgePickup.FORGE_ITEM)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Instant now = core.clock().instant();
        if (activity.isAfk(player.getUniqueId(), now)) {
            return;
        }
        Participant picker = playing.participant();
        Match match = playing.runtime().match();
        Material type = item.getItemStack().getType();
        int amount = 0;
        for (Item same : ForgePickup.gather(item)) {
            amount += same.getItemStack().getAmount();
            same.remove();
        }
        give(player, playing.state(), type, amount);
        if (!ForgePickup.splits(type)) {
            return;
        }
        double radius = settings.forges().splitRadius();
        for (Entity nearby : player.getNearbyEntities(radius, radius, ForgePickup.SPLIT_HEIGHT)) {
            if (nearby instanceof Player mate && !mate.equals(player)) {
                int shared = amount;
                match.player(mate.getUniqueId())
                    .filter(other -> other.team() == picker.team() && other.isAlive() && !activity.isAfk(other.id(), now))
                    .ifPresent(other -> give(mate, playing.state(), type, shared));
            }
        }
    }

    boolean mayStore(Player player, ItemStack item, ItemStack cursor) {
        List<Material> carried = new ArrayList<>();
        for (ItemStack held : player.getInventory().getContents()) {
            if (held != null) {
                carried.add(held.getType());
            }
        }
        if (cursor != null) {
            carried.add(cursor.getType());
        }
        switch (ChestDeposits.verdict(item.getType(), carried)) {
            case KEPT -> player.sendMessage(messages.get(player, "chest-kept"));
            case LAST_SWORD -> player.sendMessage(messages.get(player, "chest-last-one"));
            case STORE, NOTHING -> {
                return true;
            }
        }
        return false;
    }

    private void deposit(Player player, BedWarsMatch state, Block block, ItemStack item) {
        if (item.getType() == Material.AIR || !mayStore(player, item, null)) {
            return;
        }
        Inventory target;
        if (block.getType() == Material.ENDER_CHEST) {
            target = state.enderChest(player);
        } else if (state.mayOpenChest(player, block.getLocation().add(0.5, 0, 0.5)) && block.getState() instanceof Chest chest) {
            target = chest.getInventory();
        } else {
            return;
        }
        int held = item.getAmount();
        int kept = target.addItem(item.clone()).values().stream().mapToInt(ItemStack::getAmount).sum();
        int stored = held - kept;
        if (stored <= 0) {
            player.sendMessage(messages.get(player, "chest-full"));
            return;
        }
        String name = names(player).display(item);
        if (kept == 0) {
            player.setItemInHand(null);
        } else {
            item.setAmount(kept);
            player.setItemInHand(item);
        }
        player.sendMessage(messages.get(player, "chest-deposited", "item", name, "amount", String.valueOf(stored)));
        MatchSounds.play(settings.sounds().deposited(), player);
    }

    private void give(Player player, BedWarsMatch state, Material type, int amount) {
        if (amount <= 0) {
            return;
        }
        player.getInventory().addItem(names(player).named(new ItemStack(type, amount))).values()
            .forEach(rest -> player.getWorld().dropItem(player.getLocation(), rest));
        player.playSound(player.getLocation(), Sound.ITEM_PICKUP, ForgePickup.SOUND_VOLUME, ForgePickup.SOUND_PITCH);
        if (type == Material.EMERALD) {
            state.room().player(player.getUniqueId()).addEmeralds(amount);
        }
    }

    private void sling(Player player, BedWarsMatch state) {
        Duration cooldown = state.rules().slingshotCooldown(Slingshot.COOLDOWN);
        if (!slingshot.use(player.getUniqueId(), core.clock().instant(), cooldown)) {
            return;
        }
        consumeOne(player);
        Vector look = player.getLocation().getDirection();
        Vec push = Slingshot.push(new Vec(look.getX(), look.getY(), look.getZ()));
        player.setVelocity(new Vector(push.x(), push.y(), push.z()));
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Slingshot.SLIME_BLOCK_ID);
        for (int index = 0; index < Slingshot.SOUND_PITCHES.size(); index++) {
            float pitch = Slingshot.SOUND_PITCHES.get(index);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.getWorld().playSound(player.getLocation(), Sound.SLIME_WALK2, 1, pitch);
                }
            }, (long) index * Slingshot.SOUND_GAP_TICKS);
        }
        String label = messages.get(player, "slingshot-label");
        String decimalSeparator = messages.get(player, "decimal-separator");
        new BukkitRunnable() {
            @Override
            public void run() {
                Optional<Duration> left = slingshot.remaining(player.getUniqueId(), core.clock().instant());
                if (!player.isOnline() || left.isEmpty()) {
                    cancel();
                    return;
                }
                ActionBar.send(player, Slingshot.actionBar(label, decimalSeparator, left.get(), cooldown));
            }
        }.runTaskTimer(plugin, 0L, SLINGSHOT_BAR_TICKS);
    }

    private ItemNames names(Player player) {
        return localized.in(messages.language(player)).items();
    }

    @SuppressWarnings("deprecation")
    private static void launch(Player player, Entity explosive, boolean fight) {
        Location blast = explosive.getLocation();
        Location feet = player.getLocation();
        Vec push = ExplosionKnockback.of(explosive instanceof Fireball ? Source.FIREBALL : Source.TNT,
            new Vec(blast.getX(), blast.getY(), blast.getZ()), new Vec(feet.getX(), feet.getY(), feet.getZ()), player.isOnGround());
        double lift = fight && explosive instanceof Fireball ? push.y() * CustomRules.FIGHT_LIFT : push.y();
        player.setVelocity(new Vector(push.x(), lift, push.z()));
        player.playEffect(EntityEffect.HURT);
    }

    private static void clearFoliage(Block block) {
        if (Foliage.isTall(block.getType())) {
            for (BlockFace face : List.of(BlockFace.UP, BlockFace.DOWN)) {
                Block half = block.getRelative(face);
                if (Foliage.isTall(half.getType())) {
                    half.setType(Material.AIR, false);
                }
            }
        }
        block.setType(Material.AIR, false);
    }

    private static void consumeOne(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand.getAmount() <= 1) {
            player.setItemInHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }
}
