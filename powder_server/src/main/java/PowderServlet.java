import java.io.IOException;
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
        return;
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
        if (urlParts.length == 0) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }

        if (urlParts[1].equals("resort")) {
            if (urlParts.length == 4
                    && urlParts[2].equals("day")
                    && urlParts[3].equals("top10vert")
            ) {
                urlValidResponse(res);
                String queryString = req.getQueryString();
                // TODO: process qs
                // res.getWriter().write(queryString);

                return;
            }
        }
        else if (urlParts[1].equals("skiers")) {
            if (urlParts.length == 4
//                    && urlParts[2] in skierIDs
                    && urlParts[3].equals("vertical")
            ) {
                urlValidResponse(res);
                return;
            }
            else if (urlParts.length == 7
//                    && urlParts[2] in resortIDs
                    && urlParts[3].equals("days")
//                    && urlParts[4] in dayIDs
                    && urlParts[5].equals("skiers")
//                    && urlParts[6] in skiersIDs
            ) {
                urlValidResponse(res);
                return;
            }
        }

        // default to invalid url
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return;

//            // for debug
//            res.getWriter().write(Arrays.toString(urlParts));

    }

    private boolean isUrlValidPost(String[] urlParts) {
        // TODO: validate the request url path according to the API spec
        if (urlParts.length == 3
                && urlParts[1].equals("skiers")
                && urlParts[2].equals("liftrides")) {
            return true;
        }
        return false;
    }

    private void urlValidResponse(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        // do any sophisticated processing with urlParts which contains all the url params
        // TODO: ?
        // res.getWriter().write("It works!");
        return;
    }
}
