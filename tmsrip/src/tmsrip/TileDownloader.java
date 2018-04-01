/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tmsrip;

import java.io.File;
import java.io.FileOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 *
 * @author thomas
 */
public class TileDownloader implements Runnable {
    private final String url;
    private final String destination;
    
    public TileDownloader(String inUrl, String inDestination) {
        url = inUrl;
        destination = inDestination;
    }
    
    @Override
    public void run() {
        downloadFile(url, destination);
    }
    
    public static void downloadFile(String url, String destination) {        
        int statusCode;
        String statusPhrase;
        File outfile = new File(destination);
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        
        // Initiate the download of the file.
        try {
            response = client.execute(new HttpGet(url));
        } catch (Exception e) {
            System.out.println("?");
        }
        
        statusCode = response.getStatusLine().getStatusCode();
        statusPhrase = response.getStatusLine().getReasonPhrase();
        
        // Good HTTP codes are less than 2xx or 3xx. 400 and above represents
        // an error.
        if ( statusCode >= 400 ) {
            System.out.println("Received HTTP " + statusCode + ": " + statusPhrase);
            // If we recieve HTTP 429, we've made too many requests, so we
            // should stop.
            if ( statusCode == 429 )
                System.exit(1);
        }
        
        // Save the file.
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {
                FileOutputStream outstream = new FileOutputStream(outfile);
                entity.writeTo(outstream);
            } catch (java.io.FileNotFoundException e) {
                System.out.println("Error: Output file/directory not found.");
            } catch (java.io.IOException e) {
                System.out.println("Error: Problem writing output file.");
            }
        } else {
            System.out.println("Error: no response from server.");
        }
    }

    
}
