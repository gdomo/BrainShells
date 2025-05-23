package com.example.gdomo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SampleImageExtractor {
    private static final int[] CARD_OFFSETS_X = new int[]{0, 72, 143, 215, 287};

    private static final Rectangle VALUE_AREA = new Rectangle(148, 590, 30, 25);
    private static final Rectangle SUIT_AREA = new Rectangle(170, 633, 30, 35);

    public static void main(String[] args) {
        final File inputFolder = new File(args[0]);

        final Map<Character, List<BufferedImage>> suitToImages = new HashMap<>();
        final Map<String, List<BufferedImage>> valueToImages = new HashMap<>();
        populateAreaSubimages(inputFolder, suitToImages, valueToImages);

        final Map<Character, BufferedImage> suitSampleImages = suitToImages.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> normalize(getMixedGrayscaleImage(e.getValue()))));

        final Map<String, BufferedImage> valueSampleImages = valueToImages.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> normalize(getMixedGrayscaleImage(e.getValue()))));

        File folder = new File("averageImages");
        //noinspection ResultOfMethodCallIgnored
        folder.mkdir();

        suitSampleImages.forEach((suit, image) -> writePngImage(image, folder, suit + ""));
        valueSampleImages.forEach((value, image) -> writePngImage(image, folder, value));
    }

    private static void populateAreaSubimages(File inputFolder, Map<Character, List<BufferedImage>> suitToImages, Map<String, List<BufferedImage>> valueToImages) {
        Arrays.stream(Optional.ofNullable(inputFolder.listFiles()).orElse(new File[0])).forEach(file -> {
            final BufferedImage image;
            try {
                image = ImageIO.read(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String cards = file.getName().substring(0, file.getName().length() - ".png".length());
            int cardNum = 0;
            while (!cards.isEmpty()) {
                final String value = cards.startsWith("10") ? "10" : cards.substring(0, 1);
                cards = cards.substring(value.length());

                final char suit = cards.charAt(0);
                cards = cards.substring(1);

                final BufferedImage valueSubimage = image.getSubimage(CARD_OFFSETS_X[cardNum] + VALUE_AREA.x, VALUE_AREA.y, VALUE_AREA.width, VALUE_AREA.height);
                final BufferedImage suitSubimage = image.getSubimage(CARD_OFFSETS_X[cardNum] + SUIT_AREA.x, SUIT_AREA.y, SUIT_AREA.width, SUIT_AREA.height);

                suitToImages.computeIfAbsent(suit, v -> new ArrayList<>()).add(suitSubimage);
                valueToImages.computeIfAbsent(value, v -> new ArrayList<>()).add(valueSubimage);

                cardNum++;
            }
        });
    }

    private static BufferedImage getMixedGrayscaleImage(List<BufferedImage> images) {
        final BufferedImage firstImage = images.stream().findFirst().orElseThrow();
        final int width = firstImage.getWidth();
        final int height = firstImage.getHeight();

        final BufferedImage averageImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                long sum = 0;
                for (BufferedImage image : images) {
                    sum += argbToGrey(image.getRGB(x, y));
                }
                final byte averageValue = (byte) (sum / images.size());
                averageImage.setRGB(x, y, averageValue | averageValue << 8 | averageValue << 16);
            }
        }


        return averageImage;
    }

    private static BufferedImage normalize(BufferedImage image) {
        final int[] argb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        final int max = Arrays.stream(argb).map(SampleImageExtractor::argbToGrey).max().orElseThrow();
        final int min = Arrays.stream(argb).map(SampleImageExtractor::argbToGrey).min().orElseThrow();

        final int avg = (max + min) / 2;

        for (int i = 0; i < argb.length; i++) {
            final int x = i % image.getWidth();
            final int y = i / image.getWidth();
            if (argbToGrey(argb[i]) < avg) {
                image.setRGB(x, y, 0xFF000000);
            } else {
                image.setRGB(x, y, 0xFFFFFFFF);
            }
        }

        return image;
    }

    private static int argbToGrey(int argb) {
        return (argb & 0x00FF0000 + argb & 0x0000FF00 + argb & 0x0000FF) / 3;
    }

    private static void writePngImage(BufferedImage image, File folder, String filename) {
        try {
            ImageIO.write(image, "PNG", new File(folder, filename + ".png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
