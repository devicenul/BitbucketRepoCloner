package com.cengenes.bitbucket.repo.cloner.api.service;

import com.cengenes.bitbucket.repo.cloner.api.model.request.CloneRequest;
import com.cengenes.bitbucket.repo.cloner.api.model.response.RepoCloneResponse;
import com.cengenes.bitbucket.repo.cloner.api.model.response.ResponseStatusType;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.HttpTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.http.HttpConnectionFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class CloneCreateRequestService {

    private final Logger log = LogManager.getLogger(this.getClass());

    private final String REST_API_CALL_V2 = "%s/2.0/repositories/%s/?limit=%d&visibility=%s&pagelen=100&page=1&q=project.key=%s";
    private final String REST_API_CALL_V1 = "%s/%s/rest/api/1.0/projects/%s/repos?limit=%d&visibility=%s&pagelen=100&page=1";

    public RepoCloneResponse cloneRepos(CloneRequest cloneRequest) {
        final RepoCloneResponse repoCloneResponse = new RepoCloneResponse(ResponseStatusType.FAILURE.getValue());

        HttpConnectionFactory preservedConnectionFactory = HttpTransport.getConnectionFactory();

        if (cloneRequest.getTrustSelfSigned()) {
            HttpTransport.setConnectionFactory( new InsecureHttpConnectionFactory() );
        }

        // Obtain repos
        try {
            final Optional<JSONArray> repositories = obtainRepositories(cloneRequest);
            if (repositories.isPresent()) {
                final Stream<Object> objectStream = arrayToStream(repositories.get());

                objectStream.filter(Objects::nonNull).map(object -> (JSONObject) object).forEach(repo -> {
                    String repoName = repo.getString("name");
                    log.trace("Repo: {}", repoName);
                    final JSONArray cloneURLs = (JSONArray) ((JSONObject) repo.get("links")).get("clone");
                    arrayToStream(cloneURLs).map(urls -> (JSONObject) urls).filter(httpsUrl -> httpsUrl.get("name").toString().startsWith("http")).forEach(url -> {
                        String repoURL = url.getString("href");
                        try {
                            log.trace("Cloning repo name: {}", repoName);
                            cloneRepository(cloneRequest.getUserName(), cloneRequest.getPassword(), getLocalRepoDir(cloneRequest, repoName), repoURL);
                            repoCloneResponse.setStatus(ResponseStatusType.SUCCESS.getValue());
                        } catch (GitAPIException e) {
                            log.error("Error on cloning Repos {}", e);
                        }
                    });

                });
            }
        } catch (UnirestException e) {
            log.error("Error on obtaining Repos {}", e);
        } finally {
            // Restore original context
            HttpTransport.setConnectionFactory( preservedConnectionFactory );
        }

        log.info("Clone Operation Complete");
        return repoCloneResponse;
    }

    private final Optional<JSONArray> obtainRepositories(final CloneRequest cloneRequest) throws UnirestException {
        log.info("Obtaining repos from {}", cloneRequest.getProjectKey());
        log.info("Clone Request URL = \n{}", getRepoUrl(cloneRequest));

        if (cloneRequest.getTrustSelfSigned()) {
            try {
                SSLContext sslcontext = SSLContexts.custom()
                        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                        .build();

                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                CloseableHttpClient httpclient = HttpClients.custom()
                        .setSSLSocketFactory(sslsf)
                        .build();
                Unirest.setHttpClient(httpclient);

            } catch (Exception e) {
                log.error("Exception setting ssl context", e);
                throw new RuntimeException(e.getLocalizedMessage());
            }
        }

        return Optional.of(Unirest.get(getRepoUrl(cloneRequest))
                .basicAuth(cloneRequest.getUserName(), cloneRequest.getPassword())
                .header("accept", "application/json")
                .asJson()
                .getBody()
                .getObject().getJSONArray("values"));
    }

    private final Stream<Object> arrayToStream(final JSONArray array) {
        return StreamSupport.stream(array.spliterator(), false);
    }

    private final String getLocalRepoDir(final CloneRequest cloneRequest, final String repoName) {
        return cloneRequest.getLocalRepoDirectory() + "/" + cloneRequest.getProjectKey() + "/" + repoName;
    }

    private final String getRepoUrl(final CloneRequest cloneRequest) {
        final Integer PROJECT_COUNT_TO_CLONE = cloneRequest.getProjectCount() != null ? cloneRequest.getProjectCount() : 100;
        final String PROJECT_VISIBILITY = cloneRequest.getProjectVisibility() != null ? cloneRequest.getProjectVisibility() : "all";
        final String API_VERSION = cloneRequest.getBitbucketApiVersion() != null ? cloneRequest.getBitbucketApiVersion() : "2.0";

        if (API_VERSION.equals("2.0")) {
            String encodedQuery;

            try {
                encodedQuery = URLEncoder.encode("\"" + cloneRequest.getProjectKey() + "\"", StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                log.error("Caught exception encoding query", e);
                throw new RuntimeException(e);
            }

            return String.format(REST_API_CALL_V2,
                                    cloneRequest.getBitbucketServerUrl(),
                                    cloneRequest.getOrgOrContext(),
                                    PROJECT_COUNT_TO_CLONE,
                                    PROJECT_VISIBILITY,
                                    encodedQuery);

        } else if (API_VERSION.equals("1.0")) {

            return String.format(REST_API_CALL_V1,
                                    cloneRequest.getBitbucketServerUrl(),
                                    cloneRequest.getOrgOrContext(),
                                    cloneRequest.getProjectKey(),
                                    PROJECT_COUNT_TO_CLONE,
                                    PROJECT_VISIBILITY);
        } else {
            log.error("Unknown API version {}", API_VERSION);
            throw new RuntimeException("Unknown API version " + API_VERSION);
        }
    }

    private final void cloneRepository(final String userName, final String password, final String repoDir, final String repoURL) throws GitAPIException {
        log.info("Going to clone repo {},Repository will be stored at {}", repoURL, repoDir);
        Git.cloneRepository()
                .setURI(repoURL)
                .setDirectory(new File(repoDir))
                .setCloneAllBranches(true)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, password))
                .call();
    }
}
