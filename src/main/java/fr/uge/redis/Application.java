package fr.uge.redis;

import java.util.List;
import java.util.Map;

public class Application {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/postgres";

    public static void main(String[] args) {
        JedisCachePerson jedisCachePerson = new JedisCachePerson(
                DEFAULT_URL,
                List.of(new Person("Dexter", "McPherson"),
                        new Person("Randall", "Weems"),
                        new Person("T.J.", "Detweiler"))
        );

        jedisCachePerson.fillDatabaseWithThreePersons();
        jedisCachePerson.readPersonValuesFromDatabase();

        /*
        final String keyInCache = "drug:3171914";
        final String keyNotInCache = "drug:5870734";

        long start = System.currentTimeMillis();
        JedisCache jedisCache = new JedisCache(DEFAULT_URL);
        Map<String, String> drug = jedisCache.getDrug(keyInCache);
        long end = System.currentTimeMillis();
        long timeElapsed = end - start;
        System.out.println("Time elapsed : " + timeElapsed);


       Map<String, String> drugNotInCache = jedisCache.getDrug(keyNotInCache);

         */
    }
}
//3314779
//"AMLODIPINE/VALSARTAN MYLAN 10 mg/160 mg, comprimé pelliculé"