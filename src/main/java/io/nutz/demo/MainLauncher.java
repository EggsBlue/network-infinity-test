package io.nutz.demo;

import org.nutz.boot.NbApp;
import org.nutz.http.Http;
import org.nutz.http.Response;
import org.nutz.ioc.impl.PropertiesProxy;
import org.nutz.ioc.loader.annotation.*;
import org.nutz.lang.Files;
import org.nutz.lang.Streams;
import org.nutz.lang.Strings;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.mvc.annotation.*;
import io.nutz.demo.bean.User;
import org.nutz.dao.Dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@IocBean(create = "init", depose = "depose")
public class MainLauncher {
    static Log log = Logs.get();

    @Inject
    protected PropertiesProxy conf;

    static String outPath = "";
    static String url = "https://mirrors.aliyun.com/ubuntu/pool/universe/0/0ad/0ad-dbg_0.0.20-1_amd64.deb";
    static int poolNumber = 100;

    @At("/")
    @Ok("->:/index.html")
    public void index() {
    }

    public void init() {
        if (Strings.isBlank(outPath)) {
            log.error("=====>>>outPath is null");
            return;
        }
        Files.createDirIfNoExists(new File(outPath));
        log.debug("outPath:"+outPath);
        log.debug("url:"+url);
        log.debug("poolNumber:"+poolNumber);

        startDownloadThread();
    }

    private void startDownloadThread() {
        for (int i = 0; i < 10000000; i++) {
            ThreadPoolExecutor executorService =
                    new ThreadPoolExecutor(poolNumber + 10, 500, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(2000), new ThreadPoolExecutor.AbortPolicy());

            CountDownLatch latch = new CountDownLatch(poolNumber);
            log.debug("开始下载");
            for (int j = 0; j < poolNumber; j++) {
                executorService.execute(() -> {
                    download(url, outPath);
                    latch.countDown();
                });
            }

            // 终止线程池
            try {
                latch.await(30, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                log.error(e);
            } finally {
                executorService.shutdown();

                log.debug("下载完成");
                removeDisk(outPath);
            }
        }

    }

    private void removeDisk(String outPath) {
        try {
            Files.deleteDir(new File(outPath));
        } catch (Exception e) {
            log.error(e);
        } finally {
            Files.createDirIfNoExists(new File(outPath));
            log.debug("=====>>删除目录成功");
        }
    }

    void download(String url, String outPath) {
        try {
            Response response = Http.get(url);
            InputStream stream = response.getStream();
            Streams.write(new FileOutputStream(new File(outPath + "/" + System.currentTimeMillis() + "tmp")), stream);
        } catch (Exception e) {
            log.error(e);
        }
    }

    public void depose() {

    }

    public static void main(String[] args) throws Exception {
        initArgs(args);
        new NbApp().setArgs(args).setPrintProcDoc(true).run();
    }

    private static void initArgs(String[] args) {
        for (String arg : args) {
            final String[] split = arg.split("=");
            log.debug("arg=====>" + arg);
            if (split[0].equals("outPath")) {
                outPath = split[1];
            } else if (split[0].equals("url")) {
                url = split[1];
            } else if (split[0].equals("poolNumber")) {
                poolNumber = Integer.valueOf(split[1]);
            }
        }

    }

}
