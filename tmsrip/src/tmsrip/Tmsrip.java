package tmsrip;

import org.apache.commons.cli.*;

import java.io.File;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * TODO:
 * Right:
 * -Clean up all the assignments to prevent undefined values at runtime
 * -Notify user of how many tiles it will download, progress, etc.
 * -Robust parsing of source URL
 * -Proper pausing to limit queue size
 * 
 * Fast:
 * -Generate tile URLs more efficiently
 */

/**
 * TMSRip is an application that downloads satellite imagery in bulk for
 * offline use. Unlike other tools, TMSRip does not place artificial limits on
 * what zoom levels you can use or how many tiles you can download. This makes
 * the application flexible and responsive to the user's needs, but it also
 * means that the user must exercise responsibility in order to avoid flooding
 * tile servers with unwanted requests.
 * 
 * @author Thomas Szymczak
 */
public class Tmsrip {

    /**
     * The main method
     * @param args The command line arguments. Run with -h or see usage.txt
     * for more information.
     */
    public static void main(String[] args) {
        CommandLine cmd = null;
        boolean overwrite = true;
        int zMin = 0, zMax = 0, threads = 1, limit = -1;
        double latMin = 0, latMax = 0, lonMin = 0, lonMax = 0;
        String delimRegex = "[{}]";
        String[] tokens;
        String bbox = "", outDir = "", baseURL = "";
        
        // Set up an Options object.
        Options opts = new Options();
        
        opts.addOption("z", true, "Minimum zoom level");
        opts.addOption("Z", true, "Maximum zoom level");
        opts.addOption("b", true, "Bounding box");
        opts.addOption("o", true, "Output directory");
        opts.addOption("u", true, "TMS URL");
        opts.addOption("t", true, "Number of files that can be downloaded concurrently");
        opts.addOption("h", false, "Display help text");
        opts.addOption("w", false, "Do not overwrite existing tiles");
        opts.addOption("l", true, "Stop after downloading this many tiles");
        
        // Parse Options.
        CommandLineParser parser = new DefaultParser();
        
        try {
            cmd = parser.parse(opts, args);
        } catch (org.apache.commons.cli.ParseException e) {
            System.err.println("Something strange went wrong.");
            System.exit(1);
        }
        
        // Print help text.
        if ( cmd.hasOption('h') ) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "tmsrip", opts );
        }
        
        if ( cmd.hasOption('z') ) {
            zMin = Integer.parseInt(cmd.getOptionValue('z'));
        } else {
            System.out.println("Error: No minimum zoom level specified.");
            System.exit(1);
        }
        
        if ( cmd.hasOption('Z') ) {
            zMax = Integer.parseInt(cmd.getOptionValue('Z'));
        } else {
            System.out.println("Error: No maximum zoom level specified.");
            System.exit(1);
        }
        
        if ( cmd.hasOption('b') ) {
            bbox = cmd.getOptionValue('b');
        } else {
            System.out.println("Error: No bounding box specified.");
            System.exit(1);
        }
        
        if ( cmd.hasOption('o') ) {
            outDir = cmd.getOptionValue('o');
        } else {
            System.out.println("Error: No output directory specified.");
            System.exit(1);
        }
        
        if ( cmd.hasOption('u') ) {
            baseURL = cmd.getOptionValue('u');
        } else {
            System.out.println("Error: No TMS URL specified.");
            System.exit(1);
        }
        
        if ( cmd.hasOption('t') ) {
            threads = Integer.parseInt(cmd.getOptionValue('t'));
        } else {
            // If the user doesn't specify, default to one thread.
            threads = 1;
        }
        
        if ( cmd.hasOption('w') ) {
            overwrite = false;
        }
        
        if ( cmd.hasOption('l') ) {
            limit = Integer.parseInt(cmd.getOptionValue('l'));
        }
        
        // Parse the bounding box.
        String[] bboxArray = bbox.split(",");
        lonMin = Double.parseDouble(bboxArray[0]);
        latMin = Double.parseDouble(bboxArray[1]);
        lonMax = Double.parseDouble(bboxArray[2]);
        latMax = Double.parseDouble(bboxArray[3]);
                
        // Download everything!
        downloadTiles(baseURL, zMin, zMax, latMin, latMax, lonMin, lonMax, outDir, threads, overwrite, limit);
        
        System.exit(0);
    }
    
    /**
     * Download a set of tiles from a bounding box, in all the zoom levels the
     * user wants.
     * @param zMin The minimum zoom level
     * @param zMax The maximum zoom level
     * @param latMin The minimum latitude of the bounding box
     * @param latMax The maximum latitude of the bounding box
     * @param lonMin The minimum longitude of the bounding box
     * @param lonMax The maximum longitude of the bounding box
     * @param outDir The directory to store the downloaded tiles.
     * @param threads The maximum number of threads to use for concurrent tile
     * downloading.
     * @param overwrite Whether or not to overwrite already downloaded tiles
     * @param limit Maximum number of tiles to download. If we exceed this
     * limit, exit.
     */
    public static void downloadTiles(String baseURL, int zMin, int zMax, double latMin, double latMax, double lonMin, double lonMax, String outDir, int threads, boolean overwrite, int limit)
    {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        int txMin, txMax, tyMin, tyMax, tilesDownloaded = 0;
        int[] topLeft, bottomRight;
        String delimRegex = "[{}]";
        String[] tokens;

        // Parse the input URL.
        tokens = baseURL.split(delimRegex);

        // For each zoom the user wants, fetch the tiles.
        for ( int zoom = zMin; zoom <= zMax; zoom++ ) {

            // Calculate the range of tiles we need to get the bounding box.
            topLeft = calcTile(zoom, latMax, lonMin);
            bottomRight = calcTile(zoom, latMin, lonMax);

            txMin = topLeft[0];
            txMax = bottomRight[0];
            tyMin = topLeft[1];
            tyMax = bottomRight[1];

            for ( int y = tyMin; y <= tyMax; y++ ) {
                for ( int x = txMin; x <= txMax; x++ ) {
                    // Convert the base url into the specific URL for each tile.
                    String tileUrl = "";
                    for (int i = 0; i < tokens.length; i++ ) {
                        if ( tokens[i].equals("zoom") ) {
                            tileUrl += zoom;
                        } else if ( tokens[i].equals("x") ) {
                            tileUrl += x;
                        } else if ( tokens[i].equals("y") ) {
                            tileUrl += y;
                        } else {
                            tileUrl += tokens[i];
                        }
                    }
                    
                    String destination = outDir + "/" + zoom + "/" + x + "/" + y + ".jpg";

                    // If the directory we want to put the tile in does not exist, create
                    // it.
                    File file = new File(outDir + "/" + zoom + "/" + x);
                    if ( !file.exists() ) {
                        file.mkdirs();
                    }
                    
                    // If the file already exists and we're not supposed to
                    // overwrite it, skip downloading.
                    File file2 = new File(destination);
                    if ( !file2.exists() || overwrite ) {
                        if ( limit == -1 || tilesDownloaded < limit ) {
                            // AWFUL HACK
                            // If too many threads are in the queue, wait
                            // before adding more. This is probably the worst
                            // possible way to implement this.
                            while ( executor.getQueue().size() > threads ) {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e ) {
                                    System.out.println("Sleep interrputed");
                                    System.exit(1);
                                }
                            }
                            Runnable downloader = new TileDownloader(tileUrl, destination);
                            executor.execute(downloader);
                            tilesDownloaded++;
                            System.out.println(tilesDownloaded + " tiles downloaded.");
                        } else {
                            System.out.println("Reached limit of tiles.");
                            executor.shutdown();
                            while (!executor.isTerminated() ) {
                            }
                            System.out.println("Finished downloading: all threads exited.");
                            System.exit(0);
                        }
                    }
                }
            }
        }
        
        executor.shutdown();
        while (!executor.isTerminated() ) {
        }
        System.out.println("Finished downloading: all threads exited.");
    }    

    /**
     * This is just debugging code. Do not use.
     */
    public static void downloadTileTest(String url, int zoom, int x, int y, String outDir)
    {
        String destination = outDir + "/" + zoom + "/" + x + "/" + y + ".jpg";
        
        System.out.println("Downloading " + url);
    }
    
    /**
     * Given the latitude and longitude of a point on Earth, calculate the 
     * coordinates of the tile that contains that point. For example, given
     * the latitude of London, tell us which map tile has an image of London.
     * @param zoom The zoom level
     * @param lat The latitude of the point on Earth
     * @param lon The longitude of the point on Earth
     * @return An array containing the x and y coordinates (in that order) of
     * the desired tile.
     */
    public static int[] calcTile(int zoom, double lat, double lon) {
        int[] output = {0, 0};
        int x;
        int y;
        
        x = (int)Math.floor(((lon + 180 ) / 360) * Math.pow(2, zoom));

        // Do not question this, simply accept that it works.
        // This was so complicated, I literally had to figure out a clever
        // way to transcribe equations to do this without my head exploding.
        y = (int)Math.floor(( 1 - Math.log(Math.tan(lat*(Math.PI/180)) +
         1/Math.cos(lat*(Math.PI/180))) / Math.PI )*Math.pow(2,zoom-1));
        
        output[0] = x;
        output[1] = y;
        return output;
    }
}
