package iamhardliner.megaplugin

import hazae41.minecraft.kotlin.bukkit.*
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap
import java.util.concurrent.TimeUnit


data class Flower(val name: String, val lore: Optional<List<String>>, val enchantment: Optional<Enchantment>) {
    constructor(name: String) : this("Flower of \"$name\"", Optional.empty(), Optional.empty())
    constructor(name: String, lore: List<String>) : this("Flower of \"$name\"", Optional.of(lore), Optional.empty())
    constructor(name: String, enchantment: Enchantment) : this(
        "Flower of \"$name\"",
        Optional.empty(),
        Optional.of(enchantment)
    )

    constructor(name: String, lore: List<String>, enchantment: Enchantment) : this(
        "Flower of \"$name\"",
        Optional.of(lore),
        Optional.of(enchantment)
    )
}

class Plugin : BukkitPlugin() {
    private var lastDeath: HashMap<Player, EntityDamageEvent.DamageCause> =
        HashMap<Player, EntityDamageEvent.DamageCause>()

    private fun getFlower(cause: EntityDamageEvent.DamageCause): Flower {
        return when (cause) {
            EntityDamageEvent.DamageCause.FALL -> Flower("Gravity", Enchantment.PROTECTION_FALL)
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION -> Flower("Explosions", Enchantment.PROTECTION_EXPLOSIONS)
            EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.FIRE -> Flower("Burning", Enchantment.PROTECTION_FIRE)
            EntityDamageEvent.DamageCause.WITHER -> Flower("The Wither")
            EntityDamageEvent.DamageCause.ENTITY_ATTACK -> Flower("Defense", Enchantment.DAMAGE_ALL)
            EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK -> Flower("Sweeping Defense", Enchantment.DAMAGE_ALL)
            EntityDamageEvent.DamageCause.FLY_INTO_WALL -> Flower("Flying Into Walls")
            EntityDamageEvent.DamageCause.CUSTOM -> Flower("WTF just hit me?")
            EntityDamageEvent.DamageCause.VOID -> Flower("The Void")
            EntityDamageEvent.DamageCause.PROJECTILE -> Flower("Projectiles", Enchantment.PROTECTION_PROJECTILE)
            EntityDamageEvent.DamageCause.SUICIDE -> Flower("Suicide", List(2) {
                when (it) {
                    0 -> "This flower has fire aspect, to warm your heart"
                    else -> "in order to prevent something like this happening again."
                }
            }, Enchantment.FIRE_ASPECT)
            else -> {
                Flower("$cause".toLowerCase().split("_").map {
                    it.capitalize()
                }.joinToString(" ") { it })
            }
        }
    }

    override fun onEnable() {
        enableFlowers()
//        enableRecipes()
        enableTempDeop()
        enableRandomEnchants()
//        enableTest()
    }

    private val random = Random()

    private fun randomEnchant(p: Player?) {
        try {
            if (server.onlinePlayers.isEmpty()) {
                return
            }

            val player = p ?: server.onlinePlayers.random()

            val items = player.inventory.contents.filter { item -> item != null && item.enchantments.isEmpty() }
            if (items.isEmpty()) {
                return
            }

            val item = items.random()
            val itemIndex = player.inventory.indexOf(item)

            val enchantments = Enchantment.values().filter { enchantment -> !enchantment.canEnchantItem(item) }
            if (enchantments.isEmpty()) {
                return
            }

            val enchantment = enchantments.random()

            val imd = item.itemMeta!!
            imd.addEnchant(enchantment, 1, false)
            item.itemMeta = imd

            player.inventory.setItem(itemIndex, item)
            player.msg("You got lucky!")
        } finally {
            if (p == null) {
                val nextEnchant = 30 + random.nextInt(60)
                info("Next enchant in $nextEnchant minutes!")
                schedule(false, nextEnchant.toLong(), null, TimeUnit.MINUTES) { randomEnchant(null) }
            }
        }
    }

    private fun enableRandomEnchants() {
        info("Next enchant in 30 minutes!")

        schedule(false, 30, null, TimeUnit.MINUTES) { randomEnchant(null) }

        command("randomenchant", null, "renchant") { sender, args ->
            try {
                if (sender.isOp) {
                    if (args.size == 1) {
                        var other = server.getPlayer(args[0])
                        if (other != null) {
                            sender.msg("Enchanting player ${other.name}")
                            randomEnchant(other)
                        } else {
                            sender.msg("Could not find player: ${args[0]}")
                        }
                    } else {
                        sender.msg("Please enter a player!")
                    }
                } else {
                    sender.msg("You are not allowed to use this command!")
                }
            } catch (e: Exception) {
                sender.msg(e)
            }
        }
    }

//    private fun enableTest() {
//        command("abc") { sender, args ->
//            try {
//                AnvilGUI.Builder()
//                    .onClose { it.sendMessage("You closed the inventory.") }
//                    .onComplete { player, text ->
//                        //called when the inventory output slot is clicked
//                        if (text.equals("you", true)) {
//                            player.sendMessage("You have magical powers!");
//                            return@onComplete AnvilGUI.Response.close()
//                        } else {
//                            return@onComplete AnvilGUI.Response.text("Incorrect.")
//                        }
//                    }
//                    .text("50 per item. How much?")  //sets the text the GUI should start with
//                    .plugin(this@Plugin)              //set the plugin instance
//                    .open(sender as Player)                       //opens the GUI for the player provided
//            } catch (ex: Exception) {
//
//            }
//        }
//    }

