package com.niuben.download.client;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class DownloadClient {

    private final static long PER_PAGE = 1024l * 1024L * 50l;//默认每个分片5m
    private final static String DOWN_PATH = "C:\\download";
    ExecutorService pool = Executors.newFixedThreadPool(10);//定义线程池

    /**文件大小决定分片数量。
     * 探测 下载少量 获取文件信息
     * 多线程下载
     * 等最后一个分片下载完成 开始合并
     * @return
     */
    @RequestMapping("/downloadFile/{fileName}")
    public String downloadFile(@PathVariable("fileName") String fileName) throws Exception {
        FileInfo fileInfo = download(0, 10, -1,fileName);
        if (fileInfo != null) {
            long pages = fileInfo.fileSize / PER_PAGE;
            for (long i = 0; i <= pages; i++) {
                pool.submit(new Download(i*PER_PAGE,(i+1)*PER_PAGE -1,i,fileInfo.fileName));
            }
        }
        return "success";
    }

    //下载文件
    private FileInfo download(long start,long end,long page,String fileName) throws Exception {
        File file = new File(DOWN_PATH,fileName+"-"+page);
        /**
         *这几种情况下不需要下载
         *   文件已经存在，如果文件已经存在但是文件是探测文件就必须下载，因为探测需要获取文件信息。
         *   还有一种是分片文件下载未完成。
         */
        if (file.exists() && page != -1 && file.length() == PER_PAGE) {
            return null;
        }
        HttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/download/"+fileName);
        //告诉服务端做分片下载
        httpGet.setHeader("Range","bytes="+start+"-"+end);
        HttpResponse response = client.execute(httpGet);
        String fileSize = response.getFirstHeader("fileSize").getValue();
        //entity可以直接去文件流
        HttpEntity entity = response.getEntity();
        InputStream in = entity.getContent();
        FileOutputStream out = new FileOutputStream(file);
        byte[] bytes = new byte[1024];
        int ch;
        while ((ch = in.read(bytes)) != -1) {
            out.write(bytes,0,ch);
        }
        in.close();
        out.flush();
        out.close();
        if (end - Long.valueOf(fileSize) > 0) {
            //开始合并文件
            mergeFile(fileName,page);
        }
        return new FileInfo(Long.parseLong(fileSize), fileName);
    }

    //文件合并
    private void mergeFile(String fileName, long page) throws Exception {
        File file = new File(DOWN_PATH,fileName);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        for (int i = 0; i <= page; i++) {
            File tempFile = new File(DOWN_PATH,fileName+"-"+i);
            //合并的时候 因为多线程的原因，可能前面的文件没有下载完成 所以需要等待
            while (!tempFile.exists() || (i != page && tempFile.length() < PER_PAGE)) {
                Thread.sleep(100);
            }
            byte[] bytes = FileUtils.readFileToByteArray(tempFile);
            out.write(bytes);
            out.flush();
            tempFile.delete();
        }
        File file1 = new File(DOWN_PATH,fileName+"--1");
        file1.delete();
        out.flush();
        out.close();
    }

    class Download implements Runnable{

        long start;
        long end;
        long page;
        String fileName;

        public Download(long start, long end, long page, String fileName) {
            this.start = start;
            this.end = end;
            this.page = page;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            try {
                download(start,end,page,fileName);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }


    class FileInfo{
        long fileSize;
        String fileName;

        public FileInfo(long fileSize, String fileName) {
            this.fileSize = fileSize;
            this.fileName = fileName;
        }
    }
}
