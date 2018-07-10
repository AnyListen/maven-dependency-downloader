# maven-dependency-downloader
本项目实现了两种功能：
1. 根据pom.xml文件将其全部依赖下载到指定的文件夹；
2. 根据Jar信息（groupId:artifactId:version）将其全部依赖下载到指定的文件夹。

> 需要注意的是环境变量必须配置：MAVEN_HOME

## 背景
从今年开始，内网很多项目开始使用 Maven 进行 Java 工程的 Jar 包管理，使用 Maven 自然比以前的 lib 文件夹要方便很多，其优点在这里就不罗列了，谁用谁知道。

但是在没有网络的情况下使用 Maven 还是一件很艰难的事情，还好可以在内网搭建一个 Maven 私服。但是想要把整个 Maven 仓库同步到内网也是不可能的，因此一般是需要开发的适合把其依赖项添加到 Maven 仓库。

例如我需要开发 Elasticsearch 工具包，需要用到 ES 相关的 Jar 包，我可以在外网新建一个项目，然后添加我的依赖项，最后将依赖打包到内网。

这里存在的问题就是，全量打包是很简单的，增量打包很困难，你可以`通过每次改变本地仓库地址来实现增量打包`。但是每次手动修改 Maven 的配置文件也是一件很繁琐的事情，那么是否有工具可以帮我们实现增量打包呢？

答案当然是有的，本文介绍的工具将解决增量打包的问题：
1. 根据 pom.xml 文件将其全部依赖下载到指定的文件夹；
2. 根据 Jar 信息（`groupId:artifactId:version`）将其全部依赖下载到指定的文件夹。

## 根据 pom 文件获取其全部依赖
### Apache Maven Invoker
先看一下官方对该 API 的描述：在很多情况下，我们为了避免对系统的 Maven 环境造成污染，亦或是我们想使用用户目录作为 Maven 的工作目录进行项目构建，总之，`我们希望在一个全新的环境中启动 Maven 构建`。我们可以使用此 API 触发 Maven 构建，此 API 可以执行用户提供的命令行，可以捕获命令行运行中的错误信息，同时支持用户自定义输出（InvocationOutputHandler）和输入（InputStream）类。

翻译的效果很差，举个例子：我想根据pom.xml文件获取该文件的全部依赖，借助 Maven 工具，我们只需要在 pom.xml 所在文件夹下使用命令行执行`mvn dependency:tree`即可获取其全部依赖。

现在如果我们想通过程序的方式来实现，借助`Apache Maven Invoker`即可，下面代码展示了如何根据 pom.xml 获取其全部依赖：
```
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
 * 根据请求获取最终的控制台输出文本信息
 * @param request 请求
 * @return 控制台输出文本
 */
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
```

可以看到代码新建了一个`dependency:tree`的请求，然后在环境变量找到`MAVEN_HOME`，命令执行后通过自定义的输出类（`StringBuilderOutputHandler`）获取控制台文本内容：
```
import org.apache.maven.shared.invoker.InvocationOutputHandler;

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
```

通过上述代码便可获取 pom 文件的全部外部依赖，接下来根据依赖信息把依赖文件下载下来即可。

### 下载依赖文件
下载依赖文件有两种方法：
1. 自行根据依赖信息从 Maven 仓库下载依赖项的 jar 和 pom.xml 文件，然后建立本地路径，将下载的文件放在相应的位置即可；
2. 借助工具帮助我们下载。

借助工具下载，有两种方法：
1. 之前谷歌到的一种方法，使用`org.eclipse.aether`的一系列工具包，可以实现只需要提供 Jar 包信息即可完成全部依赖下载；
2. 写文章到此刻，我觉得上述获取`dependency:tree`的方式应该也可以下载依赖。

#### 使用 org.eclipse.aether 工具包下载全部依赖
第一种方法整体流程就是设置一下自定义的 Maven 中央仓库地址，然后设置一下本地自定义的仓库目录，设置一下网络代理，最后调用 API 就会自动下载依赖，代码如下：
```
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

    RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
    CollectRequest theCollectRequest = new CollectRequest();
    theCollectRequest.setRoot(new org.eclipse.aether.graph.Dependency(artifact, theScope));
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
    return session;
}
```
本方法的确可以下载到全部依赖，但是有一个缺点就是会将一些可选的依赖下载下来，如下如所示：

![可选的依赖会被下载](http://img.luooqi.com/FjanwBuEfm1bta1eVo6UKWmRiN-w)

#### 使用 Maven Invoker 下载全部依赖
这个方法是写文章的时候进行验证的，效果很不错，代码简洁高效，是根据 pom 文件下载全部依赖的最佳方案。

其原理和上面的执行`dependency:tree`是一致的，在 Maven 官网查询到使用`dependency:resolve`可以下载到全部依赖到本地文件。那么现在只需要有个地方可以自定义本地仓库即可，幸运的是`Maven Invoker`支持自定义本地仓库：
```
Invoker invoker = new DefaultInvoker();
String mavenHome = getMavenHome();
if (StrUtil.isNotBlank(localRepo))
{
    //在此设置本地仓库
    invoker.setLocalRepositoryDirectory(new File(localRepo));
}
invoker.setMavenHome(new File(mavenHome));
```

## 总结
如果根据 pom 文件下载依赖则推荐使用`Maven Invoker`方案；如果需要通过 Artifact 信息（`groupId:artifactId:version`）下载依赖则推荐使用`org.eclipse.aether`工具包；详细使用可以参考测试代码。

> 完整代码地址：https://github.com/AnyListen/maven-dependency-downloader

---
`追求高效、有节奏的研发过程； 打造高质量、创新的研发产品。 专注技术、钟情产品！`

欢迎扫码关注『朗坤极客驿站』，遇见更优秀的自己。

![](http://img.luooqi.com/Ft0jWj-Q69B67I8DE9pIBMOa6Bl5)

## 参考
- [maven-invoker](http://maven.apache.org/shared/maven-invoker/usage.html)
- [Maven: get all dependencies programmatically
](https://stackoverflow.com/questions/40813062/maven-get-all-dependencies-programmatically)
- [dependency:tree](https://maven.apache.org/plugins/maven-dependency-plugin/tree-mojo.html)
- [Find all direct dependencies of an artifact on Maven Central
](https://stackoverflow.com/questions/39638138/find-all-direct-dependencies-of-an-artifact-on-maven-central/39641359)
- [List of dependency jar files in Maven
](https://stackoverflow.com/questions/278596/list-of-dependency-jar-files-in-maven)
- [How to download Maven artifacts with Maven >=3.1 and Eclipse Aether](https://www.mirkosertic.de/blog/2015/12/how-to-download-maven-artifacts-with-maven-3-1-and-eclipse-aether/)