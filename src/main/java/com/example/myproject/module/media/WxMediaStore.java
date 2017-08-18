package com.example.myproject.module.media;

import lombok.Builder;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * media存储器，提供媒体文件获取，媒体文件保存，转换文件等功能
 * 数据库使用内嵌数据库，经过一天的maven仓库database embedded选型，暂时决定使用MapDB(200k，其实有700K)或者kahaDB(600k)
 * 还有一个PalDB，这些都不小，真不行了我自己实现一个好了。。。暂时先用现成的
 * MapDB最新版依赖真的太多了，不想用了，先用MapDB的老版本吧
 */
public class WxMediaStore implements InitializingBean {

    private DB db;

    private HTreeMap<String, StoreEntity> tempMediaFileDb;

    private HTreeMap<String, String> tempMediaIdDb;

    private HTreeMap<String, StoreEntity> mediaFileDb;

    private HTreeMap<String, String> mediaIdDb;

    /**
     * 用于保存图片
     */
    private HTreeMap<String, String> imageDb;

    private String defaultFilePath = "~/weixin/media/file/";

    private String defaultTempFilePath = "~/weixin/media/file/temp/";

    private String defaultDbPath = "~/weixin/media/db/store.db";

    /**
     * 根据文件查找tempMediaId
     * @param file
     * @return
     */
    public String findTempMediaIdByFile(File file) {
        StoreEntity storeEntity = tempMediaFileDb.get(file.getAbsolutePath());
        // 如果保存的最后更新时间再文件的最后更新时间之前，说明文件有更新，返回空
        if (storeEntity != null && storeEntity.lastModifiedTime.getTime() >= file.lastModified()) {
            return storeEntity.mediaId;
        }
        return null;
    }

    /**
     * 根据tempMediaId查找File
     * @param mediaId
     * @return
     */
    public File findFileByTempMediaId(String mediaId) {
        String filePath = tempMediaIdDb.get(mediaId);
        return findFile(filePath);
    }

    /**
     * 保存tempMedia到File
     * @param mediaId
     * @return
     */
    public File storeTempMediaToFile(String mediaId, Resource resource) throws IOException {
        String fileName = resource.getFilename();
        if (fileName == null) {
            fileName = mediaId;
        }
        File file = new File(StringUtils.applyRelativePath(defaultTempFilePath, fileName));
        if (file.exists()) {
            return file;
        }
        StoreEntity storeEntity = storeFile(file, mediaId, resource);
        tempMediaFileDb.put(file.getAbsolutePath(), storeEntity);
        tempMediaIdDb.put(mediaId, file.getAbsolutePath());
        db.commit();
        return file;
    }

    /**
     * 保存tempMedia到mediaStore
     * @param type
     * @param file
     * @param result
     */
    public WxMedia.TempMediaResult storeFileToTempMedia(WxMedia.Type type, File file, WxMedia.TempMediaResult result) {
        StoreEntity storeEntity = StoreEntity.builder()
                .filePath(file.getAbsolutePath())
                .createTime(result.getCreatedAt())
                .mediaType(type)
                .mediaId(result.getMediaId())
                .lastModifiedTime(new Date(file.lastModified()))
                .result(result)
                .build();
        tempMediaFileDb.put(file.getAbsolutePath(), storeEntity);
        if (result.getMediaId() != null) {
            tempMediaIdDb.put(result.getMediaId(), file.getAbsolutePath());
        }
        // 每执行一个写操作，都要commit，否则强制终止程序时会导致打不开数据库文件
        db.commit();
        return result;
    }

    public String findMediaIdByFile(File file) {
        StoreEntity storeEntity = mediaFileDb.get(file.getAbsolutePath());
        // 如果保存的最后更新时间再文件的最后更新时间之前，说明文件有更新，返回空
        if (storeEntity != null && storeEntity.lastModifiedTime.getTime() >= file.lastModified()) {
            return storeEntity.mediaId;
        }
        return null;
    }

    /**
     * 根据mediaId查找File
     * @param mediaId
     * @return
     */
    public File findFileByMediaId(String mediaId) {
        String filePath = mediaIdDb.get(mediaId);
        return findFile(filePath);
    }

