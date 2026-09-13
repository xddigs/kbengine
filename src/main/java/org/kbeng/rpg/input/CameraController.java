package org.kbeng.rpg.input;

import org.kbeng.engine.input.*;
import org.kbeng.rpg.graphics.Camera;

import org.kbeng.rpg.data.BlockPos;
import org.kbeng.rpg.data.Ray;
import org.kbeng.rpg.data.View;
import org.kbeng.rpg.entity.Player;
import org.kbeng.rpg.entity.states.SwimmingState;
import org.kbeng.rpg.entity.pathfinding.GridPos;
import org.kbeng.rpg.entity.pathfinding.PathFinder;
import org.kbeng.rpg.service.BookService;
import org.kbeng.engine.service.Service;
import org.kbeng.engine.utils.Settings;
import org.kbeng.rpg.wrld.GameMaster;
import org.kbeng.rpg.wrld.World;
import org.joml.Vector3f;

import static org.joml.Math.lerp;
import static org.lwjgl.glfw.GLFW.*;

/**
 * CameraController is an immutable carrier for camera controller state in the input subsystem.
 * Record semantics make instances cheap to pass across systems while preserving value-based equality and snapshot safety.
 * The controller translates user or system signals into deterministic runtime state transitions.
 * It implements Service<Camera>, providing a concrete strategy for this subsystem contract.
 */
public record CameraController(Camera camera) implements Service<Camera> {
    private static final float NORMAL_ZOOM = 18.0f;
    private static final float ZOOMED_ZOOM = NORMAL_ZOOM / 2.5f;
    private static final float INTERIOR_ZOOM = 15.5f;
    private static final float VERTICAL_OFFSET = 0.0f;
    private static final float DISTANCE = 500.0f;
    private static final Vector3f currentOffset = new Vector3f(0, 0, 0);
    private static final Vector3f panOffset = new Vector3f(0, 0, 0);
    private static final float NORMAL_CURSOR_WEIGHT = 0.35f;
    private static final float ZOOMED_CURSOR_WEIGHT = 0.50f;
    private static final float MAX_CURSOR_OFFSET_DISTANCE = 8.0f;
    private static final float MAX_PAN_OFFSET_DISTANCE = 36.0f;
    private static final float ROTATION_STEP = 45.0f;
    private static final float ROTATION_DRAG_STEP = 0.22f;
    private static final float ROTATION_DRAG_SMOOTHING = 14.0f;
    private static final float ROTATION_DRAG_DEADZONE = 0.01f;
    private static final float PAN_DRAG_STEP = 0.020f;
    private static final float PAN_DRAG_DEADZONE = 0.5f;
    private static final float PAN_RECENTER_SPEED = 2.8f;
    private static final float ZOOM_SCROLL_STEP = 1.15f;
    private static final float MIN_ZOOM = 6.0f;
    private static final float MAX_ZOOM = 30.0f;
    private static final float ZOOM_OFFSET_MIN = -10.0f;
    private static final float ZOOM_OFFSET_MAX = 10.0f;
    private static boolean mouseCaptured = false;
    private static GridPos lastGoal = null;
    private static float zoomOffset = 0.0f;
    private static float smoothedMouseYawRotation = 0.0f;

    /**
     * Updates the current state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param delta the {@code float} supplied as {@code delta}
     */
    public void update(GameMaster gameMaster, float delta) {
        camera.updateDamageTilt(delta);
        if (gameMaster.isInventoryOpen() || gameMaster.isChatOpen()) {
            lastGoal = null;
            return;
        }

        Player player = Player.plyr;
        if (!BookService.bs.isOpen()) {
            rotateAroundPlayer(player, delta);
        }
        if (Controls.isDown(ControlAction.MOVE_FORWARD)
                || Controls.isDown(ControlAction.MOVE_BACKWARD)
                || Controls.isDown(ControlAction.MOVE_LEFT)
                || Controls.isDown(ControlAction.MOVE_RIGHT)
                || Controls.getAxis(ControlAction.MOVE_X) != 0.0f
                || Controls.getAxis(ControlAction.MOVE_Y) != 0.0f) {
            player.clearPath();
        }

        if (player.getCurrentState() instanceof SwimmingState) {
            if (Controls.isDown(ControlAction.SWIM_UP)) {
                player.getVelocity().y = 4.0f;
            } else if (Controls.isDown(ControlAction.SWIM_DOWN)) {
                player.getVelocity().y = -3.0f;
            }
        }

        boolean isZoomed = Controls.isToggled(ControlAction.ZOOM);
        boolean isMouseRotating = applyMouseCameraRotation(player, delta);
        applyMousePan(delta, isMouseRotating);
        updateZoom(gameMaster, isZoomed);
        followPlayer(gameMaster, delta, isZoomed, isMouseRotating);
    }

