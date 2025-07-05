package me.seeloewen;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;

public class PathFinder
{
    enum DiagonalDir
    {
        TopRight,
        BottomRight,
        TopLeft,
        BottomLeft
    }

    public static int gridWidth;
    public static int gridHeight;

    public static int originX;
    public static int originY;

    public static ArrayList<Node> optimalPath = new ArrayList<>();

    private static Node[] nodes;
    private static PriorityQueue<Node> openQueue = new PriorityQueue<>();
    private static HashSet<Node> openSet = new HashSet<>(); //Used for running .contains on the open nodes, faster than the queue
    private static HashSet<Node> closedSet = new HashSet<>();
    private static Node target = new Node(-70, 65);
    private static Vector2i currentPos;

    public static void findPath(Vector2f currentPos, int radius, boolean cull)
    {
        PathFinder.currentPos = new Vector2i((int) Math.floor(currentPos.x), (int) Math.floor(currentPos.y));
        gridWidth = Math.abs(radius) * 2 + 1;
        gridHeight = Math.abs(radius) * 2 + 1;
        originX = PathFinder.currentPos.x - Math.abs(radius);
        originY = PathFinder.currentPos.y - Math.abs(radius);
        nodes = new Node[gridHeight * gridWidth];
        createNodes(true);

        long startTime = System.nanoTime();
        addOpenNode(new Node(PathFinder.currentPos.x, PathFinder.currentPos.y)); //Temp start node, replace with actual start

        Node currentNode;
        while (!openQueue.isEmpty())
        {
            //Get the next node
            currentNode = pollOpenNode();

            System.out.println("Current Node: " + currentNode.x + " " + currentNode.y);
            if (Main.ENABLEUI) Main.wnd.update(currentNode.x, currentNode.y, NodeState.VISITED);

            //If the target was found, reconstruct the path and save it
            if (currentNode.equals(target))
            {
                System.out.println("Found optimal path");
                long endTime = System.nanoTime();
                System.out.println("Pathfinding took " + (endTime - startTime) / 1000000 + "ms");

                if (Main.ENABLEUI) constructPath(currentNode);
                return;
            }

            //Add the current node to the closed list and expand around it
            closedSet.add(currentNode);
            expandNode(currentNode);
        }

        if (cull)
        {
            System.out.println("No path found. Retrying without culling.");
            findPath(currentPos, radius, false);
        }
        System.out.println("Absolutely no path found, giving up");
    }

    public static void constructPath(Node currentNode)
    {
        Main.wnd.update(originX, originY, NodeState.OPTIMAL);
        while (currentNode.predecessor != null)
        {
            Main.wnd.update(currentNode.x, currentNode.y, NodeState.OPTIMAL);
            currentNode = currentNode.predecessor;
        }
    }

    public static void createNodes(boolean cull)
    {
        for (int dx = 0; dx < gridWidth; dx++)
        {
            for (int dy = 0; dy < gridHeight; dy++)
            {
                //Create nodes for the pathfinder
                int x = originX + dx;
                int y = originY + dy;

                int i = (-originX + x) + (-originY + y) * gridWidth;
                Node n = new Node(x, y);

                //Replace with actual implementation for the specific cases
                if (Main.perlin.GetNoise(x, y) > 0.1) n.state = NodeState.OCCUPIED;

                if (Main.ENABLEUI) Main.wnd.update(x, y, n.state);

                nodes[i] = n;
            }
        }
    }

    public static void expandNode(Node currentNode)
    {
        Node[] neighbours = new Node[8];
        neighbours[0] = getNode(currentNode.x - 1, currentNode.y); //left
        neighbours[1] = getNode(currentNode.x + 1, currentNode.y); //right
        neighbours[2] = getNode(currentNode.x, currentNode.y + 1); //above
        neighbours[3] = getNode(currentNode.x, currentNode.y - 1); //below
        neighbours[4] = nodeDiagonallyReachable(neighbours, getNode(currentNode.x + 1, currentNode.y + 1), DiagonalDir.TopRight); //top right
        neighbours[5] = nodeDiagonallyReachable(neighbours, getNode(currentNode.x - 1, currentNode.y + 1), DiagonalDir.TopLeft); //top left
        neighbours[6] = nodeDiagonallyReachable(neighbours, getNode(currentNode.x + 1, currentNode.y - 1), DiagonalDir.BottomRight); //bottom right
        neighbours[7] = nodeDiagonallyReachable(neighbours, getNode(currentNode.x - 1, currentNode.y - 1), DiagonalDir.BottomLeft); //bottom left

        //Check all neighbours and add them to the open list or update the optimal path to them
        for (Node neighbour : neighbours)
        {
            if (neighbour == null || neighbour.state == NodeState.OCCUPIED || closedSet.contains(neighbour)) continue;

            double g = getDistance(neighbour, currentNode); //Calculate the length on this path to the neighbour

            //If the neighbour is already on list but this path to it is not better, continue to the next neighbour
            if (openSet.contains(neighbour) && g >= neighbour.g) continue;

            double f = g + getDistance(neighbour, target); //Get the direct distance of the neighbour to the goal

            //If this way is better or completely new, update or add it to the list
            neighbour.predecessor = currentNode;
            neighbour.g = g;
            neighbour.f = f;

            //Either update or add the neighbour
            if (openSet.contains(neighbour))
            {
                //Re-add the neighbour to update the priority index - I have no idea why there isn't a better way for this (java moment)
                openQueue.remove(neighbour);
                openQueue.add(neighbour);
            }
            else
            {
                addOpenNode(neighbour);
            }
        }
    }

    public static Node nodeDiagonallyReachable(Node[] neighbours, Node n, DiagonalDir dir)
    {
        if(n == null) return null;

        //Evaluate whether the two nodes that could block diagonal movement to the specified dir actually do block it. If so, return null
        //to skip the check of the node later on entirely
        return switch (dir)
        {
            case TopRight -> neighbours[1].state == NodeState.FREE && neighbours[2].state == NodeState.FREE ? n : null;
            case TopLeft -> neighbours[0].state == NodeState.FREE && neighbours[2].state == NodeState.FREE ? n : null;
            case BottomRight ->  neighbours[1].state == NodeState.FREE && neighbours[3].state == NodeState.FREE ? n : null;
            case BottomLeft -> neighbours[0].state == NodeState.FREE && neighbours[3].state == NodeState.FREE ? n : null;
        };
    }

    public static double getDistance(Node n1, Node n2)
    {
        return Math.sqrt(Math.pow((n2.y - n1.y), 2) + Math.pow((n2.x - n1.x), 2)); //tactical pythagoras
    }

    public static void addOpenNode(Node n)
    {
        openQueue.add(n);
        openSet.add(n);
    }

    public static Node pollOpenNode()
    {
        //Get the most prioritized node
        Node n = openQueue.poll();
        openSet.remove(n);
        return n;
    }

    public static Node getNode(int x, int y)
    {
        //Create nodes for the pathfinder
        if(x < originX || y < originY || x >= originX + gridWidth || y >= originY + gridHeight)
        {
            return null; //Check if the coords are even in the allowed area (this took me 2 hours of debugging to figure out)
        }

        int i = (x - originX) + (y - originY) * gridWidth;

        if (i < 0 || i >= nodes.length) return null;
        return nodes[i];
    }
}
