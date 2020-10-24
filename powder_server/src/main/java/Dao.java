import io.swagger.client.model.LiftRide;
import org.apache.commons.dbcp2.BasicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Dao {
    protected static BasicDataSource dataSource;

    public Dao() {
       dataSource = DBCPDataSource.getDataSource();
    }
}

class LiftRideDao extends Dao {
    public LiftRideDao() {
        super();
    }

    public void createLiftRide(LiftRide ride) {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        String insertQueryStatement = "INSERT INTO LiftRides (skierID, dayID, time, resortID, liftID) " +
                "VALUES (?,?,?,?,?)";
        try {
            conn = dataSource.getConnection();
            preparedStatement = conn.prepareStatement(insertQueryStatement);
            preparedStatement.setString(1, ride.getSkierID());
            preparedStatement.setString(2, ride.getDayID());
            preparedStatement.setString(3, ride.getTime());
            preparedStatement.setString(4, ride.getResortID());
            preparedStatement.setString(5, ride.getLiftID());

            // execute insert SQL statement
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}