    /**
     * Rotates the camera in quarter turns around the player. The cursor offset
     * rotates with the camera so it does not displace the orbit's pivot.
     * @param player the player used as the orbit pivot
     * @param delta the time since the last frame
     */
    private void rotateAroundPlayer(Player player, float delta) {
        float yawRotation = 0.0f;
        float pitchRotation = 0.0f;
        if (Controls.isDown(ControlAction.CAMERA_ROTATE_LEFT)) yawRotation -= ROTATION_STEP * delta;
        if (Controls.isDown(ControlAction.CAMERA_ROTATE_RIGHT)) yawRotation += ROTATION_STEP * delta;
        if (Controls.isDown(ControlAction.CAMERA_ROTATE_UP)) pitchRotation += ROTATION_STEP * delta;
        if (Controls.isDown(ControlAction.CAMERA_ROTATE_DOWN)) pitchRotation -= ROTATION_STEP * delta;
        if (yawRotation == 0.0f && pitchRotation == 0.0f) return;

        if (yawRotation != 0.0f) {
            rotateOrbitYaw(yawRotation);
        }
        if (pitchRotation != 0.0f) camera.rotatePitch(pitchRotation);
        positionCamera(player.getPosition(), new Vector3f(currentOffset).add(panOffset));
    }

    /**
     * Applies mouse drag camera rotation using Alt + right button.
     * @param player the player used as the orbit pivot
     * @param delta the {@code float} supplied as {@code delta}
     * @return {@code true} when the rotate gesture is active
     */
    private boolean applyMouseCameraRotation(Player player, float delta) {
        boolean rotateGesture = Controls.isDown(ControlAction.CAMERA_ROTATE_MODIFIER)
                && Controls.isDown(ControlAction.CAMERA_ROTATE_DRAG);
        if (!rotateGesture) {
            float smoothing = Math.min(1.0f, ROTATION_DRAG_SMOOTHING * delta);
            smoothedMouseYawRotation = lerp(smoothedMouseYawRotation, 0.0f, smoothing);
            return false;
        }

        float deltaX = Mouse.getDeltaX();
        if (Math.abs(deltaX) < PAN_DRAG_DEADZONE) {
            return true;
        }

        float sensitivity = Math.max(0.1f, Settings.getMouseSensitivity());
        float rawYawRotation = deltaX * ROTATION_DRAG_STEP * sensitivity;
        float smoothing = Math.min(1.0f, ROTATION_DRAG_SMOOTHING * delta);
        smoothedMouseYawRotation = lerp(smoothedMouseYawRotation, rawYawRotation, smoothing);

        if (Math.abs(smoothedMouseYawRotation) >= ROTATION_DRAG_DEADZONE) {
            rotateOrbitYaw(smoothedMouseYawRotation);
            positionCamera(player.getPosition(), new Vector3f(currentOffset).add(panOffset));
        }

        return true;
    }

    /**
     * Rotates the camera orbit around the player by yaw, matching keyboard behavior.
     * @param yawRotation the signed yaw rotation in degrees
     */
    private void rotateOrbitYaw(float yawRotation) {
        camera.rotateYaw(yawRotation);
        currentOffset.rotateY((float) Math.toRadians(-yawRotation));
        panOffset.rotateY((float) Math.toRadians(-yawRotation));
    }

