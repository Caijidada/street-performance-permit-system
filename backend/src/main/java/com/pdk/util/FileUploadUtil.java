package com.pdk.util;

import com.pdk.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Component
public class FileUploadUtil {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/jpg", "application/pdf"
    );

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.access-url}")
    private String accessUrl;

    public String upload(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(400, "仅支持 JPG/PNG/PDF 格式文件");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(400, "文件大小不能超过 10MB");
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String dir = uploadPath + subDir + "/" + dateDir + "/";
        new File(dir).mkdirs();

        String ext = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            file.transferTo(new File(dir + fileName));
        } catch (IOException e) {
            throw new BusinessException(500, "文件上传失败");
        }
        return accessUrl + subDir + "/" + dateDir + "/" + fileName;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".bin";
        return filename.substring(filename.lastIndexOf("."));
    }
}
