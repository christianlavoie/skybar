package org.wtf.skybar.time;

import net.openhft.koloboke.collect.map.IntLongMap;
import org.wtf.skybar.registry.SkybarRegistry;

import java.io.*;
import java.util.Map;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Created by clavoie@sandreckoning.com on 11/11/2015.
 */
public class RegistryDumpStats extends Thread implements SkybarRegistry.DeltaListener {
    private final SkybarRegistry registry;
    private final File path;
    private final long intervalInSeconds;
    private static final Logger LOG = Log.getLogger(RegistryDumpStats.class);
    private Map<String, IntLongMap> snapshot;

    public RegistryDumpStats(SkybarRegistry registry, File path, long intervalInSeconds) {
        this.registry = registry;
        this.path = path;
        this.intervalInSeconds = intervalInSeconds;
    }

    @Override
    public void run() {
        this.snapshot = registry.getCurrentSnapshot(this);

        do {
            try {
                sleep(intervalInSeconds * 1000);
                writeSnapshotToDisk();

            } catch (InterruptedException e) {
                LOG.info("Sleep interrupted", e);
            }
        } while (true);
    }

    private void writeSnapshotToDisk() {
        synchronized (snapshot) {
            try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(this.path))) {
                for (Map.Entry<String, IntLongMap> fileEntry: this.snapshot.entrySet()) {
                    for (Map.Entry<Integer, Long> lineEntry : fileEntry.getValue().entrySet()) {
                        String file = fileEntry.getKey();
                        int lineno = lineEntry.getKey();
                        long count = lineEntry.getValue();
                        osw.write(String.format("%s:%d:%d\n", file, lineno, count));
                    }
                }
            } catch (FileNotFoundException e) {
                LOG.warn("Could not open file to dump report", e);

            } catch (IOException e) {
                LOG.warn("Barfed while writing entry to path " + this.path + ": ", e);
            }
        }
    }

    @Override
    public void accept(Map<String, IntLongMap> newMap) {
        synchronized (snapshot) {
            snapshot.putAll(newMap);
        }
    }
}
