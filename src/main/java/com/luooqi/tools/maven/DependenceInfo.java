package com.luooqi.tools.maven;

import cn.hutool.core.util.StrUtil;

/**
 * maven-dependency-downloader
 * Created by 何志龙 on 2018-07-04.
 */
public class DependenceInfo
{
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;

    public DependenceInfo(){}

    public DependenceInfo(String groupId, String artifactId, String version, String scope)
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId(String artifactId)
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    @Override
    public String toString()
    {
        return StrUtil.format("{}:{}:{}", this.getGroupId(), this.getArtifactId(), this.getVersion());
    }
}
