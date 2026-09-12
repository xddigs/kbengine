package org.kbeng.input;

import org.kbeng.data.*;
import org.kbeng.entity.Entity;
import org.kbeng.entity.Player;
import org.kbeng.entity.WorldItem;
import org.kbeng.entity.states.SneakingState;
import org.kbeng.graphics.ChunkMeshBuilder;
import org.kbeng.graphics.ParticleEngine;
import org.kbeng.graphics.SpriteSheet;
import org.kbeng.item.*;
import org.kbeng.service.*;
import org.kbeng.ui.GameUIService;
import org.kbeng.utils.*;
import org.kbeng.wrld.Chunk;
import org.kbeng.wrld.FluidSimulation;
import org.kbeng.wrld.GameMaster;
import org.kbeng.wrld.World;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

import static org.joml.Math.lerp;

/**
 * Represents the game interaction component of the kbengine runtime.
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
@Singleton
@GodObject
public class GameInteraction {
    public static final GameInteraction gami = new GameInteraction();
    private static final float PICKUP_DISTANCE = 1.5f;
    private static final Logger log = LoggerFactory.getLogger(GameInteraction.class);

    private int breakingX = Integer.MIN_VALUE;
    private int breakingY = Integer.MIN_VALUE;
    private int breakingZ = Integer.MIN_VALUE;

    private float breakProgress = 0.0f;
    private boolean isSmartShift = false;

    /**
     * Creates a new private {@code GameInteraction} instance.
     */
    private GameInteraction() {}
    
    /**
     * Updates the current state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param selectedItem the {@link Item} supplied as {@code selectedItem}
     * @return the {@link BlockPos} representing the update result
     */
    public BlockPos update(GameMaster gameMaster, Item selectedItem) {
        Player player = Player.plyr;
        boolean isCtrlHeld = Controls.isDown(ControlAction.MODIFIER);

        boolean isShiftHeld = Controls.isDown(ControlAction.SMART_SHIFT);
        isSmartShift = isShiftHeld && !GameMaster.game.isInventoryOpen()
                && !GameMaster.game.isBackpackOpen();

        boolean isLeftHeld = Controls.isDown(ControlAction.PRIMARY_ACTION);
        boolean isLeftPressed = Controls.isPressed(ControlAction.PRIMARY_ACTION);
        boolean isRightHeld = Controls.isDown(ControlAction.SECONDARY_ACTION);
        boolean isRightPressed = Controls.isPressed(ControlAction.SECONDARY_ACTION);
        boolean canInteract = player != null
                && !player.getGamemode().isNoClip()
                && !GameMaster.game.isInventoryOpen()
                && !GameMaster.game.isBackpackOpen()
                && !GameMaster.game.isChatOpen();

        if (Controls.isPressed(ControlAction.OPEN_CHAT)) {
            if (!GameMaster.game.isChatOpen()) {
                GameMaster.game.setChatOpen(true);
                GameUIService.ui.openChat();
            } else {
                String command = GameUIService.ui.getChatText();
                if (command != null && !command.isEmpty()) {
                    GameMaster.game.getCommandService().execute(command);
                }
                GameMaster.game.setChatOpen(false);
                GameUIService.ui.closeChat();
            }
        }

        if (Controls.isPressed(ControlAction.TOGGLE_HUD)) {
            GameMaster.game.toggleHUD();
        }

        if (Controls.isPressed(ControlAction.TOGGLE_DEBUG)) {
            Settings.toggleDebugInfo();
        }

        if (Controls.isPressed(ControlAction.DROP_ITEM) && canInteract) {
            boolean dropAll = Controls.isDown(ControlAction.MODIFIER);
            dropItem(selectedItem, dropAll);
        }

        if (Controls.isPressed(ControlAction.TOGGLE_SHIELD) && canInteract) {
            player.toggleShield(selectedItem);
        }

        boolean isUsingShield = canInteract && isRightHeld
                && player.getEquippedShield() != null;
        player.setShieldRaised(isUsingShield);
        if (isUsingShield) {
            isRightPressed = false;
        }

        if (Controls.isPressed(ControlAction.TOGGLE_INVENTORY) && !GameMaster.game.isChatOpen() &&
                !BookService.bs.isOpen()) {
            if (GameMaster.game.isBackpackOpen()) {
                GameMaster.game.setBackpackOpen(false);
            } else {
                GameMaster.game.toggleInventory();
            }
        }

        CraftingBook backpackBook = player.getFromBackpack(CraftingBook.class);
        if (BookService.bs.getOpenedBook() instanceof CraftingBook openedBook
                && openedBook != backpackBook) {
            BookService.bs.close();
        }

        if (Controls.isPressed(ControlAction.TOGGLE_BOOK) && !GameMaster.game.isInventoryOpen()
                && !GameMaster.game.isChatOpen() && backpackBook != null) {
            if (!BookService.bs.isOpen()) {
                BookService.bs.open(backpackBook);
            } else {
                BookService.bs.close();
            }
        } else if (Controls.isPressed(ControlAction.TOGGLE_INVENTORY)
                && BookService.bs.isOpen() &&
                backpackBook == null) {
            BookService.bs.close();
        }

        if (!Player.plyr.getGamemode().isNoClip()
                && !Controls.isPressed(ControlAction.DROP_ITEM)) {
            pickUp();
        }

        if (Controls.isPressed(ControlAction.TOGGLE_MUSIC)
                && !GameMaster.game.isChatOpen()) {
            Settings.toggleMusic();
        }

        if (isLeftPressed && canInteract) {
            if (!player.isAttacking()) {
                player.interact();
            }
        }

        BlockPos hoveredCell = HoveredCell.get(gameMaster, isShiftHeld);
        BlockPos raycastCell = hoveredCell;

        if (isRightPressed && canInteract && NPCService.npcs.interact(gameMaster, raycastCell)) {
            if (!player.isAttacking()) player.interact();
            isRightPressed = false;
        } else if (isLeftPressed && canInteract && NPCService.npcs.attack(gameMaster, raycastCell)) {
            return null;
        }

        if (selectedItem instanceof Usable usable) {
            switch (usable) {
                case Backpack backpack -> {
                    if (isRightPressed && !GameMaster.game.isInventoryOpen()
                            && !GameMaster.game.isBackpackOpen()) {
                        if (isCtrlHeld) {
                            if (backpack.isEquipped()) backpack.unequip();
                            else backpack.equip();
                        } else {
                            backpack.use(gameMaster, isCtrlHeld);
                        }
                        isRightPressed = false;
                    }
                }

                case CraftingBook book -> {
                    if (isRightPressed && !BookService.bs.isOpen()) {
                        book.use(gameMaster, isCtrlHeld);
                        isRightPressed = false;
                    }
                }

                case Bucket bucket -> {
                    if (isRightPressed && !GameMaster.game.isInventoryOpen()) {
                        bucket.use(gameMaster, isCtrlHeld);
                        isRightPressed = false;
                    }
                }

                case Wallet wallet -> {
                    if (isRightPressed && !GameMaster.game.isInventoryOpen()) {
                        wallet.use(gameMaster, isCtrlHeld);
                        isRightPressed = false;
                    }
                }

                default -> ToastFactory.error
                        (Local.lang.f("toast.unknown_item", usable));
            }
        }

        if (selectedItem instanceof Equippable e) {
            if (isRightPressed && !e.isEquipped()) {
                e.equip();
                isRightPressed = false;
            }
        }

        if (selectedItem instanceof Consumable consumable) {
            if (isRightPressed && canInteract && consumable.consume()) {
                if (!player.isAttacking()) player.interact();
                isRightPressed = false;
            }
        }

        if (isShiftHeld && hoveredCell != null) {
            byte blockType = GameMaster.game.getWorld().getBlockTypeAt(hoveredCell);
            if (blockType == BlockData.OAK_LOG.getId()) {
                int bottomY = hoveredCell.y();
                while (bottomY > 0 && GameMaster.game.getWorld().getBlockTypeAt(
                        hoveredCell.x(), bottomY - 1, hoveredCell.z()) == BlockData.OAK_LOG.getId()) {
                    bottomY--;
                }
                hoveredCell = new BlockPos(getBlockData(blockType), hoveredCell.x(), bottomY,
                        hoveredCell.z());
            }
        }


        if (hoveredCell == null) {
            resetBreaking();
            return null;
        }

        if (!isVisibleToPlayer(hoveredCell) || !isWithinRange(hoveredCell)) {
            resetBreaking();
            return null;
        }

        if (isLeftHeld && canInteract) {
            if (NPCService.npcs.getClosestBeforeBlock(gameMaster, raycastCell) == null) {
                breaking(gameMaster, hoveredCell);
            }
        } else {
            resetBreaking();
        }

        if (isRightPressed && !GameMaster.game.isInventoryOpen()) {
            if (player != null && !player.isAttacking()) {
                player.interact();
            }

            if (!(Player.plyr.getCurrentState() instanceof SneakingState)) {
                if (hoveredCell.data() instanceof BlockData data && data.isInteractive()) {
                    iBlock interactiveBlock = GameMaster.game.getWorld().getInteractiveBlockAt(
                            hoveredCell.x(), hoveredCell.y(), hoveredCell.z());
                    if (interactiveBlock != null) {
                        interactiveBlock.use();
                        return hoveredCell;
                    }
                }
            }

            place(gameMaster, hoveredCell, selectedItem);
        }
        return hoveredCell;
    }

    /**
     * Adds the item.
     */
    public void addItem() {
        Player player = Player.plyr;
        if (player == null) return;
        Iterator<Entity> iterator = GameMaster.game.getEntities().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (!(entity instanceof WorldItem worldItem)) continue;
            Item item = worldItem.getItem();
            int amount = worldItem.getAmount();

            if (item == null || amount <= 0) {
                iterator.remove();
                continue;
            }

            int remaining = storeItem(player, item, amount);
            int stored = amount - remaining;
            if (remaining > 0) {
                worldItem.setAmount(remaining);
            } else {
                iterator.remove();
            }

            if (stored > 0) {
                SoundService.fx.playEntitySound(SoundGroup.ITEMS);
                log.info("Added x{} {}", stored, item.getName());
            }
        }
    }

    /**
     * Transfers or creates the relevant entity or item for drop item.
     * @param selectedItem the {@link Item} supplied as {@code selectedItem}
     * @param dropAll the {@code boolean} supplied as {@code dropAll}
     */
    public void dropItem(Item selectedItem, boolean dropAll) {
        if (selectedItem == null) return;
        if (selectedItem instanceof Undroppable) {
            ToastFactory.error("toast.item_undroppable");
            return;
        }

        Player player = Player.plyr;
        if (player == null) return;

        if (player.isInGodMode()) {
            return;
        }

        for (InventorySlot slot : player.getInventory().getSlots()) {
            if (slot.isEmpty()) continue;

            Item item = slot.getItem();
            if (item == null) continue;
            if (!item.equals(selectedItem)) continue;
            int amount = dropAll ? slot.getAmount() : 1;
            if (amount <= 0) continue;
            Vector3f forward = new Vector3f(player.getForward()).normalize();
            Vector3f playerPosition = player.getPosition();
            Vector3f dropPosition = new Vector3f(playerPosition)
                    .add(forward.x * 0.8f, 0.8f, forward.z * 0.8f);
            WorldItem worldItem = new WorldItem(item, amount, dropPosition);

            Vector3f playerVelocity = new Vector3f(player.getVelocity());
            float inheritedVelocity = 0.35f;
            float throwStrength = 8.0f;
            float verticalStrength = 4.7f;

            Vector3f velocity = new Vector3f(playerVelocity).mul(inheritedVelocity);
            velocity.x += forward.x * throwStrength;
            velocity.z += forward.z * throwStrength;
            velocity.y += verticalStrength;

            worldItem.setVelocity(velocity);
            player.remove(item, amount);
            GameMaster.game.addEntity(worldItem);
            SoundService.fx.playEntitySound(SoundGroup.ITEMS);

            log.trace("Dropped x{} {} with velocity ({}, {}, {})", amount,
                    item.getName(), velocity.x, velocity.y, velocity.z);
            break;
        }
    }

    /**
     * Transfers or creates the relevant entity or item for pick up.
     */
    private void pickUp() {
        Player player = Player.plyr;
        if (player == null) return;

        Iterator<Entity> iterator = GameMaster.game.getEntities().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (!(entity instanceof WorldItem worldItem)) continue;

            if (!worldItem.canBePickedUp()) continue;

            Vector3f playerPos = player.getPosition();
            Vector3f itemPos = worldItem.getPosition();
            float distance = itemPos.distance(playerPos);

            if (distance <= PICKUP_DISTANCE) {
                worldItem.setAttracting(true);
            }

            if (worldItem.isAttracting()) {
                float delta = GameMaster.game.getGenDelta();
                float lerpFactor = Math.min(1.0f, 10.0f * delta);

                Vector3f targetPos = new Vector3f(playerPos.x,
                        playerPos.y + player.getDimensions().y, playerPos.z);

                itemPos.x = lerp(itemPos.x, targetPos.x, lerpFactor);
                itemPos.y = lerp(itemPos.y, targetPos.y, lerpFactor);
                itemPos.z = lerp(itemPos.z, targetPos.z, lerpFactor);

                if (itemPos.distance(targetPos) < 0.4f) {
                    Item item = worldItem.getItem();
                    int amount = worldItem.getAmount();

                    if (item != null && amount > 0) {
                        int remaining = storeItem(player, item, amount);
                        int stored = amount - remaining;
                        if (remaining > 0) {
                            worldItem.setAmount(remaining);
                            worldItem.setAttracting(false);
                        } else {
                            iterator.remove();
                        }
                        if (stored > 0) {
                            SoundService.fx.playEntitySound(SoundGroup.ITEMS);
                            log.info("Picked up x{} {}", stored, item.getName());
                        }
                    } else {
                        iterator.remove();
                    }
                }
            }
        }
    }

    /**
     * Stores as much of a world item as possible and returns the remainder.
     */
    private int storeItem(Player player, Item item, int amount) {
        int remaining = player.getInventory().add(item, amount);
        if (remaining > 0 && player.getInventory().hasBackpackEquipped()) {
            remaining = player.getBackpack().add(item, remaining);
        }
        return remaining;
    }

    /**
     * Checks whether the within range condition is met.
     * @param cell the {@link BlockPos} supplied as {@code cell}
     * @return {@code true} if within range; otherwise {@code false}
     */
    private boolean isWithinRange(BlockPos cell) {
        float distance = getDistanceToBlock(cell);
        return distance <= Settings.getMaxInteractionDistance();
    }

    /**
     * Keeps all block interactions inside the same visibility volume used by
     * the renderer and camera raycast.
     */
    private boolean isVisibleToPlayer(BlockPos cell) {
        if (cell == null) return false;
        return GameMaster.game.getViewService().isVisible(
                new Vector3f(cell.x() + 0.5f, cell.y() + 0.5f, cell.z() + 0.5f),
                GameMaster.game.getActiveCamera().getPosition());
    }

    /**
     * Returns the distance to block.
     * @param cell the {@link BlockPos} supplied as {@code cell}
     * @return {@code float}; the distance to block
     */
    public float getDistanceToBlock(BlockPos cell) {
        if (cell == null) return Float.MAX_VALUE;
        Vector3f playerPos = Player.plyr.getPosition();
        float targetX = cell.x() + 0.5f;
        float targetY = cell.y() + 0.5f;
        float targetZ = cell.z() + 0.5f;

        float dx = playerPos.x - targetX;
        float dy = playerPos.y - targetY;
        float dz = playerPos.z - targetZ;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Updates or derives runtime state for breaking according to the supplied arguments.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param cell the {@link BlockPos} supplied as {@code cell}
     */
    private void breaking(GameMaster gameMaster, BlockPos cell) {
        if (BookService.bs.isOpen()) return;
        World world = GameMaster.game.getWorld();
        int x = cell.x();
        int y = cell.y();
        int z = cell.z();

        Item selectedItem = Settings.selectedItem;
        if (!(selectedItem instanceof Tool)) return;
        if (selectedItem instanceof Sword) return;

        if (!Player.plyr.isAttacking()) {
            Player.plyr.interact();
        }

        byte blockId = world.getBlockTypeAt(x, y, z);
        BlockData blockData = getBlockData(blockId);

        Crop crop = world.getCropAt(x, y, z);
        if (crop != null) {
            if (crop.getCropType().isStackable()) {
                breakStackedCrop(world, crop);
                SoundService.fx.playBreakSound(SoundGroup.SOIL);
                GameUIService.ui.logAction(cell);
                return;
            }
            CropType cropType = crop.getCropType();
            int frameIndex = crop.getStage().getFrameIndex();
            SpriteSheet sheet = GameMaster.game.getCropSpriteSheet(cropType);
            if (crop.isReadyToHarvest()) {
                CropService.cs.harvest(crop);
            } else {
                CropService.cs.rip(crop);
            }

            SoundService.fx.playBreakSound(SoundGroup.SOIL);
            if (sheet != null) ParticleEngine.peng.spawnCrop(x, y + K.World.SHORTER_BLOCK_HEIGHT,
                    z, sheet, frameIndex);
            GameUIService.ui.logAction(cell);
            return;
        }

        iBlock interactiveBlock = world.getInteractiveBlockAt(x, y, z);
        if (interactiveBlock != null) {
            breakInteractiveBlock(gameMaster, world, interactiveBlock);
            return;
        }

        if (blockId == 0 || blockData == null) {
            resetBreaking();
            return;
        }

        if (breakingX != x || breakingY != y || breakingZ != z) {
            if (breakingX != Integer.MIN_VALUE) {
                ChunkMeshBuilder.clearBreakingBlock();
                gameMaster.rebuildBreakingChunkMeshAt(breakingX, breakingZ);
            }
            breakingX = x;
            breakingY = y;
            breakingZ = z;
            breakProgress = 0.0f;
            ChunkMeshBuilder.setBreakingBlock(cell);
            gameMaster.rebuildBreakingChunkMeshAt(x, z);
            Vector3f hitDirection = new Vector3f(x + 0.5f - Player.plyr.getPosition().x(),
                    y + 0.5f - Player.plyr.getPosition().y(),
                    z + 0.5f - Player.plyr.getPosition().z()).normalize();
        }

        float destroyTime = Player.plyr.isInGodMode() ? 0.2f :
                blockData.getDestroyTime(selectedItem);

        if (destroyTime <= 0.0f) {
            resetBreaking();
            return;
        }

        SoundService.fx.playBreakingSound(blockData.getSoundGroup());
        breakProgress += GameMaster.game.getGenDelta() / destroyTime;

        if (breakProgress >= 1.0f) {
            breakBlock(gameMaster, cell, blockData, blockId, selectedItem);
            resetBreaking();
        }
    }

    /**
     * Advances or completes the destruction of an interactive block.
     */
    private void breakInteractiveBlock(GameMaster gameMaster,
                                       World world, iBlock block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        if (breakingX != x || breakingY != y || breakingZ != z) {
            if (breakingX != Integer.MIN_VALUE) {
                ChunkMeshBuilder.clearBreakingBlock();
                gameMaster.rebuildBreakingChunkMeshAt(breakingX, breakingZ);
            }
            breakingX = x;
            breakingY = y;
            breakingZ = z;
            breakProgress = 0.0f;
            ChunkMeshBuilder.setBreakingBlock(new BlockPos(block.getType(), x, y, z));
            gameMaster.rebuildBreakingChunkMeshAt(x, z);
        }

        if (Player.plyr.getGamemode().isGodmode()) {
            destroyInteractiveBlock(gameMaster, world, block);
            resetBreaking();
            return;
        }

        breakProgress += GameMaster.game.getGenDelta()
                / block.getType().getDestroyTime();

        if (breakProgress >= 1.0f) {
            destroyInteractiveBlock(gameMaster, world, block);
            resetBreaking();
        }
    }

    /**
     * Removes an interactive block and drops it together with all its contents.
     */
    private void destroyInteractiveBlock(GameMaster gameMaster, World world, iBlock block) {
        Vector3f dropPosition = new Vector3f(
                block.getX() + 0.5f, block.getY() + 0.5f, block.getZ() + 0.5f);

        world.removeInteractiveBlockAt(block.getX(), block.getY(), block.getZ());
        gameMaster.rebuildChunkMeshAt(block.getX(), block.getZ());
        for (int offsetY = 0; offsetY < block.getType().getHeight(); offsetY++) {
            FluidSimulation.notifyBlockDestroyed(
                    block.getX(), block.getY() + offsetY, block.getZ());
        }
        gameMaster.addEntity(new WorldItem(new iBlock(block.getType()), 1,
                new Vector3f(dropPosition)));

        for (InventorySlot slot : block.getInventory().getSlots()) {
            if (slot.isEmpty()) continue;
            gameMaster.addEntity(new WorldItem(slot.getItem(), slot.getAmount(),
                    new Vector3f(dropPosition)));
        }
        block.getInventory().clear();
        GameUIService.ui.logAction(
                new BlockPos(block.getType(), block.getX(), block.getY(), block.getZ()));
        log.trace("Interactive block removed: {} at {},{},{}",
                block.getType().getName().toUpperCase(),
                block.getX(), block.getY(), block.getZ());
    }

    /**
     * Removes a stackable crop from the selected segment upwards.
     */
    private void breakStackedCrop(World world, Crop first) {
        int x = first.getX();
        int y = first.getY();
        int z = first.getZ();
        CropType type = first.getCropType();
        SpriteSheet sheet = GameMaster.game.getCropSpriteSheet(type);

        Crop crop = first;
        while (crop != null && crop.getCropType() == type) {
            int frameIndex = crop.getStage().getFrameIndex();
            CropService.cs.rip(crop);

            WorldItem drop = new WorldItem(new Produce(type), 1,
                    new Vector3f(x + 0.5f, y + 0.5f, z + 0.5f));
            GameMaster.game.addEntity(drop);

            if (sheet != null) {
                ParticleEngine.peng.spawnCrop(x, y + K.World.SHORTER_BLOCK_HEIGHT,
                        z, sheet, frameIndex);
            }

            y++;
            crop = world.getCropAt(x, y, z);
        }
    }

    /**
     * Applies the world or inventory action represented by break block.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param cell the {@link BlockPos} supplied as {@code cell}
     * @param blockable the {@link Blockable} argument; the block data value
     * @param blockId the {@code byte} supplied as {@code blockId}
     * @param selectedItem the {@link Item} supplied as {@code selectedItem}
     */
    private void breakBlock(GameMaster gameMaster, BlockPos cell, Blockable blockable,
                            byte blockId, Item selectedItem) {
        if (!(selectedItem instanceof Tool)) return;
        World world = GameMaster.game.getWorld();
        BlockData blockData = blockable instanceof BlockData data ? data : BlockData.fromId(blockId);
        if (blockData.getSoundGroup() != null) {
            SoundService.fx.playBreakSound(blockData.getSoundGroup());
        }

        Vector3f position = new Vector3f(cell.x() + 0.5f, cell.y() + 0.5f, cell.z() + 0.5f);
        Block removedBlock = new Block(blockData, cell);
        Item itemToDrop = null;

        if (selectedItem instanceof Tool tool) {
            boolean isUsableOn = tool.getType().isUsableOn(blockData);

            if (tool instanceof Axe axe && isSmartShift
                    && blockData.isLog() && isUsableOn) {
                List<BlockPos> destroyedBlocks = TreeService.chop(gameMaster, axe, cell);
                for (BlockPos pos : destroyedBlocks) {
                    GameUIService.ui.logAction(new BlockPos(blockData, pos.x(), pos.y(), pos.z()));
                    if (!(pos.data() instanceof BlockData bData)) {
                        continue;
                    }
                    Block brokenBlock = new Block(bData, pos.x(), pos.y(), pos.z());

                    if (bData.hasDrops()) {
                        Object dropObj = bData.getRandomDrop();
                        if (dropObj instanceof MaterialID mid) {
                            itemToDrop = new Material(bData.getTier(), mid);
                        } else if (dropObj instanceof MiningComponent mc) {
                            itemToDrop = mc;
                        } else if (dropObj instanceof Item item) {
                            itemToDrop = item;
                        }
                    }

                    if (itemToDrop == null) {
                        itemToDrop = brokenBlock;
                    }

                    ParticleEngine.peng.spawnBlock(pos, bData);
                    Vector3f dropPos = new Vector3f(pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f);

                    int count = (int) (Math.random() * 2) + 1;
                    if (!Player.plyr.getGamemode().isGodmode()) {
                        GameMaster.game.addEntity(new WorldItem(itemToDrop, count, dropPos));
                    }
                }

                log.info("Tree chopped successfully at base {},{},{}", cell.x(), cell.y(), cell.z());
                return;
            }

            if (!isUsableOn) {
                tool.misuse();
            } else {
                tool.use();
            }
        }

        Craftable wasBrokenIn = wasBrokenInSurvival(blockData);
        world.setBlockTypeAt(cell, BlockData.AIR.getId());
        world.setFluidLevelAt(cell.x(), cell.y(), cell.z(), (byte) 0);
        breakAbove(cell.x(), cell.y(), cell.z());
        FluidSimulation.notifyBlockDestroyed(cell.x(), cell.y(), cell.z());
        GameMaster.game.rebuildChunkMeshAt(cell);
        ParticleEngine.peng.spawnBlock(cell, blockData);

        if (wasBrokenIn instanceof MaterialID materialID) {
            itemToDrop = new Material(materialID);
        } else if (wasBrokenIn != null) {
            itemToDrop = BlockData.fromIdTo(wasBrokenIn.getId());
        } else if (removedBlock.getType().hasDrops()) {
            Object dropObj = removedBlock.getType().getRandomDrop();
            if (dropObj instanceof MaterialID mid) {
                itemToDrop = new Material(removedBlock.getType().getTier(), mid);
            } else if (dropObj instanceof MiningComponent mc) {
                itemToDrop = mc;
            } else if (dropObj instanceof Item item) {
                itemToDrop = item;
            }
        }

        if (itemToDrop == null) {
            itemToDrop = removedBlock;
        }

        WorldItem dropEntity = new WorldItem(itemToDrop, (int) (Math.random()) + 1, position);
        GameMaster.game.addEntity(dropEntity);

        GameUIService.ui.logAction(cell);
        log.trace("Block removed: {} at {},{},{}", blockData.getName().toUpperCase(), cell.x(), cell.y(), cell.z());
    }

    /**
     * Decides whether a block should yield another one
     * @param blockData the {@link BlockData} supplied as {@code blockData}
     * @return {@code true} if the block was broken; otherwise {@code false}
     */
    private Craftable wasBrokenInSurvival(BlockData blockData) {
        return switch (blockData) {
            case GRASS, TILLED_DIRT -> BlockData.fromIdTo(BlockData.DIRT.getId());
            case STONE -> BlockData.fromIdTo(BlockData.COBBLESTONE.getId());
            case OAK_LEAVES, SPRUCE_LEAVES -> MaterialID.STICK;
            default -> BlockData.fromIdTo(blockData.getId());
        };
    }

    /**
     * Resets breaking to its initial runtime state.
     */
    private void resetBreaking() {
        SoundService.fx.stopBreakingSound();
        if (breakingX != Integer.MIN_VALUE) {
            int previousX = breakingX;
            int previousZ = breakingZ;
            ChunkMeshBuilder.clearBreakingBlock();
            GameMaster.game.rebuildBreakingChunkMeshAt(previousX, previousZ);
        }
        breakingX = Integer.MIN_VALUE;
        breakingY = Integer.MIN_VALUE;
        breakingZ = Integer.MIN_VALUE;
        breakProgress = 0.0f;
    }

    /**
     * Applies the world or inventory action represented by place.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param cell the {@link BlockPos} supplied as {@code cell}
     * @param selectedItem the {@link Item} supplied as {@code selectedItem}
     */
    private void place(GameMaster gameMaster, BlockPos cell,
                       Item selectedItem) {
        World world = GameMaster.game.getWorld();
        Player player = Player.plyr;

        if (BookService.bs.isOpen()) return;
        if (player.checkCollision(world)) return;

        if (player != null) {
            player.interact();
        }

        int normalX = GameMaster.game.getCamera().getLastHitNormalX();
        int normalY = GameMaster.game.getCamera().getLastHitNormalY();
        int normalZ = GameMaster.game.getCamera().getLastHitNormalZ();

        if (selectedItem instanceof iBlock interactiveBlock) {
            int placeX = cell.x() + normalX;
            int placeY = cell.y() + normalY;
            int placeZ = cell.z() + normalZ;
            int blockHeight = interactiveBlock.getType().getHeight();

            Vector3f playerPosition = player.getPosition();
            float directionX = playerPosition.x - (placeX + 0.5f);
            float directionZ = playerPosition.z - (placeZ + 0.5f);
            float orientation = (float) Math.atan2(directionX, directionZ)
                    + (float) Math.PI;
            float quarterTurn = (float) (Math.PI * 0.5);
            orientation = Math.round(orientation / quarterTurn) * quarterTurn;
            iBlock placedBlock = new iBlock(
                    interactiveBlock.getType(), placeX, placeY, placeZ, orientation);

            if (placeY < 0 || placeY + blockHeight > Chunk.SIZE_Y) return;
            if (interactiveBlock.getType().isDoor()
                    && !world.isBlockSolid(placeX, placeY - 1, placeZ)) return;

            for (int offsetY = 0; offsetY < blockHeight; offsetY++) {
                int occupiedY = placeY + offsetY;
                if (!interactiveBlock.getType().isDoor()
                        && player.intersectsBlock(placeX, occupiedY, placeZ)) return;
                if (world.getInteractiveBlockAt(placeX, occupiedY, placeZ) != null) return;

                BlockData target = BlockData.fromId(
                        world.getBlockTypeAt(placeX, occupiedY, placeZ));
                boolean replacesFluid = FluidSimulation.forBlock(target) != null;
                if (target == null || (target != BlockData.AIR && !replacesFluid)
                        || world.getCropAt(placeX, occupiedY, placeZ) != null) {
                    return;
                }
            }
            if (interactiveBlock.getType().isDoor()
                    && player.intersects(placedBlock)) return;

            for (int offsetY = 0; offsetY < blockHeight; offsetY++) {
                int occupiedY = placeY + offsetY;
                BlockData target = BlockData.fromId(
                        world.getBlockTypeAt(placeX, occupiedY, placeZ));
                FluidSimulation targetFluid = FluidSimulation.forBlock(target);
                if (targetFluid != null
                        && !targetFluid.displaceForBlockPlacement(placeX, occupiedY, placeZ)) return;
            }

            world.addInteractiveBlock(placedBlock);
            gameMaster.rebuildChunkMeshAt(placeX, placeZ);
            for (int offsetY = 0; offsetY < blockHeight; offsetY++) {
                FluidSimulation.notifyBlockPlaced(placeX, placeY + offsetY, placeZ);
            }
            player.remove(selectedItem);
            SoundService.fx.playPlaceSound(placedBlock.getType().getSoundGroup());
            GameUIService.ui.logAction(
                    new BlockPos(placedBlock.getType(), placeX, placeY, placeZ));
            log.trace("Interactive block placed: {} at {},{},{}",
                    placedBlock.getType().getName().toUpperCase(), placeX, placeY, placeZ);
            return;
        }

        if (selectedItem instanceof Block block) {
            int placeX = cell.x() + normalX;
            int placeY = cell.y() + normalY;
            int placeZ = cell.z() + normalZ;

            if (block.getType().isTorch()
                    && normalY != 1 && normalX == 0 && normalZ == 0) return;

            BlockShape placedShape = block.getType().getShape();
            if (block.getType().isTorch()) {
                placedShape = BlockShape.torchFacing(normalX, normalY, normalZ);
            }
            if (block.getType().isSlab() && placedShape.isVerticalSlab()) {
                Vector3f playerPosition = player.getPosition();
                placedShape = BlockShape.verticalSlabFacing(
                        playerPosition.x, playerPosition.z, placeX, placeZ);
            }

            if (block.getType().isStaircase()) {
                Vector3f playerPosition = player.getPosition();
                placedShape = BlockShape.staircaseFacing(
                        playerPosition.x, playerPosition.z, placeX, placeZ);
            }

            if (player.intersectsBlock(placedShape, placeX, placeY, placeZ)) return;
            byte targetBlock = world.getBlockTypeAt(placeX, placeY, placeZ);
            BlockData target = BlockData.fromId(targetBlock);
            FluidSimulation targetFluid = FluidSimulation.forBlock(target);
            boolean replacesFluid = targetFluid != null
                    && (targetFluid.isSource(placeX, placeY, placeZ)
                    || block.getType().isSolid());

            if (target == null || (!target.equals(BlockData.AIR) && !replacesFluid)
                    || world.getCropAt(placeX, placeY, placeZ) != null) {
                return;
            }

            if (block.getType().isPlant()) {
                BlockData support = BlockData.fromId(
                        world.getBlockTypeAt(placeX, placeY - 1, placeZ));
                if (support == null || support.isPlant()
                        || world.getCropAt(placeX, placeY - 1, placeZ) != null) {
                    return;
                }
            }

            if (replacesFluid && !targetFluid.displaceForBlockPlacement(placeX, placeY, placeZ)) {
                return;
            }

            Block newBlock = new Block(block.getType(), placeX, placeY, placeZ);
            newBlock.setShape(placedShape);
            if (block.getType() == BlockData.OAK_BONSAI || block.getType() == BlockData.SPRUCE_BONSAI) {
                TreeService.ts.plant(placeX, placeY, placeZ, block.getType());
            } else if (block.getType().isFluid()) {
                FluidSimulation placedFluid = FluidSimulation.forBlock(block.getType());
                if (placedFluid == null || !placedFluid.addSource(placeX, placeY, placeZ)) return;
            } else {
                world.setBlockTypeAt(placeX, placeY, placeZ, block.getType().getId());
                if (block.getType().hasCustomShape()) world.addBlock(newBlock);
            }
            if (!block.getType().isFluid()) {
                FluidSimulation.notifyBlockPlaced(placeX, placeY, placeZ);
            }

            player.remove(selectedItem);
            SoundService.fx.playBreakSound(newBlock.getType().getSoundGroup());
            GameMaster.game.rebuildChunkMeshAt(placeX, placeZ);
            GameUIService.ui.logAction(
                    new BlockPos(newBlock.getType(), placeX, placeY, placeZ));
            log.trace("Block placed: {} at {},{},{}",
                    newBlock.getType().getName().toUpperCase(), placeX, placeY, placeZ);
            return;
        }

        if (selectedItem instanceof Hoe hoe) {
            Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
            if (!hoe.getType().isUsableOn(block.getType())) {
                hoe.misuse();
            } else {
                hoe.use(gameMaster, block);
            }
            GameMaster.game.rebuildChunkMeshAt(block.getX(), block.getZ());
            return;
        }

        if (selectedItem instanceof Plantable p) {
            if (p.getType() == null) return;
            int x = cell.x();
            int y = cell.y();
            int z = cell.z();

            Crop crop = world.getCropAt(x, y, z);
            byte blockId = world.getBlockTypeAt(x, y, z);

            if (crop != null) {
                return;
            } else if (p.getType() != CropType.SUGAR_CANE_CROP
                    && blockId != BlockData.TILLED_DIRT.getId()) {
                log.trace("Cannot plant at {},{},{}: selected block is not TILLED_DIRT", x, y, z);
                return;
            }

            Crop planted = CropService.cs.plant(x, y, z, p.getType(),
                    TimeService.ts.getCurrentSeason());

            if (planted != null) {
                GameUIService.ui.logAction(cell);
                log.info("Planted {} at {},{},{}", p.getType().getName(), x, y, z);
            }
        }
    }

    /**
     * Applies the world or inventory action represented by break above.
     * @param x the {@code int} supplied as {@code x}
     * @param y the {@code int} supplied as {@code y}
     * @param z the {@code int} supplied as {@code z}
     */
    private void breakAbove(int x, int y, int z) {
        int aboveY = y + 1;
        if (aboveY >= Chunk.SIZE_Y) {
            return;
        }

        iBlock interactiveBlock = World.wrld.getInteractiveBlockAt(x, aboveY, z);
        if (interactiveBlock != null
                && interactiveBlock.getType().isDoor()
                && interactiveBlock.getY() == aboveY) {
            destroyInteractiveBlock(GameMaster.game, World.wrld, interactiveBlock);
            return;
        }

        Crop crop = World.wrld.getCropAt(x, aboveY, z);
        if (crop != null) {
            CropService.cs.rip(crop);
            World.wrld.removeBlockAt(x, aboveY, z);
            World.wrld.setBlockTypeAt(x, aboveY, z, BlockData.AIR.getId());
            World.wrld.setFluidLevelAt(x, aboveY, z, (byte) 0);
            GameMaster.game.rebuildChunkMeshAt(x, z);
            return;
        }

        byte aboveBlockId = World.wrld.getBlockTypeAt(x, aboveY, z);
        BlockData aboveBlock = BlockData.fromId(aboveBlockId);

        if (aboveBlock == null || !aboveBlock.isPlant()) {
            return;
        }

        World.wrld.removeBlockAt(x, aboveY, z);
        World.wrld.setBlockTypeAt(x, aboveY, z, BlockData.AIR.getId());
        World.wrld.setFluidLevelAt(x, aboveY, z, (byte) 0);
        ParticleEngine.peng.spawnBlock(new BlockPos(aboveBlock, x, aboveY, z), aboveBlock);
        GameMaster.game.rebuildChunkMeshAt(x, z);
    }

    /**
     * Returns the block data.
     * @param blockId the {@code byte} supplied as {@code blockId}
     * @return the {@link BlockData} representing the block data
     */
    private BlockData getBlockData(byte blockId) {
        for (BlockData data : BlockData.values()) {
            if (data.getId() == blockId) {
                return data;
            }
        }

        return null;
    }

    /**
     * Checks whether the breaking block condition is met.
     * @return {@code true} if breaking block; otherwise {@code false}
     */
    public boolean isBreakingBlock() {
        return breakingX != Integer.MIN_VALUE;
    }

    /**
     * Returns the breaking block pos.
     * @return the {@link Vector3i} representing the breaking block pos
     */
    public Vector3i getBreakingBlockPos() {
        return new Vector3i(breakingX, breakingY, breakingZ);
    }

    /**
     * Returns the break progress.
     * @return {@code float}; the break progress
     */
    public float getBreakProgress() {
        return breakProgress;
    }

    /**
     * Checks whether the smart shift active condition is met.
     * @return {@code true} if smart shift active; otherwise {@code false}
     */
    public boolean isSmartShiftActive() {
        return Controls.isDown(ControlAction.SMART_SHIFT)
                && !GameMaster.game.isInventoryOpen()
                && !GameMaster.game.isBackpackOpen();
    }
}
