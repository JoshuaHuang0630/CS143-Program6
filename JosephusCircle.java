/**
 * A circular singly linked list representing the Josephus circle.
 * Supports adding people and eliminating every k-th person.
 */
public class JosephusCircle {
    private Person head;   // entry point into the circle
    private Person tail;   // always points to the last node (next -> head)
    private int size;

    public JosephusCircle() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Adds a new person to the end of the circle.
     */
    public void add(String name) {
        Person newPerson = new Person(name);
        if (head == null) {
            head = newPerson;
            tail = newPerson;
            newPerson.setNext(head); // points to itself
        } else {
            tail.setNext(newPerson);
            newPerson.setNext(head); // maintain circular structure
            tail = newPerson;
        }
        size++;
    }

    /**
     * Returns the current number of people in the circle.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns whether only one person remains.
     */
    public boolean hasOneRemaining() {
        return size == 1;
    }

    /**
     * Returns the name of the sole survivor (when size == 1).
     */
    public String getSurvivorName() {
        if (size == 1) {
            return head.getName();
        }
        throw new IllegalStateException("More than one person remains in the circle.");
    }

    /**
     * Eliminates every k-th person from the circle, starting the count
     * from the current head.
     *
     * @param k the elimination count (count to k, eliminate that person)
     * @return the name of the eliminated person
     */
    public String eliminate(int k) {
        if (size == 0) {
            throw new IllegalStateException("The circle is empty.");
        }

        // Walk to the person just BEFORE the k-th person so we can splice them out.
        // We need (k-1) steps from current head.
        Person prev = tail; // start: prev is behind head (tail points to head)
        Person current = head;

        for (int i = 1; i < k; i++) {
            prev = current;
            current = current.getNext();
        }

        // 'current' is now the person to eliminate
        String eliminatedName = current.getName();

        if (size == 1) {
            // Last person standing
            head = null;
            tail = null;
        } else {
            // Splice out 'current'
            prev.setNext(current.getNext());

            // If we removed the head, update head
            if (current == head) {
                head = current.getNext();
            }
            // If we removed the tail, update tail
            if (current == tail) {
                tail = prev;
            }

            // Advance head to start the next count right after the eliminated node
            head = prev.getNext();
        }

        size--;
        return eliminatedName;
    }

    /**
     * Returns a comma-separated list of all remaining people in circle order.
     */
    public String listRemaining() {
        if (size == 0) return "(none)";
        StringBuilder sb = new StringBuilder();
        Person current = head;
        for (int i = 0; i < size; i++) {
            sb.append(current.getName());
            if (i < size - 1) sb.append(", ");
            current = current.getNext();
        }
        return sb.toString();
    }
}