    /**
     * Applies tactical drag-pan while preserving follow + cursor lead behavior.
     * @param delta the {@code float} supplied as {@code delta}
     * @param isMouseRotating whether the rotate gesture is active
     */
    private void applyMousePan(float delta, boolean isMouseRotating) {
        boolean isPanning = Controls.isDown(ControlAction.CAMERA_PAN_DRAG) && !isMouseRotating;
        if (isPanning) {
            float deltaX = Mouse.getDeltaX();
            float deltaY = Mouse.getDeltaY();
            if (Math.abs(deltaX) >= PAN_DRAG_DEADZONE || Math.abs(deltaY) >= PAN_DRAG_DEADZONE) {
                Vector3f right = camera.getRightVector();
                Vector3f forward = camera.getForwardVector();
                right.y = 0.0f;
                forward.y = 0.0f;
                if (right.lengthSquared() > 0.0001f) right.normalize();
                if (forward.lengthSquared() > 0.0001f) forward.normalize();

                float panScale = PAN_DRAG_STEP * Math.max(0.2f,
                        camera.getZoom() / NORMAL_ZOOM) * Math.max(delta, 0.016f) * 60.0f;
                panOffset.add(new Vector3f(right).mul(-deltaX * panScale))
                        .add(new Vector3f(forward).mul(deltaY * panScale));

                if (panOffset.lengthSquared() > MAX_PAN_OFFSET_DISTANCE * MAX_PAN_OFFSET_DISTANCE) {
                    panOffset.normalize().mul(MAX_PAN_OFFSET_DISTANCE);
                }
            }
            return;
        }

        float recenter = Math.min(1.0f, PAN_RECENTER_SPEED * delta);
        panOffset.x = lerp(panOffset.x, 0.0f, recenter);
        panOffset.z = lerp(panOffset.z, 0.0f, recenter);
    }

    /**
     * Updates the zoom.
     * @param isZoomed whether the zoom toggle is active
     */
    private void updateZoom(GameMaster gameMaster, boolean isZoomed) {
        View view = gameMaster.getViewService().getView();
        float defaultZoom = view == View.EXTERIOR ? NORMAL_ZOOM : INTERIOR_ZOOM;
        float scrollDelta = Mouse.getScrollY();
        if (scrollDelta != 0.0f
                && Controls.isDown(ControlAction.CAMERA_ROTATE_MODIFIER)) {
            zoomOffset = Math.clamp(zoomOffset - scrollDelta * ZOOM_SCROLL_STEP,
                    ZOOM_OFFSET_MIN, ZOOM_OFFSET_MAX);
        }
        float targetZoom = Math.clamp((isZoomed ? ZOOMED_ZOOM : defaultZoom) + zoomOffset,
                MIN_ZOOM, MAX_ZOOM);
        if (camera.getZoom() != targetZoom) {
            camera.setZoom(targetZoom);
        }
    }

    /**
     * Updates movement for follow player according to the current physics and input state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param delta the {@code float} supplied as {@code delta}
     * @param isZoomed whether the zoom toggle is active
     * @param isMouseRotating whether the mouse rotate gesture is active
     */
    private void followPlayer(GameMaster gameMaster, float delta,
                              boolean isZoomed, boolean isMouseRotating) {
        Player player = Player.plyr;
        Vector3f playerPos = player.getPosition();

        if (!isMouseRotating) {
            float mouseX = Mouse.getX();
            float mouseY = Mouse.getY();
            float screenWidth = gameMaster.getWindowWidth();
            float screenHeight = gameMaster.getWindowHeight();

            Vector3f mouseWorldPos = getMouseWorldPosition(
                    mouseX, mouseY, screenWidth, screenHeight, playerPos.y);

            Vector3f directionToMouse = new Vector3f(mouseWorldPos).sub(playerPos);
            directionToMouse.y = 0.0f;

            float cursorWeight = isZoomed ? ZOOMED_CURSOR_WEIGHT : NORMAL_CURSOR_WEIGHT;

            Vector3f targetOffset = new Vector3f(directionToMouse).mul(cursorWeight);

            if (targetOffset.length() > MAX_CURSOR_OFFSET_DISTANCE) {
                targetOffset.normalize().mul(MAX_CURSOR_OFFSET_DISTANCE);
            }

            float lerpFactor = Math.min(1.0f, 8.0f * delta);
            currentOffset.x = lerp(currentOffset.x, targetOffset.x, lerpFactor);
            currentOffset.z = lerp(currentOffset.z, targetOffset.z, lerpFactor);
        }

        positionCamera(playerPos, new Vector3f(currentOffset).add(panOffset));
    }

