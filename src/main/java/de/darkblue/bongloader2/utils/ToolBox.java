/*
 * Copyright (C) 2016 Florian Frankenberger.
 *
 * This library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, see <http://www.gnu.org/licenses/>.
 */
package de.darkblue.bongloader2.utils;

import de.darkblue.bongloader2.Configuration;
import de.darkblue.bongloader2.ConfigurationKey;
import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.model.Recording.MovieFile.Quality;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Toolbox
 * 
 * @author Florian Frankenberger
 */
public final class ToolBox {

    public static final Recording DEMO_RECORDING = new Recording() {{
        this.setTitle("Foobar und die Suche nach der knolligen Kartoffel - der Film");
        this.setSubtitle("Der große Kinofilm zur Knolle");
        this.setDescription("Foobar und seine Freunde retten wieder einmal die Welt.");
        this.setDuration(60);
        this.setGenre("Drama");
        this.setChannel("Dramakanal");
        this.setStart(new Date());
        try {
            this.addFileURL(Recording.MovieFile.Quality.NQ, new Recording.MovieFile(new URL("http://bongloader.darkblue.de/bongloader.mp4"), Recording.MovieFile.Quality.NQ));
        } catch (MalformedURLException ex) {
        }
    }};

    public static final Recording DEMO_RECORDING_SERIES = new Recording() {{
        this.setTitle("Serie: Foobar und die Suche nach der knolligen Kartoffel");
        this.setSubtitle("Die Reise beginnt");
        this.setDescription("Foobar und seine Freunde begeben sich auf eine abenteuerreiche Reise in das Land der Knolle.");
        this.setDuration(120);
        this.setGenre("Komödie");
        this.setChannel("Komödienkanal");
        this.setSeriesSeason(2);
        this.setSeriesNumber(5);
        this.setSeriesCount(12);
        this.setStart(new Date());
        try {
            this.addFileURL(Recording.MovieFile.Quality.HQ, new Recording.MovieFile(new URL("http://bongloader.darkblue.de/bongloader.mp4"), Recording.MovieFile.Quality.HQ));
        } catch (MalformedURLException ex) {
        }
    }};

    public static enum ShortenStyle {
        RIGHT,
        CENTER
    }
    private static final Logger LOGGER = Logger.getLogger(ToolBox.class.getCanonicalName());
    private static final String[] ILLEGAL_FILANAME_CHARACTERS = {"\n", "\r", "\t", "\0", "\f", "`", "?", "*", "<", ">", "|"};
    private static FilterFunction<String> FILENAME_FILTER_FUNCTION;

    private ToolBox() {
    }

    public static void init(final Configuration config) {
        FILENAME_FILTER_FUNCTION = new FilterFunction<String>() {

            @Override
            public String filter(String item) {
                return item.replaceAll("[^\\s^0-9^\\w^\\_^\\-^\\.^\\p{L}\\.]", config.get(ConfigurationKey.INVALID_CHAR_REPLACEMENT));
            }

        };
    }

    public static BufferedImage resizeImage(BufferedImage image, int width, int height) {
        BufferedImage returnValue
                = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2D = returnValue.createGraphics();
        g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        AffineTransform at
                = AffineTransform.getScaleInstance((double) width / image.getWidth(),
                        (double) height / image.getHeight());
        g2D.drawRenderedImage(image, at);
        return returnValue;
    }

    public static BufferedImage grayScale(BufferedImage image) {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = newImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return newImage;
    }

    public static BufferedImage grayScaleAlpha(BufferedImage original) {

        int alpha, red, green, blue;
        int newPixel;

        BufferedImage avg_gray = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
        int[] avgLUT = new int[766];
        for (int i = 0; i < avgLUT.length; i++) {
            avgLUT[i] = (int) (i / 3);
        }

        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {

                // Get pixels by R, G, B
                int color = original.getRGB(x, y);
                alpha = color & 0xFF000000;

                red = (color >> 16) & 0xFF;
                green = (color >> 8) & 0xFF;
                blue = color & 0xFF;

                newPixel = red + green + blue;
                newPixel = avgLUT[newPixel];
                // Return back to original format
                newPixel = newPixel | (newPixel << 8) | (newPixel << 16) | alpha;

                // Write pixels into image
                avg_gray.setRGB(x, y, newPixel);

            }
        }

