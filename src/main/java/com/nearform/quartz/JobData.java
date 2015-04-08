package com.nearform.quartz;

import java.util.LinkedHashMap;

public class JobData {

  private String url;
  private String lambda;
  private String name;
  private String method;
  private String group;
  private LinkedHashMap<String, Object> payload;
  private long timestamp;

  public String getUrl() { return url; }
  public String getLambda() {return lambda; }
  public String getName() { return name; }
  public String getMethod() { return method; }
  public String getGroup() { return group; }
  public LinkedHashMap<String, Object> getPayload() { return payload; }
  public long getTimestamp() { return timestamp; }

  public void setUrl(String s) { url = s; }
  public void setLambda(String s) { lambda = s; }
  public void setName(String s) { name = s; }
  public void setMethod(String s) { method = s; }
  public void setGroup(String s) { group = s; }
  public void setPayload(LinkedHashMap<String, Object> s) { payload = s; }
  public void setTimestamp(long i) { timestamp = i; }
}
