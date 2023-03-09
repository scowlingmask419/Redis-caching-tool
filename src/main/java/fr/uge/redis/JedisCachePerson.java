package fr.uge.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class JedisCachePerson {
    private static final Logger LOGGER = Logger.getLogger(JedisCachePerson.class.getName());
    private static final int MIN_SIZE = 3;
    private final JedisPool jedisPool;
    private final String url;
    private final List<Person> personList;

    public JedisCachePerson(String url, List<Person> personList) {
        if (personList.size() < MIN_SIZE) {
            throw new IllegalArgumentException("personList < MIN_SIZE");
        }
        this.personList = Objects.requireNonNull(personList);
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        this.jedisPool = new JedisPool(jedisPoolConfig, "localhost");
        this.url = Objects.requireNonNull(url);
    }

    public void fillDatabaseWithThreePersons() {
        List<Person> personList1 = personList.subList(0, 3);
        Jedis resource = jedisPool.getResource();
        for (var person : personList1) {
            Objects.requireNonNull(person);
            LOGGER.info("Caching " + person.getFirstName() + " " + person.getLastName() + " ...");
            resource.hmset(String.valueOf(person.getId()), Map.of(
                    "firstName", person.getFirstName(),
                    "lastName", person.getLastName()
            ));
        }
    }

    public void readPersonValuesFromDatabase() {
        Jedis resource = jedisPool.getResource();
        personList.forEach(person -> {
            String key = String.valueOf(person.getId());
            if (resource.exists(key)) {
                Map<String, String> pairs = resource.hgetAll(key);
                LOGGER.info("Reads " + pairs.get("firstName") + " " + pairs.get("lastName") + " from database");
            }
        });
    }
}
