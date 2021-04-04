package com.niuben.download.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

@Controller
public class DownloadController {
    private final static String UTF8 = "utf-8";
    @RequestMapping("/download/{fileName}")
    public void downloadFile(HttpServletRequest request,
                             HttpServletResponse response,
                             @PathVariable("fileName") String fileName) throws Exception {
        File file = new File("C:\\upload\\"+fileName);
        response.setCharacterEncoding(UTF8);
        InputStream in = null;
        OutputStream out = null;
        try {
            //分片下载
            long fileSize = file.length();
            response.setContentType("application/x-download");
            String filename = URLEncoder.encode(file.getName(), UTF8);
            response.addHeader("Content-Disposition","attachment;filename="+filename);
            //设置支持分片下载
            response.setHeader("Accept-Range","bytes");
            //方便客户端获取文件长度
            response.setHeader("fileSize", String.valueOf(fileSize));
            response.setHeader("fileName",filename);
            long pos = 0,last = fileSize - 1,sum = 0;//pos分片起始位置，last最后位置
            if (request.getHeader("Range") != null) {//说明前端需要分片下载
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                String numRange = request.getHeader("Range").replaceAll("bytes=", "");
                String[] strRange = numRange.split("-");
                if (strRange.length == 2) {
                    pos = Long.parseLong(strRange[0].trim());
                    last = Long.parseLong(strRange[1].trim());
                    //可能会出现结束位置大于文件位置
                    if(last > fileSize) last = fileSize - 1;
                }else {
                    pos = Long.parseLong(numRange.replace("-", "").trim());
                }
            }
            long rangeLen = last - pos + 1;//分片长度
            String contentRange = new StringBuffer("bytes ").append(pos).append("-").append(last).append("/").append(fileSize).toString();
            response.setHeader("Content-Range",contentRange);
            response.setHeader("Content-Length",rangeLen+"");
            out = new BufferedOutputStream(response.getOutputStream());
            in = new BufferedInputStream(new FileInputStream(file));
            in.skip(pos);//跳过前面分片读
            byte[] bytes = new byte[1024];
            int len = 0;
            while (sum < rangeLen) {
                len = in.read(bytes, 0, (rangeLen - sum) <= bytes.length ? (int) (rangeLen - sum) : bytes.length);
                sum += len;
                out.write(bytes,0,len);
            }
            System.out.println("---------------下载完成！！！！！---------------");

        }finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }

    }
}
