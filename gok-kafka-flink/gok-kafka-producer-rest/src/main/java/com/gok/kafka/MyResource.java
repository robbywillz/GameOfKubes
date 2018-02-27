package com.gok.kafka;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

/**
 * Root resource (exposed at "myresource" path)
 */
@Path("myresource")
public class MyResource {

    
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public void send(String message) {
    	System.out.println("message: " + message);
    	KafkaMessager.send(message);
    }
}
