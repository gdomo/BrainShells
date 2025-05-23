package com.example.gdomo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class KeyPixelCardRecognizer {
    private static final int[] CARD_OFFSETS_X = new int[]{0, 72, 143, 215, 287};

    private static final Point BACKGROUND_PROBE = new Point(153, 650);
    private static final Point VALUE_AREA_CORNER = new Point(148, 590);
    private static final Point SUIT_AREA_CORNER = new Point(170, 633);

    private static final int BLUE_THRESHOLD = 120;

    public static void main(String[] args) throws IOException {
        recognizeCards(args[0], ((file, cards) -> System.out.println(file.getName() + " - " + cards)));
    }

    public static void recognizeCards(String folder, BiConsumer<File, String> onRecognized) throws IOException {
        final LinkedHashMap<String, Point> suitDecisionSequence = new LinkedHashMap<>();
        suitDecisionSequence.put("h", new Point(4, 7));
        suitDecisionSequence.put("c", new Point(9, 4));
        suitDecisionSequence.put("s", new Point(0, 19));
        suitDecisionSequence.put("d", new Point(13, 2));

        final LinkedHashMap<String, Point> valueDecisionSequence = new LinkedHashMap<>();
        valueDecisionSequence.put("10", new Point(1, 3));
        valueDecisionSequence.put("Q", new Point(21, 5));
        valueDecisionSequence.put("A", new Point(1, 23));
        valueDecisionSequence.put("K", new Point(21, 2));
        valueDecisionSequence.put("4", new Point(10, 8));
        valueDecisionSequence.put("J", new Point(13, 18));
        valueDecisionSequence.put("3", new Point(13, 7));
        valueDecisionSequence.put("2", new Point(4, 23));
        valueDecisionSequence.put("7", new Point(9, 17));
        valueDecisionSequence.put("9", new Point(11, 14));
        valueDecisionSequence.put("8", new Point(16, 8));
        valueDecisionSequence.put("6", new Point(4, 14));
        valueDecisionSequence.put("5", new Point(6, 2));

        final File[] files = Optional.ofNullable(new File(folder).listFiles()).orElse(new File[0]);
        for (File file : files) {
            final BufferedImage image = ImageIO.read(file);
            final StringBuilder descriptionBuilder = new StringBuilder();
            for (int cardOffset : CARD_OFFSETS_X) {
                if (probeBlue(image, new Point(0, 0), cardOffset, BACKGROUND_PROBE)) {
                    break;
                }

                descriptionBuilder.append(applyDecisionSequence(image, valueDecisionSequence, VALUE_AREA_CORNER, cardOffset));
                descriptionBuilder.append(applyDecisionSequence(image, suitDecisionSequence, SUIT_AREA_CORNER, cardOffset));
            }
            onRecognized.accept(file, descriptionBuilder.toString());
        }
    }

    private static String applyDecisionSequence(BufferedImage image, HashMap<String, Point> decisionSequence, Point areaCorner, int cardOffset) {
        for (Map.Entry<String, Point> entry : decisionSequence.entrySet()) {
            if (probeBlue(image, areaCorner, cardOffset, entry.getValue())) {
                return entry.getKey();
            }
        }

        return "";
    }

    private static boolean probeBlue(BufferedImage image, Point areaCorner, int cardOffset, Point point) {
        return new Color(image.getRGB(areaCorner.x + cardOffset + point.x, areaCorner.y + point.y)).getBlue() < BLUE_THRESHOLD;
    }
}
