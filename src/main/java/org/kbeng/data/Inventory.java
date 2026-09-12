package org.kbeng.data;

import org.kbeng.entity.Character;
import org.kbeng.entity.Player;
import org.kbeng.item.*;
import org.kbeng.service.BookService;
import org.kbeng.service.SoundService;
import org.kbeng.utils.K;

import java.util.*;

/**
 * Represents the inventory component of the kbengine runtime.
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
@DataClass
@GodObject
public class Inventory {
    /** Supported global item ordering strategies. */
    public enum SortOrder {
        NAME,
        TYPE,
        CREATIVE
    }

    private final List<InventorySlot> slots;
    private final boolean hasHotbar;
    private final InventorySlot backpackSlot;
    private final InventorySlot shieldSlot;
    private final InventorySlot[] armorSlots;
    private final Character owner;

    /**
     * Creates a new {@code Inventory} instance.
     */
    public Inventory() {
        this(false, null);
    }

    /**
     * Creates a new {@code Inventory} instance owned by a character.
     */
    public Inventory(Character owner) {
        this(false, owner);
    }

    /**
     * Creates a new {@code Inventory} instance.
     * @param includeHotbar whether to allocate independent player hotbar slots
     */
    public Inventory(boolean includeHotbar, Character owner) {
        this.owner = owner;
        this.slots = new ArrayList<>();
        this.hasHotbar = includeHotbar;
        this.backpackSlot = new InventorySlot();
        this.shieldSlot = new InventorySlot();
        this.armorSlots = new InventorySlot[ArmorSlot.values().length];
        for (int i = 0; i < armorSlots.length; i++) {
            armorSlots[i] = new InventorySlot();
        }

        int capacity = includeHotbar
                ? K.UI.PLAYER_INVENTORY_SLOTS
                : K.UI.INVENTORY_SLOTS;
        for (int i = 0; i < capacity; i++) {
            this.slots.add(new InventorySlot());
        }
    }

    /**
     * Retrieves {@code owner}
     * @return {@link Character} value of owner
     */
    public Character getOwner() {
        return owner;
    }

    /**
     * Returns the items.
     * @return the {@link Map} representing the items
     */
    public Map<Item, Integer> getItems() {
        Map<Item, Integer> result = new LinkedHashMap<>();

        for (InventorySlot slot : slots) {
            if (!slot.isEmpty()) {
                result.merge(slot.getItem(), slot.getAmount(), Integer::sum);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns the backpack slot.
     * @return the {@link InventorySlot} representing the backpack slot
     */
    public InventorySlot getBackpackSlot() {
        return backpackSlot;
    }

    /**
     * Checks whether the backpack equipped condition is met.
     * @return {@code true} if backpack equipped; otherwise {@code false}
     */
    public boolean hasBackpackEquipped() {
        return !backpackSlot.isEmpty() && backpackSlot.getItem() instanceof Backpack;
    }

    /**
     * Returns the backpack.
     * @return the {@link Backpack} representing the backpack
     */
    public Backpack getBackpack() {
        return backpackSlot.getItem() instanceof Backpack backpack ? backpack : null;
    }

    /** Returns the dedicated left-hand shield slot. */
    public InventorySlot getShieldSlot() {
        return shieldSlot;
    }

    /** Returns the currently equipped shield, if any. */
    public Shield getShield() {
        return shieldSlot.getItem() instanceof Shield shield ? shield : null;
    }

    /** Returns the equipment slot reserved for the requested armor piece. */
    public InventorySlot getArmorSlot(ArmorSlot slot) {
        return slot == null ? null : armorSlots[slot.ordinal()];
    }

    /** Returns the three dedicated armor equipment slots in ArmorSlot order. */
    public InventorySlot[] getArmorSlots() {
        return armorSlots.clone();
    }

    /** Returns the total defense granted by currently equipped armor. */
    public float getArmorDefense() {
        float total = 0.0f;
        for (InventorySlot slot : armorSlots) {
            if (slot.getItem() instanceof Armor armor) total += armor.getDefense();
        }
        return total;
    }

    /** Equips an armor item from normal storage into its matching dedicated slot. */
    public boolean equipArmor(Armor armor) {
        if (armor == null) return false;
        ArmorSlot armorSlot = ArmorSlot.values()[armor.getType().getId()];
        InventorySlot destination = getArmorSlot(armorSlot);
        if (destination == null || !destination.isEmpty()) return false;

        InventorySlot source = slots.stream()
                .filter(slot -> slot.getItem() == armor && slot.getAmount() > 0)
                .findFirst().orElse(null);
        if (source == null) return false;

        source.setAmount(source.getAmount() - 1);
        destination.setItem(armor);
        destination.setAmount(1);
        SoundService.fx.playUseSound(SoundGroup.ITEMS);
        return true;
    }

    /** Returns an equipped armor item to normal storage if there is room. */
    public boolean unequipArmor(ArmorSlot armorSlot) {
        InventorySlot source = getArmorSlot(armorSlot);
        if (source == null || !(source.getItem() instanceof Armor armor)) return false;
        if (add(armor, 1) > 0) return false;
        source.clear();
        SoundService.fx.playUseSound(SoundGroup.ITEMS);
        return true;
    }

    /** Equips the exact shield instance from this inventory. */
    public boolean equipShield(Shield shield) {
        if (shield == null || getShield() != null) return false;

        InventorySlot source = slots.stream()
                .filter(slot -> slot.getItem() == shield && slot.getAmount() > 0)
                .findFirst().orElse(null);
        if (source == null) return false;

        source.clear();
        shieldSlot.setItem(shield);
        SoundService.fx.playUseSound(SoundGroup.ITEMS);
        return true;
    }

    /** Returns the equipped shield to normal storage when space is available. */
    public boolean unequipShield() {
        Shield shield = getShield();
        if (shield == null || add(shield, 1) > 0) return false;
        shieldSlot.clear();
        SoundService.fx.playUseSound(SoundGroup.ITEMS);
        return true;
    }

    /** Removes a broken shield directly from its equipment slot. */
    public void breakShield() {
        shieldSlot.clear();
    }

    /**
     * Applies equip backpack and updates the affected character or item state.
     * @param backpack the {@link Backpack} supplied as {@code backpack}
     */
    public void equipBackpack(Backpack backpack) {
        equipBackpack(backpack, true);
    }

    /** Equips a backpack, optionally playing the player-facing equip sound. */
    public void equipBackpack(Backpack backpack, boolean playSound) {
        if (backpack == null || hasBackpackEquipped()) return;
        remove(backpack, 1);
        backpackSlot.setItem(backpack);
        if (playSound) SoundService.fx.playUseSound(SoundGroup.ITEMS);
    }

    /**
     * Applies unequip backpack and updates the affected character or item state.
     */
    public void unequipBackpack() {
        if (!hasBackpackEquipped()) return;
        Item backpack = backpackSlot.getItem();
        if (add(backpack, 1) > 0) return;
        backpackSlot.clear();
        SoundService.fx.playUseSound(SoundGroup.ITEMS);
    }

    /**
     * Adds addEnemy.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     * @return {@code int}; the addEnemy result
     */
    public int add(Item item, int amount) {
        if (item == null || amount <= 0) {
            return amount;
        }

        int remaining = amount;
        int hotbarStart = getHotbarStart();

        remaining = addToExistingStacks(item, remaining, hotbarStart, slots.size());
        remaining = addToEmptySlots(item, remaining, hotbarStart, slots.size());

        if (remaining > 0) {
            remaining = addToExistingStacks(item, remaining, 0, hotbarStart);
            remaining = addToEmptySlots(item, remaining, 0, hotbarStart);
        }

        if (remaining < amount && Player.plyr != null
                && Player.plyr.getInventory() == this) {
            BookService.bs.reloadOpenCraftingBook();
        }
        return remaining;
    }

    /**
     * Adds an item only within the supplied slot range, filling compatible
     * stacks before the first empty slot.
     *
     * @param item the item to addEnemy
     * @param amount the amount to addEnemy
     * @param startInclusive first destination slot
     * @param endExclusive slot after the final destination
     * @return amount that did not fit
     */
    public int addToRange(Item item, int amount, int startInclusive, int endExclusive) {
        if (item == null || amount <= 0) return amount;

        int start = Math.max(0, startInclusive);
        int end = Math.min(slots.size(), endExclusive);
        if (start >= end) return amount;

        int remaining = addToExistingStacks(item, amount, start, end);
        remaining = addToEmptySlots(item, remaining, start, end);
        if (remaining < amount && Player.plyr != null
                && Player.plyr.getInventory() == this) {
            BookService.bs.reloadOpenCraftingBook();
        }
        return remaining;
    }

    /**
     * Adds the to existing stacks.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     * @param start the {@code int} supplied as {@code start}
     * @param end the {@code int} supplied as {@code end}
     * @return {@code int}; the addEnemy to existing stacks result
     */
    private int addToExistingStacks(Item item, int amount, int start, int end) {
        int remaining = amount;

        for (int i = start; i < end && remaining > 0; i++) {
            InventorySlot slot = slots.get(i);

            if (slot.isEmpty()) {
                continue;
            }

            if (!isSameType(slot.getItem(), item)) {
                continue;
            }

            int maxStack = getMaxStack(item);
            int space = maxStack - slot.getAmount();

            if (space <= 0) {
                continue;
            }

            int added = Math.min(remaining, space);
            slot.addAmount(added);
            remaining -= added;
        }

        return remaining;
    }

    /**
     * Adds the to empty slots.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     * @param start the {@code int} supplied as {@code start}
     * @param end the {@code int} supplied as {@code end}
     * @return {@code int}; the addEnemy to empty slots result
     */
    private int addToEmptySlots(Item item, int amount, int start, int end) {
        int remaining = amount;

        for (int i = start; i < end && remaining > 0; i++) {
            InventorySlot slot = slots.get(i);

            if (!slot.isEmpty()) {
                continue;
            }

            int added = Math.min(remaining, getMaxStack(item));

            slot.setItem(item);
            slot.setAmount(added);

            remaining -= added;
        }

        return remaining;
    }

    /**
     * Removes removeEnemy.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     */
    public void remove(Item item, int amount) {
        if (item == null || amount <= 0) {
            return;
        }

        int remaining = amount;
        for (InventorySlot slot : slots) {
            if (remaining <= 0) {
                break;
            }

            if (slot.isEmpty() || !isSameType(slot.getItem(), item)) {
                continue;
            }

            int current = slot.getAmount();
            if (current <= remaining) {
                remaining -= current;
                slot.clear();
            } else {
                slot.setAmount(current - remaining);
                remaining = 0;
            }
        }
    }

    /**
     * Reorganizes inventory state for sort.
     */
    public void sort() {
        group();
        List<Stack> stacks = new ArrayList<>();
        int firstMutableSlot = getFirstMutableSlotIndex();
        int mutableSlotLimit = getMutableSlotLimit();
        for (int i = firstMutableSlot; i < mutableSlotLimit; i++) {
            InventorySlot slot = slots.get(i);
            if (!slot.isEmpty()) {
                stacks.add(new Stack(slot.getItem(), slot.getAmount()));
                slot.clear();
            }
        }

        stacks.sort(Comparator.comparing((Stack stack) -> stack.item().getClass().getSimpleName())
                .thenComparing(stack -> stack.item().getName(), Comparator.nullsLast(String::compareTo))
                .thenComparingInt(Stack::amount).reversed());

        int index = firstMutableSlot;
        for (Stack stack : stacks) {
            int remaining = stack.amount();

            while (remaining > 0 && index < mutableSlotLimit) {
                int amount = Math.min(remaining, getMaxStack(stack.item()));

                InventorySlot slot = slots.get(index++);
                slot.setItem(stack.item());
                slot.setAmount(amount);

                remaining -= amount;
            }
        }
    }

    /**
     * Returns the creative catalog ordering by item category, numeric id and
     * localized name, in that order.
     * @return the creative item comparator
     */
    public static Comparator<Item> sorter() {
        return sorter(SortOrder.CREATIVE);
    }

    /**
     * Returns the shared item comparator for inventories and crafting books.
     * @param order ordering strategy to apply
     * @return comparator implementing the selected ordering
     */
    public static Comparator<Item> sorter(SortOrder order) {
        Comparator<Item> byName = Comparator.comparing(
                Item::getDisplayName, String.CASE_INSENSITIVE_ORDER);
        if (order == SortOrder.NAME) return byName;

        Comparator<Item> comparator = Comparator.comparingInt(Inventory::sortByOrder);
        if (order == SortOrder.TYPE) {
            comparator = comparator.thenComparingInt(Inventory::toolTierOrder);
        } else {
            comparator = comparator.thenComparingInt(Item::getId);
        }
        return comparator.thenComparing(byName);
    }

    /**
     * Returns the category position used by the creative catalog.
     * @param item the item to classify
     * @return the item category position
     */
    public static int sortByOrder(Item item) {
        return switch (item) {
            case Block ignored -> 0;
            case Tool ignored -> 1;
            case Usable ignored -> 2;
            case Material ignored -> 3;
            case Food ignored -> 4;
            case Produce ignored -> 5;
            case null, default -> 6;
        };
    }

    /**
     * Returns the ascending tier position for tools without affecting other categories.
     * @param item item whose tool tier is inspected
     * @return tier ordinal, or zero for non-tools
     */
    private static int toolTierOrder(Item item) {
        return item instanceof Tool tool ? tool.getTier().ordinal() : 0;
    }

    /**
     * Creates or returns group from the supplied arguments.
     */
    public void group() {
        int firstMutableSlot = getFirstMutableSlotIndex();
        int mutableSlotLimit = getMutableSlotLimit();
        for (int i = firstMutableSlot; i < mutableSlotLimit; i++) {
            InventorySlot currentSlot = slots.get(i);

            if (currentSlot.isEmpty()) {
                continue;
            }

            Item currentItem = currentSlot.getItem();

            for (int j = i + 1; j < mutableSlotLimit; j++) {
                InventorySlot targetSlot = slots.get(j);

                if (targetSlot.isEmpty()) {
                    continue;
                }

                if (!isSameType(currentItem, targetSlot.getItem())) {
                    continue;
                }

                int maxStack = getMaxStack(currentItem);
                int spaceLeft = maxStack - currentSlot.getAmount();

                if (spaceLeft <= 0) {
                    continue;
                }

                int transfer = Math.min(spaceLeft, targetSlot.getAmount());

                currentSlot.addAmount(transfer);
                targetSlot.setAmount(targetSlot.getAmount() - transfer);
            }
        }
    }

    /**
     * Adds the to stack.
     * @param targetIndex the {@code int} supplied as {@code targetIndex}
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     * @return {@code int}; the addEnemy to stack result
     */
    public int addToStack(int targetIndex, Item item, int amount) {
        if (isValidIndex(targetIndex) || item == null || amount <= 0) {
            return 0;
        }

        InventorySlot target = slots.get(targetIndex);

        if (target.isEmpty()) {
            int added = Math.min(amount, getMaxStack(item));
            target.setItem(item);
            target.setAmount(added);

            return added;
        }

        if (!isSameType(target.getItem(), item)) {
            return 0;
        }

        int space = getMaxStack(item) - target.getAmount();
        if (space <= 0) {
            return 0;
        }

        int added = Math.min(amount, space);
        target.addAmount(added);

        return added;
    }

    /**
     * Adds the one.
     * @param targetIndex the {@code int} supplied as {@code targetIndex}
     * @param item the {@link Item} supplied as {@code item}
     * @return {@code int}; the addEnemy one result
     */
    public int addOne(int targetIndex, Item item) {
        return addToStack(targetIndex, item, 1);
    }

    /**
     * Returns the hotbar items.
     * @return the {@link List} representing the hotbar items
     */
    public List<Item> getHotbarItems() {
        List<Item> hotbar = new ArrayList<>();
        int hotbarStart = getHotbarStart();
        for (int i = 0; i < K.UI.INVENTORY_COLUMNS; i++) {
            int index = hotbarStart + i;

            if (index >= slots.size()) {
                break;
            }

            InventorySlot slot = slots.get(index);
            if (!slot.isEmpty()) {
                hotbar.add(slot.getItem());
            }
        }

        return hotbar;
    }

    /**
     * Removes clear.
     */
    public void clear() {
        for (InventorySlot slot : slots) {
            slot.clear();
        }
        shieldSlot.clear();
    }

    /**
     * Checks whether the full condition is met.
     * @return {@code true} if full; otherwise {@code false}
     */
    public boolean isFull() {
        return slots.stream().noneMatch(InventorySlot::isEmpty);
    }

    /**
     * Checks whether the empty condition is met.
     * @return {@code true} if empty; otherwise {@code false}
     */
    public boolean isEmpty() {
        return slots.stream().allMatch(
                slot -> slot.isEmpty() || slot.getAmount() <= 0);
    }

    /**
     * Returns the number or extent represented by size.
     * @return {@code int}; the size result
     */
    public int size() {
        return (int) slots.stream().filter(slot -> !slot.isEmpty()).count();
    }

    /**
     * Returns get.
     * @param index the {@code int} supplied as {@code index}
     * @return the {@link Item} representing the get result
     */
    public Item get(int index) {
        InventorySlot slot = getSlot(index);

        if (slot.isEmpty()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        return slot.getItem();
    }

    /**
     * Returns get.
     * @param item the {@link Item} supplied as {@code item}
     * @return the {@link Item} representing the get result
     */
    public Item get(Item item) {
        if (item == null) {
            return null;
        }

        for (InventorySlot slot : slots) {
            if (!slot.isEmpty() && isSameType(slot.getItem(), item)) {
                return slot.getItem();
            }
        }

        return null;
    }

    /**
     * Returns the amount.
     * @param item the {@link Item} supplied as {@code item}
     * @return {@code int}; the amount
     */
    public int getAmount(Item item) {
        if (item == null) {
            return 0;
        }

        int amount = 0;

        for (InventorySlot slot : slots) {
            if (!slot.isEmpty() && isSameType(slot.getItem(), item)) {
                amount += slot.getAmount();
            }
        }

        return amount;
    }

    /**
     * Checks whether the item of type condition is met.
     * @param <T> the generic type
     * @param type the {@link Class} supplied as {@code type}
     * @return {@code true} if item of type; otherwise {@code false}
     */
    public <T extends Item> boolean hasItemOfType(Class<T> type) {
        return slots.stream().filter(slot -> !slot.isEmpty()).anyMatch(
                slot -> type.isInstance(slot.getItem()) && slot.getAmount() > 0);
    }

    /**
     * Returns the item of type.
     * @param <T> the generic type
     * @param type the {@link Class} supplied as {@code type}
     * @return the {@link Optional} representing the item of type
     */
    public <T extends Item> Optional<T> getItemOfType(Class<T> type) {
        return slots.stream().filter(slot -> !slot.isEmpty()).filter(
                slot -> type.isInstance(slot.getItem()) && slot.getAmount() > 0).map(
                        slot -> type.cast(slot.getItem())).findFirst();
    }

    /**
     * Returns the first item id of type.
     * @param <T> the generic type
     * @param type the {@link Class} supplied as {@code type}
     * @return the {@link Optional} representing the first item id of type
     */
    public <T extends Item> Optional<Byte> getFirstItemIdOfType(Class<T> type) {
        return slots.stream().filter(slot -> !slot.isEmpty()).filter(
                slot -> type.isInstance(slot.getItem()) && slot.getAmount() > 0).map(
                        slot -> slot.getItem().getId()).findFirst();
    }

    /**
     * Checks whether the item with id condition is met.
     * @param <T> the generic type
     * @param type the {@link Class} supplied as {@code type}
     * @param id the {@code byte} supplied as {@code id}
     * @return {@code true} if item with id; otherwise {@code false}
     */
    public <T extends Item> boolean hasItemWithId(Class<T> type, byte id) {
        return slots.stream().filter(slot -> !slot.isEmpty()).anyMatch(
                slot -> type.isInstance(slot.getItem())
                        && slot.getItem().getId() == id && slot.getAmount() > 0);
    }

    /**
     * Returns the amount of material.
     * @param id the {@link MaterialID} supplied as {@code id}
     * @return {@code int}; the amount of material
     */
    public int getAmountOfMaterial(MaterialID id) {
        return slots.stream()
                .filter(slot -> !slot.isEmpty())
                .filter(slot -> slot.getItem()
                        instanceof Material mid && mid.getMaterialID() == id)
                .mapToInt(InventorySlot::getAmount)
                .sum();
    }

    /**
     * Returns the slots.
     * @return the {@link List} representing the slots
     */
    public List<InventorySlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    /**
     * Returns the slot.
     * @param index the {@code int} supplied as {@code index}
     * @return the {@link InventorySlot} representing the slot
     */
    public InventorySlot getSlot(int index) {
        if (isValidIndex(index)) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + slots.size());
        }

        return slots.get(index);
    }

    /**
     * Returns the slot amount.
     * @param index the {@code int} supplied as {@code index}
     * @return {@code int}; the slot amount
     */
    public int getSlotAmount(int index) {
        return getSlot(index).getAmount();
    }

    /**
     * Returns the hotbar start.
     * @return {@code int}; the hotbar start
     */
    public int getHotbarStart() {
        return hasHotbar ? K.UI.INVENTORY_SLOTS : slots.size();
    }

    /**
     * Returns the first slot that inventory-wide actions may modify.
     * Player hotbar slots are deliberately excluded from sorting and grouping.
     * @return the first mutable slot index
     */
    private int getFirstMutableSlotIndex() {
        return 0;
    }

    /**
     * Returns the exclusive upper bound for inventory-wide actions.
     * @return the first hotbar slot for players, otherwise the inventory size
     */
    private int getMutableSlotLimit() {
        return hasHotbar ? getHotbarStart() : slots.size();
    }

    /**
     * Returns the max stack.
     * @param item the {@link Item} supplied as {@code item}
     * @return {@code int}; the max stack
     */
    public int getMaxStack(Item item) {
        if (item == null) {
            return 0;
        }

        return switch (item) {
            case Tool ignored -> 1;
            case Armor ignored -> 1;
            case Usable usable -> switch (usable) {
                case Bucket bucket -> bucket.isFull() ? 1 : 16;
                default -> 1;
            };
            case Seed ignored -> K.World.MAX_STACK * 2;
            default -> K.World.MAX_STACK;
        };
    }

    /**
     * Checks whether the same type condition is met.
     * @param a the {@link Item} supplied as {@code a}
     * @param b the {@link Item} supplied as {@code b}
     * @return {@code true} if same type; otherwise {@code false}
     */
    private boolean isSameType(Item a, Item b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.getClass() != b.getClass()) {
            return false;
        }

        return switch (a) {
            case Armor armorA when b instanceof Armor armorB -> armorA.getType() == armorB.getType()
                    && armorA.getTier() == armorB.getTier();
            case Seed s1 when b instanceof Seed s2 -> s1.getType() == s2.getType();
            case Crop c1 when b instanceof Crop c2 -> c1.getCropType() == c2.getCropType();
            case Block b1 when b instanceof Block b2 -> b1.getType() == b2.getType();
            case Tool t1 when b instanceof Tool t2 -> t1.getId() == t2.getId() && t1.getTier() == t2.getTier();
            default -> Objects.equals(a.getName(), b.getName());
        };

    }

    /**
     * Checks whether the valid index condition is met.
     * @param index the {@code int} supplied as {@code index}
     * @return {@code true} if valid index; otherwise {@code false}
     */
    private boolean isValidIndex(int index) {
        return index < 0 || index >= slots.size();
    }
}
