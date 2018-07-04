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
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

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

    public static String getDependenceByInfo()
    {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        RepositorySystem system = newRepositorySystem(locator);
        RepositorySystemSession session = newSession(system);

        RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();

        Artifact artifact = new DefaultArtifact("cn.hutool:hutool-log:4.1.1");
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(artifact, Collections.singletonList(central), null);
        ArtifactDescriptorResult result = null;
        String theScope = JavaScopes.COMPILE; // Replace with your Artifact
        DependencyFilter theDependencyFilter = DependencyFilterUtils.classpathFilter(theScope);

        CollectRequest theCollectRequest = new CollectRequest();
        theCollectRequest.setRoot(new org.eclipse.aether.graph.Dependency(artifact, theScope));
        //for (RemoteRepository theRepository : remoteRepositories) {
            theCollectRequest.addRepository(central);
        //}
        DependencyRequest theDependencyRequest = new DependencyRequest(theCollectRequest, theDependencyFilter);
        try
        {
            //result = system.readArtifactDescriptor(session, request);
            DependencyResult theDependencyResult = system.resolveDependencies(session, theDependencyRequest);
            for (ArtifactResult theArtifactResult : theDependencyResult.getArtifactResults()) {
                Artifact theResolved = theArtifactResult.getArtifact();
                System.out.println(theResolved);
                // Now we have the artifact file locally stored available
                // and we can do something with it
                File theLocallyStoredFile = theResolved.getFile();
                //theResult.add(theLocallyStoredFile);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

//        assert result != null;
//        for (Dependency dependency : result.getDependencies()) {
//            System.out.println(dependency);
//        }

        return "";
    }

    private static RepositorySystem newRepositorySystem(DefaultServiceLocator locator) {
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        LocalRepository localRepo = new LocalRepository("target/local-repo");
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
        // set possible proxies and mirrors
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
