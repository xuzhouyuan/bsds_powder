import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.gson.Gson;
import io.swagger.client.model.LiftRide;

import java.sql.SQLException;
import java.util.List;

public class ConsumerWorker implements Runnable {
    private static final String QUEUE_ENDPOINT = System.getenv("SQS_ENDPOINT");
    private AmazonSQS sqs;

    private LiftRideDao dao;

    public ConsumerWorker(LiftRideDao dao) {
        AWSCredentials credentials = new BasicAWSCredentials(
                System.getenv("ADMIN_ACCESS_KEY"),
                System.getenv("ADMIN_SECRET_KEY")
        );
        sqs = AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.US_WEST_2)
                .build();
        this.dao = dao;
    }

    @Override
    public void run() {
        while (true) {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(QUEUE_ENDPOINT)
                    .withMaxNumberOfMessages(10);
            List<Message> sqsMessages = sqs.receiveMessage(receiveMessageRequest).getMessages();

            for (Message m: sqsMessages) {
                Gson gson = new Gson();
                LiftRide ride = gson.fromJson(m.getBody(), LiftRide.class);

                try {
                    dao.writeNewLiftRide(ride);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ConnectionLeakException e) {
                    e.printStackTrace();
                }

                sqs.deleteMessage(new DeleteMessageRequest()
                        .withQueueUrl(QUEUE_ENDPOINT)
                        .withReceiptHandle(m.getReceiptHandle()));
            }
        }
    }
}
