package com.luooqi.tools.maven;

import org.apache.maven.shared.invoker.InvocationOutputHandler;

/**
 * maven-dependency-downloader
 * Created by 何志龙 on 2018-07-04.
 */
public class StringBuilderOutputHandler implements InvocationOutputHandler
{
    private StringBuilder output = new StringBuilder();

    public void consumeLine(String s)
    {
        output.append(s).append("\r\n");
    }

    public String getOutput()
    {
        return output.toString();
    }
}
