package org.kbeng.entity.plyr;

import org.kbeng.entity.Player;
import org.kbeng.data.PlayerState;
import org.kbeng.entity.states.GroundedState;
import org.kbeng.entity.states.SneakingState;
import org.kbeng.input.ControlAction;
import org.kbeng.input.Controls;
import org.kbeng.pathfinding.GridPos;
import org.kbeng.service.BookService;
import org.kbeng.service.NPCService;
import org.kbeng.wrld.GameMaster;
import org.kbeng.wrld.World;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;

import static org.joml.Math.lerp;

/**
 * Manages player state, input-driven movement, paths and edge-safe sneaking.
 */
public final class PlayerManager {
    private static final float ZERO = 0.0f;
    private static final float EYE_HEIGHT = 1.6f;
    private static final float EYE_LERP_SPEED = 10.0f;
    private static final float PATH_DISTANCE_SQUARED = 0.01f;
    private static final float WAYPOINT_OFFSET = 0.5f;

    private List<GridPos> path = new LinkedList<>();
    private int pathIndex;
    private float currentEyeHeight = EYE_HEIGHT;
    private float targetEyeHeight = EYE_HEIGHT;
    private boolean falling;
    private PlayerState currentState;

    /**
     * Creates the shared player's movement manager.
     */
    public PlayerManager() {}

    /**
     * Installs the initial grounded state.
     */
    public void initialize() {
        currentState = new GroundedState();
        currentState.enter();
    }

    /**
     * Updates this object for the current simulation step.
     * @param delta the {@code float} argument; frame time in seconds
     */
    public void update(float delta) {
        updateFocus();
        currentState.input(GameMaster.game);
        currentState.update(delta);
        currentEyeHeight = lerp(currentEyeHeight, targetEyeHeight,
                Math.clamp(delta * EYE_LERP_SPEED, ZERO, 1.0f));
    }

    /** Selects or releases the nearest NPC on the configured focus-button edge. */
    private void updateFocus() {
        GameMaster game = GameMaster.game;
        if (game == null || game.isInventoryOpen() || game.isBackpackOpen()
                || game.isChatOpen() || BookService.bs.isOpen()) return;
        if (Controls.isPressed(ControlAction.THIRD_ACTION)) {
            Player.plyr.focus(NPCService.npcs.getClosest());
        }
    }

    /**
     * Updates or derives runtime state for change state according to the supplied arguments.
     * @param newState the {@link PlayerState} argument; state to enter
     */
    public void changeState(PlayerState newState) {
        if (currentState != null) currentState.exit();
        currentState = newState;
        currentState.enter();
    }

    /**
     * Updates movement for move according to the current physics and input state.
     * @param delta the {@code float} argument; frame time
     */
    public void move(float delta) {
        Player player = Player.plyr;
        World world = World.wrld;
        if (!isFollowingPath()) {
            player.setVelocity(new Vector3f(ZERO, player.getVelocity().y, ZERO));
        } else {
            GridPos target = path.get(pathIndex);
            float dx = target.x() + WAYPOINT_OFFSET - player.getPosition().x;
            float dz = target.z() + WAYPOINT_OFFSET - player.getPosition().z;
            if (dx * dx + dz * dz < PATH_DISTANCE_SQUARED) {
                pathIndex++;
                if (!isFollowingPath()) player.setVelocity(new Vector3f(ZERO, player.getVelocity().y, ZERO));
            } else {
                Vector3f direction = new Vector3f(dx, ZERO, dz);
                if (direction.lengthSquared() > ZERO) direction.normalize();
                Vector3f velocity = direction.mul(player.getSpeed());
                velocity.y = player.getVelocity().y;
                player.setVelocity(velocity);
            }
        }
        if (!(currentState instanceof SneakingState)) player.autoJump(player.getVelocity(), delta);
        player.collide(world, player.getVelocity(), delta);
    }

    /**
     * Updates movement for wasd according to the current physics and input state.
     * @param delta the {@code float} argument; frame time
     * @param cameraYaw the {@code float} argument; camera yaw
     * @param flying the {@code boolean} argument; flight flag
     */
    public void wasd(float delta, float cameraYaw, boolean flying) {
        Player player = Player.plyr;
        World world = World.wrld;
        if (GameMaster.game.isChatOpen() || GameMaster.game.isInventoryOpen() || BookService.bs.isOpen()) return;
        if (isFollowingPath()) { move(delta); return; }
        float x = Controls.getAxis(ControlAction.MOVE_X);
        float z = Controls.getAxis(ControlAction.MOVE_Y);
        if (Controls.isDown(ControlAction.MOVE_FORWARD)) z--;
        if (Controls.isDown(ControlAction.MOVE_BACKWARD)) z++;
        if (Controls.isDown(ControlAction.MOVE_LEFT)) x--;
        if (Controls.isDown(ControlAction.MOVE_RIGHT)) x++;
        Vector3f input = new Vector3f(x, ZERO, z);
        if (input.lengthSquared() > ZERO) {
            input.normalize();
            float yaw = (float) Math.toRadians(cameraYaw);
            float sin = (float) Math.sin(yaw), cos = (float) Math.cos(yaw);
            Vector3f velocity = new Vector3f(
                    (input.x * cos - input.z * sin) * player.getSpeed(),
                    player.getVelocity().y,
                    (input.x * sin + input.z * cos) * player.getSpeed());
            if (!flying) {
                if (!(currentState instanceof SneakingState)) player.autoJump(velocity, delta);
                player.collide(world, velocity, delta);
            }
        } else if (!flying) {
            player.collide(world, new Vector3f(ZERO, player.getVelocity().y, ZERO), delta);
        }
    }

