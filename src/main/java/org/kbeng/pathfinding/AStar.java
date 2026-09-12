package org.kbeng.pathfinding;

import org.kbeng.wrld.World;

import java.util.*;

/**
 * Represents the astar component of the kbengine runtime.
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
public class AStar {
    private static final float STRAIGHT_COST = 1.0f;
    private static final float DIAGONAL_COST = 1.414f;
    private static final float UP_COST = 1.2f;
    private static final float DOWN_COST = 1.0f;

    /**
     * Finds and returns the path.
     * @param world the {@link World} supplied as {@code world}
     * @param start the {@link GridPos} supplied as {@code start}
     * @param goal the {@link GridPos} supplied as {@code goal}
     * @return the {@link List} representing the located path
     */
    public static List<GridPos> findPath(World world, GridPos start, GridPos goal) {
        if (start == null || goal == null) return List.of();
        if (start.equals(goal)) return List.of(start);
        if (!canStandAt(world, goal)) return List.of();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::fCost));
        Map<GridPos, Node> nodes = new HashMap<>();
        Set<GridPos> closedSet = new HashSet<>();

        Node startNode = new Node(start, null, 0.0f, heuristic(start, goal));
        openSet.add(startNode);
        nodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (closedSet.contains(current.position())) continue;
            if (current.position().equals(goal)) return reconstructPath(current);

            closedSet.add(current.position());
            for (Neighbor neighbor : getNeighbors(world, current.position())) {
                GridPos position = neighbor.position();
                if (closedSet.contains(position)) {
                    continue;
                }

                float tentativeG = current.gCost() + neighbor.cost();
                Node existing = nodes.get(position);
                if (existing == null || tentativeG < existing.gCost()) {
                    Node next = new Node(position, current, tentativeG, heuristic(position, goal));
                    nodes.put(position, next);
                    openSet.add(next);
                }
            }
        }

        return List.of();
    }

    /**
     * Returns the neighbors.
     * @param world the {@link World} supplied as {@code world}
     * @param current the {@link GridPos} supplied as {@code current}
     * @return the {@link List} representing the neighbors
     */
    private static List<Neighbor> getNeighbors(World world, GridPos current) {
        List<Neighbor> neighbors = new ArrayList<>(8);

        addHorizontalNeighbor(world, neighbors, current, 1, 0, STRAIGHT_COST);
        addHorizontalNeighbor(world, neighbors, current, -1, 0, STRAIGHT_COST);
        addHorizontalNeighbor(world, neighbors, current, 0, 1, STRAIGHT_COST);
        addHorizontalNeighbor(world, neighbors, current, 0, -1, STRAIGHT_COST);

        addDiagonalNeighbor(world, neighbors, current, 1, 1);
        addDiagonalNeighbor(world, neighbors, current, 1, -1);
        addDiagonalNeighbor(world, neighbors, current, -1, 1);
        addDiagonalNeighbor(world, neighbors, current, -1, -1);

        return neighbors;
    }

    /**
     * Adds the horizontal neighbor.
     * @param world the {@link World} supplied as {@code world}
     * @param neighbors the {@link List} supplied as {@code neighbors}
     * @param current the {@link GridPos} supplied as {@code current}
     * @param dx the {@code int} supplied as {@code dx}
     * @param dz the {@code int} supplied as {@code dz}
     * @param baseCost the {@code float} supplied as {@code baseCost}
     */
    private static void addHorizontalNeighbor(World world, List<Neighbor> neighbors,
                                              GridPos current, int dx, int dz, float baseCost) {
        int x = current.x() + dx;
        int z = current.z() + dz;
        GridPos sameLevel = new GridPos(x, current.y(), z);

        if (canStandAt(world, sameLevel)) {
            neighbors.add(new Neighbor(sameLevel, baseCost));
            return;
        }

        GridPos up = new GridPos(x, current.y() + 1, z);
        if (canStandAt(world, up)) {
            neighbors.add(new Neighbor(up, baseCost * UP_COST));
            return;
        }

        GridPos down = new GridPos(x, current.y() - 1, z);
        if (canStandAt(world, down)) {
            neighbors.add(new Neighbor(down, baseCost * DOWN_COST));
        }
    }

    /**
     * Adds the diagonal neighbor.
     * @param world the {@link World} supplied as {@code world}
     * @param neighbors the {@link List} supplied as {@code neighbors}
     * @param current the {@link GridPos} supplied as {@code current}
     * @param dx the {@code int} supplied as {@code dx}
     * @param dz the {@code int} supplied as {@code dz}
     */
    private static void addDiagonalNeighbor(World world, List<Neighbor> neighbors, GridPos current, int dx, int dz) {
        GridPos side1 = new GridPos(current.x() + dx, current.y(), current.z());
        GridPos side2 = new GridPos(current.x(), current.y(), current.z() + dz);

        if (!canStandAt(world, side1) || !canStandAt(world, side2)) {
            return;
        }

        addHorizontalNeighbor(world, neighbors, current, dx, dz, DIAGONAL_COST);
    }

    /**
     * Checks whether the stand at condition is met.
     * @param world the {@link World} supplied as {@code world}
     * @param position the {@link GridPos} supplied as {@code position}
     * @return {@code true} if stand at; otherwise {@code false}
     */
    private static boolean canStandAt(World world, GridPos position) {
        int x = position.x();
        int y = position.y();
        int z = position.z();

        if (y < 0) {
            return false;
        }

        byte feet = world.getBlockTypeAt(x, y, z);
        byte head = world.getBlockTypeAt(x, y + 1, z);
        byte ground = world.getBlockTypeAt(x, y - 1, z);
        return feet == 0 && head == 0 && ground != 0;
    }

    /**
     * Calculates the value represented by heuristic from the current state.
     * @param a the {@link GridPos} supplied as {@code a}
     * @param b the {@link GridPos} supplied as {@code b}
     * @return {@code float}; the heuristic result
     */
    private static float heuristic(GridPos a, GridPos b) {
        float dx = Math.abs(a.x() - b.x());
        float dy = Math.abs(a.y() - b.y());
        float dz = Math.abs(a.z() - b.z());

        float minXZ = Math.min(dx, dz);
        float maxXZ = Math.max(dx, dz);
        return (DIAGONAL_COST * minXZ) + (STRAIGHT_COST * (maxXZ - minXZ)) + dy;
    }

    /**
     * Refreshes dependent runtime state for reconstruct path.
     * @param node the {@link Node} supplied as {@code node}
     * @return the {@link List} representing the reconstruct path result
     */
    private static List<GridPos> reconstructPath(Node node) {
        LinkedList<GridPos> path = new LinkedList<>();
        Node current = node;

        while (current != null) {
            path.addFirst(current.position());
            current = current.parent();
        }

        return path;
    }

    /**
     * Immutable value object containing neighbor.
     */
    private record Neighbor(GridPos position, float cost) {}
}
