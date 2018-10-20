package com.DevDashboard.demo;


/**
 * Used by Spring to return a list of developers in an organization
 *
 * @author ViseshiniReddy
 */
public class Developer {
    private String login;
    private String name;
    private float score;
    private String teams;
    private int commits;
    private int prs; 
    private int criticalIssues;
    private int successfulBuilds;
    private int failedBuilds;
    private int reviews;
    private float codeCoverage;
    
    public void setTeams(String teams)
    {
        this.teams = teams;
    }
    
    public String getTeams()
    {
        return this.teams;
    }
    
    public String getLogin() {
        return this.login;
    }

    public void setLogin(final String login) {
        this.login = login;
    }
    
   public float getScore()
    {
        return this.score;
    } 
    
    public void setScore(float score)
    {
        this.score = score;
    }

    public int getCommits() {
        return commits;
    }

    public void setCommits(int commits) {
        this.commits = commits;
    }

    public int getPrs() {
        return prs;
    }

    public void setPrs(int prs) {
        this.prs = prs;
    }

    public int getCriticalIssues() {
        return criticalIssues;
    }

    public void setCriticalIssues(int criticalIssues) {
        this.criticalIssues = criticalIssues;
    }
  
    public int getSuccessfulBuilds() {
        return successfulBuilds;
    }

    public void setSuccessfulBuilds(int successfulBuilds) {
        this.successfulBuilds = successfulBuilds;
    }

    public float getCodeCoverage() {
        return codeCoverage;
    }

    public void setCodeCoverage(float codeCoverage) {
        this.codeCoverage = codeCoverage;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFailedBuilds() {
        return failedBuilds;
    }

    public void setFailedBuilds(int failedBuids) {
        this.failedBuilds = failedBuids;
    }

    public int getReviews() {
        return reviews;
    }

    public void setReviews(int reviews) {
        this.reviews = reviews;
    }
    
}
