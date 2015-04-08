package com.nearform.quartz;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvokeAsyncRequest;
import com.amazonaws.services.lambda.model.InvokeAsyncResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.TriggerKey;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.io.IOException;
import java.util.LinkedHashMap;
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

            log.info("Executing job "
                    + context.getJobDetail().getKey().toString()
                    + ", method:" + method
                    + ", url:" + url
                    + ", lambda:" + lambda
                    + ", payload:" + json);

            if (lambda == null && url == null)
                log.error("error!!!! missing lambda and url");
            else {
                if (url != null) executeRestJob(method, url, json);
                if (lambda != null) executeLambdaJob(lambda, json);
            }
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

            /*
                The default credential provider chain looks for credentials in this order

                Environment Variables – AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY. The SDK for Java uses the EnvironmentVariableCredentialsProvider class to load these credentials.

                Java System Properties – aws.accessKeyId and aws.secretKey. The SDK for Java uses the SystemPropertiesCredentialsProvider to load these credentials.

                The default credential profiles file – typically located at ~/.aws/credentials (this location may vary per platform), this credentials file is shared by many of the AWS SDKs and by the AWS CLI. The SDK for Java uses the ProfileCredentialsProvider to load these credentials.

                You can create a credentials file by using the aws configure command provided by the AWS CLI, or you can create it by hand-editing the file with a text editor. For information about the credentials file format, see AWS Credentials File Format.

                Instance profile credentials – these credentials can be used on EC2 instances, and are delivered through the Amazon EC2 metadata service. The SDK for Java uses the InstanceProfileCredentialsProvider to load these credentials.
             */

            credentials = new DefaultAWSCredentialsProviderChain().getCredentials();

        } catch (Exception e) {
            log.error("Cannot load the credentials from Environment Variables, Java System Properties and The default credential profiles file");

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
