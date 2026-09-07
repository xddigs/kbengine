package com.isofarm.service;

import com.isofarm.data.*;
import com.isofarm.entity.Player;
import com.isofarm.item.*;
import com.isofarm.utils.Local;
import com.isofarm.utils.ToastFactory;
import com.isofarm.wrld.GameMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Encapsulates the state and operations required by library within the game runtime.
 */
@SuppressWarnings("all")
public class Library implements Service<GameMaster> {
    private static final Logger log = LoggerFactory.getLogger(Library.class);

    /**
     * Initializes the items.
     * @param itemR the {@link ItemRegistry} supplied as {@code itemR}
     */
    public static void initItems(ItemRegistry itemR) {
        registerDefault(itemR, Backpack::new);
        registerDefault(itemR, CraftingBook::new);
        registerDefault(itemR, () -> new Book(false));
        registerDefault(itemR, () -> new Bucket(BlockData.AIR));
        registerDefault(itemR, () -> new Wallet());

        MaterialID.forEach(material -> {
            if (material.equals(MaterialID.INGOT) || material.equals(MaterialID.RAW_ORE)) return;
            registerDefault(itemR, () -> new Material(material));
        });

        Tier.forEach(tier -> {
            if (tier.isInvalidTier()) return;
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
        });

        for (CropType type : CropType.values()) {
            registerDefault(itemR, () -> new Produce(type));
            if (type.equals(CropType.SUGAR_CANE_CROP)) continue;
            registerDefault(itemR, () -> new Seed(type));
        }

        for (BlockData block : BlockData.values()) {
            if (block.getId() > 0 && !block.isInteractive()) {
                registerDefault(itemR, () -> new Block(block));
            }
            if (block.isInteractive()) {
                registerDefault(itemR, () -> new iBlock(block));
            }
        }
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
        }

        itemR.register(getFormattedName(rawName), supplier);
    }

    /**
     * Initializes the commands.
     * @param delta the {@code float} supplied as {@code delta}
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    public static void initCommands(float delta, GameMaster gameMaster) {
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
                log.warn("Usage: /give <namespace:item> <amount>");
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
                return;
            }
            if (amount <= 0) {
                log.warn("Amount must be greater than zero.");
                return;
            }

            Item item = ir.create(itemId);
            if (item == null) {
                ToastFactory.error(Local.lang.f("toast.unknown_item", itemId));
                return;
            }

            if (itemId.equals(null)) {
                player.earn(amount);
            } else if (!player.hasSpace()) {
                player.addToBackpack(item, amount);
            } else if (player.hasSpace()){
                player.add(item, amount);
            }

            log.info("Command add executed: {} x{}", itemId, amount);
            ToastFactory.success(Local.lang.f("toast.item_added", amount, item.getDisplayName()));
        }));

        cr.register(new Command("/rain", new CommandArgument[]{literal("action", "start", "stop")}, args -> {
            if (player == null) {
                log.warn("Cannot execute command: player does not exist.");
                return;
            }
            if (args.length < 1) {
                log.warn("Usage: /rain <action>");
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
                log.warn("Usage: /time set <amount>");
                return;
            }
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                log.warn("Invalid amount: {}", args[1]);
                return;
            }
            if (amount < 0 || amount > 24000) {
                log.warn("Amount must be between 0 and 24000.");
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
                log.warn("Usage: /gm <mode>");
                return;
            }
            Gamemode targetMode = Gamemode.fromString(args[0]);
            if (targetMode == null) {
                log.warn("Invalid gamemode: {}", args[0]);
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
                if (currentValue instanceof Boolean) {
                    if (!valueString.equalsIgnoreCase("true") && !valueString.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("Expected true or false");
                    }
                    newValue = Boolean.parseBoolean(valueString);
                } else if (currentValue instanceof Integer) {
                    newValue = Integer.parseInt(valueString);
                } else if (currentValue instanceof Float) {
                    newValue = Float.parseFloat(valueString);
                } else {
                    throw new IllegalArgumentException("Unsupported gamerule type");
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
            return values.stream().filter(value -> value != null).filter(
                            value -> value.regionMatches(true, 0, prefix, 0, prefix.length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        });
    }

    /**
     * Returns the formatted name.
     * @param name an array of {@link String} values supplied as {@code name}
     * @return the {@link String} representing the formatted name
     */
    public static String getFormattedName(String... name) {
        StringBuilder builder = new StringBuilder();
        for (String s : name) {
            builder.append(s);
        }
        return builder.toString().trim()
                .replaceAll(" ", "_").toLowerCase();
    }
}
