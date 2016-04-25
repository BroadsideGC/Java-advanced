package ru.ifmo.ctddev.zemskov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;
import net.java.quickcheck.collection.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Basic implementation of {@link info.kgeorgiy.java.advanced.crawler.Crawler}.
 *
 * @author Kirill Zemskov 
 */
public class WebCrawler implements Crawler {
    private final ExecutorService downloadThreadPool;
    private final ExecutorService extractThreadPool;
    private final Downloader downloader;
    private final ConcurrentHashMap<String, Integer> hostCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockingQueue<Supplier<String>>> left = new ConcurrentHashMap<>();
    private final Set<String> downloaded = ConcurrentHashMap.newKeySet();
    private final int perHost;

    /**
     * Class constructor, specifying what {@link Downloader} to use, number of threads,
     * which download, number of threads, which extract and maximal number of threads,
     * which can download from the same host simultaneously ({@code perHost}).
     *
     * @param downloader  downloader, which will be used to get web-page
     * @param downloaders number of threads for downloading
     * @param extractors  number of threads for extracting links
     * @param perHost     maximal number of threads, which can download from the same
     *                    host simultaneously
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloadThreadPool = Executors.newFixedThreadPool(downloaders);
        extractThreadPool = Executors.newFixedThreadPool(extractors);
    }

    /**
     * Gets list of all URLs, that were visited by crawler, starting from {@code url}
     * and lifting by {@code depth} down as most.
     *
     * @param url   url, specifying starting position of crawler
     * @param depth maximal depth of web-pages, which will be visited by crawler
     * @return {@link info.kgeorgiy.java.advanced.crawler.Result}, containing list of all
     * downloaded links and all errors, which happened during execution
     */
    @Override
    public Result download(String url, int depth) {
        BlockingQueue<Pair<Future<String>, String>> queue = new LinkedBlockingQueue<>();
        List<String> result = new ArrayList<>();
        Map<String, IOException> errors = new HashMap<>();
        try {
            try {
                hostCount.put(URLUtils.getHost(url), 1);
            } catch (IOException e) {
                errors.put(url, e);
            }
            queue.add(new Pair<>(downloadThreadPool.submit(() -> processDownloader(url, 1, depth, queue)), url));
            while (!queue.isEmpty()) {
                Pair<Future<String>, String> pair = queue.take();
                String res;
                try {
                    res = pair.getFirst().get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof IOException) {
                        errors.put(pair.getSecond(), (IOException) e.getCause());
                        continue;
                    }
                    throw e;
                }
                if (res != null && !errors.containsKey(res)) {
                    result.add(res);
                }
            }
            return new Result(result, errors);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Shutdowns all threads, created by crawler. All invocations of {@link
     * #download(String, int)}, that didn't finish yet, will return {@code null}
     * as result.
     */
    @Override
    public void close() {
        downloadThreadPool.shutdown();
        extractThreadPool.shutdown();
    }


    private void processExtractor(Document document, int depth, int maxDepth, BlockingQueue<Pair<Future<String>, String>> queue) throws IOException, InterruptedException {
        List<String> links;
        links = document.extractLinks();
        for (String link : links) {
            synchronized (hostCount) {
                hostCount.putIfAbsent(URLUtils.getHost(link), 0);
                if (hostCount.get(URLUtils.getHost(link)) < perHost) {
                    hostCount.compute(URLUtils.getHost(link), (s, i) -> i + 1);
                    queue.add(new Pair<>(downloadThreadPool.submit(() -> processDownloader(link, depth + 1, maxDepth, queue)), link));
                } else {
                    left.putIfAbsent(URLUtils.getHost(link), new LinkedBlockingQueue<>());
                    left.get(URLUtils.getHost(link)).put(() -> {
                        try {
                            return processDownloader(link, depth + 1, maxDepth, queue);
                        } catch (Exception e) {
                            System.err.println("Some problem during downloading: " + e.getMessage());
                            return null;
                        }
                    });
                }
            }
        }
    }

    private String processDownloader(String url, int depth, int maxDepth, BlockingQueue<Pair<Future<String>, String>> queue) throws IOException, InterruptedException {
        if (downloaded.add(url)) {
            Document document = downloader.download(url);
            if (depth < maxDepth) {
                queue.put(new Pair<>(extractThreadPool.submit(() -> {
                    processExtractor(document, depth, maxDepth, queue);
                    return null;
                }), url));
            }
        }

        synchronized (hostCount) {
            BlockingQueue<Supplier<String>> q = left.get(URLUtils.getHost(url));
            if (q != null && !q.isEmpty()) {
                Supplier<String> supplier = q.take();
                queue.put(new Pair<>(downloadThreadPool.submit(supplier::get), url));
            } else {
                hostCount.compute(URLUtils.getHost(url), (s, integer) -> integer - 1);
            }
        }
        return url;
    }

    /**
     * Main function, which performs crawling of specified url, using specified
     * number of thread for downloading, extracting. Also maximal number of downloaders
     * for the same host can be specified.
     * <p>
     * Usage: WebCrawler url [downloaders [extractors [perHost]]]
     *
     * @param args array of string arguments, which must match to "Usage"
     */
    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 4) {
            System.err.println("Usage: WebCrawler url [downloaders [extractors [perHost]]]");
            return;
        }
        int downloaders = 1;
        int extractors = 1;
        int perHost = 1;
        if (args.length > 1) {
            downloaders = Integer.parseInt(args[1]);
        }

        if (args.length > 2) {
            extractors = Integer.parseInt(args[2]);
        }

        if (args.length > 3) {
            perHost = Integer.parseInt(args[3]);
        }

        try (WebCrawler webCrawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
            System.out.println(webCrawler.download(args[0], 3));
        } catch (IOException e) {
            System.err.println("Couldn't download page: " + e.getMessage());
        }
    }
}
