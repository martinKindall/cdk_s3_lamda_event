package com.codigomorsa.app;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import java.util.Map;


public class Pokedex implements RequestHandler<S3Event, Map<String, Object>> {

    @Override
    public Map<String, Object> handleRequest(S3Event event, Context context) {
        var data = event.getRecords().get(0);
        System.out.println("bucket: " + data.getS3().getBucket().getName());
        System.out.println("object: " + data.getS3().getObject().getKey());

        return null;
    }
}
