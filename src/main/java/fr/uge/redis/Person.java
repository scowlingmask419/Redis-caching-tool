package fr.uge.redis;

import java.util.UUID;

/**
 * Represents a person.
 */
public class Person {
    private final long id;
    private final String firstName;
    private final String lastName;

    /**
     * Creates a person.
     * @param firstName the person's firstname
     * @param lastName the person's lastname
     */
    public Person(String firstName, String lastName) {
        this.id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Gets this person id.
     * @return this id
     */
    public long getId() {return id;}

    /**
     * Gets this person firstname.
     * @return this firstname
     */
    public String getFirstName() {return firstName;}

    /**
     * Gets this person lastname.
     * @return this lastname
     */
    public String getLastName() {return lastName;}

}
