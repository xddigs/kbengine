package org.kbeng.service;

import org.kbeng.data.*;
import org.kbeng.entity.Player;
import org.kbeng.item.*;
import org.kbeng.utils.Local;
import org.kbeng.utils.ToastFactory;
import org.kbeng.wrld.GameMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents the library component of the Isofarm runtime.
 *
 * <p>This type centralizes the state, lifecycle and behavior required by its callers,
 * keeping domain rules together with the data they operate on.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li><b>State:</b> Owns the data needed to represent the component consistently.</li>
 *   <li><b>Lifecycle:</b> Exposes the operations used to create, update and release its state.</li>
 *   <li><b>Integration:</b> Coordinates with the surrounding game, rendering or UI systems through its public API.</li>
 * </ul>
 *
 * <p>Callers should use the documented public operations and allow this type to preserve
 * its invariants rather than modifying implementation details directly.
 */
public class Library implements Service<GameMaster> {
    private static final Logger log = LoggerFactory.getLogger(Library.class);
    private static final String NAMESPACE = "kbengine";
    private static final String SEPARATOR = ":";
    public static final String NAMESPACE_ID = NAMESPACE + SEPARATOR;

    /**
     * Initializes the items.
     * @param itemR the {@link ItemRegistry} supplied as {@code itemR}
     */
    public static void initItems(ItemRegistry itemR) {
        registerDefault(itemR, Backpack::new);
        registerDefault(itemR, CraftingBook::new);
        registerDefault(itemR, () -> new Book(false));
        registerDefault(itemR, () -> new Bucket(BlockData.AIR));
        registerDefault(itemR, () -> new Bucket(BlockData.WATER));
        registerDefault(itemR, () -> new Bucket(BlockData.LAVA));
        registerDefault(itemR, Wallet::new);

        MaterialID.forEach(material -> {
            if (material.equals(MaterialID.INGOT) || material.equals(MaterialID.RAW_ORE)) return;
            registerDefault(itemR, () -> new Material(material));
        });

        Tier.forEach(tier -> {
            if (tier.isInvalidTier() || tier.equals(Tier.STONE)) return;
            registerDefault(itemR, () -> new MiningComponent(tier, MaterialID.RAW_ORE));
            registerDefault(itemR, () -> new MiningComponent(tier, MaterialID.INGOT));
        });

        Tier.forEach(tier -> {
            if (tier.equals(Tier.NONE) || tier.equals(Tier.LEATHER)) return;
            registerDefault(itemR, () -> new Sword(tier));
            registerDefault(itemR, () -> new Pickaxe(tier));
            registerDefault(itemR, () -> new Axe(tier));
            registerDefault(itemR, () -> new Hoe(tier));
            registerDefault(itemR, () -> new Shovel(tier));
            registerDefault(itemR, () -> new Shield(tier));
        });

        ArmorData.forEach(armorType -> Tier.forEach(tier -> {
            if (tier == Tier.NONE || tier == Tier.WOODEN) return;
            registerDefault(itemR, () -> createArmor(armorType, tier));
        }));
        
        FoodData.forEach(foodData -> registerDefault(itemR, () -> new Food(foodData)));

        CropType.forEach(type -> {
            registerDefault(itemR, () -> new Produce(type));
            if (type.equals(CropType.SUGAR_CANE_CROP)) return;
            registerDefault(itemR, () -> new Seed(type));
        });

        BlockData.forEach(block -> {
            if (block.equals(BlockData.WATER) || block.equals(BlockData.LAVA)) return;
            if (block.getId() > 0 && !block.isInteractive()) {
                registerDefault(itemR, () -> new Block(block));
            }
            if (block.isInteractive()) {
                registerDefault(itemR, () -> new iBlock(block));
            }
        });
    }

    /**
     * Adds default to the corresponding collection or processing queue.
     * @param itemR the {@link ItemRegistry} supplied as {@code itemR}
     * @param supplier the {@link Supplier} supplied as {@code supplier}
     */
    private static void registerDefault(ItemRegistry itemR, Supplier<Item> supplier) {
        Item item = supplier.get();
        String rawName = item.getName();
        if (item instanceof Tool tool && tool.getTier() != null && tool.getTier() != Tier.NONE) {
            rawName = tool.getTier().getName() + " " + rawName;
        } else if (item instanceof Armor armor) {
            rawName = armor.getTier().getName() + " " + rawName;
        }

        itemR.register(getFormattedName(rawName), supplier);
    }

    /** Creates the concrete armor item associated with the supplied data value. */
    private static Armor createArmor(ArmorData armorType, Tier tier) {
        return switch (armorType) {
            case HELMET -> new Helmet(tier);
            case CHESTPLATE -> new Chestplate(tier);
            case BOOTS -> new Boots(tier);
        };
    }

