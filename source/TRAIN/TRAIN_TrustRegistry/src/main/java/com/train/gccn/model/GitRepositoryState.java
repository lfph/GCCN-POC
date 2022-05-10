package com.train.gccn.model;

import java.io.IOException;
import java.util.Properties;

public class GitRepositoryState {
    
    private static GitRepositoryState gitRepositoryState;
    private final String buildVersion;
    
    private GitRepositoryState(Properties properties) {
//        this.tags = String.valueOf(properties.get("git.tags"));
//        this.branch = String.valueOf(properties.get("git.branch"));
//        this.dirty = String.valueOf(properties.get("git.dirty"));
//        this.remoteOriginUrl = String.valueOf(properties.get("git.remote.origin.url"));
//
//        this.commitId = String.valueOf(properties.get("git.commit.id.full")); // OR properties.get("git.commit.id") depending on your configuration
//        this.commitIdAbbrev = String.valueOf(properties.get("git.commit.id.abbrev"));
//        this.describe = String.valueOf(properties.get("git.commit.id.describe"));
//        this.describeShort = String.valueOf(properties.get("git.commit.id.describe-short"));
//        this.commitUserName = String.valueOf(properties.get("git.commit.user.name"));
//        this.commitUserEmail = String.valueOf(properties.get("git.commit.user.email"));
//        this.commitMessageFull = String.valueOf(properties.get("git.commit.message.full"));
//        this.commitMessageShort = String.valueOf(properties.get("git.commit.message.short"));
//        this.commitTime = String.valueOf(properties.get("git.commit.time"));
//        this.closestTagName = String.valueOf(properties.get("git.closest.tag.name"));
//        this.closestTagCommitCount = String.valueOf(properties.get("git.closest.tag.commit.count"));
//
//        this.buildUserName = String.valueOf(properties.get("git.build.user.name"));
//        this.buildUserEmail = String.valueOf(properties.get("git.build.user.email"));
//        this.buildTime = String.valueOf(properties.get("git.build.time"));
//        this.buildHost = String.valueOf(properties.get("git.build.host"));
        this.buildVersion = String.valueOf(properties.get("git.build.version"));
//        this.buildNumber = String.valueOf(properties.get("git.build.number"));
//        this.buildNumberUnique = String.valueOf(properties.get("git.build.number.unique"));
    }
    
    public static GitRepositoryState get() throws IOException {
        if(GitRepositoryState.gitRepositoryState == null) {
            Properties properties = new Properties();
            properties.load(GitRepositoryState.class.getClassLoader().getResourceAsStream("git.properties"));
            
            GitRepositoryState.gitRepositoryState = new GitRepositoryState(properties);
        }
        return GitRepositoryState.gitRepositoryState;
    }
    
    public String getBuildVersion() {
        return this.buildVersion;
    }
}
