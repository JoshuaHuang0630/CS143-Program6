/**
 * Represents a person in the Josephus circle.
 * Acts as a node in a circular linked list.
 */
public class Person {
    private String name;
    private Person next;

    public Person(String name) {
        this.name = name;
        this.next = null;
    }

    public String getName() {
        return name;
    }

    public Person getNext() {
        return next;
    }

    public void setNext(Person next) {
        this.next = next;
    }

    @Override
    public String toString() {
        return name;
    }
}
