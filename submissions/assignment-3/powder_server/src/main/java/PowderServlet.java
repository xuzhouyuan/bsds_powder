import com.google.gson.Gson;
import io.swagger.client.model.LiftRide;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;

public class PowderServlet extends javax.servlet.http.HttpServlet {
    private LiftRideDao dao = new LiftRideDao();

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        ServletContext context = getServletContext();
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();

        context.log("post");

        String[] urlParts;
        try {
            urlParts = splitUrl(urlPath);
        } catch (InvalidUrlException e) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("{'message':'missing parameters'}");
            return;
        }

        if (urlParts.length == 3
                && urlParts[1].equals("skiers")
                && urlParts[2].equals("liftrides")) {
            writeNewLiftRide(req, res);
            return;
        }

        // default response to invalid url
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        res.getWriter().write("{'message':'invalid url'}");
        return;
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        ServletContext context = getServletContext();
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();

        context.log("get");

        // check we have a URL!
        String[] urlParts;
        try {
            urlParts = splitUrl(urlPath);
        } catch (InvalidUrlException e) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("{'message':'missing parameters'}");
            return;
        }

        if (urlParts[1].equals("health_check")) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        else if (urlParts[1].equals("resort")) {
            if (urlParts.length == 4
                    && urlParts[2].equals("day")
                    && urlParts[3].equals("top10vert")
            ) {
                getTopTenVert(req, res);
                return;
            }
        }
        else if (urlParts[1].equals("skiers")) {
            if (urlParts.length == 4
//                    && urlParts[2] in skierIDs
                    && urlParts[3].equals("vertical")
            ) {
                getSkierResortTotals(req, res, urlParts[2]);
                return;
            }
            else if (urlParts.length == 7
//                    && urlParts[2] in resortIDs
                    && urlParts[3].equals("days")
//                    && urlParts[4] in dayIDs
                    && urlParts[5].equals("skiers")
//                    && urlParts[6] in skiersIDs
            ) {
                getSkierDayVertical(req, res, urlParts[2], urlParts[4], urlParts[6]);
                return;
            }
        }

        // default response to invalid url
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        res.getWriter().write("{'message':'invalid url'}");
        return;

    }

    // POST
    private void writeNewLiftRide(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // verify and unpack request body to json, then write to db through dao
        BufferedReader reader = req.getReader();
        if (!reader.ready()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"message\":\"Invalid Inputs\"}");
            return;
        }
        Gson gson = new Gson();
        LiftRide ride = gson.fromJson(reader, LiftRide.class);
        if (StringUtils.isEmpty(ride.getDayID()) ||
                StringUtils.isEmpty(ride.getLiftID()) ||
                StringUtils.isEmpty(ride.getResortID()) ||
                StringUtils.isEmpty(ride.getSkierID()) ||
                StringUtils.isEmpty(ride.getTime())) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"message\":\"Invalid Inputs\"}");
            return;
        }

        dao.writeNewLiftRide(ride);

        res.setStatus(HttpServletResponse.SC_CREATED);
        return;
    }

    private void getSkierDayVertical(HttpServletRequest req, HttpServletResponse res,
                                      String resortID, String dayID, String skierID) throws IOException {
        if (StringUtils.isEmpty(resortID) ||
                StringUtils.isEmpty(dayID) ||
                StringUtils.isEmpty(skierID) ||
                Integer.parseInt(dayID) < 1 ||
                Integer.parseInt(dayID) > 366) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"message\":\"Invalid Inputs\"}");
            return;
        }

//        LiftRideDao dao = new LiftRideDao();
        int totalVert = -1;
        try {
            totalVert = dao.getSkierDayVertical(resortID, dayID, skierID);
        } catch (SQLException e) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write( "{\"message\":\"Data Not Found\"}");
            return;
        } catch (ConnectionLeakException e) {
            e.printStackTrace();
        }

        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write("{\"resortID\":\"" + resortID + "\"," +
                "\"totalVert\":" + totalVert + "}");
        return;
    }

    private void getSkierResortTotals(HttpServletRequest req, HttpServletResponse res,
                                        String skierID) throws IOException {
        // TODO: validate query
        // verify and unpack request body to json, then write to db through dao
        String queryString = req.getQueryString();
        String[] params = queryString.split("&");
        String resortID = "";
        for (String parameter:params) {
            String[] pair = parameter.split("=");
            // according to swagger
            if (pair[0].equals("resort")) {
                resortID = pair[1];
            }
        }
        if (resortID.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"message\":\"Invalid Inputs\"}");
            return;
        }

//        LiftRideDao dao = new LiftRideDao();
        int totalVert = -1;
        try {
            totalVert = dao.getSkierResortTotals(skierID, resortID);
        } catch (SQLException e) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("{\"message\":\"Data Not Found\"}");
            return;
        } catch (ConnectionLeakException e) {
            e.printStackTrace();
        }

        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write("{\"resortID\":\"" + resortID + "\"," +
                "\"totalVert\":" + totalVert + "}");
        return;
    }

    private void getTopTenVert(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String queryString = req.getQueryString();
        // TODO: process qs
        // res.getWriter().write(queryString);

        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write("{'message':'called getTopTenVert'}");
        return;
    }

    private String[] splitUrl(String urlPath) throws InvalidUrlException {
        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            throw new InvalidUrlException();
        }
        String[] urlParts = urlPath.split("/");
        if (urlParts.length == 0) {
            throw new InvalidUrlException();
        }
        return urlParts;
    }

}
