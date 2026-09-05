package com.isofarm.data;

import com.isofarm.item.*;
import com.isofarm.service.SoundService;
import com.isofarm.utils.K;

import java.util.*;

/**
 * Encapsulates the state and operations required by inventory within the game runtime.
 */
@DataClass
public class Inventory {
    private final List<InventorySlot> slots;
    private final List<InventorySlot> equippedExtraItems = new ArrayList<>();
    private final InventorySlot backpackSlot;
    private final InventorySlot bookSlot;

    /**
     * Creates a new {@code Inventory} instance.
     */
    public Inventory() {
        this(false);
    }

    /**
     * Creates a new {@code Inventory} instance.
     * @param includeHotbar whether to allocate independent player hotbar slots
     */
    public Inventory(boolean includeHotbar) {
        this.slots = new ArrayList<>();
        this.backpackSlot = new InventorySlot();
        this.bookSlot = new InventorySlot();

        int capacity = includeHotbar
                ? K.UI.PLAYER_INVENTORY_SLOTS
                : K.UI.INVENTORY_SLOTS;
        for (int i = 0; i < capacity; i++) {
            this.slots.add(new InventorySlot());
        }
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
     * Returns the equipped extra items.
     * @return the {@link List} representing the equipped extra items
     */
    public List<InventorySlot> getEquippedExtraItems() {
        return equippedExtraItems;
    }

    /**
     * Returns the backpack slot.
     * @return the {@link InventorySlot} representing the backpack slot
     */
    public InventorySlot getBackpackSlot() {
        return backpackSlot;
    }

    /**
     * Returns the book slot.
     * @return the {@link InventorySlot} representing the book slot
     */
    public InventorySlot getBookSlot() {
        return bookSlot;
    }

    /**
     * Checks whether the backpack equipped condition is met.
     * @return {@code true} if backpack equipped; otherwise {@code false}
     */
    public boolean hasBackpackEquipped() {
        return !backpackSlot.isEmpty() && backpackSlot.getItem() instanceof Backpack;
    }

    /**
     * Checks whether the book equipped condition is met.
     * @return {@code true} if book equipped; otherwise {@code false}
     */
    public boolean hasBookEquipped() {
        return !bookSlot.isEmpty() && bookSlot.getItem() instanceof CraftingBook;
    }

    /**
     * Returns the backpack.
     * @return the {@link Backpack} representing the backpack
     */
    public Backpack getBackpack() {
        return backpackSlot.getItem() instanceof Backpack backpack ? backpack : null;
    }

    /**
     * Returns the book.
     * @return the {@link CraftingBook} representing the book
     */
    public CraftingBook getBook() {
        return bookSlot.getItem() instanceof CraftingBook book ? book : null;
    }

    /**
     * Applies equip backpack and updates the affected character or item state.
     * @param backpack the {@link Backpack} supplied as {@code backpack}
     */
    public void equipBackpack(Backpack backpack) {
        remove(backpack, 1);
        backpackSlot.setItem(backpack);
        equippedExtraItems.add(backpackSlot);
        SoundService.fx.playUseSound(SoundGroup.ITEMS);
    }

    /**
     * Applies equip book and updates the affected character or item state.
     * @param book the {@link Book} supplied as {@code book}
     */
    public void equipBook(Book book) {
        remove(book, 1);
        bookSlot.setItem(book);
        equippedExtraItems.add(bookSlot);
        SoundService.fx.playUseSound(SoundGroup.ITEMS);
    }

    /**
     * Applies unequip backpack and updates the affected character or item state.
     */
    public void unequipBackpack() {
        Item backpack = backpackSlot.getItem();
        equippedExtraItems.remove(backpack);
        add(backpack, 1);
        backpackSlot.clear();
        SoundService.fx.playUseSound(SoundGroup.ITEMS);
    }

    /**
     * Applies unequip book and updates the affected character or item state.
     */
    public void unequipBook() {
        Item book = bookSlot.getItem();
        equippedExtraItems.remove(book);
        add(book, 1);
        bookSlot.clear();
        SoundService.fx.playUseSound(SoundGroup.ITEMS);
    }

    /**
     * Adds add.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     * @return {@code int}; the add result
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

        return remaining;
    }

    /**
     * Adds the to existing stacks.
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     * @param start the {@code int} supplied as {@code start}
     * @param end the {@code int} supplied as {@code end}
     * @return {@code int}; the add to existing stacks result
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
     * @return {@code int}; the add to empty slots result
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
     * Removes remove.
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
        for (InventorySlot slot : slots) {
            if (!slot.isEmpty()) {
                stacks.add(new Stack(slot.getItem(), slot.getAmount()));
                slot.clear();
            }
        }

        stacks.sort(Comparator.comparing((Stack stack) -> stack.item().getClass().getSimpleName())
                .thenComparing(stack -> stack.item().getName(), Comparator.nullsLast(String::compareTo))
                .thenComparingInt(Stack::amount).reversed());

        int index = 0;
        for (Stack stack : stacks) {
            int remaining = stack.amount();

            while (remaining > 0 && index < slots.size()) {
                int amount = Math.min(remaining, getMaxStack(stack.item()));

                InventorySlot slot = slots.get(index++);
                slot.setItem(stack.item());
                slot.setAmount(amount);

                remaining -= amount;
            }
        }
    }

    /**
     * Creates or returns group from the supplied arguments.
     */
    public void group() {
        for (int i = 0; i < slots.size(); i++) {
            InventorySlot currentSlot = slots.get(i);

            if (currentSlot.isEmpty()) {
                continue;
            }

            Item currentItem = currentSlot.getItem();

            for (int j = i + 1; j < slots.size(); j++) {
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
     * Transfers or creates the relevant entity or item for take.
     * @param index the {@code int} supplied as {@code index}
     * @param amount the {@code int} supplied as {@code amount}
     * @return {@code int}; the take result
     */
    public int take(int index, int amount) {
        if (!isValidIndex(index) || amount <= 0) {
            return 0;
        }

        InventorySlot slot = slots.get(index);

        if (slot.isEmpty()) {
            return 0;
        }

        int taken = Math.min(amount, slot.getAmount());
        slot.setAmount(slot.getAmount() - taken);
        return taken;
    }

    /**
     * Adds the to stack.
     * @param targetIndex the {@code int} supplied as {@code targetIndex}
     * @param item the {@link Item} supplied as {@code item}
     * @param amount the {@code int} supplied as {@code amount}
     * @return {@code int}; the add to stack result
     */
    public int addToStack(int targetIndex, Item item, int amount) {
        if (!isValidIndex(targetIndex) || item == null || amount <= 0) {
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
     * @return {@code int}; the add one result
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
        if (!isValidIndex(index)) {
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
        return K.UI.INVENTORY_SLOTS;
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
        return index >= 0 && index < slots.size();
    }
}
