package com.ticket.shared.port;

import java.io.InputStream;

public interface CloudStoragePort {

    /**
     * Upload binary file lên Cloud Storage và trả về public URL
     *
     * @param inputStream Nội dung file
     * @param originalFilename Tên file gốc
     * @param contentType MIME type (image/png, image/jpeg, etc.)
     * @param folder Thư mục trên storage (vehicles, banners, logos, etc.)
     * @return Public CDN URL vĩnh viễn để truy cập ảnh
     */
    String uploadFile(InputStream inputStream, String originalFilename, String contentType, String folder);

    /**
     * Xóa file khỏi Cloud Storage theo public URL
     *
     * @param fileUrl URL của file cần xóa
     * @return true nếu xóa thành công
     */
    boolean deleteFile(String fileUrl);
}
