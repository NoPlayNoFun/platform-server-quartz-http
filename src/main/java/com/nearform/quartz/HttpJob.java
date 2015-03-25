package com.nearform.quartz;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvokeAsyncRequest;
import com.amazonaws.services.lambda.model.InvokeAsyncResult;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.TriggerKey;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.io.IOException;
import java.util.concurrent.Future;

import com.nearform.quartz.JobDataId;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(com.nearform.quartz.HttpJob.class);

	public void execute(JobExecutionContext context)
			throws JobExecutionException {


			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			String url = dataMap.getString("url");
            String lambda = dataMap.getString("lambda");
			String method = dataMap.getString("method");
			String json = dataMap.getString("payload");

			log.debug("Executing job "
					+ context.getJobDetail().getKey().toString()
                    + ", method:" + method
                    + ", url:" + url
                    + ", lambda:" + lambda
                    + ", payload:" + json);

            if (lambda == null && url == null)
                log.error("error!!!! missing lambda and url");
            else if (url != null)
                executeRestJob(method, url, json);
            else
                executeLambdaJob(lambda, json);
	}

    private void executeRestJob(String method, String url, String jsonPayload) {

        CloseableHttpClient httpclient = HttpClients.createDefault();

        try {
            CloseableHttpResponse response = null;
            StringEntity reqEntity = null;

            if (jsonPayload != null)
                reqEntity = new StringEntity(jsonPayload,
                        ContentType.create("application/json", "UTF-8"));

            switch (method.toUpperCase()) {
                case "POST":
                    HttpPost httppost = new HttpPost(url);
                    httppost.setEntity(reqEntity);
                    System.out.println("Executing post request: "
                            + httppost.getRequestLine());
                    response = httpclient.execute(httppost);
                    break;
                case "PUT":
                    HttpPut httpput = new HttpPut(url);
                    httpput.setEntity(reqEntity);
                    System.out.println("Executing put request: "
                            + httpput.getRequestLine());
                    response = httpclient.execute(httpput);
                    break;
                case "GET":
                    HttpGet httpget = new HttpGet(url);
                    System.out.println("Executing get request: "
                            + httpget.getRequestLine());
                    response = httpclient.execute(httpget);
                    break;
                case "DELETE":
                    HttpDelete httpdelete = new HttpDelete(url);
                    System.out.println("Executing delete request: "
                            + httpdelete.getRequestLine());
                    response = httpclient.execute(httpdelete);
                    break;
            }

            if (response != null) {
                try {
                    log.error("http response: " + response.getStatusLine());
                    EntityUtils.consume(response.getEntity());
                } finally {
                    response.close();
                }
            }
        } catch (IOException ioe) {
            log.error("executeRestJob - ", ioe);
        } finally {
            try {
                httpclient.close();
            } catch (IOException ioe) {
                log.error("executeRestJob - error in closing the http client", ioe);
            }
        }
    }


    private void executeLambdaJob(String lambdaFunction, String json) {
        AWSCredentials credentials = null;
        try {
            log.debug("create credentials");
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            log.error("Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.");
            return;
        }


        try {
            log.debug("create InvokeAsyncRequest");
            InvokeAsyncRequest invokeAsyncRequest = new InvokeAsyncRequest().withFunctionName(lambdaFunction).withInvokeArgs(json);
            log.debug("end InvokeAsyncRequest");

            log.debug("create AWSLambdaAsyncClient");
            AWSLambdaAsyncClient client = new AWSLambdaAsyncClient(credentials);
            log.debug("end AWSLambdaAsyncClient");

            log.debug("set region");
            Region usWest2 = Region.getRegion(Regions.US_WEST_2);
            client.setRegion(usWest2);
            log.debug("end set region");

            log.debug("calling aws lambda");
            InvokeAsyncResult invokeAsyncResult = client.invokeAsync(invokeAsyncRequest);
            log.debug("end calling aws lambda");

            log.debug("invokeAsyncResultFuture: " + invokeAsyncResult);
        } catch (AmazonServiceException ase) {
            log.error("Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon Lambda, but was rejected with an error response for some reason.");
            log.error("Error Message:    " + ase.getMessage());
            log.error("HTTP Status Code: " + ase.getStatusCode());
            log.error("AWS Error Code:   " + ase.getErrorCode());
            log.error("Error Type:       " + ase.getErrorType());
            log.error("Request ID:       " + ase.getRequestId());
        }

    }

}
