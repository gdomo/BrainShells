package com.example.gdomo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.AttributedString;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RenderedImageCardRecognizer {
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
                        IntStream.rangeClosed(2, 10).mapToObj(String::valueOf),
                        Stream.of("A", "J", "Q", "K")
                )
                .collect(Collectors.toMap(Function.identity(), s -> renderNormalized(s, VALUE_AREA.width, VALUE_AREA.height)));

        final Map<String, BufferedImage> suitsToSample = Map.of("c", "♣", "s", "♠", "d", "♦", "h", "♥")
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> renderNormalized(e.getValue(), SUIT_AREA.width, SUIT_AREA.height)));

        final File[] files = Optional.ofNullable(new File(folder).listFiles()).orElse(new File[0]);
        for (File file : files) {
            final BufferedImage image = ImageIO.read(file);
            final StringBuilder descriptionBuilder = new StringBuilder();
            for (int cardOffset : CARD_OFFSETS_X) {
                if (new Color(image.getRGB(cardOffset + BACKGROUND_PROBE.x, BACKGROUND_PROBE.y)).getBlue() < BACKGROUND_THRESHOLD) {
                    break;
                }

                final BufferedImage valueSubimage = toNormalized(image.getSubimage(VALUE_AREA.x + cardOffset, VALUE_AREA.y, VALUE_AREA.width, VALUE_AREA.height));
                final BufferedImage suitSubimage = toNormalized(image.getSubimage(SUIT_AREA.x + cardOffset, SUIT_AREA.y, SUIT_AREA.width, SUIT_AREA.height));

                descriptionBuilder.append(getKeyForClosest(valueSubimage, valuesToSample));
                descriptionBuilder.append(getKeyForClosest(suitSubimage, suitsToSample));
            }
            onRecognized.accept(file, descriptionBuilder.toString());
        }
    }

    private static BufferedImage renderNormalized(String text, int width, int height) {
        final BufferedImage renderedSample = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final Graphics graphics = renderedSample.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        final String fontName = System.getProperty("os.name").toUpperCase().contains("WIN") ? "MS PGothic" : "Tahoma";
        Font font = new Font(fontName, Font.PLAIN, (int) (height * 1.3)); // points to pixels
        font = font.deriveFont(Map.of(TextAttribute.TRACKING, -0.15));

        final AttributedString attributedText = new AttributedString(text);
        attributedText.addAttribute(TextAttribute.FONT, font);
        attributedText.addAttribute(TextAttribute.FOREGROUND, Color.BLACK);
        graphics.drawString(attributedText.getIterator(), 0, height);

        return toNormalized(renderedSample);
    }

    private static BufferedImage toNormalized(BufferedImage image) {
        final int[] argb = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        final int max = Arrays.stream(argb).map(RenderedImageCardRecognizer::argbToGray).max().orElseThrow();
        final int min = Arrays.stream(argb).map(RenderedImageCardRecognizer::argbToGray).min().orElseThrow();

        final int avg = (max + min) / 2;

        double left = image.getWidth() - 1;
        double right = 0;
        double top = image.getHeight() - 1;
        double bottom = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                // to binary colored
                // and store content boundaries
                final int rgb = image.getRGB(x, y);
                if (argbToGray(rgb) < avg) {
                    image.setRGB(x, y, 0xFF000000);

                    left = Math.min(x, left);
                    right = Math.max(x, right);
                    top = Math.min(y, top);
                    bottom = Math.max(y, bottom);
                } else {
                    image.setRGB(x, y, 0xFFFFFFFF);
                }
            }
        }

        final AffineTransform transform = new AffineTransform();
        transform.scale(image.getWidth() / (1 + right - left), image.getHeight() / (1 + bottom - top));
        transform.translate(-left, -top);
        final BufferedImage scaledImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        new AffineTransformOp(transform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(image, scaledImage);

        return scaledImage;
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
