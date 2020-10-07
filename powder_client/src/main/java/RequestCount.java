import java.sql.PseudoColumnUsage;

public class RequestCount {
    public int success;
    public int failure;

    public RequestCount() {
        success = 0;
        failure = 0;
    }

    public RequestCount(int success, int failure) {
        this.success = success;
        this.failure = failure;
    }
}
