package com.luooqi.tools.maven;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * maven-dependency-downloader
 * Created by 何志龙 on 2018-07-09.
 */
public class Main
{
    public static void main(String[] args) throws FileNotFoundException
    {
        if (args.length < 2)
        {
            System.out.println("参数个数有误：\r\n 第一个参数：POM文件路径或者JAR信息 \r\n" + " 第二个参数：依赖存储路径 \r\n");
        }
        String pomPath = args[0];
        String respPath = args[1];
        if (pomPath.toLowerCase().endsWith(".xml"))
        {
            DependenceHelper.downloadDependenceWithPomFile(pomPath, respPath);
        }
        else
        {
            List<DependenceInfo> dependenceInfoList = DependenceHelper.downloadDependency(pomPath, respPath);
            System.out.println("已下载以下依赖：");
            for (DependenceInfo dep:dependenceInfoList)
            {
                System.out.println(dep);
            }
            System.out.println("总依赖数：" + dependenceInfoList.size());
        }
    }
}