    /**
     * Initializes the commands.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    public static void initCommands(GameMaster gameMaster) {
        Player player = Player.plyr;
        CommandRegistry cr = gameMaster.getCommandRegistry();
        ItemRegistry ir = gameMaster.getItemRegistry();
        WeatherService weatherService = WeatherService.wes;

        cr.register(new Command("/give", new CommandArgument[]{dynamic("item", ir::getIds),
                new CommandArgument("amount")}, args -> {
            if (player == null) {
                log.warn("Cannot execute command: player does not exist.");
                return;
            }

            if (args.length < 2) {
                String usage = "/give <item> <amount>";
                log.warn("Usage: {}", usage);
                ToastFactory.error(Local.lang.f("toast.invalid_usage", usage));
                return;
            }

            String itemId = args[0];
            if (itemId == null || itemId.isBlank()) {
                log.warn("Item ID cannot be empty.");
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                log.warn("Invalid amount: {}", args[1]);
                ToastFactory.error(Local.lang.f("toast.invalid_amount", args[1]));
                return;
            }

            if (amount < 0) {
                log.warn("Amount mustn't be less than zero.");
                ToastFactory.error(Local.lang.f("toast.invalid_amount", args[1]));
                return;
            }

            Item item = ir.create(itemId);
            if (item == null) {
                ToastFactory.error(Local.lang.f("toast.unknown_item", itemId));
                return;
            }

            if (!player.hasSpace()) {
                player.addToBackpack(item, amount);
            } else if (player.hasSpace()){
                player.add(item, amount);
            }

            log.info("Command addEnemy executed: {} x{}", itemId, amount);
            ToastFactory.reward(Local.lang.f("toast.item_added", amount, item.getDisplayName()));
        }));

        cr.register(new Command("/earn", new CommandArgument[]{literal("amount")}, args -> {
            if (player == null) {
                log.warn("Cannot execute command: player does not exist.");
                return;
            }

            if (args.length < 1) {
                String usage = "/earn <amount>";
                log.warn("Usage: {}", usage);
                ToastFactory.error(Local.lang.f("toast.invalid_usage", usage));
            }

            int amount = 0;
            try {
                amount += Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                log.warn("Invalid amount: {}", args[0]);
                ToastFactory.error(Local.lang.f("toast.invalid_amount", args[0]));
                return;
            }

            player.earn(amount);
            log.info("Command earn executed: {}", amount);
            ToastFactory.success(Local.lang.f("toast.earned_coins", amount));
        }));

        cr.register(new Command("clear", new CommandArgument[]{}, args -> {
            if (player == null) {
                log.warn("Cannot execute command: player does not exist.");
                return;
            }

            player.clear();
            log.info("Command clear executed");
            ToastFactory.success(Local.lang.t("toast.inventory_cleared"));
        }));

        cr.register(new Command("/rain", new CommandArgument[]{literal("action", "start", "stop")}, args -> {
            if (player == null) {
                log.warn("Cannot execute command: player does not exist.");
                return;
            }

            if (args.length < 1) {
                String usage = "/rain <action>";
                log.warn("Usage: {}", usage);
                ToastFactory.error(Local.lang.f("toast.invalid_usage", usage));
                return;
            }

            switch (args[0].toLowerCase()) {
                case "start" -> {
                    weatherService.setWeather(WeatherType.RAIN);
                    log.info("Command rain executed");
                    ToastFactory.success(Local.lang.t("toast.started_rain"));
                }
                case "stop" -> {
                    weatherService.setWeather(WeatherType.CLEAR);
                    log.info("Command rain executed");
                    ToastFactory.success(Local.lang.t("toast.stopped_rain"));
                }
                default -> log.warn("Unknown rain action: {}", args[0]);
            }
        }));

        cr.register(new Command("/time", new CommandArgument[]{literal("action", "set"),
                new CommandArgument("amount")}, args -> {
            if (player == null) {
                log.warn("Cannot execute command: player does not exist.");
                return;
            }
            if (args.length < 2) {
                String usage = "/time <action> <amount>";
                log.warn("Usage: {}", usage);
                ToastFactory.error(Local.lang.f("toast.invalid_usage", usage));
                return;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                log.warn("Invalid amount: {}", args[1]);
                ToastFactory.error(Local.lang.f("toast.invalid_amount", args[1]));
                return;
            }

            if (amount < 0 || amount > 24000) {
                log.warn("Amount must be between 0 and 24000.");
                ToastFactory.error(Local.lang.f("toast.invalid_amount", args[1]));
                return;
            }
            TimeService.ts.setTimeScale(amount / 24000.0f);
        }));

        cr.register(new Command("/gm", new CommandArgument[]{dynamic("mode", () ->
                Arrays.stream(Gamemode.values()).map(Gamemode::getName).toList())}, args -> {
            if (player == null) {
                log.warn("Cannot execute command: player does not exist.");
                return;
            }

            if (args.length < 1) {
                String usage = "/gm <mode>";
                log.warn("Usage: {}", usage);
                ToastFactory.error(Local.lang.f("toast.invalid_usage", usage));
                return;
            }

            Gamemode targetMode = Gamemode.fromString(args[0]);
            if (targetMode == null) {
                log.warn("Invalid gamemode: {}", args[0]);
                ToastFactory.error(Local.lang.f("toast.invalid_usage", args[0]));
                return;
            }

            player.setGamemode(targetMode);
            if (targetMode.isNoClip()) gameMaster.toggleHUD();
            log.info("Command gamemode executed: {}", targetMode);
            ToastFactory.success(Local.lang.f("toast.gamemode_changed", targetMode.getName()));
        }));

        cr.register(new Command("/gamerule", new CommandArgument[]{dynamic("rule", () ->
                GameRules.getRules().keySet()), new CommandArgument("value")}, args -> {
            if (player == null) {
                log.warn("Cannot execute command: player does not exist.");
                return;
            }

            if (args.length == 0) {
                ToastFactory.info(Local.lang.t("toast.available_gamerules"));
                GameRules.getRules().forEach((rule, value) -> ToastFactory.info(Local.lang.f("toast.gamerule_value", rule, value)));
                return;
            }

            String rule = args[0];
            if (!GameRules.exists(rule)) {
                log.warn("Unknown gamerule: {}", rule);
                ToastFactory.error(Local.lang.f("toast.unknown_gamerule", rule));
                return;
            }

            if (args.length == 1) {
                Object value = GameRules.get(rule);
                log.info("Gamerule {} = {}", rule, value);
                ToastFactory.info(Local.lang.f("toast.gamerule_value", rule, value));
                return;
            }

            String valueString = args[1];
            Object currentValue = GameRules.get(rule);
            Object newValue;
            try {
                switch (currentValue) {
                    case Boolean ignored -> {
                        if (!valueString.equalsIgnoreCase("true") && !valueString.equalsIgnoreCase("false")) {
                            throw new IllegalArgumentException("Expected true or false");
                        }
                        newValue = Boolean.parseBoolean(valueString);
                    }
                    case Integer ignored -> newValue = Integer.parseInt(valueString);
                    case Float ignored -> newValue = Float.parseFloat(valueString);
                    case null, default -> throw new IllegalArgumentException("Unsupported gamerule type");
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid value for gamerule {}: {}", rule, valueString);
                ToastFactory.error(Local.lang.f("toast.invalid_gamerule_value", rule));
                return;
            }

            try {
                GameRules.set(rule, newValue);
                log.info("Gamerule changed: {} = {}", rule, newValue);
                ToastFactory.success(Local.lang.f("toast.gamerule_changed", rule, newValue));
            } catch (IllegalArgumentException e) {
                log.warn("Could not set gamerule {}: {}", rule, e.getMessage());
                ToastFactory.error(e.getMessage());
            }
        }));

        cr.register(new Command("/kill", new CommandArgument[]{}, args -> {
            if (player == null) {
                log.warn("Cannot execute command: player does not exist.");
                return;
            }

            player.kill(Cause.SELF);
            SoundService.fx.playEntitySound(SoundGroup.ENTITY);
            log.info("Command kill executed");
            ToastFactory.success(Local.lang.t("toast.self_kill"));
        }));
    }

    /**
     * Creates or returns literal from the supplied arguments.
     * @param name the {@link String} supplied as {@code name}
     * @param values an array of {@link String} values supplied as {@code values}
     * @return the {@link CommandArgument} representing the literal result
     */
    private static CommandArgument literal(String name, String... values) {
        return CommandArgument.of(name, (text, cursorPosition) -> {
            String prefix = text == null ? "" : text.substring(0, Math.min(cursorPosition, text.length()));
            return Arrays.stream(values).filter(value ->
                    value.regionMatches(true, 0, prefix, 0, prefix.length())).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        });
    }

    /**
     * Creates or returns dynamic from the supplied arguments.
     * @param name the {@link String} supplied as {@code name}
     * @param supplier the {@link Supplier} supplied as {@code supplier}
     * @return the {@link CommandArgument} representing the dynamic result
     */
    private static CommandArgument dynamic(String name, Supplier<Collection<String>> supplier) {
        return CommandArgument.of(name, (text, cursorPosition) -> {
            String prefix = text == null ? "" : text.substring(0, Math.min(cursorPosition, text.length()));
            Collection<String> values = supplier.get();
            if (values == null) {
                return java.util.List.of();
            }
            return values.stream().filter(Objects::nonNull).filter(
                            value -> value.regionMatches(true, 0, prefix, 0, prefix.length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        });
    }

    /**
     * Returns the formatted name.
     * @param names an array of {@link String} values supplied as {@code name}
     * @return the {@link String} representing the formatted name
     */
    public static String getFormattedName(String... names) {
        String rawName = String.join("", names);
        String formattedName = rawName.trim().replaceAll("\\s+", "_").toLowerCase(Locale.ROOT);
        return NAMESPACE_ID + formattedName;
    }
}
