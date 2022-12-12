package pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectionPool {
    static final Logger logger = LogManager.getLogger();
    private static final int POOL_SIZE = 4;
    private static Lock locker = new ReentrantLock();

    private static AtomicBoolean isInitialized = new AtomicBoolean(false);
    private static ConnectionPool connectionInstance;

    private BlockingQueue<ProxyConnection> queue;

    private ConnectionPool() {
        ConnectionCreator connectionCreator = ConnectionCreator.getInstance();
        this.queue = new LinkedBlockingQueue<>();
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                queue.offer(new ProxyConnection(connectionCreator.createConnection()));
            } catch (SQLException e) {
                logger.warn("Failed to create connection {}", e.getMessage());
            }
        }
    }

    public static ConnectionPool getInstance() {
        if (!isInitialized.get()) {
            try {
                locker.lock();
                if (connectionInstance == null) {
                    connectionInstance = new ConnectionPool();
                    isInitialized.set(true);
                }
            } finally {
                locker.unlock();
            }
        }
        return connectionInstance;
    }

    public Connection getConnection() {
        Connection connection = null;
        try {
            connection = queue.take();
        } catch (InterruptedException e) {
            logger.warn("failed to take connection from pool {}", e.getMessage());
        }
        return connection;
    }

    public void returnConnection(Connection connection) {
        try {
            if (connection instanceof ProxyConnection) {
                ProxyConnection proxy = (ProxyConnection) connection;
                queue.put(proxy);
            } else {
                logger.warn("Enemy connection");
            }
        } catch (InterruptedException e) {
            logger.warn("failed to return connection to pool {}", e.getMessage());
        }
    }

    private void deregisterDriver() {
        DriverManager.getDrivers().asIterator().forEachRemaining(driver -> {
            try {
                DriverManager.deregisterDriver(driver);
            } catch (SQLException e) {
                logger.warn("failed to deregister driver {}", e.getMessage());
            }
        });
    }

    public void destroyPool() {
        for (int i = 0; i < this.queue.size(); i++) {
            try {
                queue.take().reallyClose();
            } catch (InterruptedException e) {
                logger.warn("failed to close connection {}", e.getMessage());
            }
        }
        this.deregisterDriver();
    }

}
