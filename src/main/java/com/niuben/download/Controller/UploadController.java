package com.niuben.download.Controller;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Controller
public class UploadController {

    private final static String UTF8 = "utf-8";

    @RequestMapping("/upload")
    @ResponseBody
    public void upload(HttpServletRequest request,
                         HttpServletResponse response) throws Exception{
        response.setCharacterEncoding(UTF8);
        Integer currentChunk = null; //当前分片数
        Integer totalChunk = null; //总分片数
        String name = null; //文件名称
        String uploadPath = "C:\\upload"; //存储路径
        BufferedOutputStream os = null;
        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(1024);//缓存区大小
            factory.setRepository(new File(uploadPath));//存储路径
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setFileSizeMax(5l * 1024l * 1024l * 1024l);
            upload.setSizeMax(10l * 1024l * 1024l * 1024l);
            List<FileItem> fileItems = upload.parseRequest(request);
            //获取文件信息
            for (FileItem fileItem : fileItems) {
                if (fileItem.isFormField()) {
                    if ("chunk".equals(fileItem.getFieldName())) {
                        currentChunk = Integer.parseInt(fileItem.getString(UTF8));
                    }
                    if ("chunks".equals(fileItem.getFieldName())) {
                        totalChunk = Integer.parseInt(fileItem.getString(UTF8));
                    }
                    if ("name".equals(fileItem.getFieldName())) {
                        name = fileItem.getString(UTF8);
                    }
                }
            }
            //获取文件分片
            for (FileItem fileItem : fileItems) {
                if (!fileItem.isFormField()) {
                    String temFileName = name;
                    if (name != null) {
                        if (currentChunk != null) {
                            temFileName = name +"_"+ currentChunk;
                        }
                        File temFile = new File(uploadPath,temFileName);
                        if(!temFile.exists()) {//如果文件不存在,就写入
                            fileItem.write(temFile);
                        }

                    }
                }
            }
            //文件合并
            if (currentChunk != null && currentChunk.intValue() == totalChunk.intValue()-1) {  //说明有分片需要合并，并且是最后一个分片的话就需要合并。
                File temFile = new File(uploadPath,name);
                os = new BufferedOutputStream(new FileOutputStream(temFile));
                //合并
                for (int i = 0; i < totalChunk; i++) {
                    File file = new File(uploadPath,name+"_"+i);
                    while (file.exists()) { //判断分片是否存在，因为发送分片是一个并发的过程，最后一个分片到了，之前的分片不一定到了
                        Thread.sleep(100);//休眠一会，等会继续获取
                    }
                    byte[] bytes = FileUtils.readFileToByteArray(file);
                    os.write(bytes);
                    os.flush();
                    file.delete();
                }
                os.flush();
                response.getWriter().write("上传成功"+name);
            }
        }finally {
            try {
                if (os != null) { //关闭流
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
