package com.cengenes.bitbucket.repo.cloner.api.model.request;

import javax.validation.constraints.NotNull;

public class CloneRequest {

    @NotNull
    private String bitbucketServerUrl;

    @NotNull
    private String orgOrContext;

    @NotNull
    private String projectKey;

    @NotNull
    private String userName;

    @NotNull
    private String password;


    private Integer projectCount;

    @NotNull
    private String localRepoDirectory;

    private String projectVisibility;

    private String bitbucketApiVersion;

    private Boolean trustSelfSigned;

    public String getBitbucketServerUrl() {
        return bitbucketServerUrl;
    }

    public void setBitbucketServerUrl(String bitbucketServerUrl) {
        this.bitbucketServerUrl = bitbucketServerUrl;
    }

    public String getOrgOrContext() {
        return orgOrContext;
    }

    public void setOrgOrContext(final String orgOrContext) {
        this.orgOrContext = orgOrContext;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getProjectCount() {
        return projectCount;
    }

    public void setProjectCount(Integer projectCount) {
        this.projectCount = projectCount;
    }

    public String getLocalRepoDirectory() {
        return localRepoDirectory;
    }

    public void setLocalRepoDirectory(String localRepoDirectory) {
        this.localRepoDirectory = localRepoDirectory;
    }

    public String getProjectVisibility() {
        return projectVisibility;
    }

    public void setProjectVisibility(final String projectVisibility) {
        this.projectVisibility = projectVisibility;
    }

    public String getBitbucketApiVersion() {
        return bitbucketApiVersion;
    }

    public void setBitbucketApiVersion(final String bitbucketApiVersion) {
        this.bitbucketApiVersion = bitbucketApiVersion;
    }

    public Boolean getTrustSelfSigned() {
        return (trustSelfSigned != null ? trustSelfSigned : false);
    }

    public void setTrustSelfSigned(final Boolean trustSelfSigned) {
        this.trustSelfSigned = trustSelfSigned;
    }
}
