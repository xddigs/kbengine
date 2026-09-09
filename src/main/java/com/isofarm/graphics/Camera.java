package com.isofarm.graphics;

import com.isofarm.data.BlockData;
import com.isofarm.data.BlockPos;
import com.isofarm.data.BlockShape;
import com.isofarm.data.Ray;
import com.isofarm.item.Bucket;
import com.isofarm.utils.K;
import com.isofarm.utils.Settings;
import com.isofarm.wrld.World;
import com.isofarm.wrld.GameMaster;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.joml.Math.lerp;

/**
 * Encapsulates the state and operations required by camera within the game runtime.
 */
public class Camera implements CameraView {
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 80.0f;

    private static final float DEFAULT_YAW = 45.0f;
    private static final float PITCH = 35.2643897f;
    private static final float MIN_PITCH = 15.0f;
    private static final float MAX_PITCH = 80.0f;
    private static final float MAX_DAMAGE_TILT = 8.0f;
    private static final float DAMAGE_TILT_RECOVERY = 7.0f;

    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 2000.0f;
    private static final float ZOOM_SPEED = 0.2f;
    private float yaw = DEFAULT_YAW;
    private float pitch = PITCH;
    private final Vector3f position;
    private final Matrix4f projectionMatrix;
    private float zoom = 25.0f;
    private float aspectRatio = 1.0f;
    private float damageTilt;
    private boolean tiltRight;
    private BlockPos lastHit;
    private int lastHitNormalX;
    private int lastHitNormalY;
    private int lastHitNormalZ;

    /**
     * Creates a new {@code Camera} instance.
     *
     * @param width                the {@code float} supplied as {@code width}
     * @param height               the {@code float} supplied as {@code height}
     * @param renderDistanceChunks the {@code int} supplied as {@code renderDistanceChunks}
     */
    public Camera(float width, float height, int renderDistanceChunks) {
        this.position = new Vector3f();
        this.projectionMatrix = new Matrix4f();
        updateProjection(width, height, renderDistanceChunks);
    }

    /**
     * Updates the projection.
     *
     * @param width                the {@code float} supplied as {@code width}
     * @param height               the {@code float} supplied as {@code height}
     * @param renderDistanceChunks the {@code int} supplied as {@code renderDistanceChunks}
     */
    public void updateProjection(float width, float height, int renderDistanceChunks) {
        this.aspectRatio = width / Math.max(height, 1.0f);
        float farPlane = (renderDistanceChunks + 2) * 16.0f;
        projectionMatrix.identity().ortho(-zoom * aspectRatio,
                zoom * aspectRatio, -zoom, zoom, NEAR_PLANE, Math.max(farPlane, FAR_PLANE));
    }

