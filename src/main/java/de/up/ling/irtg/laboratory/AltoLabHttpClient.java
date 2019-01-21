/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.laboratory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.apache.http.HttpHeaders.USER_AGENT;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author koller
 */
public class AltoLabHttpClient {
    private ObjectMapper mapper = new ObjectMapper();
    private HttpClient httpClient;

    public AltoLabHttpClient(String altolabUrl, String username, String password) throws IOException {
        this.httpClient = HttpClientBuilder.create().build();

        HttpPost loginRequest = new HttpPost(altolabUrl + "login?next=%2F");
        loginRequest.setHeader("User-Agent", USER_AGENT);

        List<NameValuePair> urlParameters = new ArrayList<>();

        urlParameters.add(new BasicNameValuePair("name", username));
        urlParameters.add(new BasicNameValuePair("passwd", password));
        urlParameters.add(new BasicNameValuePair("next", "/"));

        loginRequest.setEntity(new UrlEncodedFormEntity(urlParameters));
        HttpResponse response = httpClient.execute(loginRequest);

        // 302 FOUND => authentication successful, Alto Lab redirected me from /login to / as intended
        // 200 OK => auth failed, Alto Lab showed the login form again
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
            throw new IOException("Login to Alto Lab failed for user '" + username + "', check your password.");
        }
    }

    /**
     * Performs a HTTP POST request to the given URL, posting the given object
     * as JSON data. If anything went wrong, throws an IOException. Otherwise,
     * returns the body of the HTTP response.
     *
     * @param url
     * @param json
     * @return
     * @throws IOException
     */
    String postJson(String url, Object data) throws IOException {
        String json = mapper.writeValueAsString(data);
        HttpPost request = new HttpPost(url);
        StringEntity params = new StringEntity(json);
        request.addHeader("content-type", "application/json");
        request.setEntity(params);
        HttpResponse response = httpClient.execute(request);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            System.err.println("JSON for HTTP error was: " + json);
            throw new IOException("HTTP error: " + response.getStatusLine().toString() + " while trying to POST to " + url);
        } else {
            return EntityUtils.toString(response.getEntity());
        }
    }

    /**
     * Performs a synchronous HTTP GET request to the given URL.
     * If anything went wrong, throws an IOException. Otherwise,
     * returns the body of the HTTP response.
     * 
     * @param url
     * @return
     * @throws IOException 
     */
    String get(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        HttpResponse response = httpClient.execute(request);
        
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("HTTP error: " + response.getStatusLine().toString());
        } else {
            return EntityUtils.toString(response.getEntity());
        }
    }
}
