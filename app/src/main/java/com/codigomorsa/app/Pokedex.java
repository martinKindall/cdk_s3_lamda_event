package com.codigomorsa.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectCustomLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.util.Map;


public class Pokedex implements RequestHandler<S3Event, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(S3Event event, Context context) {
        var data = event.getRecords().get(0);
        System.out.println("bucket: " + data.getS3().getBucket().getName());
        System.out.println("object: " + data.getS3().getObject().getKey());

        RekognitionClient rekClient = RekognitionClient.builder()
                .region(Region.US_EAST_1)
                .build();

        String prediction = detectImageLabels(rekClient, data.getS3().getBucket().getName(), data.getS3().getObject().getKey());

        SesV2Client sesv2Client = SesV2Client.builder()
                .region(Region.US_EAST_1)
                .build();

        sendPredictionViaEmail(sesv2Client, prediction);

        return null;
    }

    private static void sendPredictionViaEmail(SesV2Client sesv2Client, String prediction) {

        var emailRequest = SendEmailRequest.builder()
                .fromEmailAddressIdentityArn(System.getenv("SES_ARN"))
                .fromEmailAddress(System.getenv("EMAIL_ADDR"))
                .destination(Destination.builder()
                        .toAddresses(System.getenv("TO_ADDR"))
                        .build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder()
                                        .data("Tu pokemon es...")
                                        .build())
                                .body(Body.builder()
                                        .text(Content.builder()
                                                .data(prediction)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        sesv2Client.sendEmail(emailRequest);
    }

    private static String detectImageLabels(RekognitionClient rekClient, String bucket, String name) {
        var detectLabelsRequest = DetectCustomLabelsRequest.builder()
                .image(Image.builder().s3Object(S3Object.builder().bucket(bucket).name(name).build()).build())
                .projectVersionArn(System.getenv("REKOGNITION_MODEL"))
                .build();

        var response = rekClient.detectCustomLabels(detectLabelsRequest);
        var labels = response.customLabels();

        return "label: " + labels.get(0).name() + ", accuracy: " + labels.get(0).confidence();
    }
}
