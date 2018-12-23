import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

public class DBCPDataSource {
     
    private static BasicDataSource ds = new BasicDataSource();
    private static DBCPDataSource datasource;
    static {
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUsername("root");
        ds.setPassword("root");
        ds.setUrl("jdbc:mysql://localhost/stock");
        ds.setInitialSize(10);
        ds.setMinIdle(20);
        ds.setMaxIdle(5);
        ds.setMaxOpenPreparedStatements(180);
    }
     
    public static DBCPDataSource getInstance() throws IOException, SQLException, PropertyVetoException {
        if (datasource == null) {
            datasource = new DBCPDataSource();
            return datasource;
        } else {
            return datasource;
        }
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}