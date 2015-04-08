
Schedule HTTP callouts with a given payload through HTTP
========================================================

# To add a job to the queue with callback url:

    POST localhost:8080/scheduler/api
    {
      "timestamp": 1397733237027,
      "method" : "POST",
      "url": "http://localhost:3000",
      "payload": {"Hello":"world"}
    }
    
    return : 
    {
      "jobId": "http::1e7dbcb1-a7dc-4a93-ad07-3281ac00b436_73423260-f694-4376-8bcf-2af13aed3f48"
    }
    
will schedule a callout ```POST http://localhost:3000/``` with ```{"Hello": "world"}``` as the body.

# To add a job to the queue with lambda call:
    POST localhost:8080/scheduler/api
    {
      "timestamp": 1397733237027,
      "method" : "POST",
      "lambda": "helloworld",
      "payload": {"Hello":"world"}
    }
    
    return : 
    {
        "jobId": "http::1e7dbcb1-a7dc-4a93-ad07-3281ac00b436_73423260-f694-4376-8bcf-2af13aed3f48"
    }

You will receive back a JSON object that has the group::name pair. You will need to hang on to this if you ever intend to cancel the job.

# To update a job from the queue:
    You will need the jobId that return from post during the PUT

    PUT localhost:8080/scheduler/api
    {
      "jobId": "http::1e7dbcb1-a7dc-4a93-ad07-3281ac00b436_73423260-f694-4376-8bcf-2af13aed3f48" 
      "timestamp": 1397733237027,
      "method" : "POST",
      "lambda": "helloworld",
      "payload": {"Hello":"world"}
    }
    
    return:
    {
        "jobId": "http::1e7dbcb1-a7dc-4a93-ad07-3281ac00b436_73423260-f694-4376-8bcf-2af13aed3f48"
    }
    
# To remove a job from the queue:

    DELETE localhost:8080/scheduler/api/group::name
    
    return:
    {
        "jobId": "true"
    }

This will remove a previously scheduled job with the unique key of group::name. The JSON returned will show failure if the job cannot be found or it was not possible to cancel it. 

# HTTP verbs supported

Currently supports Http POST, JSON content and one single date to add to the schedule and DELETE with the group::name on the url to remote a scheduled job.

# run server:

    mvn jetty:run
    
Will download, build, run the server on the default port 8080

    mvn -Djetty.port=8090 jetty:run

Will download, build, run the server on port 8090

# compile war file to /target directory:

    mvn package 

# Testing server

If you use a browser (or use curl with -GET) to navigate to the URL that the scheduler is running on your should receive this type of error:

HTTP ERROR 405
Problem accessing /scheduler/api. Reason:
HTTP method GET is not supported by this URL

This is good and what you should expect from a GET. The URL will only support POST, PUT and DELETE.
