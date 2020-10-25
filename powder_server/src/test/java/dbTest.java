import io.swagger.client.model.LiftRide;

import java.sql.SQLException;

public class dbTest {
    public static void main(String[] args) throws SQLException {
        LiftRideDao dao = new LiftRideDao();
        LiftRide ride = new LiftRide();
        ride.setDayID("dayID");
        ride.setLiftID("hhliftID");
        ride.setResortID("resortID");
        ride.setSkierID("skierID");
        ride.setTime("dminute");
        dao.writeNewLiftRide(ride);
        System.out.println("check db entry");
    }
}
