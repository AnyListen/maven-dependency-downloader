package com.luooqi.tools.maven;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.StaticLog;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.shared.invoker.*;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

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

    /**
     * 根据POM文件获取全部依赖信息
     * @param pomFilePath POM文件路径
     * @return 全部依赖信息
     * @throws FileNotFoundException MAVEN_HOME或者POM文件不存在
     */
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

    /**
     * 根据Artifact信息下载其相关依赖（默认Scope为COMPILE），并存储到指定的文件夹
     * @param artifactStr Artifact信息，例如：org.apache.maven.shared:maven-invoker:3.0.1
     * @param storePath 存储的路径
     * @return 全部依赖信息
     */
    public static List<DependenceInfo> downloadDependency(String artifactStr, String storePath)
    {
        return downloadDependency(artifactStr, storePath, JavaScopes.COMPILE);
    }

    /**
     * 根据Artifact信息下载其相关依赖，并存储到指定的文件夹
     * @param artifactStr Artifact信息，例如：org.apache.maven.shared:maven-invoker:3.0.1
     * @param storePath 存储的路径
     * @param theScope Scope
     * @return 全部依赖信息
     */
    public static List<DependenceInfo> downloadDependency(String artifactStr, String storePath, String theScope)
    {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        RepositorySystem system = newRepositorySystem(locator);
        RepositorySystemSession session = newSession(system, storePath);

        Artifact artifact = new DefaultArtifact(artifactStr);
        DependencyFilter theDependencyFilter = DependencyFilterUtils.classpathFilter(theScope);

        //RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
        RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
        CollectRequest theCollectRequest = new CollectRequest();
        theCollectRequest.setRoot(new org.eclipse.aether.graph.Dependency(artifact, theScope));
        //theCollectRequest.addRepository(central);
        theCollectRequest.addRepository(central);

        DependencyRequest theDependencyRequest = new DependencyRequest(theCollectRequest, theDependencyFilter);
        List<DependenceInfo> resultList = new ArrayList<DependenceInfo>();
        try
        {
            DependencyResult theDependencyResult = system.resolveDependencies(session, theDependencyRequest);
            for (ArtifactResult theArtifactResult : theDependencyResult.getArtifactResults()) {
                Artifact theResolved = theArtifactResult.getArtifact();
                DependenceInfo depInfo = new DependenceInfo(theResolved.getGroupId(), theResolved.getArtifactId(), theResolved.getVersion(), JavaScopes.COMPILE);
                resultList.add(depInfo);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return resultList;
    }

    private static RepositorySystem newRepositorySystem(DefaultServiceLocator locator) {
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession newSession(RepositorySystem system, String storePath) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository(storePath);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        // set possible proxies and mirrorsD:
        //session.setProxySelector(new DefaultProxySelector().add(new Proxy(Proxy.TYPE_HTTP, "host", 3625), Arrays.asList("localhost", "127.0.0.1")));
        //session.setMirrorSelector(new DefaultMirrorSelector().add("my-mirror", "http://mirror", "default", false, "external:*", null));
        return session;
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

    /**
     * 获取Maven_Home
     */
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


    /**
     * 根据pom.xml下载全部依赖（默认作用于为COMPILE）到指定的路径
     * @param pomFilePath POM文件路径
     * @param repositoryPath 存储的路径
     * @throws FileNotFoundException MAVEN_HOME或者POM文件不存在
     */
    public static void downloadDependenceWithPomFile(String pomFilePath, String repositoryPath) throws FileNotFoundException
    {
        downloadDependenceWithPomFile(pomFilePath, repositoryPath, JavaScopes.COMPILE);
    }

    /**
     * 根据pom.xml下载全部依赖到指定的路径
     * @param pomFilePath POM文件路径
     * @param repositoryPath 存储的路径
     * @param scope 作用域
     * @throws FileNotFoundException MAVEN_HOME或者POM文件不存在
     */
    public static void downloadDependenceWithPomFile(String pomFilePath, String repositoryPath, String scope) throws FileNotFoundException
    {
        List<DependenceInfo> depList = getDependenceListByPom(pomFilePath);
        if (depList == null)
        {
            StaticLog.info("未解析到任何依赖！");
            return;
        }
        List<DependenceInfo> resultList = new ArrayList<DependenceInfo>();
        System.out.println("开始下载：");
        for (DependenceInfo dep:depList)
        {
            List<DependenceInfo> list = downloadDependency(dep.toString(), repositoryPath, scope);
            if (list != null && list.size() > 0)
            {
                resultList.addAll(list);
                for(DependenceInfo dep1:list)
                {
                    System.out.println("已下载：" + dep1);
                }
            }
        }
        StaticLog.info("----------------------------------------");
        StaticLog.info("下载完成，总依赖个数：" + resultList.size());
    }
}
