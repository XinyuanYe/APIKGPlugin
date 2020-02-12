package KGtree;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

public class KGnode {

    String key;
    String reference;
    public Vector<KGnode> children = new Vector<>();

    // Utility function to create a new tree node
    public static KGnode newNode(String key, String methodName)
    {

        KGnode temp = new KGnode();
        temp.key = key;
        if (methodName != null) {
            temp.reference = methodName;
        }
        return temp;
    }

    public String getReference() {
        return this.reference;
    }

    public String toString() {
        return this.key;
    }

    // Prints the n-ary tree level wise
    public static void LevelOrderTraversal(KGnode root) {
        if (root == null)
            return;

        // Standard level order traversal code
        // using queue
        Queue<KGnode > q = new LinkedList<>(); // Create a queue
        q.add(root); // Enqueue root
        while (!q.isEmpty()) {
            int n = q.size();

            // If this node has children
            while (n > 0) {
                // Dequeue an item from queue
                // and print it
                KGnode p = q.peek();
                q.remove();
                System.out.print(p.key + " ");

                // Enqueue all children of
                // the dequeued item
                for (int i = 0; i < p.children.size(); i++)
                    q.add(p.children.get(i));
                n--;
            }

            // Print new line between two levels
            System.out.println();
        }
    }



}
