package com.genymobile.scrcpy;

import android.graphics.Rect;
import android.media.MediaCodecInfo;

import java.util.Locale;

public final class Server {

    private Server() {
        // not instantiable
    }

    private static void parseArguments(Options options, VideoSettings videoSettings, String... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Missing client version");
        }

        String clientVersion = args[0];
        if (!clientVersion.equals(BuildConfig.VERSION_NAME)) {
            throw new IllegalArgumentException(
                    "The server version (" + BuildConfig.VERSION_NAME + ") does not match the client " + "(" + clientVersion + ")");
        }

        if (args[1].equalsIgnoreCase("web")) {
            options.setServerType(Options.TYPE_WEB_SOCKET);
            if (args.length > 2) {
                Ln.Level level = Ln.Level.valueOf(args[2].toUpperCase(Locale.ENGLISH));
                options.setLogLevel(level);
            }
            if (args.length > 3) {
                int portNumber = Integer.parseInt(args[3]);
                options.setPortNumber(portNumber);
            }
            if (args.length > 4) {
                boolean listenOnAllInterfaces = Boolean.parseBoolean(args[4]);
                options.setListenOnAllInterfaces(listenOnAllInterfaces);
            }
            return;
        }
        // TODO: put here original scrcpy params parsing
    }

    private static Rect parseCrop(String crop) {
        if (crop.isEmpty()) {
            return null;
        }
        // input format: "width:height:x:y"
        String[] tokens = crop.split(":");
        if (tokens.length != 4) {
            throw new IllegalArgumentException("Crop must contains 4 values separated by colons: \"" + crop + "\"");
        }
        int width = Integer.parseInt(tokens[0]);
        int height = Integer.parseInt(tokens[1]);
        int x = Integer.parseInt(tokens[2]);
        int y = Integer.parseInt(tokens[3]);
        return new Rect(x, y, x + width, y + height);
    }

    private static void suggestFix(Throwable e) {
        if (e instanceof InvalidDisplayIdException) {
            InvalidDisplayIdException idie = (InvalidDisplayIdException) e;
            int[] displayIds = idie.getAvailableDisplayIds();
            if (displayIds != null && displayIds.length > 0) {
                Ln.e("Try to use one of the available display ids:");
                for (int id : displayIds) {
                    Ln.e("    scrcpy --display " + id);
                }
            }
        } else if (e instanceof InvalidEncoderException) {
            InvalidEncoderException iee = (InvalidEncoderException) e;
            MediaCodecInfo[] encoders = iee.getAvailableEncoders();
            if (encoders != null && encoders.length > 0) {
                Ln.e("Try to use one of the available encoders:");
                for (MediaCodecInfo encoder : encoders) {
                    Ln.e("    scrcpy --encoder '" + encoder.getName() + "'");
                }
            }
        }
    }

    public static void main(String... args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Ln.e("Exception on thread " + t, e);
                suggestFix(e);
            }
        });

        Options options = new Options();
        VideoSettings videoSettings = new VideoSettings();
        parseArguments(options, videoSettings, args);
        Ln.initLogLevel(options.getLogLevel());
        if (options.getServerType() == Options.TYPE_LOCAL_SOCKET) {
            new DesktopConnection(options, videoSettings);
        } else if (options.getServerType() == Options.TYPE_WEB_SOCKET) {
            WSServer wsServer = new WSServer(options);
            wsServer.setReuseAddr(true);
            wsServer.run();
        }
    }
}
