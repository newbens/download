package com.niuben.download;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
class DownloadApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void getFiles() {
        File file = new File("C:\\upload");
        File[] files = file.listFiles();
        for (File f : files) {
            System.out.println(f.getName()+"---"+f.length());
        }
    }

}
