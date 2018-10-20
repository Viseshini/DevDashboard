package com.DevDashboard.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Returns data during a web request
 *
 * @author ViseshiniReddy
 */
@Configuration
@PropertySource("classpath:application.properties")
@RestController
public class SpringController {

    @Autowired
    private Environment env;

    /**
     * 
     * @return JSON object with fields from application.properties file
     */
    @RequestMapping("/requestStructure")
    public Map<String, Object> requestStructure() {
        Map<String, Object> requestStruct = new HashMap<String, Object>();
        requestStruct.put("startDate", env.getProperty("startDate"));
        requestStruct.put("endDate", env.getProperty("endDate"));
        requestStruct.put("orgName", env.getProperty("orgname"));
        requestStruct.put("teamName", env.getProperty("teamName"));
        requestStruct.put("commits", env.getProperty("commits"));
        requestStruct.put("prs", env.getProperty("prs"));
        requestStruct.put("criticalIssues", env.getProperty("criticalIssues"));
        requestStruct.put("codeCoverage", env.getProperty("codeCoverage"));
        requestStruct.put("reviews", env.getProperty("reviews"));
        requestStruct.put("successfulBuilds", env.getProperty("successfulBuilds"));
        requestStruct.put("failedBuilds", env.getProperty("failedBuilds"));
        return requestStruct;
    }
    /**
     * 
     * @param Developer's ID
     * @return Number of pull requests made by the developer
     */
    private int getPrs(String ID) {
        int prs = 0;
        HttpURLConnection con = null;
        try {
            URL url1 = new URL("https://github.****.com/api/graphql");
            con = (HttpURLConnection) url1.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("Tprs"));
            String POST_PARAMS = "{\"query\": \"query { search(type:ISSUE, query:\\\"type:pr created:" + startDate + ".." + endDate + " involves:" + ID
                    + "\\\") {issueCount}}\"}";
            con.setDoOutput(true);
            java.io.OutputStream os = con.getOutputStream();
            os.write(POST_PARAMS.getBytes("UTF-8"));
            os.flush();
            os.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer content = new StringBuffer();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JSONObject myResponse = new JSONObject(content.toString());
            prs = myResponse.getJSONObject("data").getJSONObject("search").getInt("issueCount");
            in.close();
        } catch (Exception e) {
            System.out.println("Exception when calculating PRs: " + e);
        } finally {
            con.disconnect();
        }
        return prs;
    }
    
    /**
     * 
     * @param Developer's ID
     * @return Number of times the Developer was tagged as a reviewer
     */
    private int getReviews(String ID) {
        HttpURLConnection con = null;
        int reviews = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            long sTimeStamp = (dateFormat.parse(startDate)).getTime();
            long eTimeStamp = (dateFormat.parse(endDate)).getTime();
            URL url = new URL("http://crucible02.****.com/viewer/rest-service/reviews-v1/filter?fromDate=" + sTimeStamp + "&toDate=" + eTimeStamp + "&reviewer=" + ID);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Authorization", "Basic " + env.getProperty("tCrucible"));
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuffer content = new StringBuffer();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                JSONObject myResponse = new JSONObject(content.toString());
                reviews = myResponse.getJSONArray("reviewData").length();
            }
        } catch (Exception e) {
            System.out.println("Error in calculating number of reviews:" + e);
        } finally {
            con.disconnect();
        }
        return reviews;

    }
    /**
     * 
     * @param Developer's ID
     * @return Number of critical issues created by the developer
     */
    private int getCriticalIssues(String ID) {
        HttpURLConnection con = null;

        int total1 = 0;
        try {
            //[MAJOR] or [MINOR] issues can also be fetched by changing the filter
            URL url1 = new URL("https://github.****.com/api/v3/search/issues?q=[CRITICAL]%20type:pr%20commenter:" + env.getProperty("bot") + "%20involves:" + ID
                    + "%20created:" + startDate + ".." + endDate);
            con = (HttpURLConnection) url1.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("TcriticalIssues"));
            con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuffer content = new StringBuffer();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                JSONObject myResponse = new JSONObject(content.toString());
                JSONArray items = myResponse.getJSONArray("items");
                JSONObject item;
                String comments_url;
                HttpURLConnection con1 = null;
                for (int i = 0; i < items.length(); i++) {
                    item = items.getJSONObject(i);
                    comments_url = item.getString("comments_url");
                    url1 = new URL(comments_url);
                    try {
                        con1 = (HttpURLConnection) url1.openConnection();
                        con1.setRequestMethod("GET");
                        con1.setRequestProperty("Authorization", "Bearer " + env.getProperty("TcriticalIssues"));
                        con1.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
                        in = new BufferedReader(new InputStreamReader(con1.getInputStream()));
                        content = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                        con1.disconnect();

                        JSONArray comments = new JSONArray(content.toString());
                        JSONObject comment;
                        for (int x = 0; x < comments.length(); x++) {
                            comment = comments.getJSONObject(x);
                            String body = comment.getString("body");
                            if (body.contains("SonarQube analysis reported") == true) {
                                Pattern pattern = Pattern.compile("[0-9]+ critical");
                                Matcher matcher = pattern.matcher(body);
                                if (matcher.find()) {
                                    total1 = total1 + Integer.parseInt(matcher.group().substring(0, 1));

                                }

                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error in getting sonarBot comments:" + e);
                    } finally {
                        con1.disconnect();
                    }

                }

            }
        } catch (Exception e) {
            System.out.println("Exception in calculating critical issues: " + e);
        } finally {
            con.disconnect();
        }
        return total1;
    }
    /**
     * 
     * @param Developer's ID
     * @return Name of the developer 
     */
    private String getName(String ID) {
        String name = "";
        HttpURLConnection con = null;
        try {
            URL url1 = new URL("https://github.****.com/api/graphql");
            con = (HttpURLConnection) url1.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("Tname"));
            String POST_PARAMS = "{\"query\": \"query { user(login:\\\"" + ID + "\\\") {name}}\"}";
            con.setDoOutput(true);
            java.io.OutputStream os = con.getOutputStream();
            os.write(POST_PARAMS.getBytes("UTF-8"));
            os.flush();
            os.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer content = new StringBuffer();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JSONObject myResponse = new JSONObject(content.toString());
            name = myResponse.getJSONObject("data").getJSONObject("user").getString("name");
            in.close();
        } catch (Exception e) {
            System.out.println("Exception when getting name " + e);
        } finally {
            con.disconnect();
        }
        return name;

    }

    /**
     * 
     * @param Developer's ID
     * @return Code coverage delta value after a branch is modified
     */
    private float getCodeCoverage(String ID) {
        float total = 0;
        HttpURLConnection con = null;
        try {
            URL url1 = new URL("https://github.****.com/api/v3/search/issues?q=type:pr%20commenter:" + env.getProperty("bot") + "%20involves:" + ID + "%20created:"
                    + startDate + ".." + endDate);
            con = (HttpURLConnection) url1.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("TcodeCoverage"));
            con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");

            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuffer content = new StringBuffer();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                con.disconnect();
                JSONObject myResponse = new JSONObject(content.toString());
                JSONArray items = myResponse.getJSONArray("items");
                JSONObject item;
                String comments_url;
                for (int i = 0; i < items.length(); i++) {
                    item = items.getJSONObject(i);
                    comments_url = item.getString("comments_url");
                    url1 = new URL(comments_url);
                    con = (HttpURLConnection) url1.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Authorization", "Bearer " + env.getProperty("TcodeCoverage"));
                    con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
                    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    con.disconnect();
                    JSONArray comments = new JSONArray(content.toString());
                    JSONObject comment;
                    for (int x = 0; x < comments.length(); x++) {
                        comment = comments.getJSONObject(x);
                        String body = comment.getString("body");
                        if (body.contains("coverage") == true) {
                            //[![78% (0.0%) vs master 78%]
                            Pattern pattern = Pattern.compile("\\[!\\[[0-9][0-9]% \\([0-9]+.[0-9]+%");
                            Matcher matcher = pattern.matcher(body);
                            if (matcher.find()) {
                                total = total + Float.parseFloat(matcher.group().substring(8, 10));

                            }

                        }
                    }
                }

            }
        } catch (Exception e) {
            System.out.println("Exception in getting code coverage: " + e);

        } finally {
            con.disconnect();
        }
        return total;
    }
    
    /**
     * 
     * @param Developer's ID
     * @return Number of commits made by the developer
     */
    private int getCommits(String ID) {
        HttpURLConnection con = null;
        int commits = 0;
        try {
            URL url1 = new URL("https://github.****.com/api/v3/search/commits?q=committer:" + ID + "%20committer-date:" + startDate + ".." + endDate);
            con = (HttpURLConnection) url1.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("Tcommits"));
            con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            JSONObject myResponse = new JSONObject(content.toString());
            commits = myResponse.getInt("total_count");
            in.close();
        } catch (Exception e) {
            System.out.println("Exception in calculating number of commits");
        } finally {
            con.disconnect();
        }
        return commits;
    }
    
    /**
     * 
     * @param Developer's ID
     * @return Number of failed Jenkins builds made by the developer
     */
    private int getFailedBuilds(String ID) {
        HttpURLConnection con = null;
        int failedBuilds = 0;
        try {
            URL url1 = new URL("https://github.****.com/api/v3/search/commits?q=committer:" + ID + "%20committer-date:" + startDate + ".." + endDate);
            con = (HttpURLConnection) url1.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("TfailedBuilds"));
            con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            JSONObject myResponse = new JSONObject(content.toString());
            in.close();
            con.disconnect();

            JSONArray items = myResponse.getJSONArray("items");
            JSONObject item;
            ArrayList<String> listRepos = new ArrayList<String>();
            for (int i = 0; i < items.length(); i++) {
                item = items.getJSONObject(i);
                //if repo doesn't already exist in the list, add it to the list
                if (listRepos.indexOf(item.getJSONObject("repository").getString("full_name")) == -1)
                    listRepos.add(item.getJSONObject("repository").getString("full_name"));
            }
            String[] repo = new String[2];
            Object[] ArrayRepos = listRepos.toArray();
            String jenkins_folder = env.getProperty("jenkins_folder");

            for (int j = 0; j < ArrayRepos.length; j++) {
                String url = "https://jenkins.****.com/dwx2/job/" + jenkins_folder + "/job/";
                repo = (ArrayRepos[j].toString()).split("/");
                for (int i = 0; i < repo.length; i++) {
                    url = url + repo[i] + "/job/";
                }

                url = url.substring(0, url.length() - 4);
                url = url + "api/json?pretty=true";
                try {
                    url1 = new URL(url);
                    con = (HttpURLConnection) url1.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Authorization", "Bearer " + env.getProperty("tJenkins"));
                    if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                    }
                } catch (Exception e) {
                    System.out.println("Exception: " + e);
                } finally {
                    con.disconnect();
                }

                myResponse = new JSONObject(content.toString());
                if (myResponse.has("jobs")) {

                    JSONArray jobs = myResponse.getJSONArray("jobs");

                    String jobsUrl;
                    JSONObject job;

                    for (int i = 0; i < jobs.length(); i++) {
                        job = jobs.getJSONObject(i);
                        jobsUrl = job.getString("url") + "api/json?pretty=true";
                        try {
                            url1 = new URL(jobsUrl);
                            con = (HttpURLConnection) url1.openConnection();
                            con.setRequestMethod("GET");
                            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("tJenkins"));
                            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                                content = new StringBuffer();
                                while ((inputLine = in.readLine()) != null) {
                                    content.append(inputLine);
                                }
                                in.close();
                            }

                            myResponse = new JSONObject(content.toString());
                        } catch (Exception e) {
                            System.out.println("Exception: " + e);
                        } finally {
                            con.disconnect();
                        }

                    }
                    JSONArray builds = myResponse.getJSONArray("builds");
                    String buildsUrl;
                    JSONObject build;
                    for (int k = 0; k < builds.length(); k++) {
                        build = builds.getJSONObject(k);
                        buildsUrl = build.getString("url") + "api/json?pretty=true";
                        try {
                            url1 = new URL(buildsUrl);
                            con = (HttpURLConnection) url1.openConnection();
                            con.setRequestMethod("GET");
                            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("tJenkins"));
                            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                                content = new StringBuffer();
                                while ((inputLine = in.readLine()) != null) {
                                    content.append(inputLine);
                                }
                                in.close();
                            }
                        } catch (Exception e) {
                            System.out.println("Exception: " + e);
                        } finally {
                            con.disconnect();
                        }

                        myResponse = new JSONObject(content.toString());
                        if (myResponse.getString("result") instanceof String && myResponse.getString("result") != null) {
                            String result = myResponse.getString("result");
                            if (!result.equals("SUCCESS")) {
                                failedBuilds = failedBuilds + 1;
                            }

                        }
                    }

                }
            }

        } catch (Exception e) {
            System.out.println("Exception in calculating failed builds: " + e);
        } finally {
            con.disconnect();
        }

        return failedBuilds;

    }

    /**
     * 
     * @param Developer's ID
     * @return Number of successful Jenkins builds made by the developer
     */
    private int getSuccessfulBuilds(String ID) {
        HttpURLConnection con = null;
        int successfulBuilds = 0;
        try {
            URL url1 = new URL("https://github.****.com/api/v3/search/commits?q=committer:" + ID + "%20committer-date:" + startDate + ".." + endDate);
            con = (HttpURLConnection) url1.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("TsuccessfulBuilds"));
            con.setRequestProperty("Accept", "application/vnd.github.cloak-preview");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            JSONObject myResponse = new JSONObject(content.toString());
            in.close();
            con.disconnect();

            JSONArray items = myResponse.getJSONArray("items");
            JSONObject item;
            ArrayList<String> listRepos = new ArrayList<String>();
            for (int i = 0; i < items.length(); i++) {
                item = items.getJSONObject(i);
                //if repo doesn't already exist in the list, add it to the list
                if (listRepos.indexOf(item.getJSONObject("repository").getString("full_name")) == -1)
                    listRepos.add(item.getJSONObject("repository").getString("full_name"));
            }
            String[] repo = new String[2];
            Object[] ArrayRepos = listRepos.toArray();
            String jenkins_folder = env.getProperty("jenkins_folder");

            for (int j = 0; j < ArrayRepos.length; j++) {
                String url = "https://jenkins.****.com/dwx2/job/" + jenkins_folder + "/job/";
                repo = (ArrayRepos[j].toString()).split("/");
                for (int i = 0; i < repo.length; i++) {
                    url = url + repo[i] + "/job/";
                }

                url = url.substring(0, url.length() - 4);
                url = url + "api/json?pretty=true";
                try {
                    url1 = new URL(url);
                    con = (HttpURLConnection) url1.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Authorization", "Bearer " + env.getProperty("tJenkins"));
                    if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                    }
                } catch (Exception e) {
                    System.out.println("Exception: " + e);
                } finally {
                    con.disconnect();
                }

                myResponse = new JSONObject(content.toString());
                if (myResponse.has("jobs")) {

                    JSONArray jobs = myResponse.getJSONArray("jobs");

                    String jobsUrl;
                    JSONObject job;

                    for (int i = 0; i < jobs.length(); i++) {
                        job = jobs.getJSONObject(i);
                        jobsUrl = job.getString("url") + "api/json?pretty=true";
                        try {
                            url1 = new URL(jobsUrl);
                            con = (HttpURLConnection) url1.openConnection();
                            con.setRequestMethod("GET");
                            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("tJenkins"));
                            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                                content = new StringBuffer();
                                while ((inputLine = in.readLine()) != null) {
                                    content.append(inputLine);
                                }
                                in.close();
                            }

                            myResponse = new JSONObject(content.toString());
                        } catch (Exception e) {
                            System.out.println("Exception: " + e);
                        } finally {
                            con.disconnect();
                        }

                    }
                    JSONArray builds = myResponse.getJSONArray("builds");
                    String buildsUrl;
                    JSONObject build;
                    for (int k = 0; k < builds.length(); k++) {
                        build = builds.getJSONObject(k);
                        buildsUrl = build.getString("url") + "api/json?pretty=true";
                        try {
                            url1 = new URL(buildsUrl);
                            con = (HttpURLConnection) url1.openConnection();
                            con.setRequestMethod("GET");
                            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("tJenkins"));
                            if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                                content = new StringBuffer();
                                while ((inputLine = in.readLine()) != null) {
                                    content.append(inputLine);
                                }
                                in.close();
                            }
                        } catch (Exception e) {
                            System.out.println("Exception: " + e);
                        } finally {
                            con.disconnect();
                        }

                        myResponse = new JSONObject(content.toString());
                        if (myResponse.getString("result") instanceof String && myResponse.getString("result") != null) {
                            String result = myResponse.getString("result");
                            if (result.equals("SUCCESS")) {
                                successfulBuilds = successfulBuilds + 1;
                            }

                        }
                    }

                }
            }

        } catch (Exception e) {
            System.out.println("Exception in calculating successful builds: " + e);
        } finally {
            con.disconnect();
        }

        return successfulBuilds;

    }
    
    /**
     * 
     * @param dev object
     */
    private void getTeams(Developer dev) {
        HttpURLConnection con = null;
        try {
            String name = dev.getLogin();
            URL url1 = new URL("https://github.****.com/api/graphql");
            con = (HttpURLConnection) url1.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + env.getProperty("Tteams"));
            String POST_PARAMS = "{\"query\": \"query { organization(login:\\\"" + env.getProperty("orgname") + "\\\") {teams(first:10,userLogins:\\\"" + name
                    + "\\\"){edges{node{ name}}}}}\"}";
            con.setDoOutput(true);
            java.io.OutputStream os = con.getOutputStream();
            os.write(POST_PARAMS.getBytes("UTF-8"));
            os.flush();
            os.close();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer content = new StringBuffer();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            JSONObject myResponse = new JSONObject(content.toString());
            JSONObject teams = myResponse.getJSONObject("data").getJSONObject("organization").getJSONObject("teams");
            JSONArray edges = new JSONArray();
            edges = teams.getJSONArray("edges");
            JSONObject t;
            String y = "";
            for (int x = 0; x < edges.length(); x++) {
                t = edges.getJSONObject(x);
                y = y + t.getJSONObject("node").getString("name") + ", ";
            }
            //removes the last ', '
            if (y.length() > 3) {
                y = y.substring(0, y.length() - 2);
            }
            dev.setTeams(y);
            in.close();
            con.disconnect();
        } catch (Exception e) {
            System.out.println("Exception when getting teams " + e);
        } finally {
            con.disconnect();
        }
    }

    private String startDate;
    private String endDate;
    /**
     * 
     * @param request
     * @return List of developers with metrics populated
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ArrayList<Developer> list(HttpServletRequest request) {

        int sum = Integer.parseInt(env.getProperty("commits")) + Integer.parseInt(env.getProperty("prs")) + Integer.parseInt(env.getProperty("criticalIssues"))
                + Integer.parseInt(env.getProperty("successfulBuilds")) + Integer.parseInt(env.getProperty("codeCoverage")) + Integer.parseInt(env.getProperty("failedBuilds"))
                + Integer.parseInt(env.getProperty("reviews"));
        
        endDate = env.getProperty("endDate");
        startDate = env.getProperty("startDate");

        ArrayList<Developer> developers = new ArrayList<Developer>();
        //Creating thread pool to increase performance
        ExecutorService threadPool = Executors.newFixedThreadPool(5);

        if (env.getProperty("teamName") == null) { 
            System.out.println("Querying the entire org");
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + env.getProperty("Tname"));
            headers.set("Accept", "application/vnd.github.cloak-preview");
            HttpEntity<?> entity = new HttpEntity<>(headers);
            String GITHUB_URL = "https://github.****.com/api/v3/orgs";
            String ORG_NAME = env.getProperty("orgname");
            String url = GITHUB_URL + "/" + ORG_NAME + "/members";
            url += "?role=member&page=1";

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<ArrayList<Developer>> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity,
                    new ParameterizedTypeReference<ArrayList<Developer>>() {
                    });
            HttpHeaders resHeaders = response.getHeaders();
            String linkHeader = resHeaders.getFirst("Link");

            //get page number of the last page to get the entire list
            int lastPage = 0;
            if (linkHeader != null) {
                int endIndex = linkHeader.indexOf(">; rel=\"last\"");
                int x = endIndex - 1;
                String pageNumber = "";
                while (linkHeader.charAt(x) != '=') {
                    pageNumber = pageNumber + linkHeader.charAt(x);
                    x--;
                }
                StringBuilder sb = new StringBuilder(pageNumber);
                sb.reverse();
                lastPage = Integer.parseInt(sb.toString());
            }

            int i = 1;
            do {
                for (Developer dev : response.getBody()) {
                    if (dev == null)
                        continue;
                    String ID = dev.getLogin();
                    threadPool.submit(new Runnable() {
                        public void run() {

                            //Get full name of the developer
                            dev.setName(getName(ID));

                            //Get all the teams a developer is a part of
                            getTeams(dev);

                            float score = 0;
                            int intScore;

                            //CODE COVERAGE DELTA
                            if (!env.getProperty("codeCoverage").equals("0")) {
                                float total = getCodeCoverage(ID);
                                dev.setCodeCoverage(total);
                                intScore = Integer.parseInt(env.getProperty("commits")) * (int) total;
                                score = score - (float) intScore;
                            }

                            //NUMBER OF PRs MADE BY THE USER 
                            if (!env.getProperty("prs").equals("0")) {
                                int prs = getPrs(ID);
                                dev.setPrs(prs);
                                intScore = Integer.parseInt(env.getProperty("prs")) * prs;
                                score = score + (float) intScore;
                            }

                            //NUMBER OF CRITICAL ISSUES REPORTED BY SONARBOT
                            if (!env.getProperty("criticalIssues").equals("0")) {
                                int criticalIssues = getCriticalIssues(ID);
                                dev.setCriticalIssues(criticalIssues);
                                intScore = Integer.parseInt(env.getProperty("criticalIssues")) * criticalIssues;
                                score = score - (float) intScore;
                            }

                            //NUMBER OF COMMITS MADE BY THE USER
                            if (!env.getProperty("commits").equals("0")) {
                                int commits = getCommits(ID);
                                dev.setCommits(commits);
                                intScore = Integer.parseInt(env.getProperty("commits")) * commits;
                                score = score + (float) intScore;
                            }

                            //NUMBER OF SUCCESSFUL JENKINS BUILDS
                            if (!env.getProperty("successfulBuilds").equals("0")) {
                                int successfulBuilds = getSuccessfulBuilds(ID);
                                dev.setSuccessfulBuilds(successfulBuilds);
                                intScore = Integer.parseInt(env.getProperty("successfulBuilds")) * successfulBuilds;
                                score = score + (float) intScore;
                            }

                            //NUMBER OF FAILED JENKINS BUILDS
                            if (!env.getProperty("failedBuilds").equals("0")) {
                                int failedBuilds = getFailedBuilds(ID);
                                dev.setFailedBuilds(failedBuilds);
                                intScore = Integer.parseInt(env.getProperty("failedBuilds")) * failedBuilds;
                                score = score - (float) intScore;
                            }

                            //NUMBER OF TIMES THE USER WAS ADDED AS A REVIEWER
                            if (!env.getProperty("reviews").equals("0")) {
                                int reviews = getReviews(ID);
                                dev.setReviews(reviews);
                                intScore = Integer.parseInt(env.getProperty("reviews")) * reviews;
                                score = score + (float) intScore;
                            }

                            DecimalFormat df = new DecimalFormat("###.##");
                            dev.setScore(Float.valueOf(df.format(score / (float) sum)));
                            developers.add(dev);
                        }
                    });
                }
                i++;
                builder = UriComponentsBuilder.fromHttpUrl(GITHUB_URL + "/" + ORG_NAME + "/members?role=member&page=" + i);
                restTemplate = new RestTemplate();
                response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, new ParameterizedTypeReference<ArrayList<Developer>>() {
                });
            } while (i <= lastPage);
        } else {
            
            HttpURLConnection con = null;
            JSONArray edges;
            try {
                URL url1 = new URL("https://github.****.com/api/graphql");
                con = (HttpURLConnection) url1.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Authorization", "Bearer " + env.getProperty("Tname"));
                String POST_PARAMS = "{\"query\": \"query { organization(login:\\\"" + env.getProperty("orgname") + "\\\") {team(slug:\\\"" + env.getProperty("teamName")
                        + "\\\"){members(first:30){edges{node{login}}}}}}\"}";
                con.setDoOutput(true);
                java.io.OutputStream os = con.getOutputStream();
                os.write(POST_PARAMS.getBytes("UTF-8"));
                os.flush();
                os.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuffer content = new StringBuffer();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                JSONObject myResponse = new JSONObject(content.toString());
                edges = myResponse.getJSONObject("data").getJSONObject("organization").getJSONObject("team").getJSONObject("members").getJSONArray("edges");

                for (int i = 0; i < edges.length(); i++) {
                    Developer dev = new Developer();
                    dev.setLogin(edges.getJSONObject(i).getJSONObject("node").getString("login"));

                    String ID = dev.getLogin();
                    threadPool.submit(new Runnable() {
                        public void run() {

                            //Get full name of the developer
                            dev.setName(getName(ID));

                            float score = 0;
                            int intScore;

                            //CODE COVERAGE DELTA
                            if (!env.getProperty("codeCoverage").equals("0")) {
                                float total = getCodeCoverage(ID);
                                dev.setCodeCoverage(total);
                                intScore = Integer.parseInt(env.getProperty("commits")) * (int) total;
                                score = score - (float) intScore;
                            }

                            //NUMBER OF PRs MADE BY THE USER 
                            if (!env.getProperty("prs").equals("0")) {
                                int prs = getPrs(ID);
                                dev.setPrs(prs);
                                intScore = Integer.parseInt(env.getProperty("prs")) * prs;
                                score = score + (float) intScore;
                            }

                            //NUMBER OF CRITICAL ISSUES REPORTED BY SONARBOT
                            if (!env.getProperty("criticalIssues").equals("0")) {
                                int criticalIssues = getCriticalIssues(ID);
                                dev.setCriticalIssues(criticalIssues);
                                intScore = Integer.parseInt(env.getProperty("criticalIssues")) * criticalIssues;
                                score = score - (float) intScore;
                            }

                            //NUMBER OF COMMITS MADE BY THE USER
                            if (!env.getProperty("commits").equals("0")) {
                                int commits = getCommits(ID);
                                dev.setCommits(commits);
                                intScore = Integer.parseInt(env.getProperty("commits")) * commits;
                                score = score + (float) intScore;
                            }

                            //NUMBER OF SUCCESSFUL JENKINS BUILDS
                            if (!env.getProperty("successfulBuilds").equals("0")) {
                                int successfulBuilds = getSuccessfulBuilds(ID);
                                dev.setSuccessfulBuilds(successfulBuilds);
                                intScore = Integer.parseInt(env.getProperty("successfulBuilds")) * successfulBuilds;
                                score = score + (float) intScore;
                            }

                            //NUMBER OF FAILED JENKINS BUILDS
                            if (!env.getProperty("failedBuilds").equals("0")) {
                                int failedBuilds = getFailedBuilds(ID);
                                dev.setFailedBuilds(failedBuilds);
                                intScore = Integer.parseInt(env.getProperty("failedBuilds")) * failedBuilds;
                                score = score - (float) intScore;
                            }

                            //NUMBER OF TIMES THE USER WAS ADDED AS A REVIEWER
                            if (!env.getProperty("reviews").equals("0")) {
                                int reviews = getReviews(ID);
                                dev.setReviews(reviews);
                                intScore = Integer.parseInt(env.getProperty("reviews")) * reviews;
                                score = score + (float) intScore;
                            }
                            DecimalFormat df = new DecimalFormat("###.##");
                            dev.setScore(Float.valueOf(df.format(score / (float) sum)));
                            developers.add(dev);
                        }
                    });
                }
            } catch (Exception e) {
                System.out.println("Exception when getting name " + e);
            } finally {
                con.disconnect();
            }
        }

        // once you've submitted your last job to the service it should be shut down
        threadPool.shutdown();
        // wait for the threads to finish if necessary
        try {
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.out.println("Exception in threading: " + e);
        }
        return developers;
    }

}
