package com.niuben.download;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.text.DecimalFormat;

@SpringBootTest
class DownloadApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void getFiles() {
        File file = new File("C:\\blog-upload-download\\upload");
        File[] files = file.listFiles();
        for (File f : files) {
            System.out.println(f.getName()+"---"+f.length());
        }
    }

    @Test
    public void setSize() {
        int size = 134343251;
        //获取到的size为：1705230
        int GB = 1024 * 1024 * 1024;//定义GB的计算常量
        int MB = 1024 * 1024;//定义MB的计算常量
        int KB = 1024;//定义KB的计算常量
        DecimalFormat df = new DecimalFormat("0.00");//格式化小数
        String resultSize = "";
        if (size / GB >= 1) {
            //如果当前Byte的值大于等于1GB
            resultSize = df.format(size / (float) GB) + "GB   ";
        } else if (size / MB >= 1) {
            //如果当前Byte的值大于等于1MB
            resultSize = df.format(size / (float) MB) + "MB   ";
        } else if (size / KB >= 1) {
            //如果当前Byte的值大于等于1KB
            resultSize = df.format(size / (float) KB) + "KB   ";
        } else {
            resultSize = size + "B   ";
        }
        System.out.println(resultSize);
    }
}