        return avg_gray;

    }

    public static BufferedImage resizeImageToTray(BufferedImage image) {
        SystemTray tray = SystemTray.getSystemTray();
        Dimension dimension = tray.getTrayIconSize();
        return resizeImage(image, dimension.width, dimension.height);
    }
    private static final String[] humanStrings = {"Bytes", "KB", "MB", "GB"};

    public static String toHumanReadableSize(long fileSize) {
        final String format = "%.2f %s";
        return toHumanReadableSize(format, fileSize);
    }

    public static String toHumanReadableSize(final String format, long fileSize) {
        for (int i = humanStrings.length - 1; i > 0; --i) {
            float thisValue = (float) Math.pow(1024, i);
            if (fileSize / thisValue >= 1) {
                return String.format(format, fileSize / thisValue, humanStrings[i]);
            }
        }

        return String.format(format, 0f, humanStrings[0]);
    }
    private static final String[] humanStringsTime = {"s", "m", "h", "d", "w"};
    private static final int[] humanStringsUnitTime = {1, 60, 60, 24, 7};

    public static String toHumanReadableTime(long seconds) {
        return toHumanReadableTime(seconds, "s");
    }

    public static String toHumanReadableTime(long seconds, String minUnit) {
        StringBuilder sb = new StringBuilder();

        if (seconds == Long.MAX_VALUE) {
            return "Unendlich";
        }

        long div = 1;
        for (int unit : humanStringsUnitTime) {
            div *= unit;
        }

        boolean alreadyWritten = false;
        boolean started = false;
        for (int i = humanStringsTime.length - 1; i >= 0; --i) {
            final String humanStringTime = humanStringsTime[i];
            long value = seconds / div;
            seconds = seconds % div;
            div /= humanStringsUnitTime[i];

            if (!started && (alreadyWritten || value != 0)) {
                if (alreadyWritten) {
                    sb.append(' ');
                }
                sb.append(String.format("%02d", value));
                sb.append(humanStringTime);
                alreadyWritten = true;
            }

            if (humanStringTime.equalsIgnoreCase(minUnit)) {
                started = true;
            }

        }

        return sb.toString();
    }

    public static BufferedImage loadGraphic(String name) {
        try {
            return ImageIO.read(ToolBox.class.getResource("/de/darkblue/bongloader2/icons/" + name + ".png"));
        } catch (IOException e) {
            throw new RuntimeException("Neccessary graphic " + name + ".png could not be loaded.");
        }
    }

    public static void openURLinBrowser(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public static Dimension getFontExtent(String string, Graphics2D context) {
        return new Dimension(
                context.getFontMetrics().stringWidth(string),
                context.getFontMetrics().getHeight());
    }

    public static float getFontBaseline(Graphics2D context) {
        final FontMetrics fontMetrics = context.getFontMetrics();
        return fontMetrics.getMaxAscent();
    }

    public static String getStringFittingInto(String str, int width, ShortenStyle shortenStyle, Graphics2D context) {
        Dimension dim = getFontExtent(str, context);

        if (shortenStyle == ShortenStyle.RIGHT) {
            boolean shortened = false;
            while (dim.width > width) {
                str = str.substring(0, str.length() - 1);
                dim = getFontExtent(str + "...", context);
                shortened = true;
            }

            return shortened ? str + "..." : str;
        } else if (shortenStyle == ShortenStyle.CENTER) {
            int counter = 0;
            String testStr = str;
            final int middle = str.length() / 2 - 1;
            while (dim.width > width) {
                counter++;
                testStr = str.substring(0, middle - counter) + "..." + str.substring(middle + counter);
                dim = getFontExtent(testStr, context);
            }

            return testStr;
        } else {
            return str;
        }
    }

    public static void setDefaultRendering(Graphics2D context) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Map desktopHints = (Map) (tk.getDesktopProperty("awt.font.desktophints"));

        if (desktopHints != null) {
            context.addRenderingHints(desktopHints);
        }
    }

    /**
     * returns the file size using the config to retrieve the current version of
     * bongloader and setting the right user-agent
     *
     * @param config
     * @param url
     * @return
     * @throws IOException
     */
    public static long getFileSize(final Configuration config, final URL url) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            if (config != null) {
                connection.setRequestProperty("User-Agent", "BongLoader2 " + config.get(ConfigurationKey.VERSION));
            }
            connection.setConnectTimeout(2000);
            connection.connect();

            final String headerField = connection.getHeaderField("Content-Length");
            if (headerField == null) {
                throw new IOException("Did not get a content length for the connection to " + url);
            }

            final String rawContentLength = headerField.trim();
            return Long.valueOf(rawContentLength);
            //return connection.getContentLength();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String getTargetFilename(final File workingDir, final String pattern, final Recording recording, final Quality quality) {
        final StringTemplate template = new StringTemplate(pattern);
        final Map<Object, Object> replacements = new HashMap<Object, Object>();
        replacements.put("recording", recording);
        replacements.put("quality", quality);
        String fileName = template.apply(replacements, FILENAME_FILTER_FUNCTION, true)
                + Utils.getFileNameExt(recording.getMovieFile(quality).getDownloadUrl().getFile());
        return fileName.replaceAll("\\s*([\\\\/])\\s*", "$1");
    }

    /**
     * returns the target name for a selected recording and the quality provided
     *
     * @param workingDir
     * @param pattern
     * @param recording
     * @param quality
     * @return
     */
    public static File getTargetFile(final File workingDir, final String pattern, final Recording recording, final Quality quality) {
        final String fileName = getTargetFilename(workingDir, pattern, recording, quality);
        final File fileDirect = new File(fileName);
        final File fileIndirect = new File(workingDir, fileName);

        if (fileName.charAt(0) == fileDirect.getAbsolutePath().charAt(0)) {
            return fileDirect;
        } else {
            return fileIndirect;
        }
    }

    /**
     * checks if there are any problems with the file name
     *
     * @param filename
     * @return
     */
    public static boolean isFilenameValid(final String filename) {
        if (!filename.matches(".*?\\s+([\\\\/]).*?") && !filename.matches(".*?([\\\\/])\\s+.*?")) {
            for (String str : ILLEGAL_FILANAME_CHARACTERS) {
                if (filename.contains(str)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * cleans the string from all unneccessary things like html entities.
     *
     * @param in
     * @return
     */
    public static String cleanString(String in) {
        return StringEscapeUtils.unescapeHtml4(in);
    }

    public static boolean checkFilenamePattern(String pattern, File workingDirectory) {
        String[] fileNames = new String[] {
            ToolBox.getTargetFilename(workingDirectory, pattern, DEMO_RECORDING, Recording.MovieFile.Quality.NQ),
            ToolBox.getTargetFilename(workingDirectory, pattern, DEMO_RECORDING_SERIES, Recording.MovieFile.Quality.HQ)
        };

        boolean ok = true;
        for (int i = 0; i < fileNames.length; ++i) {
            try {
                final String fileName = fileNames[i];
                ok = ok & ToolBox.isFilenameValid(fileName);
                final File file = new File(fileName);
                file.getCanonicalPath();
            } catch (IOException ex) {
                ok = false;
            }
        }

        return ok;
    }

//    /**
//     * checks if the given file contains invalid characters.
//     *
//     * @param file
//     * @return
//     */
//    public static boolean isFileValid(File file) {
//        try {
//            final String filePath = file.getCanonicalPath();
//            for (String str : ILLEGAL_FILANAME_CHARACTERS) {
//                if (filePath.contains(str)) {
//                    return false;
//                }
//            }
//            return true;
//        } catch (IOException e) {
//            return false;
//        }
//    }
}
