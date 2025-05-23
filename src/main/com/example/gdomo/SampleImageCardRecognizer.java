package com.example.gdomo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SampleImageCardRecognizer {
    private static final int[] CARD_OFFSETS_X = new int[]{0, 72, 143, 215, 287};

    private static final Point BACKGROUND_PROBE = new Point(153, 650);
    private static final int BACKGROUND_THRESHOLD = 120;

    private static final Rectangle VALUE_AREA = new Rectangle(148, 590, 30, 25);
    private static final Rectangle SUIT_AREA = new Rectangle(170, 633, 30, 35);

    public static void main(String[] args) throws IOException {
        recognizeCards(args[0], ((file, cards) -> System.out.println(file.getName() + " - " + cards)));
    }

    public static void recognizeCards(String folder, BiConsumer<File, String> onRecognized) throws IOException {
        final Map<String, BufferedImage> valuesToSample = Stream.concat(
                        Stream.iterate(2, i -> i + 1).limit(9).map(String::valueOf),
                        Stream.of("A", "J", "Q", "K")
                )
                .collect(Collectors.toMap(Function.identity(), s -> readFromClasspath(s + ".png")));

        final Map<String, BufferedImage> suitsToSample = Stream.of("c", "s", "d", "h")
                .collect(Collectors.toMap(Function.identity(), s -> readFromClasspath(s + ".png")));

        final File[] files = Optional.ofNullable(new File(folder).listFiles()).orElse(new File[0]);
        for (File file : files) {
            final BufferedImage image = ImageIO.read(file);
            final StringBuilder descriptionBuilder = new StringBuilder();
            for (int cardOffset : CARD_OFFSETS_X) {
                if (new Color(image.getRGB(cardOffset + BACKGROUND_PROBE.x, BACKGROUND_PROBE.y)).getBlue() < BACKGROUND_THRESHOLD) {
                    break;
                }

                final BufferedImage valueSubimage = toNormalizedGrayscale(image.getSubimage(VALUE_AREA.x + cardOffset, VALUE_AREA.y, VALUE_AREA.width, VALUE_AREA.height));
                final BufferedImage suitSubimage = toNormalizedGrayscale(image.getSubimage(SUIT_AREA.x + cardOffset, SUIT_AREA.y, SUIT_AREA.width, SUIT_AREA.height));

                descriptionBuilder.append(getKeyForClosest(valueSubimage, valuesToSample));
                descriptionBuilder.append(getKeyForClosest(suitSubimage, suitsToSample));
            }
            onRecognized.accept(file, descriptionBuilder.toString());
        }
    }

    private static BufferedImage readFromClasspath(String filename) {
        final InputStream imageResource = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(filename));
        try {
            return ImageIO.read(imageResource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BufferedImage toNormalizedGrayscale(BufferedImage image) {
        final int[] argb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        final int max = Arrays.stream(argb).map(SampleImageCardRecognizer::argbToGray).max().orElseThrow();
        final int min = Arrays.stream(argb).map(SampleImageCardRecognizer::argbToGray).min().orElseThrow();

        final int avg = (max + min) / 2;

        for (int i = 0; i < argb.length; i++) {
            final int x = i % image.getWidth();
            final int y = i / image.getWidth();
            if (argbToGray(argb[i]) < avg) {
                image.setRGB(x, y, 0xFF000000);
            } else {
                image.setRGB(x, y, 0xFFFFFFFF);
            }
        }

        return image;
    }

    private static int argbToGray(int argb) {
        return (argb & 0x00FF0000 + argb & 0x0000FF00 + argb & 0x0000FF) / 3;
    }

    private static String getKeyForClosest(BufferedImage grayscaleTarget, Map<String, BufferedImage> grayscaleCandidates) {
        return grayscaleCandidates.entrySet().stream()
                .min(Comparator.comparing(e -> getDistance(grayscaleTarget, e.getValue())))
                .orElseThrow().getKey();
    }

    private static long getDistance(BufferedImage aGrayscale, BufferedImage bGrayscale) {
        long distance = 0;
        for (int x = 0; x < aGrayscale.getWidth(); x++) {
            for (int y = 0; y < aGrayscale.getHeight(); y++) {
                distance += Math.abs((aGrayscale.getRGB(x, y) & 0xFF) - (bGrayscale.getRGB(x, y) & 0xFF));
            }
        }

        return distance;
    }
}