    private fun enableTempDeop() {
        command("tempdeop", null, "tdeop") { sender, args ->
            try {
                val player = sender as Player
                if (player.isOp) {
                    val time = if (args.size == 1) {
                        args[0].toLongOrNull() ?: 30
                    } else {
                        30
                    }
                    player.msg("Deoping for ${time} Seconds!")
                    player.isOp = false
                    val gameMode = player.gameMode
                    player.msg("Setting gamemode to survival")
                    player.gameMode = GameMode.SURVIVAL
                    schedule(false, time, null, TimeUnit.SECONDS) {
                        player.msg("Re-oping player")
                        player.isOp = true
                        player.msg("Restoring gamemode")
                        player.gameMode = gameMode
                    }
                } else {
                    player.msg("You are not allowed to use this command!")
                }
            } catch (e: Exception) {
            }
        }
    }

//    private fun enableRecipes() {
//        schedule(false, 10, null, TimeUnit.SECONDS) {
//            logger.log(Level.INFO, "test9")
//        }
//        // Our custom variable which we will be changing around.
//        val item = ItemStack(Material.DIAMOND_SWORD)
//
//        // The meta of the diamond sword where we can change the name, and properties of the item.
//        val meta = item.itemMeta
//
//        // We will initialise the next variable after changing the properties of the sword
//
//        // This sets the name of the item.
//        meta.displayName = ChatColor.GREEN + "Emerald Sword"
//
//        // Set the meta of the sword to the edited meta.
//        item.itemMeta = meta
//
//        // Add the custom enchantment to make the emerald sword special
//        // In this case, we're adding the permission that modifies the damage value on level 5
//        // Level 5 is represented by the second parameter. You can change this to anything compatible with a sword
//        item.addEnchantment(Enchantment.DAMAGE_ALL, 5)
//
//
//        // create a NamespacedKey for your recipe
//        val key = NamespacedKey(this, "emerald_sword")
//
//        // Create our custom recipe variable
//        val recipe = ShapedRecipe(key, item)
//
//        // Here we will set the places. E and S can represent anything, and the letters can be anything. Beware; this is case sensitive.
//        recipe.shape(" E ", " E ", " S ")
//
//        // Set what the letters represent.
//        // E = Emerald, S = Stick
//        recipe.setIngredient('E', Material.EMERALD)
//        recipe.setIngredient('S', Material.STICK)
//
//        // Finally, add the recipe to the bukkit recipes
//        Bukkit.addRecipe(recipe)
//    }

    private fun enableFlowers() {
        listen<PlayerDeathEvent> {
            if (it.entityType == EntityType.PLAYER) {
                val entity = it.entity
                val lastDamageCause = entity.lastDamageCause
                if (lastDamageCause != null) {
                    lastDeath[entity] = lastDamageCause.cause
                }
            }
        }
        listen<PlayerRespawnEvent> { it ->
            if (lastDeath.containsKey(it.player)) {
                val cause = lastDeath[it.player]
                if (cause != null) {
                    val itemStack = ItemStack(Material.DANDELION, 1)

                    val imd = itemStack.itemMeta!!
                    imd.isUnbreakable = true
                    imd.addEnchant(Enchantment.VANISHING_CURSE, 1, false)

                    val flower = getFlower(cause)
                    imd.setDisplayName(flower.name)
                    var lore = List<String>(0) { "" }.toSet()
                    if (flower.lore.isPresent) {
                        lore = lore.union(flower.lore.get())
                    }
                    lore = lore.union(List(2) { i ->
                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd / HH:mm")
                        val formatted = current.format(formatter)
                        when (i) {
                            0 -> ""
                            else -> "Owner: \"${it.player.name}\", Date: \"$formatted\""
                        }
                    })
                    imd.lore = lore.toList()

                    if (flower.enchantment.isPresent) {
                        imd.addEnchant(flower.enchantment.get(), 1, false)
                    }

                    itemStack.itemMeta = imd

                    it.player.inventory.addItem(itemStack)
                    lastDeath.remove(it.player)
                }
            }
        }
    }
}

private operator fun ChatColor.plus(s: String): String {
    return char + s
}
