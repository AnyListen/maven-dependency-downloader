package com.luooqi.tools.maven;


import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.List;

/**
 * maven-dependency-downloader
 * Created by 何志龙 on 2018-07-04.
 */

public class DependenceHelperTest
{

    @Test
    public void getDependenceListByPom() throws FileNotFoundException
    {
        List<DependenceInfo> list = DependenceHelper.getDependenceListByPom("D:\\Code\\Github\\maven-dependency-downloader.git\\trunk\\pom.xml");
        System.out.println(list);
    }

    @Test
    public void getDependenceByInfo()
    {
        DependenceHelper.getDependenceByInfo();
    }

    @Test
    public void downloadDependence()
    {
    }
}