    public WxMedia.MediaResult storeFileToMedia(WxMedia.Type type, File file, WxMedia.MediaResult result) {
        StoreEntity storeEntity = StoreEntity.builder()
                .filePath(file.getAbsolutePath())
                .createTime(new Date())
                .mediaType(type)
                .mediaId(result.getMediaId())
                .lastModifiedTime(new Date(file.lastModified()))
                .mediaUrl(result.getUrl())
                .result(result)
                .build();
        mediaFileDb.put(file.getAbsolutePath(), storeEntity);
        if (result.getMediaId() != null) {
            mediaIdDb.put(result.getMediaId(), file.getAbsolutePath());
        }
        if (result.getUrl() != null) {
            imageDb.put(result.getUrl(), file.getAbsolutePath());
            imageDb.put(file.getAbsolutePath(), result.getUrl());
        }
        // 每执行一个写操作，都要commit，否则强制终止程序时会导致打不开数据库文件
        db.commit();
        return result;
    }

    /**
     * 保存media到File
     * @param mediaId
     * @return
     */
    public File storeMediaToFile(String mediaId, Resource resource) throws IOException {
        String fileName = resource.getFilename();
        if (fileName == null) {
            fileName = mediaId;
        }
        File file = new File(StringUtils.applyRelativePath(defaultFilePath, fileName));
        if (file.exists()) {
            return file;
        }
        StoreEntity storeEntity = storeFile(file, mediaId, resource);
        mediaFileDb.put(file.getAbsolutePath(), storeEntity);
        mediaIdDb.put(mediaId, file.getAbsolutePath());
        db.commit();
        return file;
    }

    private File findFile(String filePath) {
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    private StoreEntity storeFile(File file, String mediaId, Resource resource) throws IOException {
        file.createNewFile();
        file.setLastModified(0l);
        try (FileOutputStream fos = new FileOutputStream(file);
             InputStream inputStream = resource.getInputStream()
        ) {
            StreamUtils.copy(inputStream, fos);
        }
        StoreEntity storeEntity = StoreEntity.builder()
                .filePath(file.getAbsolutePath())
                .createTime(new Date())
                .mediaType(null) //有必要的话可以尝试解析文件名来获取mediaType，暂时不想做
                .mediaId(mediaId)
                .lastModifiedTime(new Date(0l))
                .build();
        return storeEntity;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        File filePath = new File(defaultFilePath);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        filePath = new File(defaultTempFilePath);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        File dbFile = new File(defaultDbPath);
        if (!dbFile.exists()) {
            dbFile.getParentFile().mkdirs();
            dbFile.createNewFile();
        }
        db = DBMaker.newFileDB(dbFile)
                .transactionDisable()
                .cacheDisable()
                .asyncWriteEnable()
                .checksumEnable()
                .closeOnJvmShutdown().make();
        tempMediaFileDb = db.createHashMap("tempMediaFile").expireAfterWrite(3, TimeUnit.DAYS).makeOrGet();
        tempMediaIdDb = db.createHashMap("tempMediaId").expireAfterWrite(3, TimeUnit.DAYS).makeOrGet();
        mediaFileDb = db.createHashMap("mediaFile").makeOrGet();
        mediaIdDb = db.createHashMap("mediaId").makeOrGet();
        imageDb = db.createHashMap("imageUrl").makeOrGet();
    }

    public static void main(String[] args) throws IOException {
        File file = new File("~/weixin/media/db/store.db");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        DB db = DBMaker.newFileDB(file)
                .transactionDisable().asyncWriteFlushDelay(100).closeOnJvmShutdown().make();
//        db.catPut("a", "b");
        String b = db.catGet("a");
        // 2.0支持对更新创建和获取操作加入过期时间
        HTreeMap map = db.getHashMap("tempMedia");
        System.out.println(b);

        WxMedia.TempMediaResult result = new WxMedia.TempMediaResult();
        result.setCreatedAt(new Date());
        result.setType(WxMedia.Type.IMAGE);
        result.setMediaId("asfsfsafsfdsf");

        StoreEntity storeEntity = StoreEntity.builder()
                .filePath(file.getAbsolutePath())
                .createTime(new Date())
                .mediaType(WxMedia.Type.VIDEO)
                .mediaId("adsfsfsffs")
                .lastModifiedTime(new Date(file.lastModified()))
                .result(result)
                .build();
//        map.put(file.getPath(), storeEntity);
        Object o = map.get(file.getAbsolutePath());
        System.out.println(o);
    }

    /**
     * 用于存储的实体
     */
    @Builder
    private static class StoreEntity implements Serializable {
        /**
         * 文件路径
         */
        private String filePath;

        /**
         * 媒体ID
         */
        private String mediaId;

        /**
         * 媒体URL
         */
        private String mediaUrl;

        /**
         * 媒体的创建时间
         */
        private Date createTime;

        /**
         * 最后一次更新时间
         */
        private Date lastModifiedTime;

        /**
         * 媒体类型
         */
        private WxMedia.Type mediaType;

        /**
         * 原始结果
         */
        private WxMedia.Result result;
    }

}