    /**
     * {@inheritDoc}
     * Returns the projection matrix.
     *
     * @return the {@link Matrix4f} representing the projection matrix
     */
    @Override
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    /**
     * {@inheritDoc}
     * Returns the view matrix.
     *
     * @return the {@link Matrix4f} representing the view matrix
     */
    @Override
    public Matrix4f getViewMatrix() {
        return new Matrix4f()
                .identity()
                .rotateZ((float) Math.toRadians(damageTilt))
                .rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(yaw))
                .translate(-position.x, -position.y, -position.z);
    }

    /**
     * {@inheritDoc}
     * Returns the position.
     *
     * @return the {@link Vector3f} representing the position
     */
    @Override
    public Vector3f getPosition() {
        return position;
    }

    /**
     * {@inheritDoc}
     * Returns the pitch.
     *
     * @return {@code float}; the pitch
     */
    @Override
    public float getPitch() {
        return pitch;
    }

    /**
     * {@inheritDoc}
     * Returns the yaw.
     *
     * @return {@code float}; the yaw
     */
    @Override
    public float getYaw() {
        return yaw;
    }

    /**
     * Rotates the camera horizontally by the supplied number of degrees.
     * @param degrees signed horizontal rotation in degrees
     */
    public void rotateYaw(float degrees) {
        yaw = (yaw + degrees) % K.Camera.FULL_DEGREES;
        if (yaw < 0.0f) yaw += K.Camera.FULL_DEGREES;
    }

    /**
     * Rotates the camera vertically by the supplied number of degrees
     * @param degrees signed vertical rotation in degrees
     */
    public void rotatePitch(float degrees) {
        pitch = Math.clamp(pitch + degrees, MIN_PITCH, MAX_PITCH);
    }

    /**
     * Applies a small screen tilt in response to player damage.
     * Consecutive impacts alternate direction to avoid a permanent visual bias.
     *
     * @param amount the {@code float} argument; the received damage
     */
    public void applyDamageTilt(float amount) {
        if (amount <= 0.0f) return;

        tiltRight = !tiltRight;
        float strength = Math.clamp(0.75f + amount * 0.2f,
                0.75f, MAX_DAMAGE_TILT);
        damageTilt = tiltRight ? strength : -strength;
    }

    /**
     * Smoothly restores the camera to its normal roll.
     *
     * @param delta the {@code float} argument; frame time in seconds
     */
    public void updateDamageTilt(float delta) {
        if (delta <= 0.0f || damageTilt == 0.0f) return;

        damageTilt = lerp(damageTilt, 0.0f,
                Math.clamp(DAMAGE_TILT_RECOVERY * delta, 0.0f, 1.0f));
        if (Math.abs(damageTilt) < 0.01f) damageTilt = 0.0f;
    }

    /**
     * Sets the position.
     *
     * @param x the {@code float} supplied as {@code x}
     * @param y the {@code float} supplied as {@code y}
     * @param z the {@code float} supplied as {@code z}
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    /**
     * Returns the forward vector.
     *
     * @return the {@link Vector3f} representing the forward vector
     */
    public Vector3f getForwardVector() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        Vector3f direction = new Vector3f();
        direction.x = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        direction.y = (float) (-Math.sin(pitchRad));
        direction.z = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));
        return direction.normalize();
    }

    /**
     * Returns the right vector.
     *
     * @return the {@link Vector3f} representing the right vector
     */
    public Vector3f getRightVector() {
        Vector3f forward = getForwardVector();
        Vector3f right = new Vector3f();
        right.set(-forward.z, 0.0f, forward.x);
        return right.normalize();
    }

    /**
     * Returns the zoom.
     *
     * @return {@code float}; the zoom
     */
    public float getZoom() {
        return zoom;
    }

    /**
     * Sets the zoom.
     *
     * @param zoom the {@code float} supplied as {@code zoom}
     */
    public void setZoom(float zoom) {
        this.zoom = lerp(this.zoom, Math.clamp(zoom, MIN_ZOOM, MAX_ZOOM), ZOOM_SPEED);
        updateProjection(aspectRatio, 1.0f, Settings.getRenderDistance());
    }

    /**
     * Returns the up vector.
     *
     * @return the {@link Vector3f} representing the up vector
     */
    public Vector3f getUpVector() {
        Vector3f forward = getForwardVector();
        Vector3f right = getRightVector();
        Vector3f up = new Vector3f();
        right.cross(forward, up);
        return up.normalize();
    }

    /**
     * Returns the mouse ray.
     *
     * @param mouseX       the {@code float} supplied as {@code mouseX}
     * @param mouseY       the {@code float} supplied as {@code mouseY}
     * @param screenWidth  the {@code float} supplied as {@code screenWidth}
     * @param screenHeight the {@code float} supplied as {@code screenHeight}
     * @return the {@link Ray} representing the mouse ray
     */
    public Ray getMouseRay(float mouseX, float mouseY, float screenWidth, float screenHeight) {
        float ndcX = (2.0f * mouseX / screenWidth) - 1.0f;
        float ndcY = 1.0f - (2.0f * mouseY / screenHeight);

        Matrix4f invVP = new Matrix4f();
        projectionMatrix.mul(getViewMatrix(), invVP).invert();

        Vector3f rayOrigin = new Vector3f();
        invVP.transformProject(ndcX, ndcY, -1.0f, rayOrigin);
        return new Ray(rayOrigin, getForwardVector());
    }

    /**
     * Transforms this object according to the supplied values.
     *
     * @param world        the {@link World} supplied as {@code world}
     * @param playerPos    the {@link Vector3f} supplied as {@code playerPos}
     * @param mouseX       the {@code float} supplied as {@code mouseX}
     * @param mouseY       the {@code float} supplied as {@code mouseY}
     * @param screenWidth  the {@code float} supplied as {@code screenWidth}
     * @param screenHeight the {@code float} supplied as {@code screenHeight}
     * @param smartFilter  the {@code boolean} supplied as {@code smartFilter}
     * @return the {@link BlockPos} representing the highlight result
     */
    public BlockPos highlight(World world, Vector3f playerPos, float mouseX, float mouseY,
                              float screenWidth, float screenHeight, boolean smartFilter) {
        Ray ray = getMouseRay(mouseX, mouseY, screenWidth, screenHeight);
        boolean isBucket = Settings.selectedItem instanceof Bucket;
        lastHit = raycast(world, playerPos, ray.origin(), ray.direction(), smartFilter, isBucket);
        if (lastHit == null) {
            return null;
        }

        return lastHit;
    }

    /**
     * Calculates the value represented by raycast from the current state.
     *
     * @param world         the {@link World} supplied as {@code world}
     * @param playerPos     the {@link Vector3f} supplied as {@code playerPos}
     * @param origin        the {@link Vector3f} supplied as {@code origin}
     * @param direction     the {@link Vector3f} supplied as {@code direction}
     * @param isSmartFilter the {@code boolean} supplied as {@code isSmartFilter}
     * @param isBucket      the {@code boolean} supplied as {@code isBucket}
     * @return the {@link BlockPos} representing the raycast result
     */
    private BlockPos raycast(World world, Vector3f playerPos,
                             Vector3f origin, Vector3f direction,
                             boolean isSmartFilter, boolean isBucket) {
        float tileSize = K.World.TILE_SIZE;

        int x = (int) Math.floor(origin.x / tileSize);
        int y = (int) Math.floor(origin.y / tileSize);
        int z = (int) Math.floor(origin.z / tileSize);

        int stepX = Float.compare(direction.x, 0.0f);
        int stepY = Float.compare(direction.y, 0.0f);
        int stepZ = Float.compare(direction.z, 0.0f);

        float tDeltaX = stepX != 0 ? Math.abs(tileSize / direction.x) : Float.POSITIVE_INFINITY;
        float tDeltaY = stepY != 0 ? Math.abs(tileSize / direction.y) : Float.POSITIVE_INFINITY;
        float tDeltaZ = stepZ != 0 ? Math.abs(tileSize / direction.z) : Float.POSITIVE_INFINITY;

        float tMaxX = stepX > 0 ? ((x + 1) * tileSize - origin.x) / direction.x : stepX < 0 ? (x * tileSize - origin.x) / direction.x : Float.POSITIVE_INFINITY;
        float tMaxY = stepY > 0 ? ((y + 1) * tileSize - origin.y) / direction.y : stepY < 0 ? (y * tileSize - origin.y) / direction.y : Float.POSITIVE_INFINITY;
        float tMaxZ = stepZ > 0 ? ((z + 1) * tileSize - origin.z) / direction.z : stepZ < 0 ? (z * tileSize - origin.z) / direction.z : Float.POSITIVE_INFINITY;

        int previousX = x;
        int previousY = y;
        int previousZ = z;
        World.DoorHit doorHit = world.raycastDoor(origin, direction);

        do {
            // Interaction must use the same visibility volume as rendering. In
            // underground/interior view the ray may pass through cells that
            // are outside the player's visible space; those cells must not be
            // considered interaction targets.
            boolean isVisible = GameMaster.game.getViewService().isVisible(
                    new Vector3f(x + 0.5f, y + 0.5f, z + 0.5f));

            var interactiveBlock = world.getInteractiveBlockAt(x, y, z);
            if (isVisible && interactiveBlock != null
                    && !interactiveBlock.getType().isDoor()) {
                float distToPlayer = playerPos.distance(x + 0.5f, y + 0.5f, z + 0.5f);
                if (distToPlayer > Settings.getMaxInteractionDistance()) return null;

                lastHitNormalX = previousX - x;
                lastHitNormalY = previousY - y;
                lastHitNormalZ = previousZ - z;
                return new BlockPos(interactiveBlock.getType(), x, y, z);
            }

            byte block = world.getBlockTypeAt(x, y, z);
            BlockData data = BlockData.fromId(block);
            boolean hasBlock = data != null && data != BlockData.AIR;
            float cellExit = Math.min(tMaxX, Math.min(tMaxY, tMaxZ));
            BlockShape blockShape = hasBlock
                    ? world.getBlockShapeAt(x, y, z) : BlockShape.FULL_CUBE;
            BlockShape.RayHit shapeHit = hasBlock && !blockShape.isFullCube()
                    ? blockShape.raycast(origin, direction, x, y, z) : null;
            boolean hitsShape = data != null && (blockShape.isFullCube()
                    || (shapeHit != null && shapeHit.distance() <= cellExit));

            if (isVisible && hasBlock && hitsShape && (!data.isFluid() || isBucket)) {
                boolean isTransparentObject = data.isTransparent()
                        || data == BlockData.OAK_LEAVES
                        || data == BlockData.SPRUCE_LEAVES;

                if (!isSmartFilter || !isTransparentObject) {
                    float blockCenterX = x + 0.5f;
                    float blockCenterY = y + 0.5f;
                    float blockCenterZ = z + 0.5f;

                    float distToPlayer = playerPos.distance(
                            blockCenterX,
                            blockCenterY,
                            blockCenterZ
                    );

                    if (distToPlayer <= Settings.getMaxInteractionDistance()) {
                        lastHitNormalX = shapeHit == null ? previousX - x : shapeHit.normalX();
                        lastHitNormalY = shapeHit == null ? previousY - y : shapeHit.normalY();
                        lastHitNormalZ = shapeHit == null ? previousZ - z : shapeHit.normalZ();

                        return new BlockPos(data, x, y, z);
                    } else {
                        return null;
                    }
                }
            }

            if (doorHit != null && doorHit.distance() <= cellExit) {
                var door = doorHit.block();
                float distance = playerPos.distance(
                        door.getX() + 0.5f, door.getY() + 1.0f, door.getZ() + 0.5f);
                if (distance > Settings.getMaxInteractionDistance()) return null;

                lastHitNormalX = previousX - x;
                lastHitNormalY = previousY - y;
                lastHitNormalZ = previousZ - z;
                return new BlockPos(door.getType(), door.getX(), door.getY(), door.getZ());
            }

            previousX = x;
            previousY = y;
            previousZ = z;

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }
        } while (!(tMaxX > 2000.0f) || !(tMaxY > 2000.0f) || !(tMaxZ > 2000.0f));

        return null;
    }

    /**
     * Returns the last hit normal x.
     *
     * @return {@code int}; the last hit normal x
     */
    public int getLastHitNormalX() {
        return lastHitNormalX;
    }

    /**
     * Returns the last hit normal y.
     *
     * @return {@code int}; the last hit normal y
     */
    public int getLastHitNormalY() {
        return lastHitNormalY;
    }

    /**
     * Returns the last hit normal z.
     *
     * @return {@code int}; the last hit normal z
     */
    public int getLastHitNormalZ() {
        return lastHitNormalZ;
    }
}
