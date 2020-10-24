import io.swagger.client.model.LiftRide;

public class dbTest {
    public static void main(String[] args) {
        LiftRideDao dao = new LiftRideDao();
        LiftRide ride = new LiftRide();
        ride.setDayID("dayID");
        ride.setLiftID("hhliftID");
        ride.setResortID("resortID");
        ride.setSkierID("skierID");
        ride.setTime("dminute");
        dao.createLiftRide(ride);
        System.out.println("check db entry");
    }
}
