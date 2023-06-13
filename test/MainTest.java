import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;

import java.io.File;
import java.io.IOException;

public class MainTest {

    public static void main(String[] args) {
        captureScreenshot();
    }

    public static void captureScreenshot() {
        // Replace videoElement with the appropriate video element in your Java application
        File screenshotFile = new File("./img/output.png");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addPart("file", new FileBody(screenshotFile, ContentType.create("image/png"), "output.png"));

        HttpEntity multipartEntity = builder.build();
        HttpPost request = new HttpPost("http://ec2-18-228-199-45.sa-east-1.compute.amazonaws.com:5000/photos");
        //HttpPost request = new HttpPost("http://192.168.1.34:5000/photos");
        request.setEntity(multipartEntity);

        HttpClient httpClient = HttpClients.createDefault();
        try {
            HttpResponse response = httpClient.execute(request);
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