    /** Positions the camera behind a focus offset relative to the player. */
    private void positionCamera(Vector3f playerPos, Vector3f focusOffset) {
        Vector3f targetFocus = new Vector3f(playerPos).add(focusOffset);
        Vector3f camForward = camera.getForwardVector();
        Vector3f camUp = camera.getUpVector();
        Vector3f cameraPos = new Vector3f(targetFocus)
                .sub(new Vector3f(camForward).mul(DISTANCE))
                .add(new Vector3f(camUp).mul(VERTICAL_OFFSET));

        camera.getPosition().set(cameraPos);
    }

    /**
     * Handles click and applies its effect to the current interaction state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     * @param world the {@link World} supplied as {@code world}
     */
    private void click(GameMaster gameMaster, World world) {
        Player player = Player.plyr;
        boolean isRightClickDown = Controls.isDown(ControlAction.PATHFIND);
        if (!isRightClickDown) {
            lastGoal = null;
            return;
        }

        float mouseX = Mouse.getX();
        float mouseY = Mouse.getY();
        float screenWidth = gameMaster.getWindowWidth();
        float screenHeight = gameMaster.getWindowHeight();

        BlockPos blockPos = camera.highlight(world, player.getPosition(), mouseX, mouseY,
                screenWidth, screenHeight, false);

        if (blockPos == null) return;
        GridPos start = PathFinder.getPlayerGridPosition();
        GridPos goal = getGoalPosition(world, blockPos);
        if (goal == null) return;
        if (start.equals(goal)) return;
        if (goal.equals(lastGoal)) return;
        var path = PathFinder.findPath(world, start, goal);
        if (path.isEmpty()) return;
        player.setPath(path);
        lastGoal = goal;
    }

    /**
     * Updates movement for follow path according to the current physics and input state.
     * @param world the {@link World} supplied as {@code world}
     * @param delta the {@code float} supplied as {@code delta}
     */
    private void followPath(World world, float delta) {
        Player player = Player.plyr;
        if (player.isFollowingPath()) {
            player.move(delta);
        }
    }

    /**
     * Returns the goal position.
     * @param world the {@link World} supplied as {@code world}
     * @param blockPos the {@link BlockPos} supplied as {@code blockPos}
     * @return the {@link GridPos} representing the goal position
     */
    private GridPos getGoalPosition(World world, BlockPos blockPos) {
        int x = blockPos.x();
        int z = blockPos.z();

        GridPos highestAltitude = world.getHighestY(x + 0.5f, z + 0.5f);
        int walkY = highestAltitude.y();
        if (walkY < 0) return null;
        return new GridPos(x, walkY, z);
    }

    /**
     * Returns the mouse world position.
     * @param mouseX the {@code float} supplied as {@code mouseX}
     * @param mouseY the {@code float} supplied as {@code mouseY}
     * @param screenWidth the {@code float} supplied as {@code screenWidth}
     * @param screenHeight the {@code float} supplied as {@code screenHeight}
     * @param planeY the {@code float} supplied as {@code planeY}
     * @return the {@link Vector3f} representing the mouse world position
     */
    public Vector3f getMouseWorldPosition(float mouseX, float mouseY, float screenWidth,
                                          float screenHeight, float planeY) {
        Ray ray = camera.getMouseRay(mouseX, mouseY, screenWidth, screenHeight);
        if (Math.abs(ray.direction().y) < 0.0001f) {
            return new Vector3f(ray.origin());
        }

        float t = (planeY - ray.origin().y) / ray.direction().y;
        return new Vector3f(ray.origin()).add(new Vector3f(ray.direction()).mul(t));
    }

    /**
     * Deactivates mouse and releases its transient state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    private void releaseMouse(GameMaster gameMaster) {
        if (!mouseCaptured) return;
        glfwSetInputMode(gameMaster.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        mouseCaptured = false;
    }

    /**
     * Deactivates this object and releases its transient state.
     * @param gameMaster the {@link GameMaster} supplied as {@code gameMaster}
     */
    public void release(GameMaster gameMaster) {
        releaseMouse(gameMaster);
    }
}
