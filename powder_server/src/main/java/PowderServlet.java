import java.io.IOException;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;

public class PowderServlet extends javax.servlet.http.HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        ServletContext context = getServletContext();
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();

        context.log("post");

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");
        if (!isUrlValidPost(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            res.setStatus(HttpServletResponse.SC_ACCEPTED);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: process url params in `urlParts`
            res.getWriter().write("Post Request Received!");
        }

    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        ServletContext context = getServletContext();
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();

        context.log("get");

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }

        String[] urlParts = urlPath.split("/");
        if (!isUrlValidGet(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);

            // for debug
//            res.getWriter().write(Arrays.toString(urlParts));
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: process url params in `urlParts`
            res.getWriter().write("It works!");
        }
    }

    private boolean isUrlValidPost(String[] urlParts) {
        // TODO: validate the request url path according to the API spec
        if (urlParts.length == 4
                && urlParts[1].equals("resort")
                && urlParts[2].equals("day")
                && urlParts[3].equals("top10vert")) {
            return true;
        }
        return false;
    }

    private boolean isUrlValidGet(String[] urlParts) {
        // TODO: validate the request url path according to the API spec
        if (urlParts.length > 1 && urlParts[1].equals("skiers")) {
            return true;
        }
        return false;
    }
}