    /**
     * Updates movement for fly according to the current physics and input state.
     * @param delta the {@code float} argument; frame time
     * @param yaw the {@code float} argument; camera yaw
     * @param flying the {@code boolean} argument; flight flag
     */
    public void fly(float delta, float yaw, boolean flying) {
        if (!Player.plyr.isOnGround()) wasd(delta, yaw, flying);
    }

    /**
     * Determines whether ground below is satisfied by the current state.
     * @return {@code true} if a solid block supports at least one player corner; otherwise {@code false}
     */
    public boolean hasGroundBelow(float testX, float testZ) {
        Player player = Player.plyr;
        World world = World.wrld;
        float epsilon = 0.001f;
        float halfWidth = player.getDimensions().x / 2.0f - epsilon;
        float halfDepth = player.getDimensions().z / 2.0f - epsilon;
        int y = (int) Math.floor(player.getPosition().y - 0.2f);
        for (float x : new float[]{testX - halfWidth, testX + halfWidth})
            for (float z : new float[]{testZ - halfDepth, testZ + halfDepth})
                if (world.isBlockSolid((int) Math.floor(x), y, (int) Math.floor(z))) return true;
        return false;
    }

    /**
     * Restricts sneaking velocity just enough to retain support while allowing edge travel.
     */
    public void adjustVelocity(float delta) {
        Player player = Player.plyr;
        World world = World.wrld;
        if (!(currentState instanceof SneakingState) || delta <= ZERO) return;
        Vector3f position = player.getPosition(), velocity = player.getVelocity();
        if (!player.isOnGround() && !hasGroundBelow(position.x, position.z)) return;
        float x = velocity.x * delta, z = velocity.z * delta;
        float step = 0.05f;
        while (x != ZERO && !hasGroundBelow(position.x + x, position.z)) x = towardZero(x, step);
        while (z != ZERO && !hasGroundBelow(position.x, position.z + z)) z = towardZero(z, step);
        while (x != ZERO && z != ZERO && !hasGroundBelow(position.x + x, position.z + z)) {
            x = towardZero(x, step); z = towardZero(z, step);
        }
        velocity.x = x / delta; velocity.z = z / delta;
    }

    private static float towardZero(float value, float amount) {
        return Math.abs(value) <= amount ? ZERO : value - Math.copySign(amount, value);
    }

    /**
     * Returns current state according to the current object state.
     * @return the {@link PlayerState} result; active state
     */
    public PlayerState getCurrentState() { return currentState; }
    /**
     * Sets current state and updates the associated state.
     * @param state the {@link PlayerState} argument; state to store
     */
    public void setCurrentState(PlayerState state) { currentState = state; }
    /**
     * Returns current eye height according to the current object state.
     * @return {@code float}; interpolated eye height
     */
    public float getCurrentEyeHeight() { return currentEyeHeight; }
    /**
     * Returns target eye height according to the current object state.
     * @return {@code float}; desired eye height
     */
    public float getTargetEyeHeight() { return targetEyeHeight; }
    /**
     * Sets target eye height and updates the associated state.
     * @param height the {@code float} argument; desired eye height
     */
    public void setTargetEyeHeight(float height) { targetEyeHeight = height; }
    /**
     * Determines whether falling is satisfied by the current state.
     * @return {@code true} if falling; otherwise {@code false}
     */
    public boolean isFalling() { return falling; }
    /**
     * Sets falling and updates the associated state.
     * @param value the {@code boolean} argument; falling flag
     */
    public void setFalling(boolean value) { falling = value; }
    /**
     * Returns forward according to the current object state.
     * @return {@code float}; movement direction angle
     */
    public float getForward() { return (float) Math.atan2(Player.plyr.getVelocity().z, Player.plyr.getVelocity().x); }
    /**
     * Determines whether following path is satisfied by the current state.
     * @return {@code true} if a waypoint remains; otherwise {@code false}
     */
    public boolean isFollowingPath() { return pathIndex < path.size(); }
    /**
     * Returns path according to the current object state.
     * @return the {@link List} result; active path
     */
    public List<GridPos> getPath() { return path; }
    /**
     * Sets path and updates the associated state.
     * @param value the {@link List} argument; path to follow
     */
    public void setPath(List<GridPos> value) { path = value != null ? value : List.of(); pathIndex = 0; }
    /**
     * Returns path index according to the current object state.
     * @return {@code int}; path index
     */
    public int getPathIndex() { return pathIndex; }
    /**
     * Sets path index and updates the associated state.
     * @param value the {@code int} argument; path index
     */
    public void setPathIndex(int value) { pathIndex = Math.max(0, value); }
    /**
     * Clears the active path.
     */
    public void clearPath() { path = List.of(); pathIndex = 0; }
}
