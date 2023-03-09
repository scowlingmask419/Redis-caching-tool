package fr.uge.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.*;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a caching tool by the help of Java Redis Client, Jedis.
 */
public class JedisCache {
    private static final Logger LOGGER = Logger.getLogger(JedisCache.class.getName());
    private static final Connection DATABASE_ACCESS_ERROR = null;
    private final JedisPool jedisPool;
    private final String url;
    private final Connection connection;

    /**
     * Creates and initialises cache from a local database.
     * @param url the local database address
     */
    public JedisCache(String url) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        this.jedisPool = new JedisPool(jedisPoolConfig, "localhost");
        this.url = Objects.requireNonNull(url);
        Connection connectionAttempt = initRedisCacheFromPostgreSql();
        if (initRedisCacheFromPostgreSql() == DATABASE_ACCESS_ERROR) {
            throw new IllegalStateException("A database access error occurs from " + url);
        }
        this.connection = connectionAttempt;
    }

    /**
     * Gets this local database address.
     * @return this local database address
     */
    public String getUrl() {
        return url;
    }

    /**
     * Initialises the Redis cache.
     * @return a @{@link Connection} to the Redis Client
     */
    private Connection initRedisCacheFromPostgreSql() {
        LOGGER.info("Try connection with " + url + " ...");
        try {
            Connection connection = DriverManager.getConnection(url, "postgres", "0000");
            if (connection != null) {
                LOGGER.info("Successfully connecting to " + url);
                LOGGER.info("Redis cache initialization ...");
                getInitRequest().accept(connection);
                return connection;
            }
            return connection;
        } catch (SQLException sqlException) {
            LOGGER.log(Level.SEVERE, "Unable to access to " + url);
        }
        return DATABASE_ACCESS_ERROR;
    }

    /**
     * Fetches the cache initialization data from the local database.
     * @return a {@link Consumer}
     */
    private Consumer<Connection> getInitRequest() {
        return connection -> {
            try {
                String sqlQuery = "SELECT bdpm_cis.cis, denom, cip7 FROM bdpm_cis, bdpm_ciscip2 WHERE bdpm_cis.cis = bdpm_ciscip2.cis ORDER BY cip7 LIMIT 1000;";
                PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
                ResultSet resultSet = preparedStatement.executeQuery();
                Jedis resource = jedisPool.getResource();
                Map<String, String> pairs;
                while (resultSet.next()) {
                    pairs = Map.of(
                            "cis", String.valueOf(resultSet.getLong("CIS")),
                            "denom", resultSet.getString("DENOM")
                    );
                    resource.hmset("drug:" + resultSet.getString("CIP7"), pairs);
                    LOGGER.info("cis : " + pairs.get("cis") +
                            "; denom : " + pairs.get("denom") +
                            "; cip7 : " + resultSet.getString("CIP7"));

                }
            } catch (SQLException sqlException) {
                LOGGER.log(Level.SEVERE, "Exception while fetching data from " + url);
                LOGGER.info("SQL Message : " + sqlException.getMessage());
            }
        };
    }

    /**
     * Fetches the row from the local database according to the key.
     * @param key the key (e.g., drug:"485469")
     * @return a {@link Consumer}
     */
    private Consumer<Connection> getKeyRequest(String key) {
        final long dbSize = jedisPool.getResource().dbSize();
        String cip7 = key.split(":")[1];
        return connection -> {
            try {
                String sqlQuery = "SELECT bdpm_cis.cis, denom, cip7 " +
                        "FROM bdpm_cis, bdpm_ciscip2 " +
                        "WHERE bdpm_cis.cis = bdpm_ciscip2.cis AND " +
                        "cip7 = " + cip7 + ";";
                PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
                ResultSet resultSet = preparedStatement.executeQuery();
                Jedis resource = jedisPool.getResource();
                if (resultSet.getFetchSize() < 1) {
                    LOGGER.warning("Unable to find cip7 " + cip7 + " from database");
                }
                resultSet.next();
                resource.hmset("drug:" + cip7, Map.of(
                        "cis", resultSet.getString("CIS"),
                        "denom", resultSet.getString("DENOM")));
                if (dbSize < jedisPool.getResource().dbSize()) {
                    LOGGER.log(Level.FINE, "Caching cip7 " + cip7 + " successfully");
                }
                else {
                    LOGGER.warning("");
                }
            } catch (SQLException sqlException) {
                LOGGER.log(Level.SEVERE, "Exception while fetching cip7 " + cip7 + " from " + url);
                LOGGER.info("SQL Message : " + sqlException.getMessage());
            }
        };
    }

    /**
     * Fetches the values of the specified key from the cache.
     * @param key the key (e.g., drug:"485469")
     * @return values of the specified key
     */
    public Map<String, String> getDrug(String key) {
        Objects.requireNonNull(key);
        Jedis resource = jedisPool.getResource();
        if (!resource.exists(key)) {
            LOGGER.warning("A cache-miss occurred for " + key);
            updateCache(key);
            return resource.hgetAll(key);
        } else {
            LOGGER.info("A cache-hit occurred for " + key);
            return resource.hgetAll(key);
        }
    }

    /**
     * Closes this Jedis connection.
     */
    public void closeConnection() {
        jedisPool.close();
    }

    /**
     * Updates the cache by adding a new key.
     * @param key the key to add (e.g., drug:"485469")
     */
    public void updateCache(String key) {
        Objects.requireNonNull(key);
        LOGGER.warning("Try caching ...");
        getKeyRequest(key).accept(connection);
    }
}
