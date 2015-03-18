package com.nearform.quartz;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.TriggerKey;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.io.IOException;

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

public class HttpJob implements Job {

	public void execute(JobExecutionContext context)
			throws JobExecutionException {

		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {

			JobDataMap dataMap = context.getJobDetail().getJobDataMap();

			String url = dataMap.getString("url");
			String method = dataMap.getString("method");
			String jsonPayload = dataMap.getString("payload");

			System.out.println("Executing job "
					+ context.getJobDetail().getKey().toString() + ", method:"
					+ method + ", url:"
					+ url + ", payload:" + jsonPayload);

			TriggerKey triggerKey = context.getTrigger().getKey();

			String key = triggerKey.getGroup() + JobDataId.groupDelimiter + triggerKey.getName() + JobDataId.triggerJobDelimiter + context.getJobDetail().getKey().getName();

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
					System.out
							.println("http response: " + response.getStatusLine());
					EntityUtils.consume(response.getEntity());
				} finally {
					response.close();
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				httpclient.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}
