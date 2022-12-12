package pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ConnectionCreator {
    static final Logger logger = LogManager.getLogger();
    private static final String PROPERTIES_PATH = "datares\\datebase.properties";
    private static final String DATABASE_URL;
    private static Lock locker = new ReentrantLock();
    private static AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static final Properties PROPERTIES = new Properties();
    private static ConnectionCreator connectionInstance;

    static {
        try {
            PROPERTIES.load(new FileReader(PROPERTIES_PATH));
            String driverName = (String) PROPERTIES.get("db.driver");
            Class.forName(driverName);
        } catch (ClassNotFoundException | IOException e) {
            logger.warn("failed to set connection parameters {}", e.getMessage());
        }
        DATABASE_URL = (String) PROPERTIES.get("db.url");
    }

    private ConnectionCreator() {

    }

    static ConnectionCreator getInstance() {
        if (!isInitialized.get()) {
            try {
                locker.lock();
                if (connectionInstance == null) {
                    connectionInstance = new ConnectionCreator();
                    isInitialized.set(true);
                }
            } finally {
                locker.unlock();
            }
        }
        return connectionInstance;
    }

    static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, PROPERTIES);
    }
}


