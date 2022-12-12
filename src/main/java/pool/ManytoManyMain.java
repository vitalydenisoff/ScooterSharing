package pool;

import pool.ConnectionPool;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class ManytoManyMain {
    public static void main(String[] args) {
        ConnectionPool connectionPool=ConnectionPool.getInstance();
        Connection connection= connectionPool.getConnection();
        try(
            Statement statement=connection.createStatement())
        {
            String sql="INSERT INTO orders (order_mileage) VALUES(113)";
            statement.execute(sql);
            System.out.println("ok");
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            connectionPool.returnConnection(connection);
        }
    }
}
