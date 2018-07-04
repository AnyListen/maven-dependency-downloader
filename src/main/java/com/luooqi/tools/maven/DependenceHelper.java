package com.luooqi.tools.maven;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.StaticLog;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * maven-dependency-downloader
 * Created by 何志龙 on 2018-07-04.
 */
public class DependenceHelper
{
    private static final Pattern depPattern = Pattern.compile("\\[INFO][^\\w]+([\\w.\\-]+):([\\w.\\-]+):jar:([\\w.\\-]+):([\\w.\\-]+)");
    public static List<DependenceInfo> getDependenceListByPom(String pomFilePath) throws FileNotFoundException
    {
        InvocationRequest request = new DefaultInvocationRequest();
        File file = new File(pomFilePath);
        if (!file.exists())
        {
            throw new FileNotFoundException("pom文件路径有误：" + pomFilePath);
        }
        request.setPomFile(file);
        request.setGoals(Collections.singletonList("dependency:tree"));
        String output = getInvokeOutput(request);
        if (StrUtil.isBlank(output))
        {
            return null;
        }
        Matcher matcher = depPattern.matcher(output);
        List<DependenceInfo> result = new ArrayList<DependenceInfo>();
        while (matcher.find())
        {
            DependenceInfo dependenceInfo = new DependenceInfo(matcher.group(1),matcher.group(2),matcher.group(3),matcher.group(4));
            result.add(dependenceInfo);
        }
        return result;
    }

    private static String getInvokeOutput(InvocationRequest request) throws FileNotFoundException
    {
        Invoker invoker = new DefaultInvoker();
        String mavenHome = getMavenHome();
        if (StrUtil.isBlank(mavenHome))
        {
            throw new FileNotFoundException("未检测到MAVEN_HOME，请在环境变量中进行配置。");
        }
        invoker.setMavenHome(new File(mavenHome));
        StringBuilderOutputHandler outputHandler = new StringBuilderOutputHandler();
        invoker.setOutputHandler(outputHandler);
        try
        {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0)
            {
                StaticLog.error("Build failed.-------------->");
                StaticLog.error(outputHandler.getOutput());
                return null;
            }
            return outputHandler.getOutput();
        }
        catch (Exception ex)
        {
            StaticLog.error(ex);
        }
        return null;
    }

    private static String getMavenHome()
    {
        String[] arr = new String[]{"MAVEN_HOME", "MVN_HOME", "M2_HOME", "M3_HOME"};
        String home;
        for (String str : arr)
        {
            home = System.getenv(str);
            if (!StrUtil.isBlank(home))
            {
                return home;
            }
        }
        home = System.getenv("PATH");
        if (!StrUtil.isBlank(home) && home.toLowerCase().contains("maven"))
        {
            String[] split = home.split("[:;]+");
            for (String str : split)
            {
                if (str.toLowerCase().contains("maven"))
                {
                    return str;
                }
            }
        }
        arr = new String[]{"maven.home", "mvn.home", "m2.home", "m3.home"};
        for (String str : arr)
        {
            home = System.getProperty(str);
            if (!StrUtil.isBlank(home))
            {
                return home;
            }
        }
        return null;
    }

    public static void downloadDependence(String pomFilePath, String repositoryPath)
    {

    }
}